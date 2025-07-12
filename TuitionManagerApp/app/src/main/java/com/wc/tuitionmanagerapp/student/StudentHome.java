package com.wc.tuitionmanagerapp.student;

import static com.google.api.client.util.Data.mapOf;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.wc.tuitionmanagerapp.MainActivity;
import com.wc.tuitionmanagerapp.R;

public class StudentHome extends AppCompatActivity {

    private TextView txtWelcome;
    private String studentName;
    private String studentId;
    private FirebaseFirestore firestoreDB;
    private boolean qrVisible = false;
    private ImageView imgQR;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        // Initialize Firestore
        firestoreDB = FirebaseFirestore.getInstance();

        // Edge-to-edge UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        LinearLayout qrCodeWiggle = findViewById(R.id.showQRCode);
        Animation wiggleAnimation = AnimationUtils.loadAnimation(this,R.anim.wiggle);
        qrCodeWiggle.startAnimation(wiggleAnimation);

        // Get student name from login
        studentName = getIntent().getStringExtra("username");
        txtWelcome = findViewById(R.id.txtWelcome);
        txtWelcome.setText("Welcome, " + studentName);

        SharedPreferences prefStudent = getSharedPreferences("student_prefs", MODE_PRIVATE);

        if (studentName != null) {
            prefStudent.edit().putString("username", studentName).apply();
        } else {
            studentName = prefStudent.getString("username", "User");
        }

    }

    // Add this interface at the bottom of the file (before the last closing brace)
    interface StudentIdCallback {
        void onSuccess(String studentId);
        void onFailure(Exception e);
    }

    // Add this new method
    private void getStudentIdByUsername(String username, StudentIdCallback callback) {
        firestoreDB.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String studentId = task.getResult().getDocuments().get(0).getString("userId");
                        if (studentId != null) {
                            callback.onSuccess(studentId);
                        } else {
                            callback.onFailure(new Exception("Student ID not found in document"));
                        }
                    } else {
                        callback.onFailure(task.getException() != null ?
                                task.getException() :
                                new Exception("User not found"));
                    }
                });
    }

    // Update your existing goToAttendance method
    public void goToAttendance(View view) {
        getStudentIdByUsername(studentName, new StudentIdCallback() {
            @Override
            public void onSuccess(String studentId) {
                Intent intent = new Intent(StudentHome.this, ViewAttendance.class);
                intent.putExtra("studentId", studentId);
                startActivity(intent);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(StudentHome.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("StudentHome", "Failed to get student ID", e);
            }
        });
    }

    // Keep your other goTo methods unchanged
    public void goToAssignments(View view) {
        startActivity(new Intent(this, ViewAssignments.class));
    }

    public void goToResults(View view) {
        Intent intent = new Intent(this, ViewResults.class);
        intent.putExtra("username", studentName); // studentName is from your existing code
        startActivity(intent);
    }

    public void goToCourseMaterials(View view) {
        startActivity(new Intent(this, ViewCourseMaterials.class));
    }

    public void logoutStudent(View view){
        Intent logoutStudnetIntent = new Intent(this, MainActivity.class);
        startActivity(logoutStudnetIntent);
    }

    public void exitAppFromStudent(View view){
        finishAffinity();
        System.exit(0);
    }

    public void showQRCode(View view) {
        getStudentIdByUsername(studentName, new StudentIdCallback() {
            @Override
            public void onSuccess(String studentId) {
                String data = studentName + ":" + studentId;
                int size = 512;

                try {
                    if(!qrVisible) {
                        BarcodeEncoder encoder = new BarcodeEncoder();
                        Bitmap bitmap = encoder.encodeBitmap(data, BarcodeFormat.QR_CODE, size, size);

                        imgQR = findViewById(R.id.currentQR);
                        imgQR.setImageBitmap(bitmap);
                        imgQR.setVisibility(View.VISIBLE);
                        qrVisible = true;
                    }
                    else{
                        qrVisible = false;
                        imgQR = findViewById(R.id.currentQR);
                        imgQR.setImageResource(R.drawable.qr_code_48px);
                        imgQR.setVisibility(View.VISIBLE);
                    }

                } catch (WriterException e) {
                    Toast.makeText(StudentHome.this, "QR generation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("StudentHome", "QR encode error", e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(StudentHome.this, "Error fetching ID: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("StudentHome", "Failed to get student ID", e);
            }
        });
    }


}