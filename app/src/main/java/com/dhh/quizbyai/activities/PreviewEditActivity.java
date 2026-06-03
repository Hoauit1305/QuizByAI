package com.dhh.quizbyai.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dhh.quizbyai.R;
import com.dhh.quizbyai.models.QuestionModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PreviewEditActivity extends AppCompatActivity {

    private LinearLayout containerQuestions;
    private Button btnSave, btnCancel;
    private String quizId;
    private List<QuestionModel> questionList = new ArrayList<>();

    // Lưu lại index của đáp án đúng cho mỗi câu hỏi để đồng bộ khi Save
    private List<Integer> correctAnswersIndexes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_preview_edit);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        quizId = getIntent().getStringExtra("QUIZ_ID");

        containerQuestions = findViewById(R.id.container_questions);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        // Nút Hủy: Đóng Activity, không làm gì cả
        btnCancel.setOnClickListener(v -> finish());

        // Nút Lưu
        btnSave.setOnClickListener(v -> saveChangesToFirebase());

        loadQuestionsFromFirebase();
    }

    private void loadQuestionsFromFirebase() {
        if (quizId == null) return;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Quizzes").child(quizId).child("questions");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                questionList.clear();
                correctAnswersIndexes.clear();
                for (DataSnapshot qSnap : snapshot.getChildren()) {
                    QuestionModel q = qSnap.getValue(QuestionModel.class);
                    if (q != null) {
                        questionList.add(q);

                        // Tìm vị trí của đáp án đúng trong mảng options
                        int correctIdx = 0;
                        if(q.getOptions() != null) {
                            String correctAns = q.getAnswer().trim(); // Lấy đáp án chuẩn (có thể là "B" hoặc "B. Đáp án...")

                            for (int i = 0; i < q.getOptions().size(); i++) {
                                String optionText = q.getOptions().get(i).trim();

                                // LOGIC MỚI: Khớp hoàn toàn HOẶC chữ cái đầu của Option giống với Answer (Kèm dấu chấm hoặc khoảng trắng)
                                if (optionText.equalsIgnoreCase(correctAns) ||
                                        optionText.toUpperCase().startsWith(correctAns.toUpperCase() + ".") ||
                                        optionText.toUpperCase().startsWith(correctAns.toUpperCase() + " ")) {
                                    correctIdx = i;
                                    break;
                                }
                            }
                        }
                        correctAnswersIndexes.add(correctIdx);
                    }
                }
                renderQuestionsToUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PreviewEditActivity.this, "Lỗi tải dữ liệu!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderQuestionsToUI() {
        containerQuestions.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < questionList.size(); i++) {
            QuestionModel q = questionList.get(i);
            View questionView = inflater.inflate(R.layout.item_edit_question, containerQuestions, false);

            TextView txtQuestionNum = questionView.findViewById(R.id.txt_question_num);
            txtQuestionNum.setText("Q" + (i + 1) + ":");

            EditText edtQuestion = questionView.findViewById(R.id.edt_question_text);
            ImageButton btnEditQuestion = questionView.findViewById(R.id.btn_edit_question);

            EditText edtA = questionView.findViewById(R.id.edt_option_a);
            ImageButton btnA = questionView.findViewById(R.id.btn_edit_a);
            EditText edtB = questionView.findViewById(R.id.edt_option_b);
            ImageButton btnB = questionView.findViewById(R.id.btn_edit_b);
            EditText edtC = questionView.findViewById(R.id.edt_option_c);
            ImageButton btnC = questionView.findViewById(R.id.btn_edit_c);
            EditText edtD = questionView.findViewById(R.id.edt_option_d);
            ImageButton btnD = questionView.findViewById(R.id.btn_edit_d);

            // MỚI: Ánh xạ 4 nút RadioButton
            RadioButton radioA = questionView.findViewById(R.id.radio_a);
            RadioButton radioB = questionView.findViewById(R.id.radio_b);
            RadioButton radioC = questionView.findViewById(R.id.radio_c);
            RadioButton radioD = questionView.findViewById(R.id.radio_d);

            edtQuestion.setText(q.getQuestion());

            if (q.getOptions() != null && q.getOptions().size() >= 4) {
                edtA.setText(q.getOptions().get(0));
                edtB.setText(q.getOptions().get(1));
                edtC.setText(q.getOptions().get(2));
                edtD.setText(q.getOptions().get(3));
            }

            setupEditToggle(edtQuestion, btnEditQuestion);
            setupEditToggle(edtA, btnA);
            setupEditToggle(edtB, btnB);
            setupEditToggle(edtC, btnC);
            setupEditToggle(edtD, btnD);

            // --- LOGIC HIỂN THỊ VÀ CHỌN LẠI ĐÁP ÁN ĐÚNG ---

            // 1. Hiển thị đáp án đúng hiện tại lên UI (Dựa vào index đã lưu)
            int currentCorrectIndex = correctAnswersIndexes.get(i);
            radioA.setChecked(currentCorrectIndex == 0);
            radioB.setChecked(currentCorrectIndex == 1);
            radioC.setChecked(currentCorrectIndex == 2);
            radioD.setChecked(currentCorrectIndex == 3);

            // 2. Xử lý sự kiện khi User click vào một RadioButton bất kỳ
            int finalI = i; // Biến hằng để dùng trong lambda
            View.OnClickListener radioClickListener = v -> {
                // Đảm bảo chỉ có 1 nút được bật
                radioA.setChecked(v.getId() == R.id.radio_a);
                radioB.setChecked(v.getId() == R.id.radio_b);
                radioC.setChecked(v.getId() == R.id.radio_c);
                radioD.setChecked(v.getId() == R.id.radio_d);

                // Cập nhật lại Index mới vào mảng dữ liệu
                if (v.getId() == R.id.radio_a) correctAnswersIndexes.set(finalI, 0);
                else if (v.getId() == R.id.radio_b) correctAnswersIndexes.set(finalI, 1);
                else if (v.getId() == R.id.radio_c) correctAnswersIndexes.set(finalI, 2);
                else if (v.getId() == R.id.radio_d) correctAnswersIndexes.set(finalI, 3);
            };

            // Gắn sự kiện click cho cả 4 nút
            radioA.setOnClickListener(radioClickListener);
            radioB.setOnClickListener(radioClickListener);
            radioC.setOnClickListener(radioClickListener);
            radioD.setOnClickListener(radioClickListener);

            containerQuestions.addView(questionView);
        }
    }


    // Hàm dùng chung để tạo logic Bật/Tắt cây bút và dấu tick
    private void setupEditToggle(EditText editText, ImageButton btnEdit) {
        // Mặc định khóa (chỉ hiển thị)
        editText.setEnabled(false);
        editText.setBackgroundResource(android.R.color.transparent);

        btnEdit.setOnClickListener(v -> {
            if (!editText.isEnabled()) {
                // ĐANG KHÓA -> MỞ KHÓA ĐỂ SỬA
                editText.setEnabled(true);
                editText.setBackgroundResource(R.drawable.bg_rounded_card); // Đổi nền báo hiệu đang sửa
                btnEdit.setImageResource(R.drawable.check_24px); // Đổi icon thành dấu tick
                editText.requestFocus();
            } else {
                // ĐANG SỬA -> BẤM TICK ĐỂ LƯU LẠI VÀ KHÓA
                editText.setEnabled(false);
                editText.setBackgroundResource(android.R.color.transparent);
                btnEdit.setImageResource(R.drawable.edit_24px); // Trả lại icon bút
            }
        });
    }

    private void saveChangesToFirebase() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isGuest = prefs.getBoolean("IS_GUEST", false);

        if (isGuest) {
            Toast.makeText(this, "Chế độ khách chưa hỗ trợ sửa!", Toast.LENGTH_SHORT).show();
            return;
        }

        List<QuestionModel> updatedList = new ArrayList<>();

        // Quét toàn bộ View trên màn hình để lấy dữ liệu mới
        for (int i = 0; i < containerQuestions.getChildCount(); i++) {
            View questionView = containerQuestions.getChildAt(i);

            EditText edtQuestion = questionView.findViewById(R.id.edt_question_text);
            EditText edtA = questionView.findViewById(R.id.edt_option_a);
            EditText edtB = questionView.findViewById(R.id.edt_option_b);
            EditText edtC = questionView.findViewById(R.id.edt_option_c);
            EditText edtD = questionView.findViewById(R.id.edt_option_d);

            // Tạo list 4 đáp án mới
            List<String> newOptions = new ArrayList<>();
            newOptions.add(edtA.getText().toString());
            newOptions.add(edtB.getText().toString());
            newOptions.add(edtC.getText().toString());
            newOptions.add(edtD.getText().toString());

            // Tìm lại đáp án đúng (Answer String) dựa vào vị trí Index đã lưu
            int correctIndex = correctAnswersIndexes.get(i);
            String newAnswerString = newOptions.get(correctIndex);

            // Gói lại thành Object QuestionModel
            QuestionModel updatedQ = new QuestionModel(
                    edtQuestion.getText().toString(),
                    newOptions,
                    newAnswerString
            );

            updatedList.add(updatedQ);
        }

        // Push mảng mới lên Firebase đè cái cũ
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Quizzes").child(quizId).child("questions");
        ref.setValue(updatedList).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(PreviewEditActivity.this, "Lưu thay đổi thành công!", Toast.LENGTH_SHORT).show();
                finish(); // Đóng màn hình
            } else {
                Toast.makeText(PreviewEditActivity.this, "Lỗi khi lưu!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}