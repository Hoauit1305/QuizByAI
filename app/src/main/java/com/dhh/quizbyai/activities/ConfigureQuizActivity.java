package com.dhh.quizbyai.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dhh.quizbyai.R;
import com.google.android.material.chip.ChipGroup;

public class ConfigureQuizActivity extends BaseActivity { // Kế thừa BaseActivity để dùng chung các tính năng cốt lõi

    // 1. Khai báo các thành phần giao diện (UI)
    private SeekBar seekBarQuestions;
    private TextView txtSelected;
    private Button btnStartGenerating;
    private Button btnBack;
    private ChipGroup chipGroupTime;

    // 2. Khai báo các biến lưu trữ dữ liệu người dùng chọn
    private int selectedQuestionCount = 20; // Mặc định là 20 câu (nấc số 3 trên SeekBar)
    private int selectedTime = 15;          // Mặc định là 15 giây (do XML đang check sẵn chip15s)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_configure_quiz);

        // Xử lý giao diện tràn viền (Edge-to-Edge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 3. Ánh xạ ID từ XML sang Java
        seekBarQuestions = findViewById(R.id.seekBar);
        txtSelected = findViewById(R.id.textView13);      // Dòng chữ "Selected: 20 questions"
        btnStartGenerating = findViewById(R.id.button3);  // Nút tạo Quiz
        btnBack = findViewById(R.id.button2);             // Nút quay lại
        chipGroupTime = findViewById(R.id.chipGroupTime); // Nhóm nút thời gian 15s, 30s, 60s

        // 4. Kích hoạt các tính năng trên màn hình
        setupSeekBar();
        setupTimeSelection();
        setupButtons();
    }

    // =========================================================================
    // CÁC HÀM XỬ LÝ LOGIC (Tách riêng ra cho dễ quản lý)
    // =========================================================================

    /**
     * Hàm xử lý khi kéo thanh chọn số lượng câu hỏi
     */
    private void setupSeekBar() {
        if (seekBarQuestions == null || txtSelected == null) return;

        seekBarQuestions.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Công thức quy đổi: Nấc kéo (0-5) -> Số câu hỏi (5, 10, 15, 20, 25, 30)
                selectedQuestionCount = (progress * 5) + 5;

                // Cập nhật ngay lập tức con số lên màn hình
                txtSelected.setText(selectedQuestionCount + " questions");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    /**
     * Hàm xử lý khi bấm vào các nút thời gian (15s, 30s, 60s)
     */
    private void setupTimeSelection() {
        if (chipGroupTime == null) return;

        chipGroupTime.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                // Lấy ID của cái nút đang được bấm
                int checkedId = checkedIds.get(0);

                // Kiểm tra xem đó là nút nào và lưu số giây tương ứng lại
                if (checkedId == R.id.chip15s) {
                    selectedTime = 15;
                } else if (checkedId == R.id.chip30s) {
                    selectedTime = 30;
                } else if (checkedId == R.id.chip60s) {
                    selectedTime = 60;
                }
            }
        });
    }

    /**
     * Hàm xử lý khi bấm các nút (Start Generating, Back)
     */
    private void setupButtons() {
        // Xử lý nút Start Generating
        if (btnStartGenerating != null) {
            btnStartGenerating.setOnClickListener(v -> {
                // TODO: Sau này bạn sẽ viết code gọi AI ở đây!
                // Hiện tại hiển thị Toast để test xem dữ liệu có lấy đúng không
                String message = "Sẵn sàng tạo: " + selectedQuestionCount + " câu, thời gian " + selectedTime + " giây/câu";
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            });
        }

        // Xử lý nút Back (Đóng màn hình)
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }
}