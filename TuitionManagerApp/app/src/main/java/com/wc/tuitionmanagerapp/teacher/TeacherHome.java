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
import com.google.firebase.firestore.QuerySnapshot;
import com.wc.tuitionmanagerapp.MainActivity;
import com.wc.tuitionmanagerapp.R;

public class TeacherHome extends AppCompatActivity {

    private String userName;
    private String userId;
    private TextView txtWelcomeMessage;
    private FirebaseFirestore firestoreDB;
    private GoogleDriveHelper googleDriveHelper;
    private boolean isDriveServiceReady = false;

    private static final int REQUEST_CODE_SIGN_IN = 1002;

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
        txtWelcomeMessage = findViewById(R.id.txt_welcomeTxt);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        userName = getIntent().getStringExtra("username");
        if (userName != null) {
            prefs.edit().putString("username", userName).apply();
        } else {
            userName = prefs.getString("username", "User");
        }

        userId = prefs.getString("userId", null);

        // If userId not stored, fetch from Firestore first
        if (userId == null) {
            getTeacherIDbyUserName(() -> {
                // After userId fetched, update UI or do something if needed
            });
        }

        txtWelcomeMessage.setText("Welcome, " + userName);

        googleDriveHelper = new GoogleDriveHelper(this);
    }

    public void goToAssignmentUi(View view) {
        startActivity(new Intent(this, Assignment.class));
    }

    public void goToCourseMaterialUi(View view) {
        if (isDriveServiceReady) {
            // userId might still be null if fetching is slow, so double-check
            if (userId != null) {
                Intent intent = new Intent(this, ManageCourseMaterials.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            } else {
                // Fetch userId first, then start activity
                getTeacherIDbyUserName(() -> {
                    Intent intent = new Intent(this, ManageCourseMaterials.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                });
            }
        } else {
            googleDriveHelper.signIn();
        }
    }

    // Called by GoogleDriveHelper when sign-in completes successfully
    public void onDriveSignInComplete() {
        isDriveServiceReady = true;

        if (userId != null) {
            Intent intent = new Intent(this, ManageCourseMaterials.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        } else {
            getTeacherIDbyUserName(() -> {
                Intent intent = new Intent(this, ManageCourseMaterials.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            });
        }
    }

    public void goToAttendenceUi(View view) {
        startActivity(new Intent(this, Attendence.class));
    }

    public void goToResultUi(View view) {
        startActivity(new Intent(this, Result.class));
    }

    /**
     * Helper method to fetch userId from Firestore by username.
     * Accepts a callback to run after fetching completes.
     */
    private void getTeacherIDbyUserName(Runnable onComplete) {
        firestoreDB.collection("users")
                .whereEqualTo("username", userName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String fetchedUserId = queryDocumentSnapshots.getDocuments()
                                .get(0).getString("userId");
                        if (fetchedUserId != null) {
                            userId = fetchedUserId;
                            getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                                    .putString("userId", userId)
                                    .apply();
                        }
                    }
                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    // You might want to handle errors here
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
    }

    // Forward the Google Sign-in result to the helper
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SIGN_IN) {
            googleDriveHelper.handleSignInResult(data);
        }
    }

    public void logoutTeacher(View view){
        finishAffinity();
        Intent gotoLogInIntent = new Intent(this, MainActivity.class);
        startActivity(gotoLogInIntent);
    }

    public void exitApp(View view){
        finishAffinity();
        System.exit(0);
    }
}
