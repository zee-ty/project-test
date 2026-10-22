using System.Threading.RateLimiting;
using CodeVerifyBackend.Auth;
using CodeVerifyBackend.Data;
using CodeVerifyBackend.Endpoints;
using CodeVerifyBackend.Models;
using CodeVerifyBackend.Services;
using Microsoft.AspNetCore.RateLimiting;
using Google.GenAI;
using MySqlConnector;

var builder = WebApplication.CreateBuilder(args);

var options = builder.Configuration.GetSection("CodeVerify").Get<CodeVerifyOptions>()
              ?? new CodeVerifyOptions();
builder.Services.AddSingleton(options);

// this makes sure large items arent added:
builder.WebHost.ConfigureKestrel(k => k.Limits.MaxRequestBodySize = 50 * 1024);

// this helps prevent excessive usage in the API:
builder.Services.AddRateLimiter(o =>
{
    o.RejectionStatusCode = 429;
    o.GlobalLimiter = PartitionedRateLimiter.Create<HttpContext, string>(ctx =>
        RateLimitPartition.GetFixedWindowLimiter(
            ctx.Connection.RemoteIpAddress?.ToString() ?? "unknown",
            _ => new FixedWindowRateLimiterOptions
            {
                PermitLimit = 30,
                Window = TimeSpan.FromMinutes(1)
            }));
    o.OnRejected = async (context, token) =>
    {
        await context.HttpContext.Response.WriteAsJsonAsync(
            new { error = "Too many requests. Please wait a moment and try again." }, token);
    };
});

// Where sessions are saved: Part A's MySQL database, or temporary memory for testing.
if (options.UseInMemoryStore)
{
    builder.Services.AddSingleton<ISessionStore, InMemorySessionStore>();
}
else
{
    var connectionString = builder.Configuration.GetConnectionString("Default");
    if (string.IsNullOrWhiteSpace(connectionString))
    {
        Console.Error.WriteLine("Missing database connection string. Run: dotnet user-secrets set \"ConnectionStrings:Default\" \"your-connection-string\"");
        Console.Error.WriteLine("(Or set \"UseInMemoryStore\": true in appsettings.json to run without a database.)");
        Environment.Exit(1);
    }
    builder.Services.AddSingleton<ISessionStore>(new MySqlSessionStore(connectionString!));
}

// Which AI to use our made one or api:
if (options.UseFakeAi)
{
    builder.Services.AddSingleton<IAiService, FakeAiService>();
}
else
{
    var apiKey = builder.Configuration["Gemini:ApiKey"];
    if (string.IsNullOrWhiteSpace(apiKey))
    {
        Console.Error.WriteLine("Missing Gemini API key. Run: dotnet user-secrets set \"Gemini:ApiKey\" \"your-key\"");
        Console.Error.WriteLine("(Or set \"UseFakeAi\": true in appsettings.json to test without a key.)");
        Environment.Exit(1);
    }
    builder.Services.AddSingleton(new Client(apiKey: apiKey));
    builder.Services.AddSingleton<IAiService, GeminiService>();
}

var app = builder.Build();

app.UseApiErrorHandling();
app.UseRateLimiter();
app.UseAuthStub();

app.MapGet("/api/health", () => Results.Json(new { status = "ok" }));

// Part A's database check: opens the MySQL connection and asks which database it reached.
app.MapGet("/api/database-test", async (IConfiguration configuration) =>
{
    try
    {
        await using var connection = Database.CreateConnection(configuration);
        await connection.OpenAsync();

        await using var command = new MySqlCommand("SELECT DATABASE()", connection);
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

app.MapSessionEndpoints();
app.MapDashboardEndpoints();
app.MapProgressEndpoints();

app.MapFallback(() => Results.Json(new { error = "Not found." }, statusCode: 404));

app.Run();