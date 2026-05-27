using System.ComponentModel.DataAnnotations;

namespace UniversityManagementApi.DTOs;

public class CreateCourseDto
{
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
}
