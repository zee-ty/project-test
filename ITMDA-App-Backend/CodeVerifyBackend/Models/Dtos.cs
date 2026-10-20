namespace CodeVerifyBackend.Models;

/* ---------- What the app SENDS us ---------- */

public class StartSessionRequest
{
    public string? Code { get; set; }
}

public class SubmitAnswerRequest
{
    public int QuestionId { get; set; }
    public string? Answer { get; set; }
}

/* ---------- What we SEND BACK to the app ---------- */

public record QuestionDto(int Id, int Number, string Text);

public record StartSessionResponse
(
    int SessionId, string Language, string Topic, int TotalQuestions, QuestionDto Question
);

public record AnswerResponse
(
    int Score, string Verdict, string Feedback,
    int QuestionsAnswered, int TotalQuestions, QuestionDto? NextQuestion
);

public record FinishResponse
(
    int SessionId, string Language, string Topic, int OverallScore,
    int QuestionsAnswered, int TotalQuestions, string Feedback
);

public record TranscriptMessage(string From, string Text);

public record SessionStateResponse
(
    int SessionId, string Language, string Topic, bool Finished,
    int TotalQuestions, int QuestionsAnswered,
    QuestionDto? CurrentQuestion, List<TranscriptMessage> Transcript
);