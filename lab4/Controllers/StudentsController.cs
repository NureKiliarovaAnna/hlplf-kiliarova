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
public class StudentsController : ControllerBase
{
    private readonly UniversityDbContext _context;
    private readonly IMemoryCache _cache;

    public StudentsController(UniversityDbContext context, IMemoryCache cache)
    {
        _context = context;
        _cache = cache;
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<StudentDto>>> GetStudents()
    {
        var students = await _context.Students
            .AsNoTracking()
            .Select(student => ToDto(student))
            .ToListAsync();

        return Ok(students);
    }

    [HttpGet("{id:int}")]
    public async Task<ActionResult<StudentDto>> GetStudent(int id)
    {
        var student = await _context.Students.AsNoTracking().FirstOrDefaultAsync(item => item.StudentId == id);

        if (student is null)
        {
            return NotFound();
        }

        return Ok(ToDto(student));
    }

    [HttpPost]
    public async Task<ActionResult<StudentDto>> CreateStudent(CreateStudentDto dto)
    {
        if (dto.DateOfBirth == default)
        {
            return BadRequest("DateOfBirth is required.");
        }

        var student = new Student
        {
            FirstName = dto.FirstName,
            LastName = dto.LastName,
            Email = dto.Email,
            GroupName = dto.GroupName,
            DateOfBirth = dto.DateOfBirth,
            CreatedAt = DateTime.UtcNow
        };

        _context.Students.Add(student);
        await _context.SaveChangesAsync();

        return CreatedAtAction(nameof(GetStudent), new { id = student.StudentId }, ToDto(student));
    }

    [HttpPut("{id:int}")]
    public async Task<IActionResult> UpdateStudent(int id, UpdateStudentDto dto)
    {
        if (dto.DateOfBirth == default)
        {
            return BadRequest("DateOfBirth is required.");
        }

        var student = await _context.Students.FindAsync(id);

        if (student is null)
        {
            return NotFound();
        }

        student.FirstName = dto.FirstName;
        student.LastName = dto.LastName;
        student.Email = dto.Email;
        student.GroupName = dto.GroupName;
        student.DateOfBirth = dto.DateOfBirth;

        await _context.SaveChangesAsync();

        return NoContent();
    }

    [HttpDelete("{id:int}")]
    public async Task<IActionResult> DeleteStudent(int id)
    {
        // Транзакція: спочатку видаляємо пов'язані оцінки, потім студента.
        await using var transaction = await _context.Database.BeginTransactionAsync();

        try
        {
            var student = await _context.Students.FindAsync(id);

            if (student is null)
            {
                await transaction.RollbackAsync();
                return NotFound();
            }

            var grades = await _context.Grades.Where(grade => grade.StudentId == id).ToListAsync();
            _context.Grades.RemoveRange(grades);
            _context.Students.Remove(student);

            await _context.SaveChangesAsync();
            await transaction.CommitAsync();

            _cache.Remove(CacheKeys.GradesList);

            return NoContent();
        }
        catch
        {
            await transaction.RollbackAsync();
            return Problem("An error occurred while deleting the student.");
        }
    }

    private static StudentDto ToDto(Student student)
    {
        return new StudentDto
        {
            StudentId = student.StudentId,
            FirstName = student.FirstName,
            LastName = student.LastName,
            Email = student.Email,
            GroupName = student.GroupName,
            DateOfBirth = student.DateOfBirth,
            CreatedAt = student.CreatedAt
        };
    }
}
