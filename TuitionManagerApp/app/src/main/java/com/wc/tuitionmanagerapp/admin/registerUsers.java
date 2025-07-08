package com.wc.tuitionmanagerapp.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.wc.tuitionmanagerapp.R;

import java.util.HashMap;
import java.util.Map;

public class registerUsers extends AppCompatActivity {

    private TextInputEditText etEmail, etUsername, etPassword, etPhone;
    private RadioGroup roleRadioGroup;
    private MaterialButton btnRegister;
    private TextView tvStatus;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_users);

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        etEmail = findViewById(R.id.etEmail);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);
        roleRadioGroup = findViewById(R.id.role_radio_group);
        btnRegister = findViewById(R.id.btnRegister);
        tvStatus = findViewById(R.id.tvStatus);

        // Set up register button click listener
        btnRegister.setOnClickListener(v -> checkUsernameAndRegister());
    }

    private void checkUsernameAndRegister() {
        String username = etUsername.getText().toString().trim();

        // First check if username exists
        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            // Username doesn't exist, proceed with registration
                            registerUser();
                        } else {
                            // Username already exists
                            showStatusMessage("Username already exists", true);
                        }
                    } else {
                        // Error checking username
                        showStatusMessage("Error checking username availability", true);
                    }
                });
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // Get selected role
        int selectedId = roleRadioGroup.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedId);
        String role = selectedRadioButton.getText().toString();

        // Validate inputs
        if (email.isEmpty() || username.isEmpty() || password.isEmpty() || phone.isEmpty()) {
            showStatusMessage("Please fill all fields", true);
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showStatusMessage("Please enter a valid email", true);
            return;
        }

        if (password.length() < 6) {
            showStatusMessage("Password must be at least 6 characters", true);
            return;
        }

        // Create user data map
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("username", username);
        user.put("password", password); // Note: In production, you should hash the password
        user.put("phone", phone);
        user.put("role", role.toLowerCase()); // Store role in lowercase

        // Add document to Firestore
        db.collection("users")
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    String successMessage = String.format(
                            "User registered successfully!\nUsername: %s\nRole: %s",
                            username, role
                    );
                    Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
                    showStatusMessage("Registration successful!", false);
                    clearForm();
                })
                .addOnFailureListener(e -> {
                    showStatusMessage("Registration failed: " + e.getMessage(), true);
                });
    }

    private void showStatusMessage(String message, boolean isError) {
        tvStatus.setText(message);
        tvStatus.setVisibility(View.VISIBLE);
        if (isError) {
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            tvStatus.setTextColor(getResources().getColor(R.color.primary_color));
        }
    }

    private void clearForm() {
        etEmail.setText("");
        etUsername.setText("");
        etPassword.setText("");
        etPhone.setText("");
        roleRadioGroup.check(R.id.rbAdmin); // Reset to default selection
    }
}