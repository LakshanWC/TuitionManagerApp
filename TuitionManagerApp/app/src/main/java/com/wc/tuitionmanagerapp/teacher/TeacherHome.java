package com.wc.tuitionmanagerapp.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.wc.tuitionmanagerapp.R;

public class TeacherHome extends AppCompatActivity {

    private String userName;
    private TextView txtWelcomMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtWelcomMessage = findViewById(R.id.txt_welcomeTxt);
        userName = getIntent().getStringExtra("username");
        txtWelcomMessage.setText("Welcome, "+userName);
    }

    public void goToAssignmentUi(View view){
        Intent assingmentIntent = new Intent(TeacherHome.this,Assignment.class);
        startActivity(assingmentIntent);
    }
    public void goToCourseMaterialUi(View view){
        Intent courseMaterialIntent = new Intent(TeacherHome.this,CourseMaterial.class);
        startActivity(courseMaterialIntent);
    }
    public void goToAttendenceUi(View view){
        Intent attendenceIntent = new Intent(TeacherHome.this,Attendence.class);
        startActivity(attendenceIntent);
    }
    public void goToReultUi(View view){
        Intent resultIntent = new Intent(TeacherHome.this,Result.class);
        startActivity(resultIntent);
    }
}