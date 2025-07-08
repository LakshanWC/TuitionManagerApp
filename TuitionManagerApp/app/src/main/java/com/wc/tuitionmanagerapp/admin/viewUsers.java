package com.wc.tuitionmanagerapp.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.wc.tuitionmanagerapp.R;

import java.util.ArrayList;
import java.util.List;

public class viewUsers extends AppCompatActivity {

    private LinearLayout usersContainer;
    private ProgressBar progressBar;
    private TextInputEditText etSearch;
    private List<User> userList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_users);

        db = FirebaseFirestore.getInstance();
        initializeViews();
        loadUsers();
        setupSearch();
    }

    private void initializeViews() {
        usersContainer = findViewById(R.id.usersContainer);
        progressBar = findViewById(R.id.progressBar);
        etSearch = findViewById(R.id.etSearch);
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        userList.clear();
                        usersContainer.removeAllViews();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            userList.add(user);
                            addUserCard(user);
                        }
                    } else {
                        Toast.makeText(this, "Error loading users", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addUserCard(User user) {
        // Create CardView
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(8));
        card.setLayoutParams(cardParams);
        card.setRadius(dpToPx(8));
        card.setCardElevation(dpToPx(2));

        // Create inner layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Add username TextView
        TextView tvUsername = createTextView(user.getUsername(), 18, true);
        layout.addView(tvUsername);

        // Add email TextView
        TextView tvEmail = createTextView(user.getEmail(), 14, false);
        tvEmail.setTextColor(ContextCompat.getColor(this, R.color.grey));
        LinearLayout.LayoutParams emailParams = (LinearLayout.LayoutParams) tvEmail.getLayoutParams();
        emailParams.setMargins(0, dpToPx(4), 0, 0);
        layout.addView(tvEmail);

        // Add role and phone horizontal layout
        LinearLayout rolePhoneLayout = new LinearLayout(this);
        rolePhoneLayout.setOrientation(LinearLayout.HORIZONTAL);

        // Add role TextView
        TextView tvRole = createTextView(user.getRole(), 14, false);
        tvRole.setBackgroundResource(R.drawable.role_background);
        tvRole.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        tvRole.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        rolePhoneLayout.addView(tvRole);

        // Add phone TextView
        TextView tvPhone = createTextView(user.getPhone(), 14, false);
        LinearLayout.LayoutParams phoneParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        phoneParams.setMargins(dpToPx(8), 0, 0, 0);
        tvPhone.setLayoutParams(phoneParams);
        tvPhone.setTextColor(ContextCompat.getColor(this, R.color.primary_color));
        rolePhoneLayout.addView(tvPhone);

        // Add rolePhoneLayout to main layout
        LinearLayout.LayoutParams rolePhoneParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rolePhoneParams.setMargins(0, dpToPx(8), 0, 0);
        rolePhoneLayout.setLayoutParams(rolePhoneParams);
        layout.addView(rolePhoneLayout);

        // Add layout to card
        card.addView(layout);

        // Add card to container
        usersContainer.addView(card);
    }

    private TextView createTextView(String text, int spSize, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(spSize);
        if (bold) {
            textView.setTypeface(textView.getTypeface(), android.graphics.Typeface.BOLD);
        }
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return textView;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterUsers(String searchText) {
        usersContainer.removeAllViews();
        for (User user : userList) {
            if (searchText.isEmpty() ||
                    user.getUsername().toLowerCase().contains(searchText.toLowerCase()) ||
                    user.getEmail().toLowerCase().contains(searchText.toLowerCase()) ||
                    user.getRole().toLowerCase().contains(searchText.toLowerCase())) {
                addUserCard(user);
            }
        }
    }

    // User model class
    private static class User {
        private String username, email, role, phone;

        public User() {}  // Needed for Firestore

        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getPhone() { return phone; }
    }
}