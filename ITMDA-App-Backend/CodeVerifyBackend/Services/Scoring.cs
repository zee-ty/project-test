using CodeVerifyBackend.Models;

namespace CodeVerifyBackend.Services;

// use for testing so dont need api 
public static class Scoring
{
    public static int ClampScore(double value)
    {
        if (double.IsNaN(value)) return 0;
        var rounded = (int)Math.Round(value, MidpointRounding.AwayFromZero);
        return Math.Max(0, Math.Min(100, rounded));
    }

  //makes sure scores are consistent 
    public static string VerdictFromScore(int score)
    {
        if (score >= 80) return "correct";
        if (score >= 40) return "partial";
        return "incorrect";
    }

    public static int OverallScore(IEnumerable<Answer> answers)
    {
        var scores = answers
            .Where(a => a.Evaluation != null)
            .Select(a => a.Evaluation!.Score)
            .ToList();

        if (scores.Count == 0) return 0;
        return ClampScore(scores.Average());
    }

    // Backup feedback if ai fails
    public static string FallbackSummary(int score, int answeredCount)
    {
        if (answeredCount == 0)
            return "You didn't answer any questions this time. Try again and answer a few to see how well you understand the code.";
        if (score >= 80)
            return "You demonstrated a good understanding of the coding principles we discussed.";
        if (score >= 50)
            return "You understand parts of this code well, but a few ideas need more practice. Review the feedback on your weaker answers.";
        return "This code is still unclear in several places. Try going through it line by line and run it to see what happens.";
    }
}