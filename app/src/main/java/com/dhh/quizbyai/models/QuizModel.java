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

    // --- CÁC BIẾN MỚI BỔ SUNG CHO TÍNH NĂNG CLONE VÀ LƯU ĐIỂM ---
    private String creatorId;
    private int score;
    private String topic_emoji;

    public QuizModel() {}

    // --- GETTER & SETTER CHO CÁC BIẾN MỚI ---
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getTopic_emoji() { return topic_emoji; }
    public void setTopic_emoji(String topic_emoji) { this.topic_emoji = topic_emoji; }

    // --- GETTER & SETTER CHO CÁC BIẾN CŨ ---
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getTimePerQuestion() { return timePerQuestion; }
    public void setTimePerQuestion(int timePerQuestion) { this.timePerQuestion = timePerQuestion; }

    public List<QuestionModel> getQuestions() { return questions; }
    public void setQuestions(List<QuestionModel> questions) { this.questions = questions; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // --- CÁC HÀM TIỆN ÍCH (Giữ nguyên của bạn) ---
    public String getFormattedDate() {
        if (createdAt <= 0) return "N/A";
        Date date = new Date(createdAt);
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
        return sdf.format(date);
    }

    public int getQuestionCount() {
        return (questions != null) ? questions.size() : 0;
    }

    public String getTotalTime(){
        int totalTime = getQuestionCount() * timePerQuestion;
        int min = totalTime / 60;
        int sec = totalTime % 60;
        String result = "";

        if(min != 0){
            result +=  min + "m";
        }
        if(sec != 0){
            result += sec + "s";
        }
        return result;
    }
}