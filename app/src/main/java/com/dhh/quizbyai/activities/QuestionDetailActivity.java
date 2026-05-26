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
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dhh.quizbyai.R;
import com.dhh.quizbyai.adapter.LiveLeaderboardAdapter;
import com.dhh.quizbyai.models.PlayerModel;
import com.dhh.quizbyai.models.QuestionModel;
import com.dhh.quizbyai.models.QuizModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class QuestionDetailActivity extends BaseActivity {
    ImageView img_avt_quiz;
    TextView txt_name_quiz, txt_quiz_day, txt_NoQ, txt_total_time, txt_time_perQ;
    Button btn_start_quiz;
    Button btn_preview;
    ImageButton btn_delete_quiz;
    LinearLayout quiz_history_list_container;
    private int currentTotalQuestions = 0; // Để hứng tổng số câu hỏi
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
                        currentTotalQuestions = quiz.getQuestionCount();
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
    protected void startQuiz(String quizID) {
        // Ánh xạ cái công tắc "Create a quiz for multiple players"
        SwitchCompat switchMultiplayer = findViewById(R.id.customSwitch);

        btn_start_quiz.setOnClickListener(v -> {
            if (switchMultiplayer.isChecked()) {
                // NẾU BẬT CÔNG TẮC -> CHẾ ĐỘ NHIỀU NGƯỜI CHƠI
                createMultiplayerRoom(quizID);
            } else {
                // NẾU TẮT CÔNG TẮC -> CHƠI SOLO NHƯ CŨ
                Intent intent = new Intent(QuestionDetailActivity.this, QuestionActivity.class);
                intent.putExtra("QUIZ_ID", quizID);
                startActivity(intent);
                finish();
            }
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
                                renderHistoryToView(historyList, quizId);
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

                        // Lấy thêm mảng userAnswers
                        List<String> userAnswers = new ArrayList<>();
                        if(snap.hasChild("userAnswers")){
                            for(DataSnapshot ansSnap : snap.child("userAnswers").getChildren()){
                                userAnswers.add(ansSnap.getValue(String.class));
                            }
                        }

                        if (timestamp != null && score != null) {
                            Map<String, Object> attempt = new HashMap<>();
                            attempt.put("timestamp", timestamp);
                            attempt.put("score", score);
                            attempt.put("userAnswers", userAnswers); // Truyền mảng này vào
                            historyList.add(attempt);
                        }
                    }
                    // THÊM BIẾN quizId VÀO HÀM RENDER ĐỂ TRUYỀN ĐI
                    renderHistoryToView(historyList, quizId);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("QUIZ_DEBUG", "Lỗi tải lịch sử: " + error.getMessage());
                }
            });
        }
    }
    // HÀM DÙNG CHUNG ĐỂ VẼ GIAO DIỆN
    // Thêm tham số String quizId
    private void renderHistoryToView(List<Map<String, Object>> historyList, String quizId) {
        quiz_history_list_container.removeAllViews();
        if (historyList == null || historyList.isEmpty()) return;

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy - HH:mm", Locale.US);

        for (Map<String, Object> attempt : historyList) {
            long timestamp = ((Number) attempt.get("timestamp")).longValue();
            int score = ((Number) attempt.get("score")).intValue();

            View historyView = getLayoutInflater().inflate(R.layout.item_quiz_history_layout, quiz_history_list_container, false);

            // Xử lý hiệu ứng click như 1 button
            historyView.setClickable(true);
            historyView.setFocusable(true);

            TextView txtDate = historyView.findViewById(R.id.txt_history_date);
            TextView txtScore = historyView.findViewById(R.id.txt_history_score);

            txtDate.setText(sdf.format(new Date(timestamp)));
            txtScore.setText(score + "%");

            // SỰ KIỆN CLICK CHUYỂN SANG MÀN HÌNH REVIEW
            historyView.setOnClickListener(v -> {
                Intent intent = new Intent(QuestionDetailActivity.this, ReviewQuizActivity.class);
                intent.putExtra("QUIZ_ID", quizId); // Để load lại list câu hỏi

                // Gửi mảng đáp án user đã chọn sang màn hình bên kia
                List<String> userAnswers = (List<String>) attempt.get("userAnswers");
                if(userAnswers != null){
                    intent.putStringArrayListExtra("USER_ANSWERS", new ArrayList<>(userAnswers));
                }
                startActivity(intent);
            });

            quiz_history_list_container.addView(historyView);
        }
    }
    // Hàm 1: Random mã 6 số và đẩy lên Firebase
    private void createMultiplayerRoom(String quizID) {
        // Tạo ngẫu nhiên 6 số
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        String roomPin = String.format("%06d", number);

        // Khởi tạo phòng trên Firebase ở nhánh "Rooms"
        DatabaseReference roomRef = FirebaseDatabase.getInstance().getReference("Rooms").child(roomPin);

        Map<String, Object> roomData = new HashMap<>();
        roomData.put("quizId", quizID);
        roomData.put("hostId", FirebaseAuth.getInstance().getCurrentUser().getUid());
        roomData.put("status", "waiting"); // Trạng thái đang chờ

        roomRef.setValue(roomData).addOnSuccessListener(aVoid -> {
            // Khởi tạo thành công thì mở Panel lên
            showWaitingRoomPanel(roomPin, roomRef);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Không thể tạo phòng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    // Hàm 2: Hiển thị Panel và Lắng nghe người chơi vào phòng
    private void showWaitingRoomPanel(String roomPin, DatabaseReference roomRef) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_waiting_room, null);
        bottomSheetDialog.setContentView(sheetView);

        // Không cho chạm ra ngoài để tắt, bắt buộc dùng nút Close
        bottomSheetDialog.setCancelable(false);
        bottomSheetDialog.setCanceledOnTouchOutside(false);

        // Ánh xạ View trong Panel
        TextView txtRoomPin = sheetView.findViewById(R.id.txt_room_pin);
        TextView txtPlayerCount = sheetView.findViewById(R.id.txt_player_count);
        RecyclerView rvPlayers = sheetView.findViewById(R.id.rv_players);
        Button btnStartGame = sheetView.findViewById(R.id.btn_start_multiplayer);
        Button btnCloseRoom = sheetView.findViewById(R.id.btn_close_room);

        txtRoomPin.setText(roomPin);

        // Setup RecyclerView
        List<PlayerModel> playerList = new ArrayList<>();
        com.dhh.quizbyai.adapters.WaitingRoomAdapter adapter = new com.dhh.quizbyai.adapters.WaitingRoomAdapter(playerList);
        rvPlayers.setLayoutManager(new LinearLayoutManager(this));
        rvPlayers.setAdapter(adapter);

        // GẮN TAI NGHE: Lắng nghe danh sách người chơi theo thời gian thực
        ValueEventListener playerListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                playerList.clear();
                for (DataSnapshot playerSnap : snapshot.getChildren()) {
                    PlayerModel player = playerSnap.getValue(PlayerModel.class);
                    if (player != null) {
                        playerList.add(player);
                    }
                }
                adapter.notifyDataSetChanged(); // Cập nhật danh sách
                txtPlayerCount.setText("Players (" + playerList.size() + ")"); // Cập nhật số lượng
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        // Chỉ lắng nghe ở nhánh "players" của phòng này
        roomRef.child("players").addValueEventListener(playerListener);

        // Xử lý nút START GAME
        btnStartGame.setOnClickListener(v -> {
            if (playerList.isEmpty()) {
                Toast.makeText(this, "Cần ít nhất 1 người chơi để bắt đầu!", Toast.LENGTH_SHORT).show();
                return;
            }
            // Đổi trạng thái phòng thành "playing", các máy con sẽ tự động nhảy vào thi
            roomRef.child("status").setValue("playing");
            bottomSheetDialog.dismiss();

            // Host cũng có thể nhảy vào màn hình theo dõi tiến độ (sẽ làm ở bước sau)
            showHostLiveDashboard(roomPin, roomRef);
        });

        // Xử lý nút ĐÓNG PHÒNG
        btnCloseRoom.setOnClickListener(v -> {
            roomRef.removeValue(); // Xóa sạch phòng trên Firebase
            bottomSheetDialog.dismiss();
            Toast.makeText(this, "Đã đóng phòng", Toast.LENGTH_SHORT).show();
        });

        bottomSheetDialog.show();
    }

    private void showHostLiveDashboard(String roomPin, DatabaseReference roomRef) {
        BottomSheetDialog dashboardDialog = new BottomSheetDialog(this);
        // Bạn có thể tái sử dụng file layout của phòng chờ (layout_bottom_sheet_waiting_room),
        // chỉ cần ẩn/hiện vài nút là thành dashboard.
        View sheetView = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_waiting_room, null);
        dashboardDialog.setContentView(sheetView);

        dashboardDialog.setCancelable(false);
        dashboardDialog.setCanceledOnTouchOutside(false);

        TextView txtTitle = sheetView.findViewById(R.id.txt_room_pin); // Tái sử dụng làm Title
        TextView txtStatus = sheetView.findViewById(R.id.txt_player_count);
        RecyclerView rvLeaderboard = sheetView.findViewById(R.id.rv_players);
        Button btnEndGame = sheetView.findViewById(R.id.btn_close_room);

        // Ẩn nút Start Game vì game đã chạy rồi
        Button btnStartGame = sheetView.findViewById(R.id.btn_start_multiplayer);
        btnStartGame.setVisibility(View.GONE);

        txtTitle.setText("Live Leaderboard (PIN: " + roomPin + ")");
        btnEndGame.setText("End Game & Close Room");
        btnEndGame.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.RED));

        // Cài đặt RecyclerView cho Bảng xếp hạng
        List<PlayerModel> livePlayerList = new ArrayList<>();
        LiveLeaderboardAdapter adapter = new LiveLeaderboardAdapter(livePlayerList, currentTotalQuestions);
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        rvLeaderboard.setAdapter(adapter);

        // BẮT ĐẦU LẮNG NGHE ĐIỂM SỐ REALTIME TỪ FIREBASE
        roomRef.child("players").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                livePlayerList.clear();
                int finishedPlayers = 0;

                for (DataSnapshot playerSnap : snapshot.getChildren()) {
                    PlayerModel player = playerSnap.getValue(PlayerModel.class);
                    if (player != null) {
                        livePlayerList.add(player);
                        // Đếm xem có bao nhiêu người đã làm xong
                        if (player.getAnswered() >= currentTotalQuestions) {
                            finishedPlayers++;
                        }
                    }
                }

                // ĐÂY LÀ ĐOẠN QUYẾT ĐỊNH: Sắp xếp người chơi từ điểm cao xuống điểm thấp
                Collections.sort(livePlayerList, (p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));

                adapter.notifyDataSetChanged();
                txtStatus.setText("Status: " + finishedPlayers + "/" + livePlayerList.size() + " completed");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Nút kết thúc game sớm (Hoặc khi mọi người làm xong hết Host tự bấm)
        btnEndGame.setOnClickListener(v -> {
            roomRef.removeValue(); // Xóa phòng trên Firebase, các máy con sẽ tự văng ra
            dashboardDialog.dismiss();
            Toast.makeText(this, "Trận đấu đã kết thúc!", Toast.LENGTH_SHORT).show();
        });

        dashboardDialog.show();
    }
}