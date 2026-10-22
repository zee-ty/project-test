
using System.Collections.Concurrent;
using System.Security.Cryptography;

namespace CodeVerifyBackend.Auth;

public static class AuthStub
{
    private static readonly ConcurrentDictionary<string, int> Tokens = new();

    public static string CreateToken(int userId)
    {
        var token = Convert.ToBase64String(
            RandomNumberGenerator.GetBytes(32));

        Tokens[token] = userId;
        return token;
    }

    public static void UseAuthStub(this WebApplication app)
    {
        app.Use(async (ctx, next) =>
        {
            var path = ctx.Request.Path.Value ?? "";

            // Registration and login are public.
            if (path.Equals("/api/auth/register",
                    StringComparison.OrdinalIgnoreCase) ||
                path.Equals("/api/auth/login",
                    StringComparison.OrdinalIgnoreCase) ||
                path.Equals("/api/health",
                    StringComparison.OrdinalIgnoreCase))
            {
                await next();
                return;
            }

            var header = ctx.Request.Headers.Authorization.ToString();

            if (header.StartsWith("Bearer ",
                    StringComparison.OrdinalIgnoreCase))
            {
                var token = header["Bearer ".Length..].Trim();

                if (Tokens.TryGetValue(token, out var userId))
                {
                    ctx.Items["userId"] = userId;
                    await next();
                    return;
                }
            }

            ctx.Response.StatusCode = StatusCodes.Status401Unauthorized;

            await ctx.Response.WriteAsJsonAsync(
                new { error = "Please log in to access this resource." });
        });
    }

    public static int GetUserId(this HttpContext ctx) =>
        ctx.Items["userId"] is int userId
            ? userId
            : throw new UnauthorizedAccessException(
                "A valid login is required.");

    public static bool RemoveToken(string token) =>
        Tokens.TryRemove(token, out _);
}
