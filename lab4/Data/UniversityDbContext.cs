using Microsoft.EntityFrameworkCore;
using UniversityManagementApi.Models;

namespace UniversityManagementApi.Data;

public class UniversityDbContext : DbContext
{
    public UniversityDbContext(DbContextOptions<UniversityDbContext> options)
        : base(options)
    {
    }

    public DbSet<Student> Students => Set<Student>();
    public DbSet<Teacher> Teachers => Set<Teacher>();
    public DbSet<Course> Courses => Set<Course>();
    public DbSet<Lesson> Lessons => Set<Lesson>();
    public DbSet<Grade> Grades => Set<Grade>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<Student>(entity =>
        {
            entity.ToTable("Students");
            entity.HasKey(student => student.StudentId);
            entity.Property(student => student.FirstName).IsRequired().HasMaxLength(50);
            entity.Property(student => student.LastName).IsRequired().HasMaxLength(50);
            entity.Property(student => student.Email).IsRequired().HasMaxLength(100);
            entity.Property(student => student.GroupName).IsRequired().HasMaxLength(20);
            entity.Property(student => student.DateOfBirth).IsRequired().HasColumnType("date");
            entity.Property(student => student.CreatedAt).IsRequired();
            entity.HasIndex(student => student.Email).IsUnique();
        });

        modelBuilder.Entity<Teacher>(entity =>
        {
            entity.ToTable("Teachers");
            entity.HasKey(teacher => teacher.TeacherId);
            entity.Property(teacher => teacher.FirstName).IsRequired().HasMaxLength(50);
            entity.Property(teacher => teacher.LastName).IsRequired().HasMaxLength(50);
            entity.Property(teacher => teacher.Email).IsRequired().HasMaxLength(100);
            entity.Property(teacher => teacher.Department).IsRequired().HasMaxLength(100);
            entity.Property(teacher => teacher.CreatedAt).IsRequired();
            entity.HasIndex(teacher => teacher.Email).IsUnique();
        });

        modelBuilder.Entity<Course>(entity =>
        {
            entity.ToTable("Courses", table =>
                table.HasCheckConstraint("CK_Courses_Credits", "[Credits] BETWEEN 1 AND 10"));
            entity.HasKey(course => course.CourseId);
            entity.Property(course => course.Title).IsRequired().HasMaxLength(100);
            entity.Property(course => course.Description).HasMaxLength(500);
            entity.Property(course => course.Credits).IsRequired();
            entity.Property(course => course.CreatedAt).IsRequired();

            entity.HasOne(course => course.Teacher)
                .WithMany(teacher => teacher.Courses)
                .HasForeignKey(course => course.TeacherId)
                .OnDelete(DeleteBehavior.Restrict);
        });

        modelBuilder.Entity<Lesson>(entity =>
        {
            entity.ToTable("Lessons");
            entity.HasKey(lesson => lesson.LessonId);
            entity.Property(lesson => lesson.Topic).IsRequired().HasMaxLength(150);
            entity.Property(lesson => lesson.LessonDate).IsRequired();
            entity.Property(lesson => lesson.Room).IsRequired().HasMaxLength(30);

            entity.HasOne(lesson => lesson.Course)
                .WithMany(course => course.Lessons)
                .HasForeignKey(lesson => lesson.CourseId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        modelBuilder.Entity<Grade>(entity =>
        {
            entity.ToTable("Grades", table =>
                table.HasCheckConstraint("CK_Grades_GradeValue", "[GradeValue] BETWEEN 0 AND 100"));
            entity.HasKey(grade => grade.GradeId);
            entity.Property(grade => grade.GradeValue).IsRequired();
            entity.Property(grade => grade.GradeDate).IsRequired();
            entity.Property(grade => grade.Comment).HasMaxLength(300);

            entity.HasOne(grade => grade.Student)
                .WithMany(student => student.Grades)
                .HasForeignKey(grade => grade.StudentId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(grade => grade.Course)
                .WithMany(course => course.Grades)
                .HasForeignKey(grade => grade.CourseId)
                .OnDelete(DeleteBehavior.Cascade);
        });
    }
}
