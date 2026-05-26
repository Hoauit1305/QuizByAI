package com.dhh.quizbyai.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dhh.quizbyai.R;
import com.dhh.quizbyai.models.QuestionModel;
import com.dhh.quizbyai.models.QuizModel;
import com.google.common.reflect.TypeToken;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class QuestionDetailActivity extends BaseActivity {
    ImageView img_avt_quiz;
    TextView txt_name_quiz, txt_quiz_day, txt_NoQ, txt_total_time, txt_time_perQ;
    Button btn_start_quiz;
    Button btn_preview;
    ImageButton btn_delete_quiz;
    LinearLayout quiz_history_list_container;
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
        setupBottomNavigation();

        initViews();

        String quiz_ID = getIntent().getStringExtra("QUIZ_ID");
        long created_at = getIntent().getLongExtra("CREATED_AT", -1);
        loadQuestionDetail(quiz_ID);

        startQuiz(quiz_ID);

        deleteQuiz(quiz_ID, created_at);

        loadHistoryUI(quiz_ID, created_at);
        btn_preview = findViewById(R.id.btn_preview);
        btn_preview.setOnClickListener(v -> {
            Intent intent = new Intent(QuestionDetailActivity.this, PreviewEditActivity.class);
            intent.putExtra("QUIZ_ID", quiz_ID);
            startActivity(intent);
        });
    }
    protected void initViews(){
        img_avt_quiz = findViewById(R.id.img_avt_quiz);
        txt_name_quiz = findViewById(R.id.txt_name_quiz);
        txt_quiz_day = findViewById(R.id.txt_quiz_day);
        txt_NoQ = findViewById(R.id.txt_NoQ);
        txt_total_time = findViewById(R.id.txt_totalTime);
        txt_time_perQ = findViewById(R.id.txt_timePerQ);
        btn_start_quiz = findViewById(R.id.btn_start);
        btn_delete_quiz = findViewById(R.id.btn_delete_quiz);
        quiz_history_list_container = findViewById(R.id.quiz_history_list_container);
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
                        txt_total_time.setText(quiz.getTotalTime());

                        // 3. Phân quyền: Kiểm tra xem user hiện tại có phải là người tạo Quiz không
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                        // Lấy creatorId từ snapshot (Do lúc tạo bài Quiz mình đã lưu "creatorId")
                        String creatorId = snapshot.child("creatorId").getValue(String.class);

                        // So sánh UID của user đang đăng nhập và creatorId của bài Quiz
                        if (currentUser != null && creatorId != null && creatorId.equals(currentUser.getUid())) {
                            // Nếu đúng là chủ sở hữu -> Hiện nút Preview / Edit
                            btn_preview.setVisibility(View.VISIBLE);
                        } else {
                            // Nếu là người lạ (hoặc Guest) -> Giữ nguyên trạng thái ẩn
                            btn_preview.setVisibility(View.GONE);
                        }
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
    protected void deleteQuizFromFireBase(String quizId){
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Quizzes").child(quizId);

        dbRef.removeValue()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Đã xóa Quiz thành công!", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    private void deleteQuizLocally(long createdAtTarget) {
        // 1. Mở "cuốn sổ tay" y hệt như lúc lưu
        SharedPreferences sharedPreferences = getSharedPreferences("GuestData", MODE_PRIVATE);
        String existingQuizzesJson = sharedPreferences.getString("local_quizzes", "[]");

        // 2. Dịch JSON ra đúng cái khuôn List<Map<String, Object>>
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> localQuizList = gson.fromJson(existingQuizzesJson, listType);

        // Đề phòng trường hợp lỗi đọc file bị null
        if (localQuizList == null) {
            localQuizList = new ArrayList<>();
        }

        boolean isDeleted = false;

        // 3. Dùng Iterator để duyệt và xóa (Tuyệt đối không dùng vòng lặp for thường khi đang xóa)
        Iterator<Map<String, Object>> iterator = localQuizList.iterator();
        while (iterator.hasNext()) {
            Map<String, Object> quiz = iterator.next();

            // Kiểm tra an toàn xem bài Quiz có trường createdAt không
            if (quiz.containsKey("createdAt")) {
                // Ép kiểu qua (Number) trước để tránh lỗi ClassCastException do Gson parse thành Double
                long currentCreatedAt = ((Number) quiz.get("createdAt")).longValue();

                if (currentCreatedAt == createdAtTarget) {
                    iterator.remove(); // Cắt bỏ bài Quiz này khỏi danh sách
                    isDeleted = true;
                    break; // Tìm thấy rồi thì thoát vòng lặp luôn
                }
            }
        }

        // 4. Đóng gói và lưu đè lại vào sổ tay (Giống hệt bước 4 của hàm save)
        if (isDeleted) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            String updatedQuizzesJson = gson.toJson(localQuizList);
            editor.putString("local_quizzes", updatedQuizzesJson);
            editor.apply();

            Toast.makeText(this, "Đã xóa bài Quiz khỏi máy!", Toast.LENGTH_SHORT).show();
            finish();
            // TODO: Viết code load lại danh sách RecyclerView (Adapter.notifyDataSetChanged) ở đây
        } else {
            Toast.makeText(this, "Không tìm thấy bài Quiz để xóa!", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteQuiz(String quizId, long createdAtTarget){
        btn_delete_quiz.setOnClickListener(v -> {

            // 1. Kiểm tra xem người dùng hiện tại là Khách hay User
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            boolean isGuest = prefs.getBoolean("IS_GUEST", false);

            // 2. Hiện hộp thoại hỏi lại cho chắc chắn
            new AlertDialog.Builder(this) // Hoặc v.getContext() nếu bạn đang viết trong Adapter
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc chắn muốn xóa bài Quiz này không? Dữ liệu không thể khôi phục.")
                    .setPositiveButton("Xóa", (dialog, which) -> {

                        // --- ĐÂY CHÍNH LÀ ĐOẠN ĐIỀU KIỆN CHIA NGÃ RẼ ---
                        if (isGuest) {
                            // TODO: Lấy biến createdAt của bài Quiz hiện tại truyền vào đây
                            deleteQuizLocally(createdAtTarget);
                        } else {
                            // TODO: Lấy cái ID (mã Push Key) của bài Quiz trên Firebase truyền vào đây
                            deleteQuizFromFireBase(quizId);
                        }

                    })
                    .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    private void loadHistoryUI(String quizId, long createdAtTarget){
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isGuest = prefs.getBoolean("IS_GUEST", false);

        if (isGuest) {
            // 1. ĐỌC LỊCH SỬ TỪ MÁY CHO KHÁCH
            SharedPreferences guestPrefs = getSharedPreferences("GuestData", MODE_PRIVATE);
            String existingJson = guestPrefs.getString("local_quizzes", "[]");
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> localQuizList = gson.fromJson(existingJson, listType);

            if (localQuizList != null) {
                for (Map<String, Object> quiz : localQuizList) {
                    if (quiz.containsKey("createdAt")) {
                        long currentCreatedAt = ((Number) quiz.get("createdAt")).longValue();
                        if (currentCreatedAt == createdAtTarget) {
                            if (quiz.containsKey("history")) {
                                // Lấy mảng history ra và vẽ lên giao diện
                                List<Map<String, Object>> historyList = (List<Map<String, Object>>) quiz.get("history");
                                renderHistoryToView(historyList);
                            }
                            break;
                        }
                    }
                }
            }
        } else {
            // 2. ĐỌC LỊCH SỬ TỪ FIREBASE CHO USER
            if (quizId == null) return;
            DatabaseReference historyRef = FirebaseDatabase.getInstance()
                    .getReference("Quizzes").child(quizId).child("history");

            historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<Map<String, Object>> historyList = new ArrayList<>();

                    for (DataSnapshot snap : snapshot.getChildren()) {
                        Long timestamp = snap.child("timestamp").getValue(Long.class);
                        Integer score = snap.child("score").getValue(Integer.class);

                        if (timestamp != null && score != null) {
                            Map<String, Object> attempt = new HashMap<>();
                            attempt.put("timestamp", timestamp);
                            attempt.put("score", score);
                            historyList.add(attempt);
                        }
                    }
                    // Vẽ lên giao diện
                    renderHistoryToView(historyList);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("QUIZ_DEBUG", "Lỗi tải lịch sử: " + error.getMessage());
                }
            });
        }
    }
    // HÀM DÙNG CHUNG ĐỂ VẼ GIAO DIỆN
    private void renderHistoryToView(List<Map<String, Object>> historyList) {
        // Xóa sạch các view cũ trước khi vẽ để không bị trùng lặp
        quiz_history_list_container.removeAllViews();

        if (historyList == null || historyList.isEmpty()) {
            return; // Chưa làm lần nào thì không hiện gì cả
        }

        // Định dạng ngày giờ: Ví dụ "May 2, 2026 - 14:30"
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy - HH:mm", Locale.US);

        // Lặp qua từng lần làm bài (Attempt)
        for (Map<String, Object> attempt : historyList) {
            long timestamp = ((Number) attempt.get("timestamp")).longValue();
            int score = ((Number) attempt.get("score")).intValue();

            // Nạp cái file XML item_history_layout vào bộ nhớ
            View historyView = getLayoutInflater().inflate(R.layout.item_quiz_history_layout, quiz_history_list_container, false);

            TextView txtDate = historyView.findViewById(R.id.txt_history_date);
            TextView txtScore = historyView.findViewById(R.id.txt_history_score);

            // Gắn dữ liệu
            txtDate.setText(sdf.format(new Date(timestamp)));
            txtScore.setText(score + "%");

            // Thêm cục view này vào danh sách dọc trên màn hình
            quiz_history_list_container.addView(historyView);
        }
    }
}