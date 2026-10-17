namespace CodeVerifyBackend.Auth;

// TEMPORARY placeholder. Part B (login) replaces this file.
// Their version must make GetUserId() return the id of the really
// logged-in user (read from their login token).
public static class AuthStub
{
    public static void UseAuthStub(this WebApplication app)
    {
        app.Use(async (ctx, next) =>
        {
            // Everyone is "user 1". To test other-user cases, send a header
            // called x-test-user with another number.
            var header = ctx.Request.Headers["x-test-user"].ToString();
            ctx.Items["userId"] = int.TryParse(header, out var id) ? id : 1;
            await next();
        });
    }

    // The endpoints call this to find out WHO is asking.
    public static int GetUserId(this HttpContext ctx) => (int)ctx.Items["userId"]!;
}