using System.ComponentModel.DataAnnotations;

namespace UniversityManagementApi.Models;

public class Student
{
    public int StudentId { get; set; }

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

    [Required]
    public DateTime CreatedAt { get; set; }

    public ICollection<Grade> Grades { get; set; } = new List<Grade>();
}
