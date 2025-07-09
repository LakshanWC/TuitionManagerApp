package com.wc.tuitionmanagerapp.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.wc.tuitionmanagerapp.R;

public class StudentHome extends AppCompatActivity {

    private TextView txtWelcome;
    private String studentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        // Edge-to-edge UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get student name from login
        studentName = getIntent().getStringExtra("username");
        txtWelcome = findViewById(R.id.txtWelcomes);
        txtWelcome.setText("Welcome, " + studentName);
    }

    // Navigate to Attendance View
    public void goToAttendance(View view) {
        startActivity(new Intent(this, ViewAttendance.class));
    }

    // Navigate to Assignments
    public void goToAssignments(View view) {
        startActivity(new Intent(this, ViewAssignments.class));
    }

    // Navigate to Results
    public void goToResults(View view) {
        startActivity(new Intent(this, ViewResults.class));
    }

    // Navigate to Course Materials
    public void goToCourseMaterials(View view) {
        startActivity(new Intent(this, ViewCourseMaterials.class));
    }
}
