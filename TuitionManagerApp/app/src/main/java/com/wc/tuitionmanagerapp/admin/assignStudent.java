package com.wc.tuitionmanagerapp.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.wc.tuitionmanagerapp.R;
import java.util.ArrayList;
import java.util.List;

public class assignStudent extends AppCompatActivity {

    private Spinner spinnerCourses;
    private TextInputEditText etStudentId;
    private MaterialButton btnAssign;
    private TextView tvStatus;
    private FirebaseFirestore db;
    private List<String> courseIds = new ArrayList<>();
    private String selectedCourseId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_student);

        db = FirebaseFirestore.getInstance();
        spinnerCourses = findViewById(R.id.spinnerCourses);
        etStudentId = findViewById(R.id.etStudentId);
        btnAssign = findViewById(R.id.btnAssign);
        tvStatus = findViewById(R.id.tvStatus);

        loadCourses();

        spinnerCourses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    selectedCourseId = courseIds.get(position - 1);
                } else {
                    selectedCourseId = "";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCourseId = "";
            }
        });

        btnAssign.setOnClickListener(v -> assignStudentToCourse());
    }

    private void loadCourses() {
        db.collection("courses").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<String> courseNames = new ArrayList<>();
                courseNames.add("Select a course");
                courseIds.clear();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    courseNames.add(document.getString("courseName"));
                    courseIds.add(document.getId());
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        courseNames
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCourses.setAdapter(adapter);
            } else {
                showStatus("Failed to load courses", true);
            }
        });
    }

    private void assignStudentToCourse() {
        String studentId = etStudentId.getText().toString().trim();

        if (selectedCourseId.isEmpty()) {
            showStatus("Please select a course", true);
            return;
        }

        if (studentId.isEmpty() || !studentId.startsWith("st")) {
            showStatus("Please enter a valid student ID (st1, st2, etc.)", true);
            return;
        }

        // Verify student exists
        db.collection("users")
                .whereEqualTo("userId", studentId)
                .whereEqualTo("role", "student")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Student exists, proceed with assignment
                        db.collection("courses").document(selectedCourseId)
                                .update("students", FieldValue.arrayUnion(studentId))
                                .addOnSuccessListener(aVoid -> {
                                    showStatus("Student assigned successfully!", false);
                                    Toast.makeText(this, "Student assigned", Toast.LENGTH_SHORT).show();
                                    resetForm();
                                })
                                .addOnFailureListener(e -> {
                                    showStatus("Assignment failed: " + e.getMessage(), true);
                                });
                    } else {
                        showStatus("Student ID not found or invalid", true);
                    }
                });
    }

    private void resetForm() {
        etStudentId.setText("");
        spinnerCourses.setSelection(0);
        selectedCourseId = "";
    }

    private void showStatus(String message, boolean isError) {
        tvStatus.setText(message);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setTextColor(getResources().getColor(
                isError ? R.color.negative_red : R.color.primary_color));
    }
}