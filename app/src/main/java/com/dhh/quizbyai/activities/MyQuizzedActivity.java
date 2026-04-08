package com.dhh.quizbyai.activities;

import android.content.Intent;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MyQuizzedActivity extends BaseActivity {

    private LinearLayout quizListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_quizzed);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        setupBottomNavigation();

        quizListContainer = findViewById(R.id.quiz_list_container);

        // Gọi hàm load dữ liệu
        loadMyQuizzes();
    }

    private void loadMyQuizzes() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance("https://quizbyai-4d9d2-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Quizzes");

        // Lấy dữ liệu (có thể lọc theo creatorId nếu team bạn đã học)
        ref.orderByChild("creatorId").equalTo(currentUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                quizListContainer.removeAllViews(); // Xoá danh sách cũ trước khi load mới

                for (DataSnapshot quizSnap : snapshot.getChildren()) {
                    String quizId = quizSnap.getKey();
                    String title = quizSnap.child("title").getValue(String.class);
                    Long createdAt = quizSnap.child("createdAt").getValue(Long.class);
                    Integer questionCount = quizSnap.child("questionCount").getValue(Integer.class);
                    Integer score = quizSnap.child("score").getValue(Integer.class);

                    // Xử lý dữ liệu null phòng hờ
                    if (title == null) title = "Untitled Quiz";
                    if (questionCount == null) questionCount = 0;
                    if (score == null) score = 0;

                    // Format ngày tháng (vd: May 2, 2026)
                    String dateStr = "Unknown Date";
                    if (createdAt != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
                        dateStr = sdf.format(new Date(createdAt));
                    }

                    String infoText = dateStr + " • " + questionCount + " Qs";

                    // Gọi hàm tạo Giao diện cho từng Quiz
                    addQuizItemToView(quizId, title, infoText, score);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyQuizzedActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addQuizItemToView(String quizId, String title, String info, int score) {
        // Inflate layout item_quiz_layout.xml
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_quiz_layout, quizListContainer, false);

        // Ánh xạ UI trong item
        TextView txtTitle = itemView.findViewById(R.id.txt_quiz_title);
        TextView txtInfo = itemView.findViewById(R.id.txt_quiz_info);
        TextView txtScore = itemView.findViewById(R.id.txt_quiz_score);

        // Gán dữ liệu
        txtTitle.setText(title);
        txtInfo.setText(info);
        txtScore.setText(score + "%");

        // Set sự kiện khi nhấn vào cái khung xám này
        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(MyQuizzedActivity.this, QuestionDetailActivity.class);
            intent.putExtra("QUIZ_ID", quizId); // Truyền ID sang màn hình QuestionDetail
            startActivity(intent);
        });

        // Thêm view này vào ScrollView
        quizListContainer.addView(itemView);
    }
}