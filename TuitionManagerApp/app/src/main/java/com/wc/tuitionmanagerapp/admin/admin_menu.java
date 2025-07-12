package com.wc.tuitionmanagerapp.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.wc.tuitionmanagerapp.MainActivity;
import com.wc.tuitionmanagerapp.R;

public class admin_menu extends AppCompatActivity {

    private String userName;
    private TextView welcomeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_menu);

        welcomeText = findViewById(R.id.welcome_text);
        userName = getIntent().getStringExtra("username");
        welcomeText.setText("Welcome, " + userName + " (Admin)");
    }

    public void goToManageUsers(View view) {
        Intent intent = new Intent(admin_menu.this, manageUsers.class);
        intent.putExtra("admin_username", userName);
        startActivity(intent);
    }

    public void goToManageCourses(View view) {
        Intent intent = new Intent(admin_menu.this, manageCourses.class);
        intent.putExtra("admin_username", userName);
        startActivity(intent);
    }

    public void goToAssignStudents(View view) {
        Intent intent = new Intent(admin_menu.this, assignStudent.class);
        intent.putExtra("admin_username", userName);
        startActivity(intent);
    }

    public void goToViewReports(View view) {
        Intent intent = new Intent(admin_menu.this, manageReports.class);
        intent.putExtra("admin_username", userName);
        startActivity(intent);
    }

    public void exitAppForAdmin(View view){
        finishAffinity();
        System.exit(0);
    }

    public void logoutAdmin(View view){
        Intent goToLoginForAdmin = new Intent(this, MainActivity.class);
        startActivity(goToLoginForAdmin);
    }
}