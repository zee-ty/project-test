namespace CodeVerifyBackend.Models;

// One question the AI asked (Question table).
public class Question
{
    public int Id { get; set; }
    public string Text { get; set; } = "";
    public string Type { get; set; } = "";
}

// The AI's mark for one answer (Evaluation table).
public class Evaluation
{
    public int Score { get; set; }
    public string Verdict { get; set; } = "";
    public string Feedback { get; set; } = "";
}

// What the student answered (Answer table).
public class Answer
{
    public int Id { get; set; }
    public int QuestionId { get; set; }
    public int UserId { get; set; }
    public string Text { get; set; } = "";
    public DateTime AnsweredAt { get; set; }
    public Evaluation? Evaluation { get; set; }
}

// One whole session (CodeSubmission table) with its questions and answers.
public class Session
{
    public int Id { get; set; }
    public int UserId { get; set; }
    public string Code { get; set; } = "";
    public string Language { get; set; } = "";
    public string Topic { get; set; } = "";
    public DateTime SubmittedAt { get; set; }
    public int? OverallScore { get; set; }
    public string? SummaryFeedback { get; set; }
    public DateTime? FinishedAt { get; set; }
    public List<Question> Questions { get; set; } = new();
    public List<Answer> Answers { get; set; } = new();

    // Makes an independent copy, the way a database hands back fresh data.
    public Session Clone() => new()
    {
        Id = Id,
        UserId = UserId,
        Code = Code,
        Language = Language,
        Topic = Topic,
        SubmittedAt = SubmittedAt,
        OverallScore = OverallScore,
        SummaryFeedback = SummaryFeedback,
        FinishedAt = FinishedAt,
        Questions = Questions
            .Select(q => new Question { Id = q.Id, Text = q.Text, Type = q.Type })
            .ToList(),
        Answers = Answers
            .Select(a => new Answer
            {
                Id = a.Id,
                QuestionId = a.QuestionId,
                UserId = a.UserId,
                Text = a.Text,
                AnsweredAt = a.AnsweredAt,
                Evaluation = a.Evaluation == null
                    ? null
                    : new Evaluation
                    {
                        Score = a.Evaluation.Score,
                        Verdict = a.Evaluation.Verdict,
                        Feedback = a.Evaluation.Feedback
                    }
            })
            .ToList()
    };
}