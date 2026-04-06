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

        isGuest = getIntent().getBooleanExtra("IS_GUEST", false);

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
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                FirebaseAuth.getInstance().signOut();

                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
                GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);

                googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    startActivity(intent);
                    finish();
                });
            }
        });
    }
}