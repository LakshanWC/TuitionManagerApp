package com.wc.tuitionmanagerapp.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class ViewAssignments extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private AssignmentAdapter adapter;
    private List<Assignment> assignments = new ArrayList<>();
    private FirebaseFirestore db;
    private String username;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_assignments);

        // Get username from intent
        username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "User not identified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerViewAssignments);
        tvEmpty = findViewById(R.id.tvEmpty);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AssignmentAdapter();
        recyclerView.setAdapter(adapter);

        // First find the student ID based on username
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
                        loadAssignmentsForCourses(registeredCourseIds);
                    } else {
                        showError("Error loading courses");
                    }
                });
    }

    private void loadAssignmentsForCourses(Set<String> courseIds) {
        if (courseIds.isEmpty()) {
            showEmptyState("You are not registered for any courses");
            return;
        }

        db.collection("assignments")
                .whereIn("courseId", new ArrayList<>(courseIds))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        assignments.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String assignmentId = document.getId();
                            String name = document.getString("assignmentName");
                            String courseId = document.getString("courseId");
                            String dueDate = document.getString("dueDate");

                            // Get course name
                            db.collection("courses").document(courseId)
                                    .get()
                                    .addOnSuccessListener(courseDoc -> {
                                        String courseName = courseDoc.getString("courseName");
                                        assignments.add(new Assignment(assignmentId, courseId, courseName, name, dueDate));
                                        adapter.setAssignments(assignments);

                                        if (assignments.isEmpty()) {
                                            showEmptyState("No assignments found for your courses");
                                        } else {
                                            tvEmpty.setVisibility(View.GONE);
                                            recyclerView.setVisibility(View.VISIBLE);
                                        }
                                    });
                        }
                    } else {
                        showError("Error loading assignments");
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

    // Assignment model class
    private static class Assignment {
        String id;
        String courseId;
        String courseName;
        String name;
        String dueDate;

        public Assignment(String id, String courseId, String courseName, String name, String dueDate) {
            this.id = id;
            this.courseId = courseId;
            this.courseName = courseName;
            this.name = name;
            this.dueDate = dueDate;
        }
    }

    // Adapter for RecyclerView
    private class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.ViewHolder> {
        private List<Assignment> assignments = new ArrayList<>();

        public void setAssignments(List<Assignment> assignments) {
            this.assignments = assignments;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_assignment, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Assignment assignment = assignments.get(position);
            holder.tvCourseName.setText(assignment.courseName);
            holder.tvAssignmentName.setText(assignment.name);
            holder.tvDueDate.setText("Due: " + assignment.dueDate);

            holder.btnSubmit.setOnClickListener(v -> {
                Intent intent = new Intent(ViewAssignments.this, SubmitAssignment.class);
                intent.putExtra("assignmentId", assignment.id);
                intent.putExtra("assignmentName", assignment.name);
                intent.putExtra("courseId", assignment.courseId);
                intent.putExtra("studentId", studentId);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return assignments.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCourseName, tvAssignmentName, tvDueDate;
            Button btnSubmit;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCourseName = itemView.findViewById(R.id.tvCourseName);
                tvAssignmentName = itemView.findViewById(R.id.tvAssignmentName);
                tvDueDate = itemView.findViewById(R.id.tvDueDate);
                btnSubmit = itemView.findViewById(R.id.btnSubmit);
            }
        }
    }
}