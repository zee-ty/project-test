namespace CodeVerifyBackend.Models;

// The settings from the "CodeVerify" section of appsettings.json.
public class CodeVerifyOptions
{
    public string Model { get; set; } = "gemini-3.6-flash";
    public int NumQuestions { get; set; } = 4;
    public int MaxCodeChars { get; set; } = 6000;
    public int MaxAnswerChars { get; set; } = 1000;
    public bool UseFakeAi { get; set; } = false;
}
