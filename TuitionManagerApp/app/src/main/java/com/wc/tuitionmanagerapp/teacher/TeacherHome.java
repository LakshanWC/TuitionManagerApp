package com.wc.tuitionmanagerapp.teacher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.wc.tuitionmanagerapp.R;

public class TeacherHome extends AppCompatActivity {

    private String userName;
    private String userId;
    private TextView txtWelcomMessage;
    private FirebaseFirestore firestoreDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firestoreDB = FirebaseFirestore.getInstance();
        txtWelcomMessage = findViewById(R.id.txt_welcomeTxt);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        userName = getIntent().getStringExtra("username");

        if (userName != null) {
            prefs.edit().putString("username", userName).apply();
        } else {
            userName = prefs.getString("username", "User");
        }

        userId = prefs.getString("userId", null);
        if (userId == null) {
            getTeacherIDbyUserName(); // and store it once fetched
        }

        txtWelcomMessage.setText("Welcome, " + userName);
    }

    public void goToAssignmentUi(View view) {
        startActivity(new Intent(this, Assignment.class));
    }

    public void goToCourseMaterialUi(View view) {
        Intent intent = new Intent(this, ManageCourseMaterials.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }

    public void goToAttendenceUi(View view) {
        startActivity(new Intent(this, Attendence.class));
    }

    public void goToReultUi(View view) {
        startActivity(new Intent(this, Result.class));
    }

    private void getTeacherIDbyUserName() {
        firestoreDB.collection("users")
                .whereEqualTo("username", userName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String fetchedUserId = queryDocumentSnapshots.getDocuments()
                                .get(0).getString("userId");
                        if (fetchedUserId != null) {
                            userId = fetchedUserId;

                            // Save it for future use
                            getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                                    .putString("userId", userId)
                                    .apply();
                        }
                    }
                });
    }
}
