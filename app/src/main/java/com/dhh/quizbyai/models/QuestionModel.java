package com.dhh.quizbyai.models;

import java.util.List;

public class QuestionModel {
    private String question;
    private List<String> options;
    private String answer;

    public QuestionModel() { } // Cần có constructor rỗng cho Firebase

    public QuestionModel(String question, List<String> options, String answer) {
        this.question = question;
        this.options = options;
        this.answer = answer;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
    public void setAnswer(String answer) {
        this.answer = answer;
    }
    public void setOptions(List<String> options) {
        this.options = options;
    }
    public String getQuestion() { return question; }
    public List<String> getOptions() { return options; }
    public String getAnswer() { return answer; }
}
