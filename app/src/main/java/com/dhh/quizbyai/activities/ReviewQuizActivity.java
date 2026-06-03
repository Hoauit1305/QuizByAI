package com.dhh.quizbyai.activities;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dhh.quizbyai.R;
import com.dhh.quizbyai.models.QuestionModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ReviewQuizActivity extends AppCompatActivity {

    private LinearLayout containerReviewQuestions;
    private String quizId;
    private List<String> userAnswers = new ArrayList<>();
    private List<QuestionModel> questionList = new ArrayList<>();

    // Bảng màu
    private final int COLOR_CORRECT = Color.parseColor("#4CAF50"); // Xanh lá
    private final int COLOR_WRONG = Color.parseColor("#F44336");   // Đỏ
    private final int COLOR_DEFAULT = Color.parseColor("#9E9E9E"); // Xám mặc định

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_review_quiz);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        containerReviewQuestions = findViewById(R.id.container_review_questions);

        // Nhận dữ liệu từ Intent
        quizId = getIntent().getStringExtra("QUIZ_ID");
        if (getIntent().hasExtra("USER_ANSWERS")) {
            userAnswers = getIntent().getStringArrayListExtra("USER_ANSWERS");
        }

        loadQuestionsFromFirebase();
    }

    private void loadQuestionsFromFirebase() {
        if (quizId == null) return;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Quizzes").child(quizId).child("questions");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot qSnap : snapshot.getChildren()) {
                    QuestionModel q = qSnap.getValue(QuestionModel.class);
                    if (q != null) questionList.add(q);
                }
                renderReviewUI();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ReviewQuizActivity.this, "Lỗi tải dữ liệu!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderReviewUI() {
        LayoutInflater inflater = LayoutInflater.from(this);

        // Nếu bản test cũ không có mảng userAnswers, phòng hờ lỗi
        if (userAnswers == null) userAnswers = new ArrayList<>();

        for (int i = 0; i < questionList.size(); i++) {
            QuestionModel q = questionList.get(i);
            View reviewView = inflater.inflate(R.layout.item_review_question, containerReviewQuestions, false);

            TextView txtQuestion = reviewView.findViewById(R.id.txt_review_question);
            TextView txtA = reviewView.findViewById(R.id.txt_review_a);
            TextView txtB = reviewView.findViewById(R.id.txt_review_b);
            TextView txtC = reviewView.findViewById(R.id.txt_review_c);
            TextView txtD = reviewView.findViewById(R.id.txt_review_d);

            txtQuestion.setText("Q" + (i + 1) + ": " + q.getQuestion());

            if (q.getOptions() != null && q.getOptions().size() >= 4) {
                txtA.setText(q.getOptions().get(0));
                txtB.setText(q.getOptions().get(1));
                txtC.setText(q.getOptions().get(2));
                txtD.setText(q.getOptions().get(3));
            }

            // Đặt màu mặc định là Xám cho tất cả
            txtA.setBackgroundTintList(ColorStateList.valueOf(COLOR_DEFAULT));
            txtB.setBackgroundTintList(ColorStateList.valueOf(COLOR_DEFAULT));
            txtC.setBackgroundTintList(ColorStateList.valueOf(COLOR_DEFAULT));
            txtD.setBackgroundTintList(ColorStateList.valueOf(COLOR_DEFAULT));

            // Lấy đáp án chuẩn và đáp án user đã chọn
            String correctAns = q.getAnswer().trim();
            String userAns = "";
            if (i < userAnswers.size()) {
                userAns = userAnswers.get(i).trim();
            }

            // MẢNG ÁNH XẠ NÚT ĐỂ XỬ LÝ CHO NHANH
            TextView[] optionViews = {txtA, txtB, txtC, txtD};

            // Tiến hành tô màu
            for (TextView optionView : optionViews) {
                String optionText = optionView.getText().toString().trim();

                // 1. Luôn tô xanh đáp án chuẩn (Sử dụng hàm isAnswerMatch vì correctAns có thể chỉ là "A", "B")
                if (isAnswerMatch(optionText, correctAns)) {
                    optionView.setBackgroundTintList(ColorStateList.valueOf(COLOR_CORRECT));
                }

                // 2. Nếu User chọn đáp án này, và nó KHÔNG khớp với đáp án chuẩn -> Tô đỏ
                if (optionText.equals(userAns) && !isAnswerMatch(optionText, correctAns)) {
                    optionView.setBackgroundTintList(ColorStateList.valueOf(COLOR_WRONG));
                }
            }

            containerReviewQuestions.addView(reviewView);
        }
    }
    // Hàm hỗ trợ kiểm tra chuỗi linh hoạt giống hệt bên QuestionActivity
    private boolean isAnswerMatch(String selectedOption, String correctAnswer) {
        String cleanSelected = selectedOption.trim();
        String cleanCorrect = correctAnswer.trim();

        return cleanSelected.equalsIgnoreCase(cleanCorrect) ||
                cleanSelected.toUpperCase().startsWith(cleanCorrect.toUpperCase() + ".") ||
                cleanSelected.toUpperCase().startsWith(cleanCorrect.toUpperCase() + " ");
    }
}