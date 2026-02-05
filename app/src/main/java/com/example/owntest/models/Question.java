package com.example.owntest.models;

import java.util.List;
import java.util.ArrayList;

public class Question {
    private String questionId;
    private String questionText;
    private String questionDescription;
    private List<Answer> answers;
    private int correctAnswerIndex;

    // Новые поля
    private String questionType; // "CHOICE" или "TEXT"
    private int maxPoints; // Макс баллы за вопрос (для ручной проверки)

    public Question() {
        this.answers = new ArrayList<>();
        this.questionType = "CHOICE"; // По умолчанию выбор
        this.maxPoints = 1; // По умолчанию 1 балл
    }

    public Question(String questionId, String questionText, String questionDescription,
                    List<Answer> answers, int correctAnswerIndex, String questionType, int maxPoints) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.questionDescription = questionDescription;
        this.answers = answers;
        this.correctAnswerIndex = correctAnswerIndex;
        this.questionType = questionType;
        this.maxPoints = maxPoints;
    }

    // Getters
    public String getQuestionId() { return questionId; }
    public String getQuestionText() { return questionText; }
    public String getQuestionDescription() { return questionDescription; }
    public List<Answer> getAnswers() { return answers; }
    public int getCorrectAnswerIndex() { return correctAnswerIndex; }
    public String getQuestionType() { return questionType; }
    public int getMaxPoints() { return maxPoints; }

    // Setters
    public void setQuestionId(String questionId) { this.questionId = questionId; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public void setQuestionDescription(String questionDescription) { this.questionDescription = questionDescription; }
    public void setAnswers(List<Answer> answers) { this.answers = answers; }
    public void setCorrectAnswerIndex(int correctAnswerIndex) { this.correctAnswerIndex = correctAnswerIndex; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public void setMaxPoints(int maxPoints) { this.maxPoints = maxPoints; }

    public boolean isCorrectAnswer(int answerIndex) {
        return answerIndex == correctAnswerIndex;
    }

    public boolean isTextQuestion() {
        return "TEXT".equals(questionType);
    }

    public boolean isChoiceQuestion() {
        return "CHOICE".equals(questionType);
    }
}