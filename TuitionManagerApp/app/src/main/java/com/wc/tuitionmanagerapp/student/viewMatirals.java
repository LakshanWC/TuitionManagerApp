package com.wc.tuitionmanagerapp.student;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.wc.tuitionmanagerapp.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class viewMatirals extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private CourseMaterialAdapter adapter;
    private List<CourseMaterial> materials = new ArrayList<>();
    private FirebaseFirestore db;
    private String username;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_matirals);

        // Get username from intent with null check
        username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "User not identified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewMaterials);
        tvEmpty = findViewById(R.id.tvEmpty);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CourseMaterialAdapter();
        recyclerView.setAdapter(adapter);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        findStudentId();
    }

    private void findStudentId() {
        db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        studentId = task.getResult().getDocuments().get(0).getString("userId");
                        if (studentId != null && !studentId.isEmpty()) {
                            loadStudentCourses();
                        } else {
                            showError("Student ID not found");
                        }
                    } else {
                        showError("User not found in database");
                    }
                });
    }

    private void loadStudentCourses() {
        db.collection("courses")
                .whereArrayContains("students", studentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Set<String> registeredCourseIds = new HashSet<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            registeredCourseIds.add(document.getId());
                        }
                        loadMaterialsForCourses(registeredCourseIds);
                    } else {
                        showError("Error loading courses");
                    }
                });
    }

    private void loadMaterialsForCourses(Set<String> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            showEmptyState("You are not registered for any courses");
            return;
        }

        db.collection("courses")
                .whereIn("__name__", new ArrayList<>(courseIds))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        materials.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                String courseId = document.getId();
                                String courseName = document.getString("courseName");
                                String materialsUrl = document.getString("courseMaterials");

                                if (materialsUrl != null && !materialsUrl.isEmpty()) {
                                    materials.add(new CourseMaterial(courseId, courseName, materialsUrl));
                                }
                            } catch (Exception e) {
                                Log.e("viewMatirals", "Error parsing course data", e);
                            }
                        }

                        if (materials.isEmpty()) {
                            showEmptyState("No materials found for your courses");
                        } else {
                            adapter.setMaterials(materials);
                            tvEmpty.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        showError("Error loading course materials: " +
                                (task.getException() != null ?
                                        task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    private void showEmptyState(String message) {
        tvEmpty.setText(message);
        tvEmpty.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        showEmptyState(message);
    }

    // CourseMaterial model class
    private static class CourseMaterial {
        String courseId;
        String courseName;
        String materialsUrl;

        public CourseMaterial(String courseId, String courseName, String materialsUrl) {
            this.courseId = courseId;
            this.courseName = courseName;
            this.materialsUrl = materialsUrl;
        }
    }

    // Adapter for RecyclerView
    private class CourseMaterialAdapter extends RecyclerView.Adapter<CourseMaterialAdapter.ViewHolder> {
        private List<CourseMaterial> materials = new ArrayList<>();

        public void setMaterials(List<CourseMaterial> materials) {
            this.materials = materials;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_course_material, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CourseMaterial material = materials.get(position);
            holder.tvCourseName.setText(material.courseName);

            holder.ivDriveIcon.setOnClickListener(v -> {
                try {
                    if (material.materialsUrl == null || material.materialsUrl.isEmpty()) {
                        Toast.makeText(viewMatirals.this, "No materials available", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Ensure URL has proper scheme
                    String url = material.materialsUrl;
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://" + url;
                    }

                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(viewMatirals.this, "Error opening materials", Toast.LENGTH_SHORT).show();
                    Log.e("viewMatirals", "Error opening URL", e);
                }
            });

            holder.ivCopyIcon.setOnClickListener(v -> {
                if (material.materialsUrl == null || material.materialsUrl.isEmpty()) {
                    Toast.makeText(viewMatirals.this, "No materials available", Toast.LENGTH_SHORT).show();
                    return;
                }

                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText(
                        "Course Materials URL", material.materialsUrl);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(viewMatirals.this, "URL copied to clipboard", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return materials.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCourseName;
            ImageView ivDriveIcon;
            ImageView ivCopyIcon;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCourseName = itemView.findViewById(R.id.tvCourseName);
                ivDriveIcon = itemView.findViewById(R.id.ivDriveIcon);
                ivCopyIcon = itemView.findViewById(R.id.ivCopyIcon);
            }
        }
    }
}