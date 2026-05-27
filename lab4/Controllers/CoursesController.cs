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
public class CoursesController : ControllerBase
{
    private readonly UniversityDbContext _context;
    private readonly IMemoryCache _cache;

    public CoursesController(UniversityDbContext context, IMemoryCache cache)
    {
        _context = context;
        _cache = cache;
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<CourseDto>>> GetCourses()
    {
        // Кешування списку курсів на 5 хвилин.
        if (!_cache.TryGetValue(CacheKeys.CoursesList, out List<CourseDto>? courses))
        {
            courses = await _context.Courses
                .AsNoTracking()
                .Include(course => course.Teacher)
                .Select(course => ToDto(course))
                .ToListAsync();

            _cache.Set(CacheKeys.CoursesList, courses, TimeSpan.FromMinutes(5));
        }

        return Ok(courses);
    }

    [HttpGet("{id:int}")]
    public async Task<ActionResult<CourseDto>> GetCourse(int id)
    {
        var course = await _context.Courses
            .AsNoTracking()
            .Include(item => item.Teacher)
            .FirstOrDefaultAsync(item => item.CourseId == id);

        if (course is null)
        {
            return NotFound();
        }

        return Ok(ToDto(course));
    }

    [HttpPost]
    public async Task<ActionResult<CourseDto>> CreateCourse(CreateCourseDto dto)
    {
        // Перевірка існування пов'язаного викладача.
        var teacherExists = await _context.Teachers.AnyAsync(teacher => teacher.TeacherId == dto.TeacherId);

        if (!teacherExists)
        {
            return BadRequest("TeacherId does not exist.");
        }

        var course = new Course
        {
            Title = dto.Title,
            Description = dto.Description,
            Credits = dto.Credits,
            TeacherId = dto.TeacherId,
            CreatedAt = DateTime.UtcNow
        };

        _context.Courses.Add(course);
        await _context.SaveChangesAsync();

        _cache.Remove(CacheKeys.CoursesList);

        var createdCourse = await _context.Courses
            .AsNoTracking()
            .Include(item => item.Teacher)
            .FirstAsync(item => item.CourseId == course.CourseId);

        return CreatedAtAction(nameof(GetCourse), new { id = course.CourseId }, ToDto(createdCourse));
    }

    [HttpPut("{id:int}")]
    public async Task<IActionResult> UpdateCourse(int id, UpdateCourseDto dto)
    {
        var course = await _context.Courses.FindAsync(id);

        if (course is null)
        {
            return NotFound();
        }

        // Перевірка існування пов'язаного викладача.
        var teacherExists = await _context.Teachers.AnyAsync(teacher => teacher.TeacherId == dto.TeacherId);

        if (!teacherExists)
        {
            return BadRequest("TeacherId does not exist.");
        }

        course.Title = dto.Title;
        course.Description = dto.Description;
        course.Credits = dto.Credits;
        course.TeacherId = dto.TeacherId;

        await _context.SaveChangesAsync();

        _cache.Remove(CacheKeys.CoursesList);

        return NoContent();
    }

    [HttpDelete("{id:int}")]
    public async Task<IActionResult> DeleteCourse(int id)
    {
        // Транзакція: оцінки курсу, заняття курсу, потім сам курс.
        await using var transaction = await _context.Database.BeginTransactionAsync();

        try
        {
            var course = await _context.Courses.FindAsync(id);

            if (course is null)
            {
                await transaction.RollbackAsync();
                return NotFound();
            }

            var grades = await _context.Grades.Where(grade => grade.CourseId == id).ToListAsync();
            var lessons = await _context.Lessons.Where(lesson => lesson.CourseId == id).ToListAsync();

            _context.Grades.RemoveRange(grades);
            _context.Lessons.RemoveRange(lessons);
            _context.Courses.Remove(course);

            await _context.SaveChangesAsync();
            await transaction.CommitAsync();

            _cache.Remove(CacheKeys.CoursesList);
            _cache.Remove(CacheKeys.GradesList);

            return NoContent();
        }
        catch
        {
            await transaction.RollbackAsync();
            return Problem("An error occurred while deleting the course.");
        }
    }

    private static CourseDto ToDto(Course course)
    {
        return new CourseDto
        {
            CourseId = course.CourseId,
            Title = course.Title,
            Description = course.Description,
            Credits = course.Credits,
            TeacherId = course.TeacherId,
            TeacherFullName = course.Teacher is null
                ? string.Empty
                : $"{course.Teacher.FirstName} {course.Teacher.LastName}",
            CreatedAt = course.CreatedAt
        };
    }
}
