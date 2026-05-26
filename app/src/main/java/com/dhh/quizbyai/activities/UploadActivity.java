package com.dhh.quizbyai.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.dhh.quizbyai.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.annotation.NonNull;
import android.view.View;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UploadActivity extends BaseActivity {
    // Khai báo launcher để xử lý kết quả sau khi chọn file
    private ActivityResultLauncher<Intent> filePickerLauncher;
    Button btnChooseFile;
    ImageView img_avatar;
    TextView txt_name, txt_gmail;
    ImageButton btn_logout;
    FirebaseAuth mAuth;
    boolean isGuest;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_upload);

        // Thiết lập tràn viền
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // 1. Kích hoạt Bottom Navigation từ BaseActivity
        setupBottomNavigation();

        // 2. Khởi tạo bộ chọn file
        setupFilePicker();

        // 3. Ánh xạ nút bấm và xử lý sự kiện click
        btnChooseFile = findViewById(R.id.button);
        btnChooseFile.setOnClickListener(v -> openFilePicker());

        displayInfo();

        logout();
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Gọi lại hàm này mỗi khi giao diện hiển thị lên màn hình
        loadRecentQuiz();
    }
    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            // Hiển thị thông báo upload thành công
                            Toast.makeText(this, "Upload thành công! Đang chuyển đến cấu hình...", Toast.LENGTH_SHORT).show();

                            // Chuyển sang giao diện ConfigureQuizActivity
                            Intent intent = new Intent(UploadActivity.this, ConfigureQuizActivity.class);

                            intent.putExtra("FILE_URI", fileUri.toString());
                            Log.d("UPLOAD", "FILE_URI được đóng gói là: " + fileUri.toString());

                            startActivity(intent);
                        }
                    }
                }
        );
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Cho phép quét qua các loại file

        // Lọc định dạng PDF và Word (.doc, .docx)
        String[] mimeTypes = {
                "application/pdf",
                "application/msword", // .doc
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document" // .docx
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        filePickerLauncher.launch(intent);
    }

    private void displayInfo(){
        img_avatar = findViewById(R.id.img_avatar);
        txt_name = findViewById(R.id.txt_Name);
        txt_gmail = findViewById(R.id.txt_gmail);

//        isGuest = getIntent().getBooleanExtra("IS_GUEST", false);
        isGuest = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .getBoolean("IS_GUEST", false);

        if(isGuest){
            txt_name.setText(R.string.txt_name_guest);
            txt_gmail.setText(R.string.txt_gmail_guest);

            img_avatar.setImageResource(R.drawable.account_circle_24px);
        } else {
            mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();

            if (currentUser != null) {
                txt_name.setText(currentUser.getDisplayName());
                txt_gmail.setText(currentUser.getEmail());

                if (currentUser.getPhotoUrl() != null) {
                    Glide.with(this)
                            .load(currentUser.getPhotoUrl())
                            .circleCrop()
                            .into(img_avatar);
                }
            }
        }
    }
    private void logout(){
        btn_logout = findViewById(R.id.btn_logout);

        btn_logout.setOnClickListener(v -> {
            Intent intent = new Intent(UploadActivity.this, LoginActivity.class);

            if(isGuest){
                getSharedPreferences("AppPrefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("IS_GUEST", false)
                        .apply();
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                // 1. Đăng xuất khỏi Firebase
                FirebaseAuth.getInstance().signOut();

                // 2. Lấy lại đúng cấu hình đăng nhập ban đầu
                String webClientId = getString(R.string.default_web_client_id);
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build();

                // 3. Tiến hành đăng xuất khỏi Google
                GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
                googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }
        });
    }
    private void loadRecentQuiz() {
        View recentQuizView = findViewById(R.id.included_recent_quiz);
        TextView txtQuickAccessTitle = findViewById(R.id.textView5); // Chữ "Quick Access"

        // Nếu là Guest thì không có dữ liệu Firebase để hiển thị bài gần nhất, ẩn khu vực này đi
        if (isGuest) {
            recentQuizView.setVisibility(View.GONE);
            txtQuickAccessTitle.setVisibility(View.GONE);
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Quizzes");

        // Lấy tất cả quiz của user này giống như bên MyQuizzedActivity
        ref.orderByChild("creatorId").equalTo(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            // Ẩn khu vực nếu người dùng chưa có bài quiz nào
                            recentQuizView.setVisibility(View.GONE);
                            txtQuickAccessTitle.setVisibility(View.GONE);
                            return;
                        }

                        DataSnapshot latestQuizSnap = null;
                        long maxTimestamp = 0;

                        // Lọc ra bài quiz có thời gian tạo gần nhất
                        for (DataSnapshot quizSnap : snapshot.getChildren()) {
                            Long createdAt = quizSnap.child("createdAt").getValue(Long.class);
                            if (createdAt != null && createdAt > maxTimestamp) {
                                maxTimestamp = createdAt;
                                latestQuizSnap = quizSnap;
                            }
                        }

                        if (latestQuizSnap != null) {
                            // Hiển thị lại UI nếu tìm thấy
                            recentQuizView.setVisibility(View.VISIBLE);
                            txtQuickAccessTitle.setVisibility(View.VISIBLE);

                            // Lấy dữ liệu
                            String quizId = latestQuizSnap.getKey();
                            String title = latestQuizSnap.child("title").getValue(String.class);
                            Integer questionCount = latestQuizSnap.child("questionCount").getValue(Integer.class);
                            Integer score = latestQuizSnap.child("score").getValue(Integer.class);

                            if (title == null) title = "Untitled Quiz";
                            if (questionCount == null) questionCount = 0;
                            if (score == null) score = 0;

                            String dateStr = "Unknown Date";
                            if (maxTimestamp > 0) {
                                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
                                dateStr = sdf.format(new Date(maxTimestamp));
                            }
                            String infoText = dateStr + " • " + questionCount + " Qs";

                            // Ánh xạ các thành phần con nằm bên trong item_quiz_layout.xml
                            TextView txtTitle = findViewById(R.id.txt_quiz_title);
                            TextView txtInfo = findViewById(R.id.txt_quiz_info);
                            TextView txtScore = findViewById(R.id.txt_quiz_score);

                            // Đổ dữ liệu
                            txtTitle.setText(title);
                            txtInfo.setText(infoText);
                            txtScore.setText(score + "%");

                            // Bắt sự kiện Click vào thẻ để chuyển sang màn hình Chi tiết giống MyQuizzedActivity
                            long finalMaxTimestamp = maxTimestamp;
                            recentQuizView.setOnClickListener(v -> {
                                Intent intent = new Intent(UploadActivity.this, QuestionDetailActivity.class);
                                intent.putExtra("QUIZ_ID", quizId);
                                intent.putExtra("CREATED_AT", finalMaxTimestamp);
                                startActivity(intent);
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Xử lý lỗi nếu có
                        Log.e("UploadActivity", "Lỗi tải Recent Quiz: " + error.getMessage());
                    }
                });
    }
}