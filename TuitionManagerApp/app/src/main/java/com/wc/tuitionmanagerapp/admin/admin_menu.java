package com.wc.tuitionmanagerapp.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.wc.tuitionmanagerapp.R;

public class admin_menu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_menu);

        // Welcome message setup
        String username = getIntent().getStringExtra("username");
        TextView welcomeText = findViewById(R.id.welcome_text);
        if (username != null && !username.isEmpty()) {
            welcomeText.setText("Welcome, " + username + " (Admin)");
            welcomeText.setVisibility(View.VISIBLE);
        }

        // Manage Users button click
        findViewById(R.id.btnManageUsers).setOnClickListener(v -> {
            Intent intent = new Intent(admin_menu.this, manageUsers.class);
            intent.putExtra("admin_username", username); // Pass admin username if needed
            startActivity(intent);
        });

        // Add this to onCreate() in admin_menu.java
        findViewById(R.id.btnManageCourses).setOnClickListener(v -> {
            Intent intent = new Intent(admin_menu.this, manageCourses.class);
            intent.putExtra("admin_username", username); // Pass admin username if needed
            startActivity(intent);
        });
    }
}