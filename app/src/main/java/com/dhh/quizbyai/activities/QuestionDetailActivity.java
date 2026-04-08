package com.dhh.quizbyai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dhh.quizbyai.R;
import com.dhh.quizbyai.models.QuestionModel;
import com.dhh.quizbyai.models.QuizModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuestionDetailActivity extends AppCompatActivity {
    ImageView img_avt_quiz;
    TextView txt_name_quiz, txt_quiz_day, txt_NoQ, txt_total_time, txt_time_perQ;
    Button btn_start_quiz;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_question_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        initViews();

        String quiz_ID = getIntent().getStringExtra("QUIZ_ID");

//        String quiz_ID = "-OpSHYDiA6rF6Ze-47MQ";
        loadQuestionDetail(quiz_ID);

        startQuiz(quiz_ID);
    }
    protected void initViews(){
        img_avt_quiz = findViewById(R.id.img_avt_quiz);
        txt_name_quiz = findViewById(R.id.txt_name_quiz);
        txt_quiz_day = findViewById(R.id.txt_quiz_day);
        txt_NoQ = findViewById(R.id.txt_NoQ);
        txt_total_time = findViewById(R.id.txt_totalTime);
        txt_time_perQ = findViewById(R.id.txt_timePerQ);
        btn_start_quiz = findViewById(R.id.btn_start);
    }

    protected void loadQuestionDetail(String quizID){
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Quizzes")
                .child(quizID);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // 1. Lấy đối tượng Quiz
                    QuizModel quiz = snapshot.getValue(QuizModel.class);

                    if (quiz != null) {
                        // 2. Truy xuất các thông tin cần
                        txt_name_quiz.setText(quiz.getTitle());
                        txt_quiz_day.setText(quiz.getFormattedDate());
                        txt_NoQ.setText(String.valueOf(quiz.getQuestionCount()));
                        txt_time_perQ.setText(quiz.getTimePerQuestion() + "s");
                        txt_total_time.setText(quiz.getTotalTime() + "s");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("QUIZ_DEBUG", "Lỗi Firebase: " + error.getMessage());
            }
        });
    }
    protected void startQuiz(String quizID){
        btn_start_quiz.setOnClickListener(v -> {
            Intent intent = new Intent(QuestionDetailActivity.this, QuestionActivity.class);
            intent.putExtra("QUIZ_ID", quizID);
            startActivity(intent);
            finish();
        });
    }
}