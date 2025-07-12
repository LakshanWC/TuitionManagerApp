package com.wc.tuitionmanagerapp.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.wc.tuitionmanagerapp.R;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Attendence extends AppCompatActivity {

    private FirebaseFirestore firestoreDB;
    private LinearLayout qrCard ;
    private Spinner courseNamesSpinner;
    private List<String> courseNamesFound = new ArrayList<>();

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

        courseNamesSpinner = findViewById(R.id.courseSpinner);
        firestoreDB = FirebaseFirestore.getInstance();

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
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String formattedDateTime = now.format(formatter);

            // Assign to UI
            TextView txtStudentName = findViewById(R.id.tvStudentName);
            TextView txtStudentId = findViewById(R.id.tvStudentId);
            TextView txtDateTime = findViewById(R.id.tvDateTime);


            txtStudentName.setText(studentName);
            txtStudentId.setText(studentId);
            txtDateTime.setText(formattedDateTime);

            fetchCourseNames(studentId);

        } else {
            Toast.makeText(this, "Invalid QR format", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchCourseNames(String studentId) {
        firestoreDB.collection("courses")
                .whereArrayContains("students", studentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            courseNamesFound.add(doc.getString("courseName"));
                        }

                        // Update Spinner
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                Attendence.this,
                                android.R.layout.simple_spinner_item,
                                courseNamesFound
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        courseNamesSpinner.setAdapter(adapter);

                    } else {
                        Log.d("COURSE_FOUND", "No courses found for studentId: " + studentId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("COURSE_FETCH_FAILED", "Error: ", e);
                });
    }

    public void markAttendance(View view) {
        String currentCName = courseNamesSpinner.getSelectedItem().toString();

        firestoreDB.collection("courses")
                .whereEqualTo("courseName", currentCName)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        String courseId = doc.getString("courseId");

                        setAttendace(courseId,view);

                        if (courseId != null) {
                            Log.d("COURSE_ID", "Found courseId: " + courseId);
                        } else {
                            Toast.makeText(this, "courseId not found in document", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "No course found with that name", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE_ERROR", "Error fetching courseId", e);
                    Toast.makeText(this, "Error fetching courseId", Toast.LENGTH_SHORT).show();
                });
    }

    public void setAttendace(String currentCourseId,View view){

        Button clickedButton = (Button) view;
        int id = clickedButton.getId();

        TextView tvStudentName = findViewById(R.id.tvStudentName);
        TextView tvStudentId = findViewById(R.id.tvStudentId);
        TextView tvDateTime = findViewById(R.id.tvDateTime);

        String currentSTName = tvStudentName.getText().toString();
        String currentSTId = tvStudentId.getText().toString();
        String currentDT = tvDateTime.getText().toString();

        Map<String,Object> attendanceData =new HashMap<>();
        attendanceData.put("userId", currentSTId);
        attendanceData.put("username", currentSTName);
        attendanceData.put("courseId", currentCourseId);
        attendanceData.put("date", currentDT);


        // Add status for each button
        if (id == R.id.btnMarkAttendance) {
            attendanceData.put("status", "Present");
        } else if (id == R.id.btnAbsent) {
            attendanceData.put("status", "Absent");
        }

        firestoreDB.collection("attendance")
                .add(attendanceData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Attendance marked successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to mark attendance: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("MARK_ATTENDANCE", "Error adding document", e);
                });
    }
}