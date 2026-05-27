using Microsoft.EntityFrameworkCore;
using UniversityManagementApi.Data;

var builder = WebApplication.CreateBuilder(args);

// Підключення БД SQL Server через Entity Framework Core.
builder.Services.AddDbContext<UniversityDbContext>(options =>
    options.UseSqlServer(builder.Configuration.GetConnectionString("DefaultConnection")));

// Кешування списків для GET-ендпоінтів.
builder.Services.AddMemoryCache();
builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseAuthorization();

app.MapControllers();

app.Run();
