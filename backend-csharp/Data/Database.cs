using MySqlConnector;

namespace backend_csharp.Data
{
    public static class Database
    {
        public static MySqlConnection CreateConnection(
            IConfiguration configuration)
        {
            string? connectionString =
                configuration.GetConnectionString("Default");

            if (string.IsNullOrWhiteSpace(connectionString))
            {
                throw new InvalidOperationException(
                    "The database connection string is missing.");
            }

            return new MySqlConnection(connectionString);
        }
    }
}
