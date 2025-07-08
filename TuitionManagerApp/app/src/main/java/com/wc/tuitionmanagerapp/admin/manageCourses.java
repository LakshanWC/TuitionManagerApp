package com.wc.tuitionmanagerapp.admin;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.wc.tuitionmanagerapp.R;

public class manageCourses extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_courses);


       findViewById(R.id.btnAddCourse).setOnClickListener(v -> {
           Intent intent = new Intent(manageCourses.this, addCourse.class);
           startActivity(intent);
        });

//        findViewById(R.id.btnRemoveCourse).setOnClickListener(v -> {
//            Intent intent = new Intent(manageCourses.this, RemoveCourseActivity.class);
//            startActivity(intent);
//        });
//
//        findViewById(R.id.btnAssignTeachers).setOnClickListener(v -> {
//            Intent intent = new Intent(manageCourses.this, AssignTeachersActivity.class);
//            startActivity(intent);
//        });
//
//        findViewById(R.id.btnAssignStudents).setOnClickListener(v -> {
//            Intent intent = new Intent(manageCourses.this, AssignStudentsActivity.class);
//            startActivity(intent);
//        });
    }
}