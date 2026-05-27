using System.ComponentModel.DataAnnotations;

namespace UniversityManagementApi.DTOs;

public class CreateStudentDto
{
    [Required]
    [MaxLength(50)]
    public string FirstName { get; set; } = string.Empty;

    [Required]
    [MaxLength(50)]
    public string LastName { get; set; } = string.Empty;

    [Required]
    [EmailAddress]
    [MaxLength(100)]
    public string Email { get; set; } = string.Empty;

    [Required]
    [MaxLength(20)]
    public string GroupName { get; set; } = string.Empty;

    [Required]
    public DateOnly DateOfBirth { get; set; }
}
