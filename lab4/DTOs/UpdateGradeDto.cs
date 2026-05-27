using System.ComponentModel.DataAnnotations;

namespace UniversityManagementApi.DTOs;

public class UpdateGradeDto
{
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
}
