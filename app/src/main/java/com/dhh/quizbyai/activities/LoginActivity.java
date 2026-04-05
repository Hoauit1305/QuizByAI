package com.dhh.quizbyai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dhh.quizbyai.R;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import android.util.Log;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {
    Button btnSignInWithGoogle, btnSignInAsGuest;
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    private static final String TAG = "GOOGLE_AUTH_DEBUG";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnSignInWithGoogle = findViewById(R.id.btn_signin_goole);
        btnSignInAsGuest = findViewById(R.id.btn_signin_guest);

        mAuth = FirebaseAuth.getInstance();

        String webClientId = getString(R.string.default_web_client_id);
        Log.d(TAG, "Using System Web Client ID: " + webClientId);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnSignInWithGoogle.setOnClickListener(v -> signInWithGoogle());
        btnSignInAsGuest.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, UploadActivity.class);

            intent.putExtra("IS_GUEST", true);

            startActivity(intent);

            finish();
        });

    }
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
                        Log.d(TAG, "ĐANG CHUYỂN SANG UploadActivity...");
                        
                        Intent intent = new Intent(LoginActivity.this, UploadActivity.class);
                        startActivity(intent);
                        finish();
                        
                        Log.d(TAG, "Đã gọi lệnh startActivity(UploadActivity) và finish() LoginActivity");
                    } else {
                        Log.e(TAG, "Firebase xác thực THẤT BẠI", task.getException());
                        Toast.makeText(this, "Lỗi xác thực Firebase", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
