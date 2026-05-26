package com.dhh.quizbyai.models;

public class PlayerModel {
    private String name;
    private int score;
    private int answered;

    // Bắt buộc phải có constructor rỗng cho Firebase
    public PlayerModel() {
    }

    public PlayerModel(String name, int score, int answered) {
        this.name = name;
        this.score = score;
        this.answered = answered;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getAnswered() { return answered; }
    public void setAnswered(int answered) { this.answered = answered; }
}