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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.wc.tuitionmanagerapp.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class assignTeacher extends AppCompatActivity {

    private Spinner spinnerCourses, spinnerTeachers;
    private MaterialButton btnAssign;
    private TextView tvStatus;
    private FirebaseFirestore db;
    private List<String> courseIds = new ArrayList<>();
    private List<String> teacherIds = new ArrayList<>();
    private String selectedCourseId = "";
    private String selectedTeacherId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_teacher);

        db = FirebaseFirestore.getInstance();
        spinnerCourses = findViewById(R.id.spinnerCourses);
        spinnerTeachers = findViewById(R.id.spinnerTeachers);
        btnAssign = findViewById(R.id.btnAssign);
        tvStatus = findViewById(R.id.tvStatus);

        loadCourses();
        loadTeachers();

        spinnerCourses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Skip hint item
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

        spinnerTeachers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Skip hint item
                    selectedTeacherId = teacherIds.get(position - 1);
                } else {
                    selectedTeacherId = "";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedTeacherId = "";
            }
        });

        btnAssign.setOnClickListener(v -> assignTeacherToCourse());
    }

    private void loadCourses() {
        db.collection("courses").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<String> courseNames = new ArrayList<>();
                courseNames.add("Select a course"); // Hint
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

    private void loadTeachers() {
        db.collection("users")
                .whereEqualTo("role", "teacher")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> teacherNames = new ArrayList<>();
                        teacherNames.add("Select a teacher"); // Hint
                        teacherIds.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            teacherNames.add(document.getString("username") + " (" + document.getString("userId") + ")");
                            teacherIds.add(document.getString("userId"));
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this,
                                android.R.layout.simple_spinner_item,
                                teacherNames
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerTeachers.setAdapter(adapter);
                    } else {
                        showStatus("Failed to load teachers", true);
                    }
                });
    }

    private void assignTeacherToCourse() {
        if (selectedCourseId.isEmpty() || selectedTeacherId.isEmpty()) {
            showStatus("Please select both course and teacher", true);
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put("teachers", com.google.firebase.firestore.FieldValue.arrayUnion(selectedTeacherId));

        db.collection("courses").document(selectedCourseId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    showStatus("Teacher assigned successfully!", false);
                    Toast.makeText(this, "Teacher assigned", Toast.LENGTH_SHORT).show();
                    resetSelection();
                })
                .addOnFailureListener(e -> {
                    showStatus("Assignment failed: " + e.getMessage(), true);
                });
    }

    private void resetSelection() {
        spinnerCourses.setSelection(0);
        spinnerTeachers.setSelection(0);
        selectedCourseId = "";
        selectedTeacherId = "";
    }

    private void showStatus(String message, boolean isError) {
        tvStatus.setText(message);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setTextColor(getResources().getColor(
                isError ? R.color.negative_red : R.color.primary_color));
    }
}