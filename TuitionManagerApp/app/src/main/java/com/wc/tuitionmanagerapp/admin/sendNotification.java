package com.wc.tuitionmanagerapp.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.wc.tuitionmanagerapp.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class sendNotification extends AppCompatActivity {

    private Spinner spinnerStudents;
    private MaterialButton btnSend;
    private TextView tvStatus;
    private TextInputEditText etTitle, etMessage;
    private FirebaseFirestore db;
    private List<String> studentIds = new ArrayList<>();
    private String selectedStudentId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_notification);

        db = FirebaseFirestore.getInstance();
        spinnerStudents = findViewById(R.id.spinnerStudents);
        btnSend = findViewById(R.id.btnSend);
        tvStatus = findViewById(R.id.tvStatus);
        etTitle = findViewById(R.id.etTitle);
        etMessage = findViewById(R.id.etMessage);

        loadStudents();

        spinnerStudents.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Skip hint item
                    selectedStudentId = studentIds.get(position - 1);
                } else {
                    selectedStudentId = "";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedStudentId = "";
            }
        });

        btnSend.setOnClickListener(v -> sendNotification());
    }

    private void loadStudents() {
        db.collection("users")
                .whereEqualTo("role", "student")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> studentNames = new ArrayList<>();
                        studentNames.add("Select a student"); // Hint
                        studentIds.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String userId = document.getString("userId");
                            String username = document.getString("username");

                            if (userId != null && userId.startsWith("st")) {
                                studentIds.add(userId);
                                studentNames.add(username + " (" + userId + ")");
                            }
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this,
                                android.R.layout.simple_spinner_item,
                                studentNames
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerStudents.setAdapter(adapter);
                    } else {
                        showStatus("Failed to load students", true);
                    }
                });
    }

    private void sendNotification() {
        String title = etTitle.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

        if (selectedStudentId.isEmpty()) {
            showStatus("Please select a student", true);
            return;
        }

        if (title.isEmpty()) {
            showStatus("Please enter a title", true);
            return;
        }

        if (message.isEmpty()) {
            showStatus("Please enter a message", true);
            return;
        }

        // Create timestamp string
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("message", message);
        notification.put("studentId", selectedStudentId);
        notification.put("isRead", false);
        notification.put("timestamp", timestamp); // Stored as string

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    showStatus("Notification sent successfully!", false);
                    Toast.makeText(this, "Notification sent", Toast.LENGTH_SHORT).show();
                    resetForm();
                })
                .addOnFailureListener(e -> {
                    showStatus("Failed to send notification: " + e.getMessage(), true);
                });
    }

    private void resetForm() {
        spinnerStudents.setSelection(0);
        etTitle.getText().clear();
        etMessage.getText().clear();
        selectedStudentId = "";
    }

    private void showStatus(String message, boolean isError) {
        tvStatus.setText(message);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setTextColor(getResources().getColor(
                isError ? R.color.negative_red : R.color.primary_color));
    }
}