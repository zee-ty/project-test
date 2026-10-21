
using CodeVerifyBackend.Models;
using CodeVerifyBackend.Services;

namespace CodeVerifyBackend.Endpoints;

public static class DashboardEndpoints
{
    public static void MapDashboardEndpoints(this WebApplication app)
    {
        app.MapGet("/api/dashboard", async (
            HttpContext ctx,
            ISessionStore store) =>
        {
            var userId = ctx.GetUserId();

            var sessions = await store.GetSessionsByUserAsync(userId);

            var completedSessions = sessions
                .Where(s => s.FinishedAt.HasValue &&
                            s.OverallScore.HasValue)
                .ToList();

            double? averageScore = completedSessions.Count > 0
                ? completedSessions.Average(s => s.OverallScore!.Value)
                : null;

            var favouriteLanguage = completedSessions
                .Where(s => !string.IsNullOrWhiteSpace(s.Language))
                .GroupBy(s => s.Language)
                .OrderByDescending(g => g.Count())
                .ThenBy(g => g.Key)
                .Select(g => g.Key)
                .FirstOrDefault();

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

            var response = new DashboardResponse
            {
                AverageScore = averageScore,
                SessionsCompleted = completedSessions.Count,
                QuestionsAnswered = sessions
                    .SelectMany(s => s.Answers)
                    .Count(a => a.UserId == userId),
                FavouriteLanguage = favouriteLanguage,
                RecentSessions = recentSessions
            };

            return Results.Ok(response);
        });
    }
}
