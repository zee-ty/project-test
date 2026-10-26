namespace CodeVerifyBackend.Models;

// One session displayed in the dashboard's recent sessions list.
public class RecentSessionDto
{
    public int SessionId { get; set; }
    public string Language { get; set; } = "";
    public string Topic { get; set; } = "";
    public int? Score { get; set; }
    public DateTime SubmittedAt { get; set; }
}

// All statistics displayed on the dashboard.
public class DashboardResponse
{
    public double? AverageScore { get; set; }
    public int SessionsCompleted { get; set; }
    public int QuestionsAnswered { get; set; }
    public string? FavouriteLanguage { get; set; }
    public List<RecentSessionDto> RecentSessions { get; set; } = new();
}