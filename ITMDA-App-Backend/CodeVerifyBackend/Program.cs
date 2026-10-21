using System.Threading.RateLimiting;
using CodeVerifyBackend.Auth;
using CodeVerifyBackend.Endpoints;
using CodeVerifyBackend.Models;
using CodeVerifyBackend.Services;
using Microsoft.AspNetCore.RateLimiting;
using Google.GenAI;

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

//(Part A) changes THIS line to the database version.
builder.Services.AddSingleton<ISessionStore, InMemorySessionStore>();

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
app.MapSessionEndpoints();
app.MapDashBoardEndpoints();
app.MapProgressEndpoints();

app.MapFallback(() => Results.Json(new { error = "Not found." }, statusCode: 404));

app.Run();