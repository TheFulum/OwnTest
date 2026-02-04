package com.example.owntest.models;

public class Answer {
    private String answerId;
    private String answerText;

    public Answer() {
        // Пустой конструктор для Firebase
    }

    public Answer(String answerId, String answerText) {
        this.answerId = answerId;
        this.answerText = answerText;
    }

    // Getters
    public String getAnswerId() { return answerId; }
    public String getAnswerText() { return answerText; }

    // Setters
    public void setAnswerId(String answerId) { this.answerId = answerId; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
}