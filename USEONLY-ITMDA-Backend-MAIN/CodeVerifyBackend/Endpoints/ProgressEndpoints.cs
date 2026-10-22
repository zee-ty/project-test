using CodeVerifyBackend.Models;
using CodeVerifyBackend.Services;
using CodeVerifyBackend.Auth;

namespace CodeVerifyBackend.Endpoints;

public static class ProgressEndpoints
{
    public static void MapProgressEndpoints(this WebApplication app)
    {
        app.MapGet("/api/progress", async (
            HttpContext ctx,
            ISessionStore store) =>
        {
            var userId = ctx.GetUserId();

            var sessions = await store.GetSessionsByUserAsync(userId);

            var completedSessions = sessions
                .Where(s => s.FinishedAt.HasValue &&
                            s.OverallScore.HasValue)
                .OrderBy(s => s.FinishedAt)
                .ToList();

            var scoreHistory = completedSessions
                .Select(s => new ScoreHistoryPointDto
                {
                    SessionId = s.Id,
                    Score = s.OverallScore!.Value,
                    CompletedAt = s.FinishedAt!.Value
                })
                .ToList();

            var languageAverages = completedSessions
                .Where(s => !string.IsNullOrWhiteSpace(s.Language))
                .GroupBy(s => s.Language)
                .Select(g => new LanguageAverageDto
                {
                    Language = g.Key,
                    AverageScore = g.Average(s => s.OverallScore!.Value)
                })
                .OrderByDescending(x => x.AverageScore)
                .ToList();

            double? improvementPercentage = null;

            if (scoreHistory.Count >= 2 &&
                scoreHistory[0].Score != 0)
            {
                var firstScore = scoreHistory[0].Score;
                var latestScore = scoreHistory[^1].Score;

                improvementPercentage =
                    ((double)(latestScore - firstScore) / firstScore) * 100;
            }

            var sessionHistory = sessions
                .OrderByDescending(s => s.SubmittedAt)
                .Select(s => new SessionHistoryDto
                {
                    SessionId = s.Id,
                    Language = s.Language,
                    Topic = s.Topic,
                    Score = s.OverallScore,
                    QuestionsAnswered = s.Answers.Count(
                        a => a.UserId == userId),
                    SubmittedAt = s.SubmittedAt,
                    CompletedAt = s.FinishedAt
                })
                .ToList();

            var response = new ProgressResponse
            {
                ScoreHistory = scoreHistory,
                LanguageAverages = languageAverages,
                ImprovementPercentage = improvementPercentage,
                SessionHistory = sessionHistory
            };

            return Results.Ok(response);
        });
    }
}
