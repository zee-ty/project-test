using System.Security.Cryptography;
using System.Text;
using CodeVerifyBackend.Auth;
using Microsoft.AspNetCore.Http;

namespace CodeVerifyBackend.Endpoints;

public static class AuthEndpoints
{
    private static readonly Dictionary<string, UserRecord> UsersByEmail =
        new(StringComparer.OrdinalIgnoreCase);

    private static readonly Dictionary<int, UserRecord> UsersById = new();

    private static readonly object LockObject = new();
    private static int NextUserId = 1;

    public static void MapAuthEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api/auth");

        group.MapPost("/register", (RegisterRequest request) =>
        {
            var name = request.FullName?.Trim();
            var email = request.Email?.Trim();
            var password = request.Password;

            if (string.IsNullOrWhiteSpace(name) ||
                string.IsNullOrWhiteSpace(email) ||
                string.IsNullOrWhiteSpace(password))
            {
                return Results.BadRequest(new
                {
                    error = "Full name, email and password are required."
                });
            }

            if (password.Length < 8)
            {
                return Results.BadRequest(new
                {
                    error = "Password must be at least 8 characters."
                });
            }

            if (!email.Contains('@'))
            {
                return Results.BadRequest(new
                {
                    error = "Enter a valid email address."
                });
            }

            lock (LockObject)
            {
                if (UsersByEmail.ContainsKey(email))
                {
                    return Results.Conflict(new
                    {
                        error = "An account with this email already exists."
                    });
                }

                var user = new UserRecord
                {
                    Id = NextUserId++,
                    FullName = name,
                    Email = email,
                    PasswordHash = HashPassword(password)
                };

                UsersByEmail[email] = user;
                UsersById[user.Id] = user;

                var token = AuthStub.CreateToken(user.Id);

                return Results.Created("/api/auth/profile", new
                {
                    user.Id,
                    user.FullName,
                    user.Email,
                    token
                });
            }
        });

        group.MapPost("/login", (LoginRequest request) =>
        {
            if (string.IsNullOrWhiteSpace(request.Email) ||
                string.IsNullOrEmpty(request.Password))
            {
                return Results.BadRequest(new
                {
                    error = "Email and password are required."
                });
            }

            lock (LockObject)
            {
                if (!UsersByEmail.TryGetValue(request.Email.Trim(), out var user) ||
                    !VerifyPassword(request.Password, user.PasswordHash))
                {
                    return Results.Json(
                        new { error = "Incorrect email or password." },
                        statusCode: StatusCodes.Status401Unauthorized);
                }

                var token = AuthStub.CreateToken(user.Id);

                return Results.Ok(new
                {
                    user.Id,
                    user.FullName,
                    user.Email,
                    token
                });
            }
        });

        group.MapGet("/profile", (HttpContext context) =>
        {
            var userId = context.GetUserId();

            lock (LockObject)
            {
                if (!UsersById.TryGetValue(userId, out var user))
                {
                    return Results.NotFound(new { error = "User not found." });
                }

                return Results.Ok(new
                {
                    user.Id,
                    user.FullName,
                    user.Email
                });
            }
        });

        group.MapPut("/profile", (HttpContext context, UpdateProfileRequest request) =>
        {
            var userId = context.GetUserId();

            lock (LockObject)
            {
                if (!UsersById.TryGetValue(userId, out var user))
                {
                    return Results.NotFound(new { error = "User not found." });
                }

                if (string.IsNullOrWhiteSpace(request.FullName))
                {
                    return Results.BadRequest(new
                    {
                        error = "Full name is required."
                    });
                }

                user.FullName = request.FullName.Trim();

                return Results.Ok(new
                {
                    user.Id,
                    user.FullName,
                    user.Email
                });
            }
        });

        group.MapPost("/change-password",
            (HttpContext context, ChangePasswordRequest request) =>
        {
            var userId = context.GetUserId();

            lock (LockObject)
            {
                if (!UsersById.TryGetValue(userId, out var user))
                {
                    return Results.NotFound(new { error = "User not found." });
                }

                if (!VerifyPassword(request.CurrentPassword, user.PasswordHash))
                {
                    return Results.Json(
                        new { error = "Current password is incorrect." },
                        statusCode: StatusCodes.Status401Unauthorized);
                }

                if (string.IsNullOrWhiteSpace(request.NewPassword) ||
                    request.NewPassword.Length < 8)
                {
                    return Results.BadRequest(new
                    {
                        error = "New password must be at least 8 characters."
                    });
                }

                user.PasswordHash = HashPassword(request.NewPassword);

                return Results.Ok(new
                {
                    message = "Password changed successfully."
                });
            }
        });

        group.MapPost("/logout", (HttpContext context) =>
        {
            var header = context.Request.Headers.Authorization.ToString();

            if (!header.StartsWith("Bearer ",
                    StringComparison.OrdinalIgnoreCase))
            {
                return Results.Unauthorized();
            }

            var token = header["Bearer ".Length..].Trim();

            AuthStub.RemoveToken(token);

            return Results.Ok(new { message = "Logged out successfully." });
        });
    }

    private static string HashPassword(string password)
    {
        byte[] salt = RandomNumberGenerator.GetBytes(16);

        byte[] hash = Rfc2898DeriveBytes.Pbkdf2(
            password,
            salt,
            100_000,
            HashAlgorithmName.SHA256,
            32);

        return $"{Convert.ToBase64String(salt)}:{Convert.ToBase64String(hash)}";
    }

    private static bool VerifyPassword(string password, string storedHash)
    {
        try
        {
            var parts = storedHash.Split(':');

            if (parts.Length != 2)
                return false;

            byte[] salt = Convert.FromBase64String(parts[0]);
            byte[] expectedHash = Convert.FromBase64String(parts[1]);

            byte[] actualHash = Rfc2898DeriveBytes.Pbkdf2(
                password,
                salt,
                100_000,
                HashAlgorithmName.SHA256,
                expectedHash.Length);

            return CryptographicOperations.FixedTimeEquals(
                actualHash, expectedHash);
        }
        catch (FormatException)
        {
            return false;
        }
    }

    private sealed class UserRecord
    {
        public int Id { get; init; }
        public required string FullName { get; set; }
        public required string Email { get; init; }
        public required string PasswordHash { get; set; }
    }

    public sealed record RegisterRequest(
        string FullName, string Email, string Password);

    public sealed record LoginRequest(
        string Email, string Password);

    public sealed record UpdateProfileRequest(
        string FullName);

    public sealed record ChangePasswordRequest(
        string CurrentPassword, string NewPassword);
}
