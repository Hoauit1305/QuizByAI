package com.dhh.quizbyai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dhh.quizbyai.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {
    Button btnSignInWithGoogle, btnContinueWithEmail;
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    private static final String TAG = "GOOGLE_AUTH_DEBUG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        // 1. Kiểm tra có phải người dùng đăng nhập ? (ĐÃ XÓA LOGIC GUEST)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Chuyển thẳng tới UploadActivity
            Intent intent = new Intent(LoginActivity.this, UploadActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0); // Tắt hiệu ứng trượt
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Ánh xạ View
        btnSignInWithGoogle = findViewById(R.id.btn_signin_goole);
        // Lưu ý: Mình vẫn dùng R.id.btn_signin_guest để khớp với file XML cũ của bạn
        // Nhưng đã đổi tên biến Java thành btnContinueWithEmail cho chuẩn logic
        btnContinueWithEmail = findViewById(R.id.btn_signin_email);

        String webClientId = getString(R.string.default_web_client_id);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Gắn sự kiện click
        btnSignInWithGoogle.setOnClickListener(v -> signInWithGoogle());

        // GỌI BOTTOM SHEET KHI BẤM NÚT EMAIL
        btnContinueWithEmail.setOnClickListener(v -> showAuthBottomSheet());
    }

    // =======================================================
    // LOGIC ĐĂNG NHẬP BẰNG GOOGLE (GIỮ NGUYÊN)
    // =======================================================
    private void signInWithGoogle() {
        // Luôn đăng xuất trước để chọn lại tài khoản
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: req=" + requestCode + ", res=" + resultCode);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    Log.d(TAG, "Đăng nhập Google THÀNH CÔNG: " + account.getEmail());
                    Log.d(TAG, "Đang tiến hành xác thực với Firebase...");
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                int statusCode = e.getStatusCode();
                Log.e(TAG, "Lỗi đăng nhập Google: " + statusCode + " - " + GoogleSignInStatusCodes.getStatusCodeString(statusCode));

                if (statusCode == 12501) {
                    Toast.makeText(this, "Bạn đã hủy đăng nhập", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Lỗi hệ thống: " + statusCode, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "Firebase xác thực THÀNH CÔNG. User UID: " + (user != null ? user.getUid() : "null"));

                        Intent intent = new Intent(LoginActivity.this, UploadActivity.class);
                        startActivity(intent);
                        finish();
                        overridePendingTransition(0, 0); // Tắt hiệu ứng trượt
                    } else {
                        Log.e(TAG, "Firebase xác thực THẤT BẠI", task.getException());
                        Toast.makeText(this, "Lỗi xác thực Firebase", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =======================================================
    // LOGIC ĐĂNG NHẬP/ĐĂNG KÝ BẰNG EMAIL (BOTTOM SHEET)
    // =======================================================
    private void showAuthBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_auth, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView txtTitle = sheetView.findViewById(R.id.txt_sheet_title);
        EditText edtEmail = sheetView.findViewById(R.id.edt_sheet_email);
        EditText edtPassword = sheetView.findViewById(R.id.edt_sheet_password);
        Button btnSubmit = sheetView.findViewById(R.id.btn_sheet_submit);
        TextView txtToggle = sheetView.findViewById(R.id.txt_sheet_toggle);

        // Mảng 1 phần tử để lưu cờ trạng thái (true = Login, false = Register)
        final boolean[] isLoginMode = {true};

        txtToggle.setOnClickListener(v -> {
            isLoginMode[0] = !isLoginMode[0];

            if (isLoginMode[0]) {
                txtTitle.setText("Login");
                btnSubmit.setText("Login");
                txtToggle.setText("Don't have an account? Register here");
            } else {
                txtTitle.setText("Register");
                btnSubmit.setText("Register");
                txtToggle.setText("Already have an account? Login here");
            }
        });

        btnSubmit.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isLoginMode[0]) {
                // XỬ LÝ ĐĂNG NHẬP
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                bottomSheetDialog.dismiss();
                                Intent intent = new Intent(LoginActivity.this, UploadActivity.class);
                                startActivity(intent);
                                finish();
                                overridePendingTransition(0, 0); // Tắt hiệu ứng trượt
                            } else {
                                Toast.makeText(this, "Lỗi đăng nhập: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                // XỬ LÝ ĐĂNG KÝ
                if (password.length() < 6) {
                    Toast.makeText(this, "Mật khẩu phải từ 6 ký tự trở lên", Toast.LENGTH_SHORT).show();
                    return;
                }
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                                bottomSheetDialog.dismiss();
                                Intent intent = new Intent(LoginActivity.this, UploadActivity.class);
                                startActivity(intent);
                                finish();
                                overridePendingTransition(0, 0); // Tắt hiệu ứng trượt
                            } else {
                                Toast.makeText(this, "Lỗi đăng ký: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        bottomSheetDialog.show();
    }
}