
namespace CodeVerifyBackend.Models;

// One point on the score history chart.
public class ScoreHistoryPointDto
{
    public int SessionId { get; set; }
    public int Score { get; set; }
    public DateTime CompletedAt { get; set; }
}

// Average score for one programming language.
public class LanguageAverageDto
{
    public string Language { get; set; } = "";
    public double AverageScore { get; set; }
}

// One session in the full progress history.
public class SessionHistoryDto
{
    public int SessionId { get; set; }
    public string Language { get; set; } = "";
    public string Topic { get; set; } = "";
    public int? Score { get; set; }
    public int QuestionsAnswered { get; set; }
    public DateTime SubmittedAt { get; set; }
    public DateTime? CompletedAt { get; set; }
}

// All statistics displayed on the progress screen.
public class ProgressResponse
{
    public List<ScoreHistoryPointDto> ScoreHistory { get; set; } = new();
    public List<LanguageAverageDto> LanguageAverages { get; set; } = new();
    public double? ImprovementPercentage { get; set; }
    public List<SessionHistoryDto> SessionHistory { get; set; } = new();
}
