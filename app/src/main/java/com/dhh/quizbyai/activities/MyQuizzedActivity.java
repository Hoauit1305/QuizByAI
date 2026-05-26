package com.dhh.quizbyai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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
import com.dhh.quizbyai.models.PlayerModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
    private Button btn_join_quiz;
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
        btn_join_quiz = findViewById(R.id.btn_join_quiz_top);
        btn_join_quiz.setOnClickListener(v -> showJoinRoomPanel());
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
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Hàm này sẽ tự động chạy mỗi khi màn hình này hiện ra trước mặt người dùng

        // Lấy lại từ khóa đang gõ dở trên ô tìm kiếm (nếu có)
        String currentKeyword = edt_name_quiz.getText().toString().trim();

        // Load lại dữ liệu từ Firebase
        loadMyQuizzes(currentKeyword);
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
                        String topicEmoji = quizSnap.child("topic_emoji").getValue(String.class);

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

                    // Gọi hàm tạo Giao diện cho từng Quiz
                        addQuizItemToView(quizId, title, infoText, score, createdAt, topicEmoji);
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

    private void addQuizItemToView(String quizId, String title, String info, int score, long createdAt, String topicEmoji) {
        // Inflate layout item_quiz_layout.xml
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_quiz_layout, quizListContainer, false);

        // Ánh xạ UI trong item
        TextView txtTitle = itemView.findViewById(R.id.txt_quiz_title);
        TextView txtInfo = itemView.findViewById(R.id.txt_quiz_info);
        TextView txtScore = itemView.findViewById(R.id.txt_quiz_score);
        TextView tvTopicEmoji = itemView.findViewById(R.id.tvTopicEmoji);

        // Gán dữ liệu
        txtTitle.setText(title);
        txtInfo.setText(info);
        txtScore.setText(score + "%");

        if (topicEmoji != null && !topicEmoji.isEmpty()) {
            tvTopicEmoji.setText(topicEmoji);
        } else {
            tvTopicEmoji.setText("📝"); // Mặc định nếu Firebase chưa có dữ liệu Emoji
        }

        // Set sự kiện khi nhấn vào cái khung xám này
        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(MyQuizzedActivity.this, QuestionDetailActivity.class);
            intent.putExtra("QUIZ_ID", quizId); // Truyền ID sang màn hình QuestionDetail
            intent.putExtra("CREATED_AT", createdAt);
            startActivity(intent);
        });

        // Thêm view này vào ScrollView
        quizListContainer.addView(itemView);
    }

    private void showJoinRoomPanel() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_join_room, null);
        bottomSheetDialog.setContentView(sheetView);

        EditText edtRoomPin = sheetView.findViewById(R.id.edt_room_pin);
        Button btnJoin = sheetView.findViewById(R.id.btn_join_room);

        btnJoin.setOnClickListener(v -> {
            String pin = edtRoomPin.getText().toString().trim();

            if (pin.length() != 6) {
                Toast.makeText(this, "Vui lòng nhập đủ mã PIN 6 số", Toast.LENGTH_SHORT).show();
                return;
            }

            btnJoin.setEnabled(false);
            btnJoin.setText("Joining...");

            DatabaseReference roomRef = FirebaseDatabase.getInstance().getReference("Rooms").child(pin);

            roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String status = snapshot.child("status").getValue(String.class);

                        if ("waiting".equals(status)) {
                            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                            String uid = currentUser.getUid();
                            String name = currentUser.getDisplayName();
                            if (name == null || name.isEmpty()) name = "Player";

                            PlayerModel me = new PlayerModel(name, 0);

                            roomRef.child("players").child(uid).setValue(me)
                                    .addOnSuccessListener(aVoid -> {
                                        bottomSheetDialog.dismiss();

                                        // GỌI HÀM PANEL CHỜ NGƯỜI CHƠI TẠI ĐÂY (BƯỚC 2 TRONG HƯỚNG DẪN TRƯỚC)
                                        showPlayerWaitingPanel(pin, roomRef, uid);
                                    });
                        } else {
                            Toast.makeText(MyQuizzedActivity.this, "Phòng này đã bắt đầu chơi hoặc đã đóng!", Toast.LENGTH_SHORT).show();
                            btnJoin.setEnabled(true);
                            btnJoin.setText("Enter");
                        }
                    } else {
                        Toast.makeText(MyQuizzedActivity.this, "Mã phòng không tồn tại!", Toast.LENGTH_SHORT).show();
                        btnJoin.setEnabled(true);
                        btnJoin.setText("Enter");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(MyQuizzedActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
                    btnJoin.setEnabled(true);
                    btnJoin.setText("Enter");
                }
            });
        });

        // ĐÂY LÀ DÒNG LỆNH QUAN TRỌNG ĐÃ BỊ THIẾU
        bottomSheetDialog.show();
    }
    private void showPlayerWaitingPanel(String pin, DatabaseReference roomRef, String uid) {
        BottomSheetDialog waitingDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_player_waiting, null);
        waitingDialog.setContentView(sheetView);

        // Khóa panel, không cho người chơi bấm ra ngoài để tắt
        waitingDialog.setCancelable(false);
        waitingDialog.setCanceledOnTouchOutside(false);

        TextView txtJoinedPin = sheetView.findViewById(R.id.txt_joined_pin);
        Button btnLeave = sheetView.findViewById(R.id.btn_leave_room);

        txtJoinedPin.setText("Room PIN: " + pin);

        // TẠO BỘ LẮNG NGHE TRẠNG THÁI PHÒNG (REALTIME)
        ValueEventListener roomStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    String quizId = snapshot.child("quizId").getValue(String.class);

                    if ("playing".equals(status)) {
                        // HỦY LẮNG NGHE TRƯỚC KHI CHUYỂN MÀN HÌNH ĐỂ TRÁNH LỖI BỘ NHỚ
                        roomRef.removeEventListener(this);
                        waitingDialog.dismiss();

                        // CHUYỂN NGƯỜI CHƠI SANG MÀN HÌNH LÀM BÀI QUÝZ
                        Intent intent = new Intent(MyQuizzedActivity.this, QuestionActivity.class);
                        intent.putExtra("QUIZ_ID", quizId);
                        intent.putExtra("ROOM_PIN", pin); // Truyền thêm mã phòng để lưu điểm sau này
                        intent.putExtra("IS_MULTIPLAYER", true);
                        startActivity(intent);
                    }
                } else {
                    // Nếu snapshot không tồn tại nghĩa là Host đã hủy/xóa phòng
                    roomRef.removeEventListener(this);
                    waitingDialog.dismiss();
                    Toast.makeText(MyQuizzedActivity.this, "The host has closed this room!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        // Bắt đầu lắng nghe sự thay đổi của phòng
        roomRef.addValueEventListener(roomStatusListener);

        // Xử lý khi Người chơi chủ động bấm nút thoát phòng (Leave Room)
        btnLeave.setOnClickListener(v -> {
            roomRef.removeEventListener(roomStatusListener); // Hủy lắng nghe
            roomRef.child("players").child(uid).removeValue(); // Xóa tên mình khỏi danh sách trên Firebase
            waitingDialog.dismiss();
            Toast.makeText(this, "You left the room", Toast.LENGTH_SHORT).show();
        });

        waitingDialog.show();
    }
}