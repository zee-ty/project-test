using CodeVerifyBackend.Services;

namespace CodeVerifyBackend.Endpoints;

// Catches every error and turns it into a simple { "error": "message" } reply.
public static class ErrorHandling
{
    public static void UseApiErrorHandling(this WebApplication app)
    {
        app.Use(async (ctx, next) =>
        {
            try
            {
                await next();
            }
            catch (ApiException ex) 
            {
                await WriteError(ctx, ex.Status, ex.Message);
            }
            catch (BadHttpRequestException ex) 
            {
                var message = ex.StatusCode == 413
                    ? "The request is too large."
                    : "The request could not be read.";
                await WriteError(ctx, ex.StatusCode, message);
            }
            catch (Exception ex)
            {
                app.Logger.LogError(ex, "Unhandled error");
                await WriteError(ctx, 500, "Something went wrong on the server.");
            }
        });
    }

    private static async Task WriteError(HttpContext ctx, int status, string message)
    {
        if (ctx.Response.HasStarted) return;
        ctx.Response.Clear();
        ctx.Response.StatusCode = status;
        await ctx.Response.WriteAsJsonAsync(new { error = message });
    }
}