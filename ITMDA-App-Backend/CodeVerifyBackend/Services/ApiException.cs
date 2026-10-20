namespace CodeVerifyBackend.Services;

// An error we WANT to show the app, with an HTTP status number:
// 400 = bad request, 404 = not found, 409 = conflict, 422 = can't process,
// 429 = too many requests, 502/503 = the AI had a problem.
public class ApiException : Exception
{
    public int Status { get; }

    public ApiException(int status, string message) : base(message)
    {
        Status = status;
    }
}

