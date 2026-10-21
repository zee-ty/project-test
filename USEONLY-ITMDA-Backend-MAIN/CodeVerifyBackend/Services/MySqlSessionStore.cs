using CodeVerifyBackend.Models;
using MySqlConnector;

namespace CodeVerifyBackend.Services;

// The REAL storage: saves sessions, questions, answers and marks in MySQL.
// It replaces InMemorySessionStore. Every table and column name is written
// in the SQL below, so if your database uses different names, change them here.
//
//   code_submissions  (code_id, user_id, code_content, programming_language, topic,
//                      submission_date, overall_score, summary_feedback, finished_at)
//   questions         (question_id, code_id, question_text, question_type)
//   answers           (answer_id, question_id, user_id, answer_text, answer_date)
//   evaluations       (evaluation_id, answer_id, score, feedback)
public class MySqlSessionStore : ISessionStore
{
    private readonly string _connectionString;

    public MySqlSessionStore(string connectionString)
    {
        _connectionString = connectionString;
    }

    // Opens a fresh connection. Each method below opens its own and closes it when done.
    private async Task<MySqlConnection> OpenAsync()
    {
        var connection = new MySqlConnection(_connectionString);
        await connection.OpenAsync();
        return connection;
    }

    /* ------------------------------------------------------------------
       Save a new session and its questions (all or nothing).
       ------------------------------------------------------------------ */
    public async Task<Session> CreateSessionAsync(
        int userId, string code, string language, string topic, List<NewQuestion> questions)
    {
        await using var connection = await OpenAsync();
        await using var transaction = await connection.BeginTransactionAsync();

        long codeId;
        await using (var insert = new MySqlCommand(@"
            INSERT INTO code_submissions
                (user_id, code_content, programming_language, topic, submission_date)
            VALUES (@userId, @code, @language, @topic, @submittedAt)", connection, transaction))
        {
            insert.Parameters.AddWithValue("@userId", userId);
            insert.Parameters.AddWithValue("@code", code);
            insert.Parameters.AddWithValue("@language", language);
            insert.Parameters.AddWithValue("@topic", topic);
            insert.Parameters.AddWithValue("@submittedAt", DateTime.UtcNow);
            await insert.ExecuteNonQueryAsync();
            codeId = insert.LastInsertedId;
        }

        foreach (var question in questions)
        {
            await using var insertQuestion = new MySqlCommand(@"
                INSERT INTO questions (code_id, question_text, question_type)
                VALUES (@codeId, @text, @type)", connection, transaction);
            insertQuestion.Parameters.AddWithValue("@codeId", codeId);
            insertQuestion.Parameters.AddWithValue("@text", question.Text);
            insertQuestion.Parameters.AddWithValue("@type", question.Type);
            await insertQuestion.ExecuteNonQueryAsync();
        }

        await transaction.CommitAsync();

        var sessions = await LoadSessionsAsync(connection, "s.code_id = @p", codeId);
        return sessions.Single();
    }

    /* ------------------------------------------------------------------
       Find one session (with its questions and answers), or null.
       ------------------------------------------------------------------ */
    public async Task<Session?> GetSessionAsync(int sessionId)
    {
        await using var connection = await OpenAsync();
        var sessions = await LoadSessionsAsync(connection, "s.code_id = @p", sessionId);
        return sessions.FirstOrDefault();
    }

    /* ------------------------------------------------------------------
       All of one user's sessions (used by Part E's dashboard and progress).
       ------------------------------------------------------------------ */
    public async Task<List<Session>> GetSessionsByUserAsync(int userId)
    {
        await using var connection = await OpenAsync();
        return await LoadSessionsAsync(connection, "s.user_id = @p", userId);
    }

    /* ------------------------------------------------------------------
       Save what the student answered. The answer is only saved if the
       question really belongs to this session.
       ------------------------------------------------------------------ */
    public async Task<int> SaveAnswerAsync(int sessionId, int questionId, int userId, string answerText)
    {
        await using var connection = await OpenAsync();
        await using var command = new MySqlCommand(@"
            INSERT INTO answers (question_id, user_id, answer_text, answer_date)
            SELECT q.question_id, @userId, @text, @answeredAt
            FROM questions q
            WHERE q.question_id = @questionId AND q.code_id = @sessionId", connection);
        command.Parameters.AddWithValue("@userId", userId);
        command.Parameters.AddWithValue("@text", answerText);
        command.Parameters.AddWithValue("@answeredAt", DateTime.UtcNow);
        command.Parameters.AddWithValue("@questionId", questionId);
        command.Parameters.AddWithValue("@sessionId", sessionId);

        var rows = await command.ExecuteNonQueryAsync();
        if (rows == 0)
            throw new InvalidOperationException("That question does not belong to that session.");

        return (int)command.LastInsertedId;
    }

    /* ------------------------------------------------------------------
       Save the AI's mark. The verdict word is NOT stored: it is worked out
       from the score when loading (Scoring.VerdictFromScore).
       ------------------------------------------------------------------ */
    public async Task SaveEvaluationAsync(
        int sessionId, int answerId, int score, string verdict, string feedback)
    {
        await using var connection = await OpenAsync();
        await using var command = new MySqlCommand(@"
            INSERT INTO evaluations (answer_id, score, feedback)
            VALUES (@answerId, @score, @feedback)", connection);
        command.Parameters.AddWithValue("@answerId", answerId);
        command.Parameters.AddWithValue("@score", score);
        command.Parameters.AddWithValue("@feedback", feedback);
        await command.ExecuteNonQueryAsync();
    }

    /* ------------------------------------------------------------------
       Save the final score and mark the session as finished.
       ------------------------------------------------------------------ */
    public async Task FinishSessionAsync(int sessionId, int overallScore, string summaryFeedback)
    {
        await using var connection = await OpenAsync();
        await using var command = new MySqlCommand(@"
            UPDATE code_submissions
            SET overall_score = @score, summary_feedback = @summary, finished_at = @finishedAt
            WHERE code_id = @id", connection);
        command.Parameters.AddWithValue("@score", overallScore);
        command.Parameters.AddWithValue("@summary", summaryFeedback);
        command.Parameters.AddWithValue("@finishedAt", DateTime.UtcNow);
        command.Parameters.AddWithValue("@id", sessionId);
        await command.ExecuteNonQueryAsync();
    }

    /* ==================================================================
       Helpers: load sessions with their questions and answers.
       "where" is a fixed piece of SQL written in this file (never user
       input); the real value always travels safely as the parameter @p.
       ================================================================== */
    private static async Task<List<Session>> LoadSessionsAsync(
        MySqlConnection connection, string where, object value)
    {
        var sessions = new Dictionary<int, Session>();

        // 1. The sessions themselves.
        await using (var command = new MySqlCommand($@"
            SELECT s.code_id, s.user_id, s.code_content, s.programming_language, s.topic,
                   s.submission_date, s.overall_score, s.summary_feedback, s.finished_at
            FROM code_submissions s
            WHERE {where}
            ORDER BY s.code_id", connection))
        {
            command.Parameters.AddWithValue("@p", value);
            await using var reader = await command.ExecuteReaderAsync();
            while (await reader.ReadAsync())
            {
                var session = new Session
                {
                    Id = reader.GetInt32("code_id"),
                    UserId = reader.GetInt32("user_id"),
                    Code = reader.GetString("code_content"),
                    Language = reader.GetString("programming_language"),
                    Topic = reader.GetString("topic"),
                    SubmittedAt = AsUtc(reader.GetDateTime("submission_date")),
                    OverallScore = NullableInt(reader, "overall_score"),
                    SummaryFeedback = NullableString(reader, "summary_feedback"),
                    FinishedAt = NullableDate(reader, "finished_at")
                };
                sessions[session.Id] = session;
            }
        }

        if (sessions.Count == 0) return new List<Session>();

        // 2. Their questions, in the order they were asked.
        await using (var command = new MySqlCommand($@"
            SELECT q.question_id, q.code_id, q.question_text, q.question_type
            FROM questions q
            JOIN code_submissions s ON s.code_id = q.code_id
            WHERE {where}
            ORDER BY q.question_id", connection))
        {
            command.Parameters.AddWithValue("@p", value);
            await using var reader = await command.ExecuteReaderAsync();
            while (await reader.ReadAsync())
            {
                sessions[reader.GetInt32("code_id")].Questions.Add(new Question
                {
                    Id = reader.GetInt32("question_id"),
                    Text = reader.GetString("question_text"),
                    Type = reader.GetString("question_type")
                });
            }
        }

        // 3. Their answers (with the AI's mark, once marked), in the order they were given.
        await using (var command = new MySqlCommand($@"
            SELECT a.answer_id, a.question_id, q.code_id, a.user_id, a.answer_text, a.answer_date,
                   e.score, e.feedback
            FROM answers a
            JOIN questions q ON q.question_id = a.question_id
            JOIN code_submissions s ON s.code_id = q.code_id
            LEFT JOIN evaluations e ON e.answer_id = a.answer_id
            WHERE {where}
            ORDER BY a.answer_id", connection))
        {
            command.Parameters.AddWithValue("@p", value);
            await using var reader = await command.ExecuteReaderAsync();
            while (await reader.ReadAsync())
            {
                var score = NullableInt(reader, "score");
                sessions[reader.GetInt32("code_id")].Answers.Add(new Answer
                {
                    Id = reader.GetInt32("answer_id"),
                    QuestionId = reader.GetInt32("question_id"),
                    UserId = reader.GetInt32("user_id"),
                    Text = reader.GetString("answer_text"),
                    AnsweredAt = AsUtc(reader.GetDateTime("answer_date")),
                    Evaluation = score == null
                        ? null
                        : new Evaluation
                        {
                            Score = score.Value,
                            Verdict = Scoring.VerdictFromScore(score.Value),
                            Feedback = NullableString(reader, "feedback") ?? ""
                        }
                });
            }
        }

        return sessions.Values.ToList();
    }

    // MySQL gives dates back with no time zone. We always store UTC, so say so.
    private static DateTime AsUtc(DateTime value) => DateTime.SpecifyKind(value, DateTimeKind.Utc);

    private static int? NullableInt(MySqlDataReader reader, string column)
    {
        var index = reader.GetOrdinal(column);
        return reader.IsDBNull(index) ? null : reader.GetInt32(index);
    }

    private static string? NullableString(MySqlDataReader reader, string column)
    {
        var index = reader.GetOrdinal(column);
        return reader.IsDBNull(index) ? null : reader.GetString(index);
    }

    private static DateTime? NullableDate(MySqlDataReader reader, string column)
    {
        var index = reader.GetOrdinal(column);
        return reader.IsDBNull(index) ? null : AsUtc(reader.GetDateTime(index));
    }
}