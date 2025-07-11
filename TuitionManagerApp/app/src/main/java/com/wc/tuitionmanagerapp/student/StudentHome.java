package com.wc.tuitionmanagerapp.student;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.wc.tuitionmanagerapp.R;

public class StudentHome extends AppCompatActivity {

    private TextView txtWelcome;
    private String studentName;
    private String studentId;
    private FirebaseFirestore firestoreDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        // Initialize Firestore
        firestoreDB = FirebaseFirestore.getInstance();

        // Edge-to-edge UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get student name from login
        studentName = getIntent().getStringExtra("username");
        txtWelcome = findViewById(R.id.txtWelcome);
        txtWelcome.setText("Welcome, " + studentName);

        SharedPreferences prefStudent = getSharedPreferences("student_prefs", MODE_PRIVATE);

        if (studentName != null) {
            prefStudent.edit().putString("username", studentName).apply();
        } else {
            studentName = prefStudent.getString("username", "User");
        }

    }

    // Add this interface at the bottom of the file (before the last closing brace)
    interface StudentIdCallback {
        void onSuccess(String studentId);
        void onFailure(Exception e);
    }

    // Add this new method
    private void getStudentIdByUsername(String username, StudentIdCallback callback) {
        firestoreDB.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String studentId = task.getResult().getDocuments().get(0).getString("userId");
                        if (studentId != null) {
                            callback.onSuccess(studentId);
                        } else {
                            callback.onFailure(new Exception("Student ID not found in document"));
                        }
                    } else {
                        callback.onFailure(task.getException() != null ?
                                task.getException() :
                                new Exception("User not found"));
                    }
                });
    }

    // Update your existing goToAttendance method
    public void goToAttendance(View view) {
        getStudentIdByUsername(studentName, new StudentIdCallback() {
            @Override
            public void onSuccess(String studentId) {
                Intent intent = new Intent(StudentHome.this, ViewAttendance.class);
                intent.putExtra("studentId", studentId);
                startActivity(intent);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(StudentHome.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("StudentHome", "Failed to get student ID", e);
            }
        });
    }

    // Keep your other goTo methods unchanged
    public void goToAssignments(View view) {
        startActivity(new Intent(this, ViewAssignments.class));
    }

    public void goToResults(View view) {
        startActivity(new Intent(this, ViewResults.class));
    }

    public void goToCourseMaterials(View view) {
        startActivity(new Intent(this, ViewCourseMaterials.class));
    }
}