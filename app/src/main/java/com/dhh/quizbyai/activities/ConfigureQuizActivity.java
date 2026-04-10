package com.dhh.quizbyai.activities;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dhh.quizbyai.R;
import com.dhh.quizbyai.models.QuestionModel;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.chip.ChipGroup;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.firebase.database.DatabaseReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConfigureQuizActivity extends BaseActivity {

    private static final String TAG = "AI_DEBUG";
    private static final String MODEL_NAME = "gemini-2.5-flash";
    private static final String API_KEY = "AIzaSyAbAq25wnucTgV-lcQQYOWL_bk4oEUmvGU";
    private boolean isGuest = false;

    // UI Elements
    private SeekBar seekBarQuestions;
    private TextView txtSelectedCount;
    private EditText etxtQuizName;
    private Button btnStartGenerating, btnBack;
    private ChipGroup chipGroupTime;

    // Data
    private int selectedQuestionCount = 20;
    private int selectedTimePerQuestion = 15;
    private String fileUriString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_configure_quiz);
        isGuest = getIntent().getBooleanExtra("IS_GUEST", false);
        initViews();
        setupWindowInsets();
        setupListeners();

        fileUriString = getIntent().getStringExtra("FILE_URI");
        Log.d(TAG, "ConfigureQuizActivity initialized. File: " + fileUriString);
    }

    private void initViews() {
        etxtQuizName = findViewById(R.id.etxt_name_quiz);
        seekBarQuestions = findViewById(R.id.seekBar_NoQ);
        txtSelectedCount = findViewById(R.id.txt_selected_NoQ);
        btnStartGenerating = findViewById(R.id.btn_start_gen);
        btnBack = findViewById(R.id.btn_back);
        chipGroupTime = findViewById(R.id.chipGroupTime);
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    private void setupListeners() {
        // SeekBar setup
        seekBarQuestions.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedQuestionCount = (progress * 5) + 5;
                txtSelectedCount.setText(String.format(Locale.getDefault(), "%d questions", selectedQuestionCount));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Time ChipGroup setup
        chipGroupTime.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int id = checkedIds.get(0);
                if (id == R.id.chip15s) selectedTimePerQuestion = 15;
                else if (id == R.id.chip30s) selectedTimePerQuestion = 30;
                else if (id == R.id.chip60s) selectedTimePerQuestion = 60;
            }
        });

        // Buttons
        btnBack.setOnClickListener(v -> finish());
        btnStartGenerating.setOnClickListener(v -> handleGenerateClick());
    }

    private void handleGenerateClick() {
        if (fileUriString == null || fileUriString.isEmpty()) {
            showError("Không tìm thấy file PDF!");
            return;
        }

        String quizName = etxtQuizName.getText().toString().trim();
        if (quizName.isEmpty()) {
            etxtQuizName.setError("Vui lòng nhập tên bài Quiz");
            return;
        }

        Uri pdfUri = Uri.parse(fileUriString);
        String docName = getFileNameFromUri(pdfUri);

        setLoadingState(true);
        startAIQuizGeneration(pdfUri, docName);
    }

    private void startAIQuizGeneration(Uri pdfUri, String docName) {
        Log.d(TAG, "Starting AI generation...");
        GenerativeModel gm = new GenerativeModel(MODEL_NAME, API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        try {
            byte[] pdfBytes = getBytesFromUri(pdfUri);
            String prompt = String.format(Locale.getDefault(),
                    "Dựa trên tài liệu PDF này, hãy tạo %d câu hỏi trắc nghiệm. " +
                            "Trả về định dạng JSON duy nhất: [{\"question\": \"...\", \"options\": [\"...\"], \"answer\": \"...\"}]",
                    selectedQuestionCount);

            Content content = new Content.Builder()
                    .addText(prompt)
                    .addBlob("application/pdf", pdfBytes)
                    .build();

            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
            Futures.addCallback(response, new FutureCallback<>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    processAIResponse(result.getText(), pdfUri, docName);
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                    handleError("AI Error: " + t.getMessage());
                }
            }, ContextCompat.getMainExecutor(this));

        } catch (IOException e) {
            handleError("Lỗi đọc file: " + e.getMessage());
        }
    }

    private void processAIResponse(String jsonOutput, Uri pdfUri, String docName) {
        if (jsonOutput == null || jsonOutput.isEmpty()) {
            handleError("AI không trả về dữ liệu.");
            return;
        }
        Log.d(TAG, "AI Response success.");

        // Truyền trực tiếp kết quả JSON vào hàm lưu Database, để trống URL
        saveQuizToDatabase(jsonOutput, docName, "");
    }
    private void saveQuizLocally(Map<String, Object> quizData) {
        // 1. Mở "cuốn sổ tay" tên là "GuestQuizzes"
        SharedPreferences sharedPreferences = getSharedPreferences("GuestData", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // 2. Lấy danh sách các bài Quiz cũ đã lưu (nếu có)
        String existingQuizzesJson = sharedPreferences.getString("local_quizzes", "[]");

        Gson gson = new Gson();
        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> localQuizList = gson.fromJson(existingQuizzesJson, listType);

        // 3. Thêm bài Quiz mới vừa tạo vào danh sách
        localQuizList.add(quizData);

        // 4. Dịch ngược cả danh sách thành chuỗi JSON và cất vào sổ
        String updatedQuizzesJson = gson.toJson(localQuizList);
        editor.putString("local_quizzes", updatedQuizzesJson);
        editor.apply(); // Lưu lại!

        Log.d(TAG, "Đã lưu Quiz vào bộ nhớ máy (Chế độ Khách)");
    }
    private void saveQuizToDatabase(String jsonResult, String docName, String fileUrl) {
        try {
            String cleanJson = jsonResult.replaceAll("```json|```", "").trim();
            Gson gson = new Gson();
            Type listType = new TypeToken<List<QuestionModel>>(){}.getType();
            List<QuestionModel> questions = gson.fromJson(cleanJson, listType);

            Map<String, Object> quizMap = new HashMap<>();
            quizMap.put("title", etxtQuizName.getText().toString().trim());
            quizMap.put("timePerQuestion", selectedTimePerQuestion);
            quizMap.put("questions", questions);
            quizMap.put("documentName", docName);
            quizMap.put("documentUrl", fileUrl); // Lưu chuỗi rỗng để cấu trúc dữ liệu đồng nhất
            quizMap.put("createdAt", System.currentTimeMillis());
            quizMap.put("score", 0);
            quizMap.put("questionCount", questions.size());

            if (isGuest) {
                saveQuizLocally(quizMap);
                showSuccess("Đã lưu Quiz vào máy!");
                setLoadingState(false);
                finish();
                return;
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                quizMap.put("creatorId", currentUser.getUid());
                quizMap.put("creatorEmail", currentUser.getEmail());

                // 1. Tách riêng lệnh push() ra và gán nó vào biến newQuizRef
                DatabaseReference newQuizRef = FirebaseDatabase.getInstance("https://quizbyai-4d9d2-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Quizzes").push();

                // 2. Lấy cái ID vừa được sinh ra (ví dụ: -Nxx_Mã_ID_Ngẫu_Nhiên)
                String quizId = newQuizRef.getKey();

                // 3. Tiến hành đẩy dữ liệu (quizMap) vào nhánh có ID đó
                newQuizRef.setValue(quizMap)
                        .addOnSuccessListener(aVoid -> {
                            showSuccess("Lưu Quiz thành công!");
                            setLoadingState(false);

                            // 4. CHUYỂN MÀN HÌNH VÀ GỬI KÈM QUIZ_ID CHO QUESTION ACTIVITY CỦA BẠN
                            android.content.Intent intent = new android.content.Intent(ConfigureQuizActivity.this, MyQuizzedActivity.class);
                            intent.putExtra("QUIZ_ID", quizId);
                            startActivity(intent);

                            finish();
                        })
                        .addOnFailureListener(e -> handleError("Lỗi Database: " + e.getMessage()));
            }

        } catch (Exception e) {
            handleError("Lỗi xử lý: " + e.getMessage());
        }
    }

    private void setLoadingState(boolean isLoading) {
        btnStartGenerating.setEnabled(!isLoading);
        btnStartGenerating.setText(isLoading ? "Processing..." : "Start Generating");
    }

    private void handleError(String message) {
        Log.e(TAG, message);
        showError(message);
        setLoadingState(false);
    }

    private void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void showSuccess(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private byte[] getBytesFromUri(Uri uri) throws IOException {
        try (InputStream is = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while (is != null && (len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        }
    }

    @SuppressLint("Range")
    private String getFileNameFromUri(Uri uri) {
        String name = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (name == null) {
            name = uri.getPath();
            int cut = name != null ? name.lastIndexOf('/') : -1;
            if (cut != -1) name = name.substring(cut + 1);
        }
        return name != null ? name : "document.pdf";
    }
}