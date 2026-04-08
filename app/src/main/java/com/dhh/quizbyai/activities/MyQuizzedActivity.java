package com.dhh.quizbyai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
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
    private ImageButton btn_find;
    private EditText edt_name_quiz; // 1. Khai báo thêm EditText

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

        // 2. Ánh xạ UI
        quizListContainer = findViewById(R.id.quiz_list_container);
        btn_find = findViewById(R.id.btn_find);
        edt_name_quiz = findViewById(R.id.name_quiz);

        // 3. Xử lý sự kiện click nút Find
        btn_find.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Lấy chữ người dùng nhập và xóa khoảng trắng thừa ở 2 đầu
                String keyword = edt_name_quiz.getText().toString().trim();

                // Gọi hàm load dữ liệu và truyền từ khóa vào
                loadMyQuizzes(keyword);
            }
        });

        // 4. Gọi hàm load dữ liệu mặc định ban đầu (Truyền chuỗi rỗng để hiển thị tất cả)
        loadMyQuizzes("");
    }

    // 5. Cập nhật hàm load có thêm tham số searchKeyword
    private void loadMyQuizzes(String searchKeyword) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Quizzes");

        // LƯU Ý QUAN TRỌNG: Đổi addValueEventListener thành addListenerForSingleValueEvent
        // Để tránh việc bấm tìm kiếm nhiều lần sinh ra nhiều luồng lắng nghe chồng chéo làm lag app.
        ref.orderByChild("creatorId").equalTo(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                quizListContainer.removeAllViews(); // Xoá danh sách cũ

                boolean isFound = false; // Biến cờ để kiểm tra xem có tìm thấy kết quả nào không

                for (DataSnapshot quizSnap : snapshot.getChildren()) {
                    String title = quizSnap.child("title").getValue(String.class);
                    if (title == null) title = "Untitled Quiz";

                    // LOGIC TÌM KIẾM:
                    // Nếu từ khóa rỗng (mặc định) HOẶC title có chứa từ khóa (đổi hết về chữ thường để không phân biệt hoa/thường)
                    if (searchKeyword.isEmpty() || title.toLowerCase().contains(searchKeyword.toLowerCase())) {

                        isFound = true; // Đánh dấu là đã có ít nhất 1 kết quả được vẽ ra màn hình

                        String quizId = quizSnap.getKey();
                        Long createdAt = quizSnap.child("createdAt").getValue(Long.class);
                        Integer questionCount = quizSnap.child("questionCount").getValue(Integer.class);
                        Integer score = quizSnap.child("score").getValue(Integer.class);

                        // Xử lý dữ liệu null phòng hờ
                        if (questionCount == null) questionCount = 0;
                        if (score == null) score = 0;

                        // Format ngày tháng
                        String dateStr = "Unknown Date";
                        if (createdAt != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
                            dateStr = sdf.format(new Date(createdAt));
                        }

                        String infoText = dateStr + " • " + questionCount + " Qs";

                        // Vẽ lên UI
                        addQuizItemToView(quizId, title, infoText, score);
                    }
                }

                // Kiểm tra sau khi chạy xong vòng lặp, nếu có nhập từ khóa mà không tìm thấy gì
                if (!isFound && !searchKeyword.isEmpty()) {
                    Toast.makeText(MyQuizzedActivity.this, "Không tìm thấy kết quả!", Toast.LENGTH_SHORT).show();
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