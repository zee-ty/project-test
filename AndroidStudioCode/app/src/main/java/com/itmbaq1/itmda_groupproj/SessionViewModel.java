package com.itmbaq1.itmda_groupproj;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.JsonParser;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SessionViewModel extends AndroidViewModel {
    public static final int MAX_CODE_CHARS = 6000;
    public static final int MAX_ANSWER_CHARS = 1000;
    private static final String THINKING = "AI is thinking...";

    public final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> showRetry = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> canSend = new MutableLiveData<>(true);
    public final MutableLiveData<String> title = new MutableLiveData<>("New Session");
    public final MutableLiveData<String> subtitle = new MutableLiveData<>("Paste Your Code Below");
    public final MutableLiveData<String> inputLabel = new MutableLiveData<>("PASTE CODE HERE:");
    public final MutableLiveData<String> inputHint = new MutableLiveData<>("Paste your code here");
    public final MutableLiveData<String> toast = new MutableLiveData<>();
    public final MutableLiveData<SessionDtos.FinishResponse> result = new MutableLiveData<>();

    private final List<ChatMessage> chat = new ArrayList<>();
    private Integer sessionId;
    private Integer questionId;
    private String lastText;
    private boolean lastWasCode;
    private boolean lastWasFinish;
    private Call<?> inFlight;

    public SessionViewModel(@NonNull Application application) {
        super(application);
    }

    public void send(String text) {
        if (Boolean.TRUE.equals(loading.getValue())) return;

        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            toast.setValue("Please enter something first");
            return;
        }

        if (sessionId == null) {
            if (trimmed.length() > MAX_CODE_CHARS) {
                toast.setValue("That code is too long. The limit is " + MAX_CODE_CHARS + " characters.");
                return;
            }
            startSession(trimmed);
            return;
        }

        if (questionId == null) {
            toast.setValue("All questions are answered. Tap End Session.");
            return;
        }
        if (trimmed.length() > MAX_ANSWER_CHARS) {
            toast.setValue("That answer is too long. The limit is " + MAX_ANSWER_CHARS + " characters.");
            return;
        }
        submitAnswer(trimmed);
    }

    public void retry() {
        if (Boolean.TRUE.equals(loading.getValue())) return;
        if (lastWasFinish) {
            finishSession();
        } else if (lastText != null) {
            if (lastWasCode) startSession(lastText);
            else submitAnswer(lastText);
        }
    }

    public void finishSession() {
        if (Boolean.TRUE.equals(loading.getValue())) return;
        if (sessionId == null) {
            toast.setValue("Submit some code first");
            return;
        }
        lastWasFinish = true;
        lastWasCode = false;
        if (!ensureOnline()) return;
        setBusy(true);
        addThinking();
        String authHeader = getAuthHeader();

        if (authHeader == null) {
            setBusy(false);
            removeThinking();
            toast.setValue("Please log in again.");
            return;
        }
        Call<SessionDtos.FinishResponse> call = ApiClient.sessions().finishSession(authHeader, sessionId);
        inFlight = call;
        call.enqueue(new Callback<SessionDtos.FinishResponse>() {
            @Override
            public void onResponse(Call<SessionDtos.FinishResponse> call, Response<SessionDtos.FinishResponse> response) {
                removeThinking();
                setBusy(false);
                if (!response.isSuccessful() || response.body() == null) {
                    fail(readError(response));
                    return;
                }
                showRetry.setValue(false);
                lastText = null;
                lastWasFinish = false;
                result.setValue(response.body());
            }

            @Override
            public void onFailure(Call<SessionDtos.FinishResponse> call, Throwable t) {
                if (call.isCanceled()) return;
                removeThinking();
                setBusy(false);
                fail(networkMessage(t));
            }
        });
    }

    public void consumeResult() {
        result.setValue(null);
        reset();
    }

    public void reset() {
        cancelInFlight();
        chat.clear();
        sessionId = null;
        questionId = null;
        lastText = null;
        lastWasCode = false;
        lastWasFinish = false;
        publishMessages();
        loading.setValue(false);
        showRetry.setValue(false);
        title.setValue("New Session");
        subtitle.setValue("Paste Your Code Below");
        inputLabel.setValue("PASTE CODE HERE:");
        inputHint.setValue("Paste your code here");
        updateCanSend();
    }

    private String getAuthHeader() {
        TokenManager tokenManager = new TokenManager(getApplication());
        String token = tokenManager.getToken();

        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        if (token.startsWith("Bearer ")) {
            return token;
        }

        return "Bearer " + token;
    }


    private void startSession(String code) {
        lastText = code;
        lastWasCode = true;
        lastWasFinish = false;
        if (!ensureOnline()) return;

        setBusy(true);
        addUserOnce(code);
        addThinking();
        String authHeader = getAuthHeader();
        if (authHeader == null) {
            setBusy(false);
            removeThinking();
            toast.setValue("Please log in again.");
            return;
        }
        Call<SessionDtos.StartSessionResponse> call = ApiClient.sessions().startSession(authHeader, new SessionDtos.StartSessionRequest(code));

        inFlight = call;
        call.enqueue(new Callback<SessionDtos.StartSessionResponse>() {
            @Override
            public void onResponse(Call<SessionDtos.StartSessionResponse> call, Response<SessionDtos.StartSessionResponse> response) {
                removeThinking();
                setBusy(false);
                if (!response.isSuccessful() || response.body() == null) {
                    fail(readError(response));
                    return;
                }
                SessionDtos.StartSessionResponse body = response.body();
                sessionId = body.sessionId;
                questionId = body.question == null ? null : body.question.id;
                title.setValue("CODE UNDERSTANDING SESSION");
                String language = body.language == null ? "-" : body.language;
                subtitle.setValue("DETECTED LANGUAGE: " + language.toUpperCase());
                inputLabel.setValue("Type your answers:");
                inputHint.setValue("Type your answer");
                showRetry.setValue(false);
                lastText = null;
                if (body.question != null && body.question.text != null) {
                    addAi(body.question.text);
                }
                updateCanSend();
            }

            @Override
            public void onFailure(Call<SessionDtos.StartSessionResponse> call, Throwable t) {
                if (call.isCanceled()) return;
                removeThinking();
                setBusy(false);
                fail(networkMessage(t));
            }
        });
    }

    private void submitAnswer(String answer) {
        lastText = answer;
        lastWasCode = false;
        lastWasFinish = false;
        if (!ensureOnline()) return;

        setBusy(true);
        addUserOnce(answer);
        addThinking();

        String authHeader = getAuthHeader();

        if (authHeader == null) {
            setBusy(false);
            removeThinking();
            toast.setValue("Please log in again.");
            return;
        }
        Call<SessionDtos.AnswerResponse> call = ApiClient.sessions().submitAnswer(authHeader, sessionId, new SessionDtos.SubmitAnswerRequest(questionId, answer));

        inFlight = call;
        call.enqueue(new Callback<SessionDtos.AnswerResponse>() {
            @Override
            public void onResponse(Call<SessionDtos.AnswerResponse> call, Response<SessionDtos.AnswerResponse> response) {
                removeThinking();
                setBusy(false);
                if (!response.isSuccessful() || response.body() == null) {
                    fail(readError(response));
                    return;
                }
                SessionDtos.AnswerResponse body = response.body();
                showRetry.setValue(false);
                lastText = null;
                if (body.feedback != null && !body.feedback.isEmpty()) {
                    addAi(body.score + "% — " + body.feedback);
                }
                if (body.nextQuestion != null) {
                    questionId = body.nextQuestion.id;
                    if (body.nextQuestion.text != null) addAi(body.nextQuestion.text);
                } else {
                    questionId = null;
                    addAi("That's all the questions. Tap End Session for your score.");
                }
                updateCanSend();
            }

            @Override
            public void onFailure(Call<SessionDtos.AnswerResponse> call, Throwable t) {
                if (call.isCanceled()) return;
                removeThinking();
                setBusy(false);
                fail(networkMessage(t));
            }
        });
    }

    private boolean ensureOnline() {
        if (isOnline()) return true;
        fail("No internet connection.");
        return false;
    }

    private boolean isOnline() {
        ConnectivityManager manager = (ConnectivityManager)
                getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) return true;
        Network network = manager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void setBusy(boolean busy) {
        loading.setValue(busy);
        if (busy) showRetry.setValue(false);
        updateCanSend();
    }

    private void updateCanSend() {
        boolean busy = Boolean.TRUE.equals(loading.getValue());
        canSend.setValue(!busy && (sessionId == null || questionId != null));
    }

    private void fail(String message) {
        toast.setValue(message);
        showRetry.setValue(true);
    }

    private void addUserOnce(String text) {
        if (!chat.isEmpty()) {
            ChatMessage last = chat.get(chat.size() - 1);
            if (last.fromUser && text.equals(last.text)) return;
        }
        chat.add(new ChatMessage(text, true));
        publishMessages();
    }

    private void addAi(String text) {
        chat.add(new ChatMessage(text, false));
        publishMessages();
    }

    private void addThinking() {
        chat.add(new ChatMessage(THINKING, false));
        publishMessages();
    }

    private void removeThinking() {
        if (chat.isEmpty()) return;
        ChatMessage last = chat.get(chat.size() - 1);
        if (!last.fromUser && THINKING.equals(last.text)) {
            chat.remove(chat.size() - 1);
            publishMessages();
        }
    }

    private void publishMessages() {
        messages.setValue(new ArrayList<>(chat));
    }

    private String readError(Response<?> response) {
        try {
            if (response.errorBody() == null) return "Something went wrong. Please try again.";
            String body = response.errorBody().string();
            return JsonParser.parseString(body).getAsJsonObject().get("error").getAsString();
        } catch (Exception ignored) {
            return "Something went wrong. Please try again.";
        }
    }

    private String networkMessage(Throwable error) {
        if (error instanceof SocketTimeoutException) {
            return "The request timed out. Please try again.";
        }
        return "Could not reach the server. Check that the backend is running.";
    }

    private void cancelInFlight() {
        if (inFlight != null) {
            inFlight.cancel();
            inFlight = null;
        }
    }

    @Override
    protected void onCleared() {
        cancelInFlight();
    }
}
