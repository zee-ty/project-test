using CodeVerifyBackend.Models;
using CodeVerifyBackend.Services;
using CodeVerifyBackend.Auth;

namespace CodeVerifyBackend.Endpoints;

public static class DashboardEndpoints
{
    public static IEndpointRouteBuilder MapDashboardEndpoints(
        this IEndpointRouteBuilder app)
    {
        app.MapGet("/api/dashboard", async (
            HttpContext context,
            ISessionStore store) =>
        {
            var userId = context.GetUserId();
            var sessions = await store.GetSessionsByUserAsync(userId);

            var completed = sessions
                .Where(s => s.FinishedAt != null && s.OverallScore != null)
                .ToList();

            var averageScore = completed.Count == 0
                ? 0.0
                : Math.Round(
                    completed.Average(s => (double)s.OverallScore!.Value), 2);

            var questionsAnswered = sessions
                .SelectMany(s => s.Answers)
                .Count(a => a.UserId == userId);

            var favouriteLanguage = sessions
                .Where(s => !string.IsNullOrWhiteSpace(s.Language))
                .GroupBy(s => s.Language.Trim(),
                    StringComparer.OrdinalIgnoreCase)
                .OrderByDescending(g => g.Count())
                .ThenBy(g => g.Key)
                .Select(g => g.Key)
                .FirstOrDefault() ?? "N/A";

            var recentSessions = sessions
                .OrderByDescending(s => s.SubmittedAt)
                .Take(5)
                .Select(s => new RecentSessionDto
                {
                    SessionId = s.Id,
                    Language = s.Language,
                    Topic = s.Topic,
                    Score = s.OverallScore,
                    SubmittedAt = s.SubmittedAt
                })
                .ToList();

            return Results.Ok(new DashboardResponse
            {
                AverageScore = averageScore,
                SessionsCompleted = completed.Count,
                QuestionsAnswered = questionsAnswered,
                FavouriteLanguage = favouriteLanguage,
                RecentSessions = recentSessions
            });
        });

        return app;
    }
}

