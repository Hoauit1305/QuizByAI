package com.dhh.quizbyai.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.dhh.quizbyai.R; // Nhớ import R của project bạn

public class BaseActivity extends AppCompatActivity {

    // Hàm này dùng để gọi ở các màn hình con
    protected void setupBottomNavigation() {
        /*BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView); // ID của thanh nav trong XML

        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();

                // Chuyển ngữ cảnh (Activity) dựa trên nút được bấm
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class));
                    return true;
                } else if (id == R.id.nav_quizzes) {
                    startActivity(new Intent(this, QuizzesActivity.class));
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
                }
                return false;
            });
        }*/
    }
}