using MySqlConnector;
using backend_csharp.Data;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var app = builder.Build();

app.UseSwagger();
app.UseSwaggerUI();

app.MapControllers();

app.MapGet("/api/health", () => Results.Ok(new
{
    status = "API is running"
}));

app.MapGet("/api/database-test", async (IConfiguration configuration) =>
{
    try
    {
        await using var connection =
            Database.CreateConnection(configuration);

        await connection.OpenAsync();

        await using var command =
            new MySqlCommand("SELECT DATABASE()", connection);

        var databaseName = await command.ExecuteScalarAsync();

        return Results.Ok(new
        {
            status = "Database connection successful",
            database = databaseName
        });
    }
    catch (Exception ex)
    {
        return Results.Problem(
            title: "Database connection failed",
            detail: ex.Message
        );
    }
});

app.Run();
