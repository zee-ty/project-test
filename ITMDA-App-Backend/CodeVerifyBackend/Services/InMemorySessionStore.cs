using CodeVerifyBackend.Models;

namespace CodeVerifyBackend.Services;

// Everything is lost when the server stops. Part A replaces it with a
// real db version of ISessionStore.
public class InMemorySessionStore : ISessionStore
{
    private readonly Dictionary<int, Session> _sessions = new();
    private readonly object _lock = new();
    private int _nextSessionId = 1;
    private int _nextQuestionId = 1;
    private int _nextAnswerId = 1;

    public Task<Session> CreateSessionAsync(
        int userId, string code, string language, string topic, List<NewQuestion> questions)
    {
        lock (_lock)
        {
            var session = new Session
            {
                Id = _nextSessionId++,
                UserId = userId,
                Code = code,
                Language = language,
                Topic = topic,
                SubmittedAt = DateTime.UtcNow,
                Questions = questions
                    .Select(q => new Question { Id = _nextQuestionId++, Text = q.Text, Type = q.Type })
                    .ToList()
            };
            _sessions[session.Id] = session;
            return Task.FromResult(session.Clone());
        }
    }

    public Task<Session?> GetSessionAsync(int sessionId)
    {
        lock (_lock)
        {
            return Task.FromResult(
                _sessions.TryGetValue(sessionId, out var s) ? s.Clone() : null);
        }
    }

    public Task<int> SaveAnswerAsync(int sessionId, int questionId, int userId, string answerText)
    {
        lock (_lock)
        {
            var session = _sessions[sessionId];
            var answer = new Answer
            {
                Id = _nextAnswerId++,
                QuestionId = questionId,
                UserId = userId,
                Text = answerText,
                AnsweredAt = DateTime.UtcNow
            };
            session.Answers.Add(answer);
            return Task.FromResult(answer.Id);
        }
    }

    public Task SaveEvaluationAsync(
        int sessionId, int answerId, int score, string verdict, string feedback)
    {
        lock (_lock)
        {
            var answer = _sessions[sessionId].Answers.First(a => a.Id == answerId);
            answer.Evaluation = new Evaluation { Score = score, Verdict = verdict, Feedback = feedback };
            return Task.CompletedTask;
        }
    }

    public Task FinishSessionAsync(int sessionId, int overallScore, string summaryFeedback)
    {
        lock (_lock)
        {
            var session = _sessions[sessionId];
            session.OverallScore = overallScore;
            session.SummaryFeedback = summaryFeedback;
            session.FinishedAt = DateTime.UtcNow;
            return Task.CompletedTask;
        }
    }
}