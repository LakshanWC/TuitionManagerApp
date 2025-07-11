package com.wc.tuitionmanagerapp.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.wc.tuitionmanagerapp.R;

import java.util.HashMap;
import java.util.Map;

public class AttendeceReport extends AppCompatActivity {

    private static final String TAG = "AttendanceReport";
    private EditText etStudentId;
    private Button btnGenerateReport;
    private TextView tvReport;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendece_report);

        // Initialize views
        etStudentId = findViewById(R.id.etStudentId);
        btnGenerateReport = findViewById(R.id.btnGenerateReport);
        tvReport = findViewById(R.id.tvReport);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        btnGenerateReport.setOnClickListener(v -> {
            String studentId = etStudentId.getText().toString().trim();
            if (validateInput(studentId)) {
                fetchStudentAttendance(studentId);
            }
        });
    }

    private boolean validateInput(String studentId) {
        if (studentId.isEmpty()) {
            etStudentId.setError("Please enter student ID");
            etStudentId.requestFocus();
            return false;
        }
        return true;
    }

    private void fetchStudentAttendance(String studentId) {
        showLoading(true);
        tvReport.setVisibility(View.GONE);

        // First check if student exists
        db.collection("students").document(studentId)
                .get()
                .addOnCompleteListener(studentTask -> {
                    if (studentTask.isSuccessful() && studentTask.getResult().exists()) {
                        // Student exists, now get their courses
                        fetchStudentCourses(studentId);
                    } else {
                        showLoading(false);
                        tvReport.setText("Student not found");
                        tvReport.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error checking student", e);
                });
    }

    private void fetchStudentCourses(String studentId) {
        db.collection("student_courses")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnCompleteListener(coursesTask -> {
                    if (coursesTask.isSuccessful() && !coursesTask.getResult().isEmpty()) {
                        // Process each course
                        processCourses(studentId, coursesTask.getResult());
                    } else {
                        showLoading(false);
                        tvReport.setText("No courses found for this student");
                        tvReport.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error fetching courses: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching courses", e);
                });
    }

    private void processCourses(String studentId, Iterable<QueryDocumentSnapshot> courses) {
        Map<String, String> courseMap = new HashMap<>();
        for (QueryDocumentSnapshot courseDoc : courses) {
            String courseId = courseDoc.getString("courseId");
            String courseName = courseDoc.getString("courseName");
            if (courseId != null) {
                courseMap.put(courseId, courseName != null ? courseName : courseId);
            }
        }

        if (courseMap.isEmpty()) {
            showLoading(false);
            tvReport.setText("No valid courses found");
            tvReport.setVisibility(View.VISIBLE);
            return;
        }

        fetchAttendanceRecords(studentId, courseMap);
    }

    private void fetchAttendanceRecords(String studentId, Map<String, String> courseMap) {
        StringBuilder reportBuilder = new StringBuilder();
        final int[] processedCourses = {0};
        final int totalCourses = courseMap.size();

        for (Map.Entry<String, String> entry : courseMap.entrySet()) {
            String courseId = entry.getKey();
            String courseName = entry.getValue();

            db.collection("attendance")
                    .whereEqualTo("studentId", studentId)
                    .whereEqualTo("courseId", courseId)
                    .whereEqualTo("status", "Present")
                    .get()
                    .addOnCompleteListener(attendanceTask -> {
                        processedCourses[0]++;

                        if (attendanceTask.isSuccessful()) {
                            int presentCount = attendanceTask.getResult().size();
                            reportBuilder.append("Course: ").append(courseName)
                                    .append("\nPresent Count: ").append(presentCount)
                                    .append("\n\n");
                        } else {
                            reportBuilder.append("Course: ").append(courseName)
                                    .append("\nError loading attendance data\n\n");
                        }

                        // Check if all courses processed
                        if (processedCourses[0] == totalCourses) {
                            showLoading(false);
                            tvReport.setText(reportBuilder.toString());
                            tvReport.setVisibility(View.VISIBLE);
                        }
                    });
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnGenerateReport.setEnabled(!isLoading);
    }
}