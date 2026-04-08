package com.dhh.quizbyai.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.dhh.quizbyai.R; // Nhớ import R của project bạn

public class BaseActivity extends AppCompatActivity {

    // Hàm này dùng để gọi ở các màn hình con
    protected void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.includedBottomNav); // ID của thanh nav trong XML

        if (bottomNav != null) {
            // Highlight đúng icon của màn hình hiện tại
            if (this instanceof UploadActivity) {
                bottomNav.setSelectedItemId(R.id.nav_home);
            } else if (this instanceof MyQuizzedActivity) {
                bottomNav.setSelectedItemId(R.id.nav_quizzes);
            }

            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                Intent intent = null;
                // Chuyển ngữ cảnh (Activity) dựa trên nút được bấm
                if (id == R.id.nav_home) {
                    // Nếu đang ở UploadActivity thì không chuyển nữa
                    if (this instanceof UploadActivity) return false;

                    intent = new Intent(this, UploadActivity.class);
                } else if (id == R.id.nav_quizzes) {
                    // Nếu đang ở MyQuizzedActivity thì không chuyển nữa
                    if (this instanceof MyQuizzedActivity) return false;
                    intent = new Intent(this, MyQuizzedActivity.class);
                }

                if (intent != null) {
                    // Tối ưu Stack bằng Flags
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

                    startActivity(intent);

                    // Thêm hiệu ứng chuyển cảnh mượt mà (không bị nháy màn hình)
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            });
        }
    }
}