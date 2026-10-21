using CodeVerifyBackend.Models;

namespace CodeVerifyBackend.Services;

// Everything Part C needs from the database. Part A writes a real version
// of this and edits Program.cs.
public interface ISessionStore
{
    // Saves a new session (CodeSubmission) with its questions (Question table).
    Task<Session> CreateSessionAsync(
        int userId, string code, string language, string topic, List<NewQuestion> questions);

    // Finds a session with its questions and answers (each answer has its
    // Evaluation filled in once marked). Returns null if it doesn't exist.
    Task<Session?> GetSessionAsync(int sessionId);

    // Saves what the is answered (Answer table). Returns the new answer's id.
    Task<int> SaveAnswerAsync(int sessionId, int questionId, int userId, string answerText);

    // Saves the AI's mark for an answer (Evaluation table).
    Task SaveEvaluationAsync(int sessionId, int answerId, int score, string verdict, string feedback);

    // Saves the final score when the session ends.
    Task FinishSessionAsync(int sessionId, int overallScore, string summaryFeedback);

    Task<List<Session>> GetSessionsByUserAsync(int userId);
}

//Tshepang this is needed to be edited by you for part A integration: