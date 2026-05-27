using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Caching.Memory;
using UniversityManagementApi.Data;
using UniversityManagementApi.DTOs;
using UniversityManagementApi.Models;
using UniversityManagementApi.Services;

namespace UniversityManagementApi.Controllers;

[ApiController]
[Route("api/[controller]")]
public class GradesController : ControllerBase
{
    private readonly UniversityDbContext _context;
    private readonly IMemoryCache _cache;

    public GradesController(UniversityDbContext context, IMemoryCache cache)
    {
        _context = context;
        _cache = cache;
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<GradeDto>>> GetGrades()
    {
        // Кешування списку оцінок на 5 хвилин.
        if (!_cache.TryGetValue(CacheKeys.GradesList, out List<GradeDto>? grades))
        {
            grades = await _context.Grades
                .AsNoTracking()
                .Include(grade => grade.Student)
                .Include(grade => grade.Course)
                .Select(grade => ToDto(grade))
                .ToListAsync();

            _cache.Set(CacheKeys.GradesList, grades, TimeSpan.FromMinutes(5));
        }

        return Ok(grades);
    }

    [HttpGet("{id:int}")]
    public async Task<ActionResult<GradeDto>> GetGrade(int id)
    {
        var grade = await _context.Grades
            .AsNoTracking()
            .Include(item => item.Student)
            .Include(item => item.Course)
            .FirstOrDefaultAsync(item => item.GradeId == id);

        if (grade is null)
        {
            return NotFound();
        }

        return Ok(ToDto(grade));
    }

    [HttpPost]
    public async Task<ActionResult<GradeDto>> CreateGrade(CreateGradeDto dto)
    {
        if (dto.GradeDate == default)
        {
            return BadRequest("GradeDate is required.");
        }

        // Перевірка існування пов'язаного студента.
        var studentExists = await _context.Students.AnyAsync(student => student.StudentId == dto.StudentId);

        if (!studentExists)
        {
            return BadRequest("StudentId does not exist.");
        }

        // Перевірка існування пов'язаного курсу.
        var courseExists = await _context.Courses.AnyAsync(course => course.CourseId == dto.CourseId);

        if (!courseExists)
        {
            return BadRequest("CourseId does not exist.");
        }

        var grade = new Grade
        {
            StudentId = dto.StudentId,
            CourseId = dto.CourseId,
            GradeValue = dto.GradeValue,
            GradeDate = dto.GradeDate,
            Comment = dto.Comment
        };

        _context.Grades.Add(grade);
        await _context.SaveChangesAsync();

        _cache.Remove(CacheKeys.GradesList);

        var createdGrade = await _context.Grades
            .AsNoTracking()
            .Include(item => item.Student)
            .Include(item => item.Course)
            .FirstAsync(item => item.GradeId == grade.GradeId);

        return CreatedAtAction(nameof(GetGrade), new { id = grade.GradeId }, ToDto(createdGrade));
    }

    [HttpPut("{id:int}")]
    public async Task<IActionResult> UpdateGrade(int id, UpdateGradeDto dto)
    {
        if (dto.GradeDate == default)
        {
            return BadRequest("GradeDate is required.");
        }

        // Транзакція: перевіряємо пов'язані записи та оновлюємо оцінку.
        await using var transaction = await _context.Database.BeginTransactionAsync();

        try
        {
            var grade = await _context.Grades.FindAsync(id);

            if (grade is null)
            {
                await transaction.RollbackAsync();
                return NotFound();
            }

            // Перевірка існування пов'язаного студента.
            var studentExists = await _context.Students.AnyAsync(student => student.StudentId == dto.StudentId);

            if (!studentExists)
            {
                await transaction.RollbackAsync();
                return BadRequest("StudentId does not exist.");
            }

            // Перевірка існування пов'язаного курсу.
            var courseExists = await _context.Courses.AnyAsync(course => course.CourseId == dto.CourseId);

            if (!courseExists)
            {
                await transaction.RollbackAsync();
                return BadRequest("CourseId does not exist.");
            }

            grade.StudentId = dto.StudentId;
            grade.CourseId = dto.CourseId;
            grade.GradeValue = dto.GradeValue;
            grade.GradeDate = dto.GradeDate;
            grade.Comment = dto.Comment;

            await _context.SaveChangesAsync();
            await transaction.CommitAsync();

            _cache.Remove(CacheKeys.GradesList);

            return NoContent();
        }
        catch
        {
            await transaction.RollbackAsync();
            return Problem("An error occurred while updating the grade.");
        }
    }

    [HttpDelete("{id:int}")]
    public async Task<IActionResult> DeleteGrade(int id)
    {
        var grade = await _context.Grades.FindAsync(id);

        if (grade is null)
        {
            return NotFound();
        }

        _context.Grades.Remove(grade);
        await _context.SaveChangesAsync();

        _cache.Remove(CacheKeys.GradesList);

        return NoContent();
    }

    private static GradeDto ToDto(Grade grade)
    {
        return new GradeDto
        {
            GradeId = grade.GradeId,
            StudentId = grade.StudentId,
            StudentFullName = grade.Student is null
                ? string.Empty
                : $"{grade.Student.FirstName} {grade.Student.LastName}",
            CourseId = grade.CourseId,
            CourseTitle = grade.Course?.Title ?? string.Empty,
            GradeValue = grade.GradeValue,
            GradeDate = grade.GradeDate,
            Comment = grade.Comment
        };
    }
}
