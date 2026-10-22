using MySqlConnector;

namespace CodeVerifyBackend.Data;

// Part A's helper: opens a connection to the MySQL database using the
// connection string called "Default" (ConnectionStrings:Default).
public static class Database
{
    public static MySqlConnection CreateConnection(IConfiguration configuration)
    {
        string? connectionString = configuration.GetConnectionString("Default");

        if (string.IsNullOrWhiteSpace(connectionString))
        {
            throw new InvalidOperationException("The database connection string is missing.");
        }

        return new MySqlConnection(connectionString);
    }
}