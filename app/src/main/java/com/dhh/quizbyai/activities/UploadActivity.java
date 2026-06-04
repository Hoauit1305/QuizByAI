package com.dhh.quizbyai.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.dhh.quizbyai.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
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

        mAuth = FirebaseAuth.getInstance();

        // KIỂM TRA PROFILE TÀI KHOẢN KHI VỪA VÀO
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String currentName = currentUser.getDisplayName();
            // Nếu tài khoản chưa có tên (thường là do vừa tạo bằng Email/Password)
            if (currentName == null || currentName.isEmpty()) {
                showSetupProfileBottomSheet(currentUser);
            }
        }

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
        loadRecentQuiz();
    }

    // ==========================================
    // LOGIC CẬP NHẬT THÔNG TIN TÀI KHOẢN (ONBOARDING)
    // ==========================================
    private void showSetupProfileBottomSheet(FirebaseUser user) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_setup_profile, null);
        bottomSheetDialog.setContentView(sheetView);

        // Bắt buộc người dùng phải điền, không cho tắt panel ngang xương
        bottomSheetDialog.setCancelable(false);
        bottomSheetDialog.setCanceledOnTouchOutside(false);

        EditText edtName = sheetView.findViewById(R.id.edt_setup_name);
        Button btnSave = sheetView.findViewById(R.id.btn_setup_save);

        ImageButton avatar1 = sheetView.findViewById(R.id.avatar_option_1);
        ImageButton avatar2 = sheetView.findViewById(R.id.avatar_option_2);
        ImageButton avatar3 = sheetView.findViewById(R.id.avatar_option_3);
        ImageButton avatar4 = sheetView.findViewById(R.id.avatar_option_4);

        // Mảng lưu trữ lựa chọn (Khởi tạo rỗng, không gán mặc định nữa)
        final String[] selectedAvatarUri = {""};

        // Xử lý sự kiện click chọn Avatar
        avatar1.setOnClickListener(v -> {
            selectedAvatarUri[0] = "android.resource://" + getPackageName() + "/" + R.drawable.icon_avata_1;
            highlightSelectedAvatar(avatar1, avatar1, avatar2, avatar3, avatar4);
        });
        avatar2.setOnClickListener(v -> {
            selectedAvatarUri[0] = "android.resource://" + getPackageName() + "/" + R.drawable.icon_avata_2;
            highlightSelectedAvatar(avatar2, avatar1, avatar2, avatar3, avatar4);
        });
        avatar3.setOnClickListener(v -> {
            selectedAvatarUri[0] = "android.resource://" + getPackageName() + "/" + R.drawable.icon_avata_3;
            highlightSelectedAvatar(avatar3, avatar1, avatar2, avatar3, avatar4);
        });
        avatar4.setOnClickListener(v -> {
            selectedAvatarUri[0] = "android.resource://" + getPackageName() + "/" + R.drawable.icon_avata_4;
            highlightSelectedAvatar(avatar4, avatar1, avatar2, avatar3, avatar4);
        });

        // Nút Lưu thông tin
        btnSave.setOnClickListener(v -> {
            String inputName = edtName.getText().toString().trim();

            if (inputName.isEmpty()) {
                Toast.makeText(this, "Please enter your display name", Toast.LENGTH_SHORT).show();
                return;
            }

            // Bắt lỗi nếu người dùng chưa chọn icon nào
            if (selectedAvatarUri[0].isEmpty()) {
                Toast.makeText(this, "Please choose an avatar", Toast.LENGTH_SHORT).show();
                return;
            }

            // Tạo gói cập nhật
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(inputName)
                    .setPhotoUri(Uri.parse(selectedAvatarUri[0]))
                    .build();

            btnSave.setEnabled(false); // Khóa nút chống spam
            btnSave.setText("Saving...");

            // Bắn lên Firebase
            user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    bottomSheetDialog.dismiss();
                    displayInfo(); // Cập nhật lại giao diện ngay lập tức
                } else {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save & Continue");
                    Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        bottomSheetDialog.show();
    }

    // Hàm phụ trợ để làm nổi bật cái Avatar đang được chọn (Ví dụ: Thêm viền cam)
    private void highlightSelectedAvatar(ImageButton selected, ImageButton a1, ImageButton a2, ImageButton a3, ImageButton a4) {
        // Reset tất cả về trạng thái mờ (alpha = 0.5f)
        a1.setAlpha(0.5f);
        a2.setAlpha(0.5f);
        a3.setAlpha(0.5f);
        a4.setAlpha(0.5f);
        // Làm sáng rõ cái đang được chọn
        selected.setAlpha(1.0f);
    }
    // ==========================================


    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            Toast.makeText(this, "Upload successful! Redirecting to configuration...", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(UploadActivity.this, ConfigureQuizActivity.class);
                            intent.putExtra("FILE_URI", fileUri.toString());
                            Log.d("UPLOAD", "FILE_URI passed: " + fileUri.toString());

                            startActivity(intent);
                        }
                    }
                }
        );
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document" // .docx
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        filePickerLauncher.launch(intent);
    }

    private void displayInfo() {
        img_avatar = findViewById(R.id.img_avatar);
        txt_name = findViewById(R.id.txt_Name);
        txt_gmail = findViewById(R.id.txt_gmail);

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            txt_name.setText(name != null && !name.isEmpty() ? name : "New User");

            txt_gmail.setText(currentUser.getEmail());

            // Hiển thị Avatar bằng Glide
            if (currentUser.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .circleCrop()
                        .into(img_avatar);
            } else {
                img_avatar.setImageResource(R.drawable.account_circle_24px); // Hình mặc định
            }
        }
    }

    private void logout() {
        btn_logout = findViewById(R.id.btn_logout);

        btn_logout.setOnClickListener(v -> {
            // 1. Đăng xuất khỏi Firebase
            mAuth.signOut();

            // 2. Đăng xuất khỏi Google
            String webClientId = getString(R.string.default_web_client_id);
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build();

            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Intent intent = new Intent(UploadActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });
    }

    private void loadRecentQuiz() {
        View recentQuizView = findViewById(R.id.included_recent_quiz);
        TextView txtQuickAccessTitle = findViewById(R.id.textView5);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Quizzes");

        ref.orderByChild("creatorId").equalTo(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            recentQuizView.setVisibility(View.GONE);
                            txtQuickAccessTitle.setVisibility(View.GONE);
                            return;
                        }

                        DataSnapshot latestQuizSnap = null;
                        long maxTimestamp = 0;

                        for (DataSnapshot quizSnap : snapshot.getChildren()) {
                            Long createdAt = quizSnap.child("createdAt").getValue(Long.class);
                            if (createdAt != null && createdAt > maxTimestamp) {
                                maxTimestamp = createdAt;
                                latestQuizSnap = quizSnap;
                            }
                        }

                        if (latestQuizSnap != null) {
                            recentQuizView.setVisibility(View.VISIBLE);
                            txtQuickAccessTitle.setVisibility(View.VISIBLE);

                            String quizId = latestQuizSnap.getKey();
                            String title = latestQuizSnap.child("title").getValue(String.class);
                            Integer questionCount = latestQuizSnap.child("questionCount").getValue(Integer.class);
                            Integer score = latestQuizSnap.child("score").getValue(Integer.class);

                            // 1. ĐỌC THÊM EMOJI TỪ FIREBASE
                            String topicEmoji = latestQuizSnap.child("topic_emoji").getValue(String.class);

                            if (title == null) title = "Untitled Quiz";
                            if (questionCount == null) questionCount = 0;
                            if (score == null) score = 0;

                            String dateStr = "Unknown Date";
                            if (maxTimestamp > 0) {
                                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
                                dateStr = sdf.format(new Date(maxTimestamp));
                            }
                            String infoText = dateStr + " • " + questionCount + " Qs";

                            TextView txtTitle = findViewById(R.id.txt_quiz_title);
                            TextView txtInfo = findViewById(R.id.txt_quiz_info);
                            TextView txtScore = findViewById(R.id.txt_quiz_score);

                            // 2. ÁNH XẠ VÀ HIỂN THỊ EMOJI
                            TextView tvTopicEmoji = findViewById(R.id.tvTopicEmoji);
                            if (tvTopicEmoji != null) {
                                tvTopicEmoji.setText(topicEmoji);
                            }

                            txtTitle.setText(title);
                            txtInfo.setText(infoText);
                            txtScore.setText(score + "%");
                            long finalMaxTimestamp = maxTimestamp;
                            recentQuizView.setOnClickListener(v -> {
                                Intent intent = new Intent(UploadActivity.this, QuestionDetailActivity.class);
                                intent.putExtra("QUIZ_ID", quizId);
                                intent.putExtra("CREATED_AT", finalMaxTimestamp);
                                startActivity(intent);
                                overridePendingTransition(0, 0);
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("UploadActivity", "Failed to load Recent Quiz: " + error.getMessage());
                    }
                });
    }
}