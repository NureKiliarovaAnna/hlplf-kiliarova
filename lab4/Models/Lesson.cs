using System.ComponentModel.DataAnnotations;

namespace UniversityManagementApi.Models;

public class Lesson
{
    public int LessonId { get; set; }

    [Required]
    public int CourseId { get; set; }

    [Required]
    [MaxLength(150)]
    public string Topic { get; set; } = string.Empty;

    [Required]
    public DateTime LessonDate { get; set; }

    [Required]
    [MaxLength(30)]
    public string Room { get; set; } = string.Empty;

    public Course? Course { get; set; }
}
