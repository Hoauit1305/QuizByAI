package com.dhh.quizbyai.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dhh.quizbyai.R;

public class UploadActivity extends BaseActivity {

    // Khai báo launcher để xử lý kết quả sau khi chọn file
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_upload);

        // Thiết lập tràn viền
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Kích hoạt Bottom Navigation từ BaseActivity
        setupBottomNavigation();

        // 2. Khởi tạo bộ chọn file
        setupFilePicker();

        // 3. Ánh xạ nút bấm và xử lý sự kiện click
        Button btnChooseFile = findViewById(R.id.button);
        btnChooseFile.setOnClickListener(v -> openFilePicker());
    }

    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            // Xử lý file sau khi chọn thành công (ví dụ: lấy tên file hoặc upload)
                            Toast.makeText(this, "Đã chọn file: " + fileUri.getPath(), Toast.LENGTH_SHORT).show();
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
}