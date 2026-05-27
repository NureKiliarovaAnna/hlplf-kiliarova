using System.ComponentModel.DataAnnotations;

namespace UniversityManagementApi.Models;

public class Teacher
{
    public int TeacherId { get; set; }

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
    [MaxLength(100)]
    public string Department { get; set; } = string.Empty;

    [Required]
    public DateTime CreatedAt { get; set; }

    public ICollection<Course> Courses { get; set; } = new List<Course>();
}
