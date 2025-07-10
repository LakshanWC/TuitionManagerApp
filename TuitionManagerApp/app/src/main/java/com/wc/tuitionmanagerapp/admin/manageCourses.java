package com.wc.tuitionmanagerapp.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import com.wc.tuitionmanagerapp.R;

public class manageCourses extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_courses);


//       findViewById(R.id.btnAddCourse).setOnClickListener(v -> {
//           Intent intent = new Intent(manageCourses.this, addCourse.class);
//           startActivity(intent);
//        });
//
//        findViewById(R.id.btnRemoveCourse).setOnClickListener(v -> {
//            Intent intent = new Intent(manageCourses.this, removeCourse.class);
//            startActivity(intent);
//        });
//
//        findViewById(R.id.btnAssignTeachers).setOnClickListener(v -> {
//            Intent intent = new Intent(manageCourses.this, assignTeacher.class);
//            startActivity(intent);
//        });
//
//        findViewById(R.id.btnAssignStudents).setOnClickListener(v -> {
//            Intent intent = new Intent(manageCourses.this, assignStudent.class);
//            startActivity(intent);
//        });
    }
    // Add these methods to your existing manageCourses class
    public void goToAddCourse(View view) {
        Intent intent = new Intent(manageCourses.this, addCourse.class);
        startActivity(intent);
    }

    public void goToRemoveCourse(View view) {
        Intent intent = new Intent(manageCourses.this, removeCourse.class);
        startActivity(intent);
    }

    public void goToAssignTeachers(View view) {
        Intent intent = new Intent(manageCourses.this, assignTeacher.class);
        startActivity(intent);
    }

    public void goToAssignStudents(View view) {
        Intent intent = new Intent(manageCourses.this, assignStudent.class);
        startActivity(intent);
    }
}