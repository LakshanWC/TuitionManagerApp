package com.wc.tuitionmanagerapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.wc.tuitionmanagerapp.admin.admin_menu;
import com.wc.tuitionmanagerapp.student.StudentHome;
import com.wc.tuitionmanagerapp.teacher.TeacherHome;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextInputEditText usernameEditText, passwordEditText;
    private MaterialButton loginButton;
    private RadioButton adminRadio, teacherRadio, studentRadio;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        usernameEditText = findViewById(R.id.username_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        adminRadio = findViewById(R.id.admin_radio);
        teacherRadio = findViewById(R.id.teacher_radio);
        studentRadio = findViewById(R.id.student_radio);

        loginButton.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String selectedRole = getSelectedRole();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .whereEqualTo("username", username)
                .whereEqualTo("role", selectedRole)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String storedPassword = task.getResult().getDocuments().get(0).getString("password");
                        if (storedPassword != null && storedPassword.equals(hashPassword(password))) {
                            redirectUser(selectedRole);
                        } else {
                            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getSelectedRole() {
        if (adminRadio.isChecked()) return "admin";
        if (teacherRadio.isChecked()) return "teacher";
        return "student";
    }

    private String hashPassword(String password) {
        // Implement proper hashing in production
        return String.valueOf(password);
    }

    private void redirectUser(String role) {
        if (role.equals("admin")) {
            Intent intent = new Intent(MainActivity.this, admin_menu.class);
            // Pass the username to admin menu
            intent.putExtra("username", usernameEditText.getText().toString());
            startActivity(intent);
            finish(); // Close the login activity
        }
        else if(role.equals("teacher")){
            Intent teacherHomeIntent = new Intent(MainActivity.this, TeacherHome.class);
            teacherHomeIntent.putExtra("username",usernameEditText.getText().toString());
            startActivity(teacherHomeIntent);
            finish();
        }
        // In MainActivity.java redirectUser method
        else if(role.equals("student")){
            Intent teacherHomeIntent = new Intent(MainActivity.this, StudentHome.class);
            teacherHomeIntent.putExtra("username",usernameEditText.getText().toString());
            startActivity(teacherHomeIntent);
            finish();
        }
    }
}