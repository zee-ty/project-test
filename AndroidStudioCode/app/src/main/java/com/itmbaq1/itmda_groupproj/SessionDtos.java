package com.itmbaq1.itmda_groupproj;

import java.util.List;

public class SessionDtos {
    public static class StartSessionRequest {
        public String code;

        public StartSessionRequest(String code) {
            this.code = code;
        }
    }

    public static class SubmitAnswerRequest {
        public int questionId;
        public String answer;

        public SubmitAnswerRequest(int questionId, String answer) {
            this.questionId = questionId;
            this.answer = answer;
        }
    }

    public static class QuestionDto {
        public int id;
        public int number;
        public String text;
    }

    public static class StartSessionResponse {
        public int sessionId;
        public String language;
        public String topic;
        public int totalQuestions;
        public QuestionDto question;
    }

    public static class AnswerResponse {
        public int score;
        public String verdict;
        public String feedback;
        public int questionsAnswered;
        public int totalQuestions;
        public QuestionDto nextQuestion;
    }

    public static class FinishResponse {
        public int sessionId;
        public String language;
        public String topic;
        public int overallScore;
        public int questionsAnswered;
        public int totalQuestions;
        public String feedback;

    }

    public static class TranscriptMessage {
        public String from;
        public String text;
    }

    public static class SessionStateResponse {
        public int sessionId;
        public String language;
        public String topic;
        public boolean finished;
        public int totalQuestions;
        public int questionsAnswered;
        public QuestionDto currentQuestion;
        public List<TranscriptMessage> transcript;
    }
}
