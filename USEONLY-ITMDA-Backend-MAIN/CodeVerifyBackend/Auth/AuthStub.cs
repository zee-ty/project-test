
using System.Collections.Concurrent;
using System.Security.Cryptography;

namespace CodeVerifyBackend.Auth;

public static class AuthStub
{
    private static readonly ConcurrentDictionary<string, int> Tokens = new();

    public static string CreateToken(int userId)
    {
        string token = Convert.ToBase64String(
            RandomNumberGenerator.GetBytes(32));

        Tokens[token] = userId;
        return token;
    }

    public static bool RemoveToken(string token)
    {
        return Tokens.TryRemove(token, out _);
    }

    public static void UseAuthStub(this WebApplication app)
    {
        app.Use(async (context, next) =>
        {
            string path = context.Request.Path.Value ?? "";

            // Public endpoints.
            if (path.Equals("/api/auth/register",
                    StringComparison.OrdinalIgnoreCase) ||
                path.Equals("/api/auth/login",
                    StringComparison.OrdinalIgnoreCase) ||
                path.Equals("/api/health",
                    StringComparison.OrdinalIgnoreCase) ||
                path.Equals("/api/database-test",
                    StringComparison.OrdinalIgnoreCase))
            {
                await next();
                return;
            }

            string header =
                context.Request.Headers.Authorization.ToString();

            if (header.StartsWith(
                    "Bearer ", StringComparison.OrdinalIgnoreCase))
            {
                string token = header["Bearer ".Length..].Trim();

                if (Tokens.TryGetValue(token, out int userId))
                {
                    context.Items["userId"] = userId;
                    await next();
                    return;
                }
            }

            context.Response.StatusCode =
                StatusCodes.Status401Unauthorized;

            await context.Response.WriteAsJsonAsync(new
            {
                error = "Please log in to access this resource."
            });
        });
    }

    public static int GetUserId(this HttpContext context)
    {
        return context.Items["userId"] is int userId
            ? userId
            : throw new UnauthorizedAccessException(
                "A valid login is required.");
    }
}
