namespace CodeVerifyBackend.Services;

// A TESTING stand-in for the AI. It needs no key or credit, so
public class FakeAiService : IAiService
{
    public Task<CodeAnalysis> AnalyseCodeAsync(string code, int questionCount)
    {
        var looksLikeCode = code.IndexOfAny(new[] { '(', ')', '{', '}', '=', ';' }) >= 0;
        if (!looksLikeCode)
            return Task.FromResult(new CodeAnalysis(false, "Unknown", "None", new List<NewQuestion>()));

        var language =
            code.Contains("Console.") || code.Contains("using System") ? "C#" :
            code.Contains("System.out") || code.Contains("public static void main") ? "Java" :
            code.Contains("def ") || code.Contains("print(") ? "Python" :
            "Unknown";

        var topic =
            code.Contains("for ") || code.Contains("while ") ? "Loops" :
            code.Contains("class ") ? "Classes" :
            code.Contains("def ") ? "Functions" : "General";

        var all = new[]
        {
            new NewQuestion("What is the main purpose of this code?", "purpose"),
            new NewQuestion("Explain step by step how this code works.", "logic"),
            new NewQuestion("What will this code output when it runs?", "output"),
            new NewQuestion("Why is this approach used in this code?", "concept"),
            new NewQuestion("What would change if you altered the first value in the code?", "logic"),
        };
        var questions = all.Take(Math.Max(1, Math.Min(questionCount, all.Length))).ToList();
        return Task.FromResult(new CodeAnalysis(true, language, topic, questions));
    }

    public Task<AnswerEvaluation> EvaluateAnswerAsync(string code, string question, string answer)
    {
        var lower = answer.ToLowerInvariant();
        if (lower.Contains("don't know") || lower.Contains("dont know"))
            return Task.FromResult(new AnswerEvaluation(10, "Not correct. Have another look at the code and try to explain what each line does."));
        if (answer.Length < 15)
            return Task.FromResult(new AnswerEvaluation(35, "Partly there, but your answer is very short. Try explaining it in a full sentence."));
        return Task.FromResult(new AnswerEvaluation(85, "Correct! You explained that clearly."));
    }

    public Task<string> SummariseSessionAsync(
        string language, string topic, int score, List<SummaryItem> items) =>
        Task.FromResult(score >= 80
            ? $"You showed a good understanding of {topic} in {language}."
            : $"You understand parts of {topic} in {language}, but it is worth reviewing the feedback on your weaker answers.");
}
