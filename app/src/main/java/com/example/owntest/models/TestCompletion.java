package com.example.owntest.models;

import java.util.Map;
import java.util.HashMap;

public class TestCompletion {
    private String completionId;
    private String userId;
    private String testId;
    private int score;
    private int totalQuestions;
    private double percentage;
    private long completedDate;
    private Map<String, Integer> userAnswers; // questionId -> answerIndex (для choice)

    // Новые поля
    private Map<String, String> textAnswers; // questionId -> text (для текстовых ответов)
    private String checkStatus; // "AUTO", "PENDING", "CHECKED"
    private int earnedPoints; // Баллы выставленные создателем (для ручной проверки)
    private int maxPoints; // Макс возможные баллы

    private boolean isCompleted;
    private int currentQuestionIndex;
    private int userRating;
    private long lastUpdated;

    public TestCompletion() {
        this.userAnswers = new HashMap<>();
        this.textAnswers = new HashMap<>();
        this.isCompleted = false;
        this.currentQuestionIndex = 0;
        this.userRating = 0;
        this.lastUpdated = System.currentTimeMillis();
        this.checkStatus = "AUTO";
        this.earnedPoints = 0;
        this.maxPoints = 0;
    }

    public TestCompletion(String completionId, String userId, String testId,
                          int score, int totalQuestions, Map<String, Integer> userAnswers,
                          Map<String, String> textAnswers, String checkStatus, int maxPoints) {
        this.completionId = completionId;
        this.userId = userId;
        this.testId = testId;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.percentage = (double) score / totalQuestions * 100;
        this.completedDate = System.currentTimeMillis();
        this.userAnswers = userAnswers;
        this.textAnswers = textAnswers != null ? textAnswers : new HashMap<>();
        this.checkStatus = checkStatus;
        this.maxPoints = maxPoints;
        this.earnedPoints = 0;
        this.isCompleted = true;
        this.currentQuestionIndex = totalQuestions;
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
    public Map<String, String> getTextAnswers() { return textAnswers; }
    public String getCheckStatus() { return checkStatus; }
    public int getEarnedPoints() { return earnedPoints; }
    public int getMaxPoints() { return maxPoints; }
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
    public void setTextAnswers(Map<String, String> textAnswers) { this.textAnswers = textAnswers; }
    public void setCheckStatus(String checkStatus) { this.checkStatus = checkStatus; }
    public void setEarnedPoints(int earnedPoints) { this.earnedPoints = earnedPoints; }
    public void setMaxPoints(int maxPoints) { this.maxPoints = maxPoints; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    public void setCurrentQuestionIndex(int currentQuestionIndex) { this.currentQuestionIndex = currentQuestionIndex; }
    public void setUserRating(int userRating) { this.userRating = userRating; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    // Проверка статуса
    public boolean isPending() { return "PENDING".equals(checkStatus); }
    public boolean isChecked() { return "CHECKED".equals(checkStatus); }
    public boolean isAuto() { return "AUTO".equals(checkStatus); }
}