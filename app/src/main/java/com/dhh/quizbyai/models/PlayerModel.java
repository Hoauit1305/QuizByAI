package com.dhh.quizbyai.models;

public class PlayerModel {
    private String name;
    private int score;

    // Bắt buộc phải có constructor rỗng cho Firebase
    public PlayerModel() {
    }

    public PlayerModel(String name, int score) {
        this.name = name;
        this.score = score;
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
}