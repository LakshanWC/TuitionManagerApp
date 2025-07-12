package com.wc.tuitionmanagerapp.student;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.wc.tuitionmanagerapp.R;

import java.util.HashMap;
import java.util.Map;

public class SubmitAssignment extends AppCompatActivity {
    private FirebaseFirestore db;
    private String assignmentId;
    private String assignmentName;
    private String courseId;
    private String studentId;
    private EditText etFileUrl;
    private Button btnSubmit;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_submit_assignment);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get data from intent
        assignmentId = getIntent().getStringExtra("assignmentId");
        assignmentName = getIntent().getStringExtra("assignmentName");
        courseId = getIntent().getStringExtra("courseId");
        studentId = getIntent().getStringExtra("studentId");

        // Initialize views
        etFileUrl = findViewById(R.id.etFileUrl);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);
        TextView tvAssignmentName = findViewById(R.id.tvAssignmentName);
        TextView tvCourseName = findViewById(R.id.tvCourseName);

        // Set assignment info
        tvAssignmentName.setText("Assignment: " + assignmentName);

        // Get course name
        db.collection("courses").document(courseId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String courseName = documentSnapshot.getString("courseName");
                    tvCourseName.setText("Course: " + (courseName != null ? courseName : courseId));
                });

        // Submit button click handler
        btnSubmit.setOnClickListener(v -> submitAssignment());
    }

    private void submitAssignment() {
        String fileUrl = etFileUrl.getText().toString().trim();

        if (fileUrl.isEmpty()) {
            Toast.makeText(this, "Please enter file URL", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!fileUrl.startsWith("http")) {
            Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        // Create submission data with empty grade
        Map<String, Object> submission = new HashMap<>();
        submission.put("courseId", courseId);
        submission.put("fileUrl", fileUrl);
        submission.put("studentId", studentId);
        submission.put("submissionDate", Timestamp.now());
        submission.put("status", "submitted");
        submission.put("assignmentId", assignmentId);
        submission.put("grade", ""); // Empty grade field added here

        // Add to submitted_assignments collection
        db.collection("submitted_assignments")
                .add(submission)
                .addOnSuccessListener(documentReference -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Assignment submitted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Submission failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}