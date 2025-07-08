package com.wc.tuitionmanagerapp.admin;

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

        // Get the username from intent extras
        String username = getIntent().getStringExtra("username");

        // Find the welcome text view
        TextView welcomeText = findViewById(R.id.welcome_text);

        // Set the welcome message
        if (username != null && !username.isEmpty()) {
            welcomeText.setText("Welcome, " + username + " (Admin)");
            welcomeText.setVisibility(View.VISIBLE);
        }
    }
}