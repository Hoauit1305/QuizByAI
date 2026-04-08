package com.dhh.quizbyai.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuizModel {
    private String title;
    private int timePerQuestion;
    private List<QuestionModel> questions; // Firebase sẽ map node "questions" vào đây
    private long createdAt;
    public QuizModel() {}
    // Getter cho createdAt gốc (nếu cần dùng số long)
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getTitle() { return title; }
    // Hàm tiện ích để lấy ngày đã định dạng
    public String getFormattedDate() {
        if (createdAt <= 0) return "N/A";

        Date date = new Date(createdAt);
        // Định dạng: May 2, 2026
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
        return sdf.format(date);
    }
    public int getTimePerQuestion() { return timePerQuestion; }
    public List<QuestionModel> getQuestions() { return questions; }

    // Hàm tiện ích để lấy số lượng câu hỏi
    public int getQuestionCount() {
        return (questions != null) ? questions.size() : 0;
    }
    //Hàm tiện ích để lấy tổng thời gian
    public int getTotalTime(){
        return getQuestionCount() * timePerQuestion;
    }
}
