package com.dhh.quizbyai.activities;

import android.content.SharedPreferences;
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
import com.google.common.reflect.TypeToken;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestionActivity extends AppCompatActivity {

    private TextView txtQuestionCount, txtTimer, txtQuestion;
    private Button btnA, btnB, btnC, btnD;

    private List<QuestionModel> questionList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private String quizId;
    private int timePerQuestion = 60; // Mặc định 60s, sẽ cập nhật lại từ Firebase
    private CountDownTimer countDownTimer;
    private boolean isAnswered = false; // Cờ kiểm tra xem user đã trả lời chưa

    // Định nghĩa các mã màu cho đẹp (bạn có thể thay đổi tùy ý)
    private final int COLOR_CORRECT = Color.parseColor("#4CAF50"); // Xanh lá
    private final int COLOR_WRONG = Color.parseColor("#F44336");   // Đỏ
    private final int COLOR_DISABLED = Color.parseColor("#9E9E9E"); // Xám
    private int correctAnswersCount = 0;
    private List<String> userSelectedAnswers = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_question);

        // Thiết lập viền
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initUI();

        quizId = getIntent().getStringExtra("QUIZ_ID");
        if (quizId != null) {
            fetchQuizDataFromFirebase();
        } else {
            Toast.makeText(this, "Không tìm thấy ID bài Quiz!", Toast.LENGTH_SHORT).show();
            finish();
        }
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
                    // 1. Lấy thời gian mỗi câu hỏi (nếu Firebase không có thì dùng mặc định 60s)
                    Integer fetchedTime = snapshot.child("timePerQuestion").getValue(Integer.class);
                    if (fetchedTime != null) {
                        timePerQuestion = fetchedTime;
                    }

                    // 2. Lấy danh sách câu hỏi
                    DataSnapshot questionsSnapshot = snapshot.child("questions");
                    for (DataSnapshot qSnap : questionsSnapshot.getChildren()) {
                        QuestionModel question = qSnap.getValue(QuestionModel.class);
                        if (question != null) {
                            questionList.add(question);
                        }
                    }

                    // 3. Bắt đầu câu hỏi đầu tiên
                    if (!questionList.isEmpty()) {
                        loadQuestion(0);
                    } else {
                        Toast.makeText(QuestionActivity.this, "Bài Quiz không có câu hỏi nào", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(QuestionActivity.this, "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadQuestion(int index) {
        if (index >= questionList.size()) {
            Toast.makeText(this, "Bạn đã hoàn thành bài trắc nghiệm!", Toast.LENGTH_LONG).show();
            calculateAndSaveScore(); // GỌI HÀM TÍNH ĐIỂM
            return;
        }

        isAnswered = false; // Reset lại cờ
        QuestionModel currentQ = questionList.get(index);

        // Hiển thị nội dung câu hỏi
        txtQuestionCount.setText("Question " + (index + 1) + " of " + questionList.size());
        txtQuestion.setText(currentQ.getQuestion());

        List<String> options = currentQ.getOptions();
        if (options != null && options.size() >= 4) {
            btnA.setText(options.get(0));
            btnB.setText(options.get(1));
            btnC.setText(options.get(2));
            btnD.setText(options.get(3));
        }

        // Đặt lại màu sắc và trạng thái nút
        resetButtonStates();

        // Gắn sự kiện click
        btnA.setOnClickListener(v -> handleUserAnswer(btnA, btnA.getText().toString(), currentQ.getAnswer()));
        btnB.setOnClickListener(v -> handleUserAnswer(btnB, btnB.getText().toString(), currentQ.getAnswer()));
        btnC.setOnClickListener(v -> handleUserAnswer(btnC, btnC.getText().toString(), currentQ.getAnswer()));
        btnD.setOnClickListener(v -> handleUserAnswer(btnD, btnD.getText().toString(), currentQ.getAnswer()));

        // Bắt đầu đếm ngược thời gian
        startTimer();
    }

    private void startTimer() {
        // Hủy timer cũ nếu có
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        txtTimer.setTextColor(ContextCompat.getColor(this, R.color.primary_orange)); // Đặt lại màu cam cho chữ

        // CountDownTimer(tổng thời gian mili giây, khoảng thời gian đếm mili giây)
        countDownTimer = new CountDownTimer(timePerQuestion * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                txtTimer.setText((millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                // KHI HẾT GIỜ (TIMEOUT)
                if (!isAnswered) {
                    txtTimer.setText("0s");
                    txtTimer.setTextColor(COLOR_WRONG); // Đổi text thành màu đỏ cảnh báo
                    handleTimeout(questionList.get(currentQuestionIndex).getAnswer());
                }
            }
        }.start();
    }

    private void handleUserAnswer(Button selectedBtn, String selectedOption, String correctAnswer) {
        if (isAnswered) return;
        isAnswered = true; // Đánh dấu đã trả lời
        userSelectedAnswers.add(selectedOption.trim());
        // Dừng đếm thời gian
        if (countDownTimer != null) countDownTimer.cancel();

        disableAllButtons(); // Khóa nút
        setAllButtonsColor(COLOR_DISABLED); // Chuyển tất cả thành màu Xám

        // Chuẩn hóa chuỗi (xóa khoảng trắng thừa) để so sánh chính xác
        String cleanSelected = selectedOption.trim();
        String cleanCorrect = correctAnswer.trim();

        if (cleanSelected.equals(cleanCorrect)) {
            // TRẢ LỜI ĐÚNG
            selectedBtn.setBackgroundTintList(ColorStateList.valueOf(COLOR_CORRECT));
            correctAnswersCount++;
        } else {
            // TRẢ LỜI SAI
            selectedBtn.setBackgroundTintList(ColorStateList.valueOf(COLOR_WRONG));
            highlightCorrectAnswer(cleanCorrect); // Hiện đáp án đúng
        }

        // Đợi 1.5 giây rồi qua câu tiếp theo
        moveToNextQuestionWithDelay();
    }

    private void handleTimeout(String correctAnswer) {
        isAnswered = true;
        userSelectedAnswers.add("");
        disableAllButtons();
        setAllButtonsColor(COLOR_DISABLED);

        // Hiện đáp án đúng màu xanh để user biết
        highlightCorrectAnswer(correctAnswer.trim());

        Toast.makeText(this, "Hết giờ!", Toast.LENGTH_SHORT).show();

        // Đợi 2 giây (lâu hơn xíu để user nhìn) rồi qua câu tiếp theo
        moveToNextQuestionWithDelay();
    }

    private void highlightCorrectAnswer(String correctAnswer) {
        if (btnA.getText().toString().trim().equals(correctAnswer)) {
            btnA.setBackgroundTintList(ColorStateList.valueOf(COLOR_CORRECT));
        } else if (btnB.getText().toString().trim().equals(correctAnswer)) {
            btnB.setBackgroundTintList(ColorStateList.valueOf(COLOR_CORRECT));
        } else if (btnC.getText().toString().trim().equals(correctAnswer)) {
            btnC.setBackgroundTintList(ColorStateList.valueOf(COLOR_CORRECT));
        } else if (btnD.getText().toString().trim().equals(correctAnswer)) {
            btnD.setBackgroundTintList(ColorStateList.valueOf(COLOR_CORRECT));
        }
    }

    private void moveToNextQuestionWithDelay() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            currentQuestionIndex++;
            loadQuestion(currentQuestionIndex);
        }, 1500);
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
        btnA.setEnabled(false);
        btnB.setEnabled(false);
        btnC.setEnabled(false);
        btnD.setEnabled(false);
    }

    private void setAllButtonsColor(int color) {
        btnA.setBackgroundTintList(ColorStateList.valueOf(color));
        btnB.setBackgroundTintList(ColorStateList.valueOf(color));
        btnC.setBackgroundTintList(ColorStateList.valueOf(color));
        btnD.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cần hủy Timer khi thoát Activity để tránh lỗi memory leak hoặc app crash ngầm
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
    private void calculateAndSaveScore() {
        int totalQuestions = questionList.size();
        int finalScore = Math.round(((float) correctAnswersCount / totalQuestions) * 100);

        DatabaseReference ref = FirebaseDatabase.getInstance("https://quizbyai-4d9d2-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Quizzes").child(quizId);

        // Tạo cục dữ liệu Lịch sử bao gồm: thời gian, điểm, và mảng đáp án đã chọn
        java.util.Map<String, Object> attempt = new java.util.HashMap<>();
        attempt.put("timestamp", System.currentTimeMillis());
        attempt.put("score", finalScore);
        attempt.put("userAnswers", userSelectedAnswers);

        // Đẩy vào nhánh "history"
        ref.child("history").push().setValue(attempt).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // (Tùy chọn) Vẫn cập nhật điểm cao nhất ở ngoài node chính
                ref.child("score").setValue(finalScore);
                Toast.makeText(this, "Điểm của bạn: " + finalScore + "%", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Lỗi lưu điểm", Toast.LENGTH_SHORT).show();
            }
            finish();
        });
    }
}