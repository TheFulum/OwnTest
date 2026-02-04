package com.example.owntest.models;

import java.util.List;
import java.util.ArrayList;

public class Test {
    private String testId;
    private String title;
    private String description;
    private String iconUrl;
    private String difficulty; // Easy, Medium, Hard
    private int questionCount;
    private int answerOptionsCount; // 2, 3, or 4
    private String creatorId;
    private String creatorNickname;
    private long createdDate;

    // Статистика
    private double averageRating;   // средний рейтинг в звёздах (1-5)
    private int completionsCount;
    private double totalRatings;    // сумма звёзд для расчёта среднего

    private List<Question> questions;

    public Test() {
        this.questions = new ArrayList<>();
    }

    public Test(String testId, String title, String description, String iconUrl,
                String difficulty, int questionCount, int answerOptionsCount,
                String creatorId, String creatorNickname) {
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
        this.averageRating = 0.0;
        this.completionsCount = 0;
        this.totalRatings = 0.0;
        this.questions = new ArrayList<>();
    }

    // Геттеры и сеттеры
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
    public double getAverageRating() { return averageRating; }
    public int getCompletionsCount() { return completionsCount; }
    public double getTotalRatings() { return totalRatings; }
    public List<Question> getQuestions() { return questions; }

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
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public void setCompletionsCount(int completionsCount) { this.completionsCount = completionsCount; }
    public void setTotalRatings(double totalRatings) { this.totalRatings = totalRatings; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }

    /**
     * Добавляет прохождение и обновляет средний рейтинг.
     * @param rating звёзды от пользователя (1-5), берутся из ratingBar
     */
    public void addCompletion(double rating) {
        this.completionsCount++;
        this.totalRatings += rating;
        this.averageRating = this.totalRatings / this.completionsCount;
    }
}