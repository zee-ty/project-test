package com.itmbaq1.itmda_groupproj;

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
}
