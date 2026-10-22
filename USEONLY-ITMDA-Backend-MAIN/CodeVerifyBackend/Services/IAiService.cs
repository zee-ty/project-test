namespace CodeVerifyBackend.Services;

// A question the AI wrote, before it is saved.
public record NewQuestion(string Text, string Type);

// The result of analysing pasted code.
public record CodeAnalysis(bool IsCode, string Language, string Topic, List<NewQuestion> Questions);

// The AI's mark for one answer.
public record AnswerEvaluation(int Score, string Feedback);

// One question's result/used to write feebback summary
public record SummaryItem(string Question, int Score, string Feedback);

// The three jobs the AI does.
// FakeAiService is a testing feature
public interface IAiService
{
    Task<CodeAnalysis> AnalyseCodeAsync(string code, int questionCount);
    Task<AnswerEvaluation> EvaluateAnswerAsync(string code, string question, string answer);
    Task<string> SummariseSessionAsync(string language, string topic, int score, List<SummaryItem> items);
}
