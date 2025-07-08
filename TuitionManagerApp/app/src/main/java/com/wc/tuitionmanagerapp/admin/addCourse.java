package com.wc.tuitionmanagerapp.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.wc.tuitionmanagerapp.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class addCourse extends AppCompatActivity {

    private TextInputEditText etCourseId, etCourseName, etStartDate, etEndDate;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_course);

        // Initialize views
        etCourseId = findViewById(R.id.etCourseId);
        etCourseName = findViewById(R.id.etCourseName);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);

        // Date Pickers
        setupDatePicker(etStartDate);
        setupDatePicker(etEndDate);

        // Add Course Button
        findViewById(R.id.btnAddCourse).setOnClickListener(v -> addCourseToFirestore());
    }

    private void setupDatePicker(TextInputEditText editText) {
        final Calendar calendar = Calendar.getInstance();
        editText.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, day) -> {
                String date = year + "-" + (month + 1) + "-" + day;
                editText.setText(date);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void addCourseToFirestore() {
        String courseId = etCourseId.getText().toString().trim();
        String courseName = etCourseName.getText().toString().trim();
        String startDate = etStartDate.getText().toString().trim();
        String endDate = etEndDate.getText().toString().trim();

        if (courseId.isEmpty() || courseName.isEmpty() || startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> course = new HashMap<>();
        course.put("courseId", courseId);
        course.put("courseName", courseName);
        course.put("startDate", startDate);
        course.put("endDate", endDate);
        course.put("teachers", new ArrayList<String>()); // Empty array for teachers
        course.put("students", new ArrayList<String>()); // Empty array for students

        db.collection("courses").document(courseId)
                .set(course)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Course added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error adding course", Toast.LENGTH_SHORT).show());
    }
}