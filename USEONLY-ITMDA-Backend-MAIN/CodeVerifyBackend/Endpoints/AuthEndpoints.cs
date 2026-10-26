
using System.Security.Cryptography;
using CodeVerifyBackend.Auth;
using CodeVerifyBackend.Data;
using Microsoft.AspNetCore.Http;
using MySqlConnector;

namespace CodeVerifyBackend.Endpoints;

public static class AuthEndpoints
{
    public static void MapAuthEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api/auth");

        // REGISTER
        group.MapPost("/register", async (
            RegisterRequest request,
            IConfiguration configuration) =>
        {
            string name = request.FullName?.Trim() ?? "";
            string email = request.Email?.Trim() ?? "";
            string password = request.Password ?? "";

            if (string.IsNullOrWhiteSpace(name) ||
                string.IsNullOrWhiteSpace(email) ||
                string.IsNullOrWhiteSpace(password))
            {
                return Results.BadRequest(new
                {
                    error = "Full name, email and password are required."
                });
            }

            if (name.Length > 100 || email.Length > 150)
            {
                return Results.BadRequest(new
                {
                    error = "Name or email is too long."
                });
            }

            if (!email.Contains('@'))
            {
                return Results.BadRequest(new
                {
                    error = "Enter a valid email address."
                });
            }

            if (password.Length < 8)
            {
                return Results.BadRequest(new
                {
                    error = "Password must contain at least 8 characters."
                });
            }

            try
            {
                await using var connection =
                    Database.CreateConnection(configuration);

                await connection.OpenAsync();

                await using var command = new MySqlCommand(@"
                    INSERT INTO users
                        (full_name, email, password_hash)
                    VALUES
                        (@name, @email, @passwordHash)",
                    connection);

                command.Parameters.AddWithValue("@name", name);
                command.Parameters.AddWithValue("@email", email);
                command.Parameters.AddWithValue(
                    "@passwordHash", HashPassword(password));

                await command.ExecuteNonQueryAsync();

                int userId = checked((int)command.LastInsertedId);
                string token = AuthStub.CreateToken(userId);

                return Results.Created("/api/auth/profile", new
                {
                    id = userId,
                    fullName = name,
                    email,
                    token
                });
            }
            catch (MySqlException ex) when (ex.Number == 1062)
            {
                return Results.Conflict(new
                {
                    error = "An account with this email already exists."
                });
            }
        });

        // LOGIN
        group.MapPost("/login", async (
            LoginRequest request,
            IConfiguration configuration) =>
        {
            string emailInput = request.Email?.Trim() ?? "";
            string password = request.Password ?? "";

            if (string.IsNullOrWhiteSpace(emailInput) ||
                string.IsNullOrEmpty(password))
            {
                return Results.BadRequest(new
                {
                    error = "Email and password are required."
                });
            }

            await using var connection =
                Database.CreateConnection(configuration);

            await connection.OpenAsync();

            await using var command = new MySqlCommand(@"
                SELECT user_id, full_name, email, password_hash
                FROM users
                WHERE email = @email
                LIMIT 1", connection);

            command.Parameters.AddWithValue("@email", emailInput);

            await using var reader = await command.ExecuteReaderAsync();

            if (!await reader.ReadAsync())
            {
                return Results.Json(
                    new { error = "Incorrect email or password." },
                    statusCode: StatusCodes.Status401Unauthorized);
            }

            int userId = reader.GetInt32("user_id");
            string fullName = reader.GetString("full_name");
            string email = reader.GetString("email");
            string storedHash = reader.GetString("password_hash");

            await reader.CloseAsync();

            if (!VerifyPassword(password, storedHash))
            {
                return Results.Json(
                    new { error = "Incorrect email or password." },
                    statusCode: StatusCodes.Status401Unauthorized);
            }

            string token = AuthStub.CreateToken(userId);

            return Results.Ok(new
            {
                id = userId,
                fullName,
                email,
                token
            });
        });

        // GET PROFILE
        group.MapGet("/profile", async (
            HttpContext context,
            IConfiguration configuration) =>
        {
            int userId = context.GetUserId();

            await using var connection =
                Database.CreateConnection(configuration);

            await connection.OpenAsync();

            await using var command = new MySqlCommand(@"
                SELECT user_id, full_name, email
                FROM users
                WHERE user_id = @userId
                LIMIT 1", connection);

            command.Parameters.AddWithValue("@userId", userId);

            await using var reader = await command.ExecuteReaderAsync();

            if (!await reader.ReadAsync())
            {
                return Results.NotFound(new
                {
                    error = "User not found."
                });
            }

            return Results.Ok(new
            {
                id = reader.GetInt32("user_id"),
                fullName = reader.GetString("full_name"),
                email = reader.GetString("email")
            });
        });

        // UPDATE PROFILE NAME
        group.MapPut("/profile", async (
            HttpContext context,
            UpdateProfileRequest request,
            IConfiguration configuration) =>
        {
            string name = request.FullName?.Trim() ?? "";

            if (string.IsNullOrWhiteSpace(name) || name.Length > 100)
            {
                return Results.BadRequest(new
                {
                    error = "Name must contain between 1 and 100 characters."
                });
            }

            int userId = context.GetUserId();

            await using var connection =
                Database.CreateConnection(configuration);

            await connection.OpenAsync();

            await using var command = new MySqlCommand(@"
                UPDATE users
                SET full_name = @name
                WHERE user_id = @userId", connection);

            command.Parameters.AddWithValue("@name", name);
            command.Parameters.AddWithValue("@userId", userId);

            await command.ExecuteNonQueryAsync();

            await using var profile = new MySqlCommand(@"
                SELECT user_id, full_name, email
                FROM users
                WHERE user_id = @userId
                LIMIT 1", connection);

            profile.Parameters.AddWithValue("@userId", userId);

            await using var reader = await profile.ExecuteReaderAsync();

            if (!await reader.ReadAsync())
            {
                return Results.NotFound(new
                {
                    error = "User not found."
                });
            }

            return Results.Ok(new
            {
                id = reader.GetInt32("user_id"),
                fullName = reader.GetString("full_name"),
                email = reader.GetString("email")
            });
        });

        // CHANGE PASSWORD
        group.MapPost("/change-password", async (
            HttpContext context,
            ChangePasswordRequest request,
            IConfiguration configuration) =>
        {
            string currentPassword = request.CurrentPassword ?? "";
            string newPassword = request.NewPassword ?? "";

            if (newPassword.Length < 8)
            {
                return Results.BadRequest(new
                {
                    error = "New password must contain at least 8 characters."
                });
            }

            int userId = context.GetUserId();

            await using var connection =
                Database.CreateConnection(configuration);

            await connection.OpenAsync();

            await using var find = new MySqlCommand(@"
                SELECT password_hash
                FROM users
                WHERE user_id = @userId
                LIMIT 1", connection);

            find.Parameters.AddWithValue("@userId", userId);

            object? result = await find.ExecuteScalarAsync();

            if (result == null || result == DBNull.Value)
            {
                return Results.NotFound(new
                {
                    error = "User not found."
                });
            }

            string storedHash = Convert.ToString(result)!;

            if (!VerifyPassword(currentPassword, storedHash))
            {
                return Results.Json(
                    new { error = "Current password is incorrect." },
                    statusCode: StatusCodes.Status401Unauthorized);
            }

            await using var update = new MySqlCommand(@"
                UPDATE users
                SET password_hash = @hash
                WHERE user_id = @userId", connection);

            update.Parameters.AddWithValue(
                "@hash", HashPassword(newPassword));
            update.Parameters.AddWithValue("@userId", userId);

            await update.ExecuteNonQueryAsync();

            return Results.Ok(new
            {
                message = "Password changed successfully."
            });
        });

        // LOGOUT
        group.MapPost("/logout", (HttpContext context) =>
        {
            string header =
                context.Request.Headers.Authorization.ToString();

            if (!header.StartsWith(
                    "Bearer ", StringComparison.OrdinalIgnoreCase))
            {
                return Results.Unauthorized();
            }

            string token = header["Bearer ".Length..].Trim();

            AuthStub.RemoveToken(token);

            return Results.Ok(new
            {
                message = "Logged out successfully."
            });
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

        return Convert.ToBase64String(salt) + ":" +
               Convert.ToBase64String(hash);
    }

    private static bool VerifyPassword(
        string password, string storedHash)
    {
        try
        {
            string[] parts = storedHash.Split(':');

            if (parts.Length != 2)
                return false;

            byte[] salt = Convert.FromBase64String(parts[0]);
            byte[] expected = Convert.FromBase64String(parts[1]);

            byte[] actual = Rfc2898DeriveBytes.Pbkdf2(
                password,
                salt,
                100_000,
                HashAlgorithmName.SHA256,
                expected.Length);

            return CryptographicOperations.FixedTimeEquals(
                actual, expected);
        }
        catch (FormatException)
        {
            return false;
        }
    }

    public sealed record RegisterRequest(
        string FullName, string Email, string Password);

    public sealed record LoginRequest(
        string Email, string Password);

    public sealed record UpdateProfileRequest(string FullName);

    public sealed record ChangePasswordRequest(
        string CurrentPassword, string NewPassword);
}
