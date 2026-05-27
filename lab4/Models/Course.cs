using System.ComponentModel.DataAnnotations;

namespace UniversityManagementApi.Models;

public class Course
{
    public int CourseId { get; set; }

    [Required]
    [MaxLength(100)]
    public string Title { get; set; } = string.Empty;

    [MaxLength(500)]
    public string? Description { get; set; }

    [Required]
    [Range(1, 10)]
    public int Credits { get; set; }

    [Required]
    public int TeacherId { get; set; }

    [Required]
    public DateTime CreatedAt { get; set; }

    public Teacher? Teacher { get; set; }

    public ICollection<Lesson> Lessons { get; set; } = new List<Lesson>();

    public ICollection<Grade> Grades { get; set; } = new List<Grade>();
}
