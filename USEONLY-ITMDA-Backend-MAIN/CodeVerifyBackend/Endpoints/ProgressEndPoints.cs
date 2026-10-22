using CodeVerifyBackend.Models;
using CodeVerifyBackend.Services;
using CodeVerifyBackend.Auth;

namespace CodeVerifyBackend.Endpoints;

public static class ProgressEndpoints
{
    public static IEndpointRouteBuilder MapProgressEndpoints(
        this IEndpointRouteBuilder app)
    {
        app.MapGet("/api/progress", async (
            HttpContext context,
            ISessionStore store) =>
        {
            var userId = context.GetUserId();
            var sessions = await store.GetSessionsByUserAsync(userId);

            var scoredSessions = sessions
                .Where(s => s.FinishedAt != null && s.OverallScore != null)
                .OrderBy(s => s.FinishedAt)
                .ToList();

            var scoreHistory = scoredSessions
                .Select(s => new ScoreHistoryPoint
                {
                    Date = (s.FinishedAt ?? s.SubmittedAt)
                        .ToString("yyyy-MM-dd"),
                    Score = s.OverallScore!.Value
                })
                .ToList();

            var languageAverages = scoredSessions
                .Where(s => !string.IsNullOrWhiteSpace(s.Language))
                .GroupBy(s => s.Language.Trim(),
                    StringComparer.OrdinalIgnoreCase)
                .Select(g => new LanguageAverage
                {
                    Language = g.Key,
                    AverageScore = Math.Round(
                        g.Average(s => (double)s.OverallScore!.Value), 2)
                })
                .OrderBy(g => g.Language)
                .ToList();

            double improvementPercentage = 0;

            if (scoredSessions.Count >= 2)
            {
                var firstScore =
                    (double)scoredSessions.First().OverallScore!.Value;

                var latestScore =
                    (double)scoredSessions.Last().OverallScore!.Value;

                if (firstScore > 0)
                {
                    improvementPercentage = Math.Round(
                        ((latestScore - firstScore) / firstScore) * 100, 2);
                }
            }

            var sessionHistory = sessions
                .OrderByDescending(s => s.SubmittedAt)
                .Select(s => new SessionHistoryItem
                {
                    SessionId = s.Id,
                    Language = s.Language,
                    Topic = s.Topic,
                    Score = s.OverallScore,
                    SubmittedAt = s.SubmittedAt
                })
                .ToList();

            return Results.Ok(new ProgressResponse
            {
                ScoreHistory = scoreHistory,
                LanguageAverages = languageAverages,
                ImprovementPercentage = improvementPercentage,
                SessionHistory = sessionHistory
            });
        });

        return app;
    }
}

public sealed class ProgressResponse
{
    public List<ScoreHistoryPoint> ScoreHistory { get; set; } = new();
    public List<LanguageAverage> LanguageAverages { get; set; } = new();
    public double ImprovementPercentage { get; set; }
    public List<SessionHistoryItem> SessionHistory { get; set; } = new();
}

public sealed class ScoreHistoryPoint
{
    public string Date { get; set; } = "";
    public double Score { get; set; }
}

public sealed class LanguageAverage
{
    public string Language { get; set; } = "";
    public double AverageScore { get; set; }
}

public sealed class SessionHistoryItem
{
    public int SessionId { get; set; }
    public string Language { get; set; } = "";
    public string Topic { get; set; } = "";
    public int? Score { get; set; }
    public DateTime SubmittedAt { get; set; }
}
