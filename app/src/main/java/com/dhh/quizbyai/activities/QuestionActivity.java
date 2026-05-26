package com.dhh.quizbyai.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dhh.quizbyai.R;
import com.dhh.quizbyai.models.QuestionModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestionActivity extends AppCompatActivity {

    private static final String TAG = "QUESTION_DEBUG";
    private TextView txtQuestionCount, txtTimer, txtQuestion;
    private Button btnA, btnB, btnC, btnD;

    private List<QuestionModel> questionList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private String quizId;
    private String roomPin;
    private boolean isMultiplayer = false;
    private int timePerQuestion = 60;

    private CountDownTimer countDownTimer;
    private boolean isAnswered = false;

    private final int COLOR_CORRECT = Color.parseColor("#4CAF50");
    private final int COLOR_WRONG = Color.parseColor("#F44336");
    private final int COLOR_DISABLED = Color.parseColor("#9E9E9E");
    private int correctAnswersCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_question);

        setupWindowInsets();
        initUI();

        // Lấy thông tin từ Intent
        quizId = getIntent().getStringExtra("QUIZ_ID");
        roomPin = getIntent().getStringExtra("ROOM_PIN");
        isMultiplayer = getIntent().getBooleanExtra("IS_MULTIPLAYER", false);

        if (quizId != null) {
            fetchQuizDataFromFirebase();
        } else {
            Toast.makeText(this, "Không tìm thấy ID bài Quiz!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initUI() {
        txtQuestionCount = findViewById(R.id.textView2);
        txtTimer = findViewById(R.id.textView16);
        txtQuestion = findViewById(R.id.textView17);
        btnA = findViewById(R.id.button2);
        btnB = findViewById(R.id.button3);
        btnC = findViewById(R.id.button4);
        btnD = findViewById(R.id.button5);
    }

    private void fetchQuizDataFromFirebase() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Quizzes").child(quizId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer fetchedTime = snapshot.child("timePerQuestion").getValue(Integer.class);
                    if (fetchedTime != null) timePerQuestion = fetchedTime;

                    DataSnapshot questionsSnapshot = snapshot.child("questions");
                    questionList.clear();
                    for (DataSnapshot qSnap : questionsSnapshot.getChildren()) {
                        QuestionModel question = qSnap.getValue(QuestionModel.class);
                        if (question != null) questionList.add(question);
                    }

                    if (!questionList.isEmpty()) {
                        loadQuestion(0);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadQuestion(int index) {
        if (index >= questionList.size()) {
            finishQuiz();
            return;
        }

        currentQuestionIndex = index;
        isAnswered = false;
        QuestionModel currentQ = questionList.get(index);

        txtQuestionCount.setText("Question " + (index + 1) + " of " + questionList.size());
        txtQuestion.setText(currentQ.getQuestion());

        List<String> options = currentQ.getOptions();
        if (options != null && options.size() >= 4) {
            btnA.setText(options.get(0));
            btnB.setText(options.get(1));
            btnC.setText(options.get(2));
            btnD.setText(options.get(3));
        }

        resetButtonStates();

        btnA.setOnClickListener(v -> handleUserAnswer(btnA, btnA.getText().toString(), currentQ.getAnswer()));
        btnB.setOnClickListener(v -> handleUserAnswer(btnB, btnB.getText().toString(), currentQ.getAnswer()));
        btnC.setOnClickListener(v -> handleUserAnswer(btnC, btnC.getText().toString(), currentQ.getAnswer()));
        btnD.setOnClickListener(v -> handleUserAnswer(btnD, btnD.getText().toString(), currentQ.getAnswer()));

        startTimer();
    }

    private void handleUserAnswer(Button selectedBtn, String selectedOption, String correctAnswer) {
        if (isAnswered) return;
        isAnswered = true;

        if (countDownTimer != null) countDownTimer.cancel();

        disableAllButtons();
        setAllButtonsColor(COLOR_DISABLED);

        boolean isCorrect = selectedOption.trim().equalsIgnoreCase(correctAnswer.trim());
        if (isCorrect) {
            selectedBtn.setBackgroundTintList(ColorStateList.valueOf(COLOR_CORRECT));
            correctAnswersCount++;
        } else {
            selectedBtn.setBackgroundTintList(ColorStateList.valueOf(COLOR_WRONG));
            highlightCorrectAnswer(correctAnswer);
        }

        // --- CẬP NHẬT REALTIME LÊN LEADERBOARD ---
        if (isMultiplayer && roomPin != null) {
            updateProgressToRoom();
        }

        moveToNextQuestionWithDelay();
    }

    private void updateProgressToRoom() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference playerRef = FirebaseDatabase.getInstance().getReference("Rooms")
                .child(roomPin)
                .child("players")
                .child(uid);

        Map<String, Object> updates = new HashMap<>();
        // Tính điểm theo % hoặc số câu đúng tùy bạn, ở đây tôi tính theo số câu đúng
        updates.put("score", correctAnswersCount); 
        updates.put("answered", currentQuestionIndex + 1);

        playerRef.updateChildren(updates).addOnFailureListener(e -> 
            Log.e(TAG, "Lỗi cập nhật Leaderboard: " + e.getMessage())
        );
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(timePerQuestion * 1000L, 1000) {
            @Override
            public void onTick(long l) { txtTimer.setText((l / 1000) + "s"); }
            @Override
            public void onFinish() {
                if (!isAnswered) {
                    handleTimeout(questionList.get(currentQuestionIndex).getAnswer());
                }
            }
        }.start();
    }

    private void handleTimeout(String correctAnswer) {
        isAnswered = true;
        disableAllButtons();
        setAllButtonsColor(COLOR_DISABLED);
        highlightCorrectAnswer(correctAnswer);
        if (isMultiplayer && roomPin != null) updateProgressToRoom();
        moveToNextQuestionWithDelay();
    }

    private void highlightCorrectAnswer(String correctAnswer) {
        String cleanCorrect = correctAnswer.trim();
        if (btnA.getText().toString().trim().equalsIgnoreCase(cleanCorrect)) btnA.setBackgroundTintList(ColorStateList.valueOf(COLOR_CORRECT));
        else if (btnB.getText().toString().trim().equalsIgnoreCase(cleanCorrect)) btnB.setBackgroundTintList(ColorStateList.valueOf(COLOR_CORRECT));
        else if (btnC.getText().toString().trim().equalsIgnoreCase(cleanCorrect)) btnC.setBackgroundTintList(ColorStateList.valueOf(COLOR_CORRECT));
        else if (btnD.getText().toString().trim().equalsIgnoreCase(cleanCorrect)) btnD.setBackgroundTintList(ColorStateList.valueOf(COLOR_CORRECT));
    }

    private void moveToNextQuestionWithDelay() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> loadQuestion(currentQuestionIndex + 1), 1500);
    }

    private void resetButtonStates() {
        int defaultColor = ContextCompat.getColor(this, R.color.primary_orange);
        setAllButtonsColor(defaultColor);
        btnA.setEnabled(true);
        btnB.setEnabled(true);
        btnC.setEnabled(true);
        btnD.setEnabled(true);
    }

    private void disableAllButtons() {
        btnA.setEnabled(false); btnB.setEnabled(false); btnC.setEnabled(false); btnD.setEnabled(false);
    }

    private void setAllButtonsColor(int color) {
        btnA.setBackgroundTintList(ColorStateList.valueOf(color));
        btnB.setBackgroundTintList(ColorStateList.valueOf(color));
        btnC.setBackgroundTintList(ColorStateList.valueOf(color));
        btnD.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    private void finishQuiz() {
        Toast.makeText(this, "Quiz Finished! Your score: " + correctAnswersCount, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
