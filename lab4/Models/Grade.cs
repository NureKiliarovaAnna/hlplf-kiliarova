using System.ComponentModel.DataAnnotations;

namespace UniversityManagementApi.Models;

public class Grade
{
    public int GradeId { get; set; }

    [Required]
    public int StudentId { get; set; }

    [Required]
    public int CourseId { get; set; }

    [Required]
    [Range(0, 100)]
    public int GradeValue { get; set; }

    [Required]
    public DateTime GradeDate { get; set; }

    [MaxLength(300)]
    public string? Comment { get; set; }

    public Student? Student { get; set; }

    public Course? Course { get; set; }
}
