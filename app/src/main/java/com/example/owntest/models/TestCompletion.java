package com.example.owntest.models;

import java.util.Map;
import java.util.HashMap;

public class TestCompletion {
    private String completionId;
    private String userId;
    private String testId;
    private int score; // Количество правильных ответов
    private int totalQuestions;
    private double percentage; // Процент правильных ответов
    private long completedDate;
    private Map<String, Integer> userAnswers; // questionId -> answerIndex

    // Новые поля
    private boolean isCompleted; // Тест завершен или в процессе
    private int currentQuestionIndex; // На каком вопросе остановился
    private int userRating; // Рейтинг от 1 до 5 звезд (0 если еще не поставлен)
    private long lastUpdated; // Когда последний раз обновлялся прогресс

    public TestCompletion() {
        // Пустой конструктор для Firebase
        this.userAnswers = new HashMap<>();
        this.isCompleted = false;
        this.currentQuestionIndex = 0;
        this.userRating = 0;
        this.lastUpdated = System.currentTimeMillis();
    }

    public TestCompletion(String completionId, String userId, String testId,
                          int score, int totalQuestions, Map<String, Integer> userAnswers) {
        this.completionId = completionId;
        this.userId = userId;
        this.testId = testId;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.percentage = (double) score / totalQuestions * 100;
        this.completedDate = System.currentTimeMillis();
        this.userAnswers = userAnswers;
        this.isCompleted = true; // По умолчанию завершен
        this.currentQuestionIndex = totalQuestions; // Все вопросы пройдены
        this.userRating = 0;
        this.lastUpdated = System.currentTimeMillis();
    }

    // Getters
    public String getCompletionId() { return completionId; }
    public String getUserId() { return userId; }
    public String getTestId() { return testId; }
    public int getScore() { return score; }
    public int getTotalQuestions() { return totalQuestions; }
    public double getPercentage() { return percentage; }
    public long getCompletedDate() { return completedDate; }
    public Map<String, Integer> getUserAnswers() { return userAnswers; }
    public boolean isCompleted() { return isCompleted; }
    public int getCurrentQuestionIndex() { return currentQuestionIndex; }
    public int getUserRating() { return userRating; }
    public long getLastUpdated() { return lastUpdated; }

    // Setters
    public void setCompletionId(String completionId) { this.completionId = completionId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTestId(String testId) { this.testId = testId; }
    public void setScore(int score) { this.score = score; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
    public void setCompletedDate(long completedDate) { this.completedDate = completedDate; }
    public void setUserAnswers(Map<String, Integer> userAnswers) { this.userAnswers = userAnswers; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    public void setCurrentQuestionIndex(int currentQuestionIndex) { this.currentQuestionIndex = currentQuestionIndex; }
    public void setUserRating(int userRating) { this.userRating = userRating; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
}