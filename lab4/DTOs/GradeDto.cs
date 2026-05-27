namespace UniversityManagementApi.DTOs;

public class GradeDto
{
    public int GradeId { get; set; }
    public int StudentId { get; set; }
    public string StudentFullName { get; set; } = string.Empty;
    public int CourseId { get; set; }
    public string CourseTitle { get; set; } = string.Empty;
    public int GradeValue { get; set; }
    public DateTime GradeDate { get; set; }
    public string? Comment { get; set; }
}
