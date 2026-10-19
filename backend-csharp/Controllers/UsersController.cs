using Microsoft.AspNetCore.Mvc;
using MySqlConnector;

namespace backend_csharp.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class UsersController : ControllerBase
    {
        private readonly IConfiguration _configuration;

        public UsersController(IConfiguration configuration)
        {
            _configuration = configuration;
        }

        [HttpGet]
        public async Task<IActionResult> GetUsers()
        {
            var connectionString =
                _configuration.GetConnectionString("Default");

            await using var connection =
                new MySqlConnection(connectionString);

            await connection.OpenAsync();

            const string sql = @"
                SELECT user_id, full_name, email, created_at
                FROM users
                ORDER BY user_id";

            await using var command = new MySqlCommand(sql, connection);
            await using var reader = await command.ExecuteReaderAsync();

            var users = new List<object>();

            while (await reader.ReadAsync())
            {
                users.Add(new
                {
                    user_id = reader.GetInt32("user_id"),
                    full_name = reader.GetString("full_name"),
                    email = reader.GetString("email"),
                    created_at = reader.GetDateTime("created_at")
                });
            }

            return Ok(users);
        }
    }
}
