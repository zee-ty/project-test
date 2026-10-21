package com.itmbaq1.itmda_groupproj;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DashboardResponse {

    @SerializedName("averageScore")
    public Double averageScore;

    @SerializedName("sessionsCompleted")
    public int sessionsCompleted;

    @SerializedName("questionsAnswered")
    public int questionsAnswered;

    @SerializedName("favouriteLanguage")
    public String favouriteLanguage;

    @SerializedName("recentSessions")
    public List<RecentSession> recentSessions;

    public static class RecentSession {

        @SerializedName("sessionId")
        public int sessionId;

        @SerializedName("language")
        public String language;

        @SerializedName("topic")
        public String topic;

        @SerializedName("score")
        public Integer score;

        @SerializedName("submittedAt")
        public String submittedAt;
    }
}
