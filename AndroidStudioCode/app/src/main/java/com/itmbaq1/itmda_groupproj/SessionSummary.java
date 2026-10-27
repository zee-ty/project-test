package com.itmbaq1.itmda_groupproj;

public class SessionSummary {
    public final String language, topic;
    public final int score;
    public SessionSummary(String language, String topic, int score) {
        this.language = language;
        this.topic = topic;
        this.score = score;
    }
}
