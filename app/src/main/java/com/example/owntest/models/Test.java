package com.example.owntest.models;

import java.util.List;
import java.util.ArrayList;

public class Test {
    private String testId;
    private String title;
    private String description;
    private String iconUrl;
    private String difficulty;
    private int questionCount;
    private int answerOptionsCount;
    private String creatorId;
    private String creatorNickname;
    private long createdDate;

    // Новое поле
    private boolean isManualCheck; // Ручная проверка теста
    private boolean allowMultipleAttempts; // Можно ли проходить несколько раз

    private double averageRating;
    private int completionsCount;
    private double totalRatings;

    private List<Question> questions;

    public Test() {
        this.questions = new ArrayList<>();
        this.isManualCheck = false; // По умолчанию автопроверка
        this.allowMultipleAttempts = true; // По умолчанию можно проходить несколько раз
    }

    public Test(String testId, String title, String description, String iconUrl,
                String difficulty, int questionCount, int answerOptionsCount,
                String creatorId, String creatorNickname, boolean isManualCheck,
                boolean allowMultipleAttempts) {
        this.testId = testId;
        this.title = title;
        this.description = description;
        this.iconUrl = iconUrl;
        this.difficulty = difficulty;
        this.questionCount = questionCount;
        this.answerOptionsCount = answerOptionsCount;
        this.creatorId = creatorId;
        this.creatorNickname = creatorNickname;
        this.createdDate = System.currentTimeMillis();
        this.isManualCheck = isManualCheck;
        this.allowMultipleAttempts = allowMultipleAttempts;
        this.averageRating = 0.0;
        this.completionsCount = 0;
        this.totalRatings = 0.0;
        this.questions = new ArrayList<>();
    }

    // Getters
    public String getTestId() { return testId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getIconUrl() { return iconUrl; }
    public String getDifficulty() { return difficulty; }
    public int getQuestionCount() { return questionCount; }
    public int getAnswerOptionsCount() { return answerOptionsCount; }
    public String getCreatorId() { return creatorId; }
    public String getCreatorNickname() { return creatorNickname; }
    public long getCreatedDate() { return createdDate; }
    public boolean isManualCheck() { return isManualCheck; }
    public boolean isAllowMultipleAttempts() { return allowMultipleAttempts; }
    public double getAverageRating() { return averageRating; }
    public int getCompletionsCount() { return completionsCount; }
    public double getTotalRatings() { return totalRatings; }
    public List<Question> getQuestions() { return questions; }

    // Setters
    public void setTestId(String testId) { this.testId = testId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
    public void setAnswerOptionsCount(int answerOptionsCount) { this.answerOptionsCount = answerOptionsCount; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public void setCreatorNickname(String creatorNickname) { this.creatorNickname = creatorNickname; }
    public void setCreatedDate(long createdDate) { this.createdDate = createdDate; }
    public void setManualCheck(boolean manualCheck) { isManualCheck = manualCheck; }
    public void setAllowMultipleAttempts(boolean allowMultipleAttempts) { this.allowMultipleAttempts = allowMultipleAttempts; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public void setCompletionsCount(int completionsCount) { this.completionsCount = completionsCount; }
    public void setTotalRatings(double totalRatings) { this.totalRatings = totalRatings; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }

    public void addCompletion(double rating) {
        this.completionsCount++;
        this.totalRatings += rating;
        this.averageRating = this.totalRatings / this.completionsCount;
    }

    // Подсчет максимальных баллов (для тестов с ручной проверкой)
    public int getTotalMaxPoints() {
        int total = 0;
        for (Question q : questions) {
            total += q.getMaxPoints();
        }
        return total;
    }
}