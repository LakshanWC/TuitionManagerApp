package com.wc.tuitionmanagerapp.admin;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.wc.tuitionmanagerapp.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class deleteUsers extends AppCompatActivity {

    private ListView lvUsers;
    private EditText etSearch;
    private Button btnSearch;
    private TextView tvEmpty;
    private FirebaseFirestore db;
    private ArrayList<Map<String, String>> userList;
    private ArrayAdapter<Map<String, String>> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_users);

        db = FirebaseFirestore.getInstance();
        lvUsers = findViewById(R.id.lvUsers);
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        tvEmpty = findViewById(R.id.tvEmpty);

        userList = new ArrayList<>();
        adapter = new ArrayAdapter<Map<String, String>>(this,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                userList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                Map<String, String> user = userList.get(position);

                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                text1.setText(user.get("username"));
                text2.setText(user.get("email") + " (" + user.get("role") + ")");

                return view;
            }
        };

        lvUsers.setAdapter(adapter);
        loadAllUsers();

        btnSearch.setOnClickListener(v -> searchUsers());
        lvUsers.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, String> selectedUser = userList.get(position);
            showDeleteDialog(selectedUser);
        });
    }

    private void loadAllUsers() {
        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    userList.clear();
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, String> user = new HashMap<>();
                            user.put("id", document.getId());
                            user.put("username", document.getString("username"));
                            user.put("email", document.getString("email"));
                            user.put("role", document.getString("role"));
                            userList.add(user);
                        }
                        updateEmptyState();
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Error loading users", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void searchUsers() {
        String searchText = etSearch.getText().toString().trim().toLowerCase();
        if (searchText.isEmpty()) {
            loadAllUsers();
            return;
        }

        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    userList.clear();
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String username = document.getString("username").toLowerCase();
                            String email = document.getString("email").toLowerCase();

                            if (username.contains(searchText) || email.contains(searchText)) {
                                Map<String, String> user = new HashMap<>();
                                user.put("id", document.getId());
                                user.put("username", document.getString("username"));
                                user.put("email", document.getString("email"));
                                user.put("role", document.getString("role"));
                                userList.add(user);
                            }
                        }
                        updateEmptyState();
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Error searching users", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showDeleteDialog(Map<String, String> user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Delete " + user.get("username") + " (" + user.get("email") + ")?")
                .setPositiveButton("Delete", (dialog, which) -> deleteUser(user.get("id")))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUser(String userId) {
        db.collection("users").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "User deleted successfully", Toast.LENGTH_SHORT).show();
                    loadAllUsers();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting user", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateEmptyState() {
        tvEmpty.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}