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

public class manageReports extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_reports);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Display admin username if passed
        String adminUsername = getIntent().getStringExtra("admin_username");
        TextView tvLoggedInAs = findViewById(R.id.tvLoggedInAs);
        if (adminUsername != null && !adminUsername.isEmpty()) {
            tvLoggedInAs.setText("Logged in as: " + adminUsername);
        }
    }

    public void goToViewAttendance(View view) {
        Intent intent = new Intent(this, viewAttendanceReport.class);
        intent.putExtra("admin_username", getIntent().getStringExtra("admin_username"));
        startActivity(intent);
    }

    public void goToGenerateReports(View view) {
        Intent intent = new Intent(this, viewResultsReport.class);
        intent.putExtra("admin_username", getIntent().getStringExtra("admin_username"));
        startActivity(intent);
    }

    public void goToStudentDistribution(View view) {
        Intent intent = new Intent(this, courseDistributionReport.class);
        intent.putExtra("admin_username", getIntent().getStringExtra("admin_username"));
        startActivity(intent);
    }

    public void repotsGoHome(View view){
        finish();
    }
}