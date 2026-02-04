package com.example.owntest.models;

import java.util.List;
import java.util.ArrayList;

public class Question {
    private String questionId;
    private String questionText;
    private String questionDescription; // Может быть пустым
    private List<Answer> answers;
    private int correctAnswerIndex; // Индекс правильного ответа

    public Question() {
        // Пустой конструктор для Firebase
        this.answers = new ArrayList<>();
    }

    public Question(String questionId, String questionText, String questionDescription,
                    List<Answer> answers, int correctAnswerIndex) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.questionDescription = questionDescription;
        this.answers = answers;
        this.correctAnswerIndex = correctAnswerIndex;
    }

    // Getters
    public String getQuestionId() { return questionId; }
    public String getQuestionText() { return questionText; }
    public String getQuestionDescription() { return questionDescription; }
    public List<Answer> getAnswers() { return answers; }
    public int getCorrectAnswerIndex() { return correctAnswerIndex; }

    // Setters
    public void setQuestionId(String questionId) { this.questionId = questionId; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public void setQuestionDescription(String questionDescription) { this.questionDescription = questionDescription; }
    public void setAnswers(List<Answer> answers) { this.answers = answers; }
    public void setCorrectAnswerIndex(int correctAnswerIndex) { this.correctAnswerIndex = correctAnswerIndex; }

    // Проверка ответа
    public boolean isCorrectAnswer(int answerIndex) {
        return answerIndex == correctAnswerIndex;
    }
}