package com.wc.tuitionmanagerapp.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.wc.tuitionmanagerapp.R;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Attendence extends AppCompatActivity {

    private LinearLayout qrCard ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_attendence);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        qrCard = findViewById(R.id.qr_Card);
        Animation wiggleAnimation = AnimationUtils.loadAnimation(this,R.anim.wiggle);
        qrCard.startAnimation(wiggleAnimation);
    }

    public void scanQRCode(View view){
        IntentIntegrator myIntentIntegrator = new IntentIntegrator(Attendence.this);
        myIntentIntegrator.initiateScan();
    }

    public void goesHome(View view) {
        NavigateUtil.goToTeacherHome(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                // Example: scannedData = "Lakshan:U101"
                String scannedData = result.getContents();
                handleScannedData(scannedData);
            } else {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleScannedData(String data) {
        // Example data format: "Lakshan:U101"
        String[] parts = data.split(":");
        if (parts.length == 2) {
            String studentName = parts[0];
            String studentId = parts[1];

            //get the date and time
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);

            // Assign to UI
            TextView txtStudentName = findViewById(R.id.tvStudentName);
            TextView txtStudentId = findViewById(R.id.tvStudentId);
            TextView txtDateTime = findViewById(R.id.tvDateTime);


            txtStudentName.setText(studentName);
            txtStudentId.setText(studentId);
            txtDateTime.setText(formattedDateTime);
        } else {
            Toast.makeText(this, "Invalid QR format", Toast.LENGTH_SHORT).show();
        }
    }


}