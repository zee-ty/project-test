using System.Collections.Concurrent;
using System.Text.Json;
using CodeVerifyBackend.Auth;
using CodeVerifyBackend.Models;
using CodeVerifyBackend.Services;

namespace CodeVerifyBackend.Endpoints;

public static class SessionEndpoints
{
    private static readonly ConcurrentDictionary<int, bool> Busy = new();

    public static void MapSessionEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api/sessions");

        group.MapPost("/", StartSession);                    
        group.MapGet("/{id:int}", GetSessionState);          
        group.MapPost("/{id:int}/answers", SubmitAnswer);    
        group.MapPost("/{id:int}/finish", FinishSession);   
    }

        private static async Task<IResult> StartSession(
        HttpContext ctx, ISessionStore store, IAiService ai, CodeVerifyOptions options)
    {
        var body = await ReadBodyAsync<StartSessionRequest>(ctx.Request);
        var code = (body.Code ?? "").Trim();

        if (code.Length == 0)
            throw new ApiException(400, "Please paste some code first.");
        if (code.Length > options.MaxCodeChars)
            throw new ApiException(400, $"That code is too long. The limit is {options.MaxCodeChars} characters.");

        // Ask the AI: is it code? what language? what topic? what questions?
        var analysis = await ai.AnalyseCodeAsync(code, options.NumQuestions);
        if (!analysis.IsCode)
            throw new ApiException(422, "That doesn't look like programming code. Please paste a code snippet.");

        var session = await store.CreateSessionAsync(
            ctx.GetUserId(), code, analysis.Language, analysis.Topic, analysis.Questions);

        var response = new StartSessionResponse(
            session.Id,
            session.Language,
            session.Topic,
            session.Questions.Count,
            ToQuestionDto(session.Questions[0], 1));

        return Results.Json(response, statusCode: 201);
    }

       private static async Task<IResult> GetSessionState(int id, HttpContext ctx, ISessionStore store)
    {
        var session = await LoadOwnedSessionAsync(id, ctx, store);
        var finished = session.FinishedAt != null;
        var answered = session.Answers.Count;
        var current = answered < session.Questions.Count ? session.Questions[answered] : null;

        var response = new SessionStateResponse(
            session.Id,
            session.Language,
            session.Topic,
            finished,
            session.Questions.Count,
            answered,
            !finished && current != null ? ToQuestionDto(current, answered + 1) : null,
            BuildTranscript(session));

        return Results.Json(response);
    }

       private static async Task<IResult> SubmitAnswer(
        int id, HttpContext ctx, ISessionStore store, IAiService ai, CodeVerifyOptions options)
    {
        var tookLock = false;
        try
        {
            // Stop double-taps: only one answer per session is marked at a time.
            // We take this lock BEFORE reading the session, so a second tap can
            // never act on out-of-date data.
            if (!Busy.TryAdd(id, true))
                throw new ApiException(429, "Still marking your previous answer.");
            tookLock = true;

            var session = await LoadOwnedSessionAsync(id, ctx, store);
            if (session.FinishedAt != null)
                throw new ApiException(409, "This session has already finished.");

            var body = await ReadBodyAsync<SubmitAnswerRequest>(ctx.Request);
            var answerText = (body.Answer ?? "").Trim();
            if (answerText.Length == 0)
                throw new ApiException(400, "Please type an answer.");
            if (answerText.Length > options.MaxAnswerChars)
                throw new ApiException(400, $"That answer is too long. The limit is {options.MaxAnswerChars} characters.");

            // Which question are we on? (the number of answers so far = the position)
            var index = session.Answers.Count;
            if (index >= session.Questions.Count)
                throw new ApiException(409, "All questions are answered. Please finish the session.");
            var current = session.Questions[index];
            if (body.QuestionId != current.Id)
                throw new ApiException(409, "That is not the current question.");

            // Ask the AI to mark it.
            var evaluation = await ai.EvaluateAnswerAsync(session.Code, current.Text, answerText);
            var verdict = Scoring.VerdictFromScore(evaluation.Score);

            // Save the answer, then save its mark.
            var answerId = await store.SaveAnswerAsync(session.Id, current.Id, ctx.GetUserId(), answerText);
            await store.SaveEvaluationAsync(session.Id, answerId, evaluation.Score, verdict, evaluation.Feedback);

            // Is there another question after this one?
            var next = index + 1 < session.Questions.Count ? session.Questions[index + 1] : null;

            var response = new AnswerResponse(
                evaluation.Score,
                verdict,
                evaluation.Feedback,
                index + 1,
                session.Questions.Count,
                next != null ? ToQuestionDto(next, index + 2) : null);

            return Results.Json(response);
        }
        finally
        {
            // Release the lock, but only if THIS request was the one that took it.
            if (tookLock) Busy.TryRemove(id, out _);
        }
    }

       private static async Task<IResult> FinishSession(
        int id, HttpContext ctx, ISessionStore store, IAiService ai)
    {
        var session = await LoadOwnedSessionAsync(id, ctx, store);

        // Finishing twice just returns the saved result.
        if (session.FinishedAt != null)
        {
            return Results.Json(ToFinishResponse(
                session, session.OverallScore ?? 0, session.SummaryFeedback ?? ""));
        }

        var marked = session.Answers.Where(a => a.Evaluation != null).ToList();
        var score = Scoring.OverallScore(marked);

        var feedback = "";
        if (marked.Count > 0)
        {
            try
            {
                var items = marked.Select(a => new SummaryItem(
                    session.Questions.FirstOrDefault(q => q.Id == a.QuestionId)?.Text ?? "",
                    a.Evaluation!.Score,
                    a.Evaluation.Feedback)).ToList();

                feedback = await ai.SummariseSessionAsync(session.Language, session.Topic, score, items);
            }
            catch (Exception)
            {
                feedback = ""; // if the AI fails here, we use the backup below
            }
        }
        if (string.IsNullOrWhiteSpace(feedback))
            feedback = Scoring.FallbackSummary(score, marked.Count);

        await store.FinishSessionAsync(session.Id, score, feedback);
        return Results.Json(ToFinishResponse(session, score, feedback));
    }
    private static async Task<T> ReadBodyAsync<T>(HttpRequest request) where T : class, new()
    {
        try
        {
            return await request.ReadFromJsonAsync<T>() ?? new T();
        }
        catch (JsonException)
        {
            throw new ApiException(400, "The request body is not valid JSON.");
        }
        catch (InvalidOperationException)
        {
            throw new ApiException(400, "Please send JSON with the header Content-Type: application/json.");
        }
    }

    // Finds a session AND checks it belongs to the logged-in user.
    // If it belongs to someone else, we pretend it doesn't exist.
    private static async Task<Session> LoadOwnedSessionAsync(int id, HttpContext ctx, ISessionStore store)
    {
        var session = await store.GetSessionAsync(id);
        if (session is null || session.UserId != ctx.GetUserId())
            throw new ApiException(404, "Session not found.");
        return session;
    }

    // What the app sees for a question (we never send internal fields).
    private static QuestionDto ToQuestionDto(Question q, int number) => new(q.Id, number, q.Text);

    private static FinishResponse ToFinishResponse(Session session, int score, string feedback) =>
        new(session.Id, session.Language, session.Topic, score,
            session.Answers.Count, session.Questions.Count, feedback);

    // Rebuilds the chat so the app can restore it after rotation or reopening.
    private static List<TranscriptMessage> BuildTranscript(Session session)
    {
        var chat = new List<TranscriptMessage> { new("user", session.Code) };

        // Show every answered question plus the current one.
        var shown = Math.Min(session.Answers.Count + 1, session.Questions.Count);
        for (var i = 0; i < shown; i++)
        {
            chat.Add(new TranscriptMessage("ai", session.Questions[i].Text));

            if (i < session.Answers.Count && session.Answers[i].Evaluation != null)
            {
                chat.Add(new TranscriptMessage("user", session.Answers[i].Text));
                chat.Add(new TranscriptMessage("ai", session.Answers[i].Evaluation!.Feedback));
            }
        }
        return chat;
    }
}