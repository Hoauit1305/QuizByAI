package com.dhh.quizbyai.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dhh.quizbyai.R;
import com.dhh.quizbyai.models.PlayerModel;
import com.dhh.quizbyai.models.QuestionModel;
import com.dhh.quizbyai.models.QuizModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
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

    // MẢNG LƯU LẠI LỊCH SỬ CHỌN ĐÁP ÁN CỦA NGƯỜI DÙNG ĐỂ REVIEW
    private List<String> userAnswersList = new ArrayList<>();

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

        // Ghi nhận đáp án người dùng chọn vào danh sách
        userAnswersList.add(selectedOption.trim());

        boolean isCorrect = isAnswerMatch(selectedOption, correctAnswer);
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
        // Tính điểm theo %
        int liveScore = (questionList.size() > 0) ? Math.round(((float) correctAnswersCount / questionList.size()) * 100) : 0;

        updates.put("score", liveScore);
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

        // Ghi nhận là bỏ trống vì hết giờ
        userAnswersList.add("");

        if (isMultiplayer && roomPin != null) updateProgressToRoom();
        moveToNextQuestionWithDelay();
    }

    private void highlightCorrectAnswer(String correctAnswer) {
        if (isAnswerMatch(btnA.getText().toString(), correctAnswer)) btnA.setBackgroundTintList(ColorStateList.valueOf(COLOR_CORRECT));
        else if (isAnswerMatch(btnB.getText().toString(), correctAnswer)) btnB.setBackgroundTintList(ColorStateList.valueOf(COLOR_CORRECT));
        else if (isAnswerMatch(btnC.getText().toString(), correctAnswer)) btnC.setBackgroundTintList(ColorStateList.valueOf(COLOR_CORRECT));
        else if (isAnswerMatch(btnD.getText().toString(), correctAnswer)) btnD.setBackgroundTintList(ColorStateList.valueOf(COLOR_CORRECT));
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
        if (countDownTimer != null) countDownTimer.cancel();

        int totalQuestions = questionList.size();
        int finalScorePercent = (totalQuestions > 0) ? Math.round(((float) correctAnswersCount / totalQuestions) * 100) : 0;

        if (isMultiplayer) {
            // NẾU LÀ NHIỀU NGƯỜI CHƠI -> BẬT BẢNG XẾP HẠNG
            showFinalLeaderboard(finalScorePercent);
        } else {
            // NẾU CHƠI SOLO -> LƯU ĐIỂM RỒI MỚI THOÁT
            saveSoloHistory(finalScorePercent);
        }
    }

    // --- HÀM LƯU LỊCH SỬ CHƠI SOLO VÀO FIREBASE ---
    // --- HÀM LƯU LỊCH SỬ CHƠI SOLO VÀO FIREBASE ---
    private void saveSoloHistory(int finalScorePercent) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || quizId == null) {
            Toast.makeText(this, "Hoàn thành! Điểm: " + finalScorePercent + "%", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        DatabaseReference quizRef = FirebaseDatabase.getInstance()
                .getReference("Quizzes")
                .child(quizId);

        Map<String, Object> attemptData = new HashMap<>();
        attemptData.put("timestamp", System.currentTimeMillis());
        attemptData.put("score", finalScorePercent);
        attemptData.put("userAnswers", userAnswersList);

        // Bước 1: Đẩy lịch sử làm bài mới nhất lên nhánh "history"
        quizRef.child("history").push().setValue(attemptData).addOnSuccessListener(aVoid -> {

            // Bước 2: Sau khi đẩy xong, đọc lại TOÀN BỘ nhánh "history" để tìm ra điểm cao nhất
            quizRef.child("history").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int maxScore = 0;

                    // Quét qua tất cả các lần làm bài (Bao gồm cả lần 1: 0% và lần 2: 60%)
                    for (DataSnapshot historySnap : snapshot.getChildren()) {
                        Integer score = historySnap.child("score").getValue(Integer.class);
                        if (score != null && score > maxScore) {
                            maxScore = score; // Cập nhật lại maxScore nếu tìm thấy số lớn hơn
                        }
                    }

                    // Bước 3: Ghi đè số lớn nhất tìm được ra ngoài node gốc
                    quizRef.child("score").setValue(maxScore);

                    Toast.makeText(QuestionActivity.this, "Đã lưu kết quả! Điểm của bạn: " + finalScorePercent + "%", Toast.LENGTH_LONG).show();
                    finish();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(QuestionActivity.this, "Đã lưu, nhưng lỗi đồng bộ điểm cao nhất!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

        }).addOnFailureListener(e -> {
            Toast.makeText(QuestionActivity.this, "Lỗi khi lưu lịch sử!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    private void showFinalLeaderboard(int myFinalScore) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_waiting_room, null);
        dialog.setContentView(sheetView);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        TextView txtTitle = sheetView.findViewById(R.id.txt_room_pin);
        TextView txtSubtitle = sheetView.findViewById(R.id.txt_player_count);
        RecyclerView rvLeaderboard = sheetView.findViewById(R.id.rv_players);
        Button btnLeave = sheetView.findViewById(R.id.btn_close_room);

        sheetView.findViewById(R.id.btn_start_multiplayer).setVisibility(View.GONE);

        txtTitle.setText("Final Results");
        txtSubtitle.setText("Waiting for everyone to finish...");
        btnLeave.setText("Save Quiz & Leave");

        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        int totalQuestions = questionList.size();

        List<PlayerModel> finalPlayers = new ArrayList<>();
        com.dhh.quizbyai.adapters.FinalLeaderboardAdapter adapter = new com.dhh.quizbyai.adapters.FinalLeaderboardAdapter(finalPlayers, totalQuestions, myUid);
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        rvLeaderboard.setAdapter(adapter);

        DatabaseReference playersRef = FirebaseDatabase.getInstance().getReference("Rooms").child(roomPin).child("players");

        playersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                finalPlayers.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    PlayerModel p = snap.getValue(PlayerModel.class);
                    if (p != null) {
                        p.setUid(snap.getKey());
                        finalPlayers.add(p);
                    }
                }
                Collections.sort(finalPlayers, (p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));
                adapter.notifyDataSetChanged();
                txtSubtitle.setText("Match Completed!");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        btnLeave.setOnClickListener(v -> {
            dialog.dismiss();
            cloneHostQuizToMyQuizzes(quizId, myFinalScore);
        });

        dialog.setOnShowListener(d -> {
            BottomSheetDialog bsd = (BottomSheetDialog) d;
            View bottomSheetInternal = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheetInternal)
                        .setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        dialog.show();
    }

    private void cloneHostQuizToMyQuizzes(String qId, int finalScore) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || qId == null) {
            finish();
            return;
        }

        String myUid = currentUser.getUid();
        DatabaseReference originalQuizRef = FirebaseDatabase.getInstance().getReference("Quizzes").child(qId);
        DatabaseReference myQuizzesRef = FirebaseDatabase.getInstance().getReference("Quizzes");

        originalQuizRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    QuizModel clonedQuiz = snapshot.getValue(QuizModel.class);

                    if (clonedQuiz != null) {
                        clonedQuiz.setCreatorId(myUid);
                        clonedQuiz.setCreatedAt(System.currentTimeMillis());
                        clonedQuiz.setTitle("[Copy] " + clonedQuiz.getTitle());
                        clonedQuiz.setScore(finalScore);

                        DatabaseReference newQuizRef = myQuizzesRef.push();
                        newQuizRef.setValue(clonedQuiz).addOnSuccessListener(aVoid -> {

                            Map<String, Object> firstAttempt = new HashMap<>();
                            firstAttempt.put("timestamp", System.currentTimeMillis());
                            firstAttempt.put("score", finalScore);
                            // Bổ sung lưu userAnswers cho trận đấu Multiplayer luôn
                            firstAttempt.put("userAnswers", userAnswersList);

                            newQuizRef.child("history").push().setValue(firstAttempt);

                            Toast.makeText(QuestionActivity.this, "Đã lưu kết quả và sao chép Quiz!", Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }
                } else {
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                finish();
            }
        });
    }
    // Hàm hỗ trợ kiểm tra xem Option người dùng bấm có khớp với Answer trên Firebase không
    private boolean isAnswerMatch(String selectedOption, String correctAnswer) {
        String cleanSelected = selectedOption.trim();
        String cleanCorrect = correctAnswer.trim();

        return cleanSelected.equalsIgnoreCase(cleanCorrect) ||
                cleanSelected.toUpperCase().startsWith(cleanCorrect.toUpperCase() + ".") ||
                cleanSelected.toUpperCase().startsWith(cleanCorrect.toUpperCase() + " ");
    }
}