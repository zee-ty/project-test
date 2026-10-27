package com.itmbaq1.itmda_groupproj;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ProgressResponse {

    @SerializedName("scoreHistory")
    public List<ScoreHistoryPoint> scoreHistory;

    @SerializedName("languageAverages")
    public List<LanguageAverage> languageAverages;

    @SerializedName("improvementPercentage")
    public Double improvementPercentage;

    @SerializedName("sessionHistory")
    public List<SessionHistoryItem> sessionHistory;


    public static class ScoreHistoryPoint {

        @SerializedName("sessionId")
        public int sessionId;

        @SerializedName("completedAt")
        public String completedAt;

        @SerializedName("score")
        public Double score;
    }


    public static class LanguageAverage {

        @SerializedName("language")
        public String language;

        @SerializedName("averageScore")
        public Double averageScore;
    }


    public static class SessionHistoryItem {

        @SerializedName("sessionId")
        public int sessionId;

        @SerializedName("language")
        public String language;

        @SerializedName("topic")
        public String topic;

        @SerializedName("score")
        public Integer score;

        @SerializedName("questionsAnswered")
        public int questionsAnswered;

        @SerializedName("submittedAt")
        public String submittedAt;

        @SerializedName("completedAt")
        public String completedAt;
    }
}