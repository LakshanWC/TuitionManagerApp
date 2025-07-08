package com.wc.tuitionmanagerapp.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.wc.tuitionmanagerapp.R;

public class manageUsers extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_users);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Display admin username if passed
        String adminUsername = getIntent().getStringExtra("admin_username");
        TextView tvAdminUsername = findViewById(R.id.tvAdminUsername);
        if (adminUsername != null && !adminUsername.isEmpty()) {
            tvAdminUsername.setText("Logged in as: " + adminUsername);
        }

        // In manageUsers.java, update the btnRegisterUser click listener:
        findViewById(R.id.btnRegisterUser).setOnClickListener(v -> {
            Intent intent = new Intent(manageUsers.this, registerUsers.class);
            intent.putExtra("admin_username", adminUsername);
            startActivity(intent);
        });

//        // Set up button click listeners
//        findViewById(R.id.btnRegisterUser).setOnClickListener(v -> {
//            // Start activity to register new user
//            Intent intent = new Intent(manageUsers.this, RegisterUserActivity.class);
//            intent.putExtra("admin_username", adminUsername);
//            startActivity(intent);
//        });
//
//        findViewById(R.id.btnViewUsers).setOnClickListener(v -> {
//            // Start activity to view all users
//            Intent intent = new Intent(manageUsers.this, ViewUsersActivity.class);
//            intent.putExtra("admin_username", adminUsername);
//            startActivity(intent);
//        });
//
//        findViewById(R.id.btnRemoveUser).setOnClickListener(v -> {
//            // Start activity to remove user
//            Intent intent = new Intent(manageUsers.this, RemoveUserActivity.class);
//            intent.putExtra("admin_username", adminUsername);
//            startActivity(intent);
//        });
    }
}