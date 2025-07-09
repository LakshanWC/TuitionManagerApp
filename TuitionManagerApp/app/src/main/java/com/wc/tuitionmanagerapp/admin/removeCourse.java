package com.wc.tuitionmanagerapp.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.wc.tuitionmanagerapp.R;
import java.util.ArrayList;

public class removeCourse extends AppCompatActivity {

    private ListView lvCourses;
    private TextView tvEmpty;
    private final ArrayList<String> courseList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remove_course);

        lvCourses = findViewById(R.id.lvCourses);
        tvEmpty = findViewById(R.id.tvEmpty);

        // Simple adapter for course list
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                courseList
        );
        lvCourses.setAdapter(adapter);

        // Load courses from Firestore
        db.collection("courses")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        courseList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String courseName = document.getString("courseName");
                            String courseId = document.getString("courseId");
                            courseList.add(courseName + " (" + courseId + ")");
                        }
                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(courseList.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        Toast.makeText(this, "Error loading courses", Toast.LENGTH_SHORT).show();
                    }
                });

        // Handle course removal
        lvCourses.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCourse = courseList.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Removal")
                    .setMessage("Delete " + selectedCourse + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // Extract courseId from the selected string
                        String courseId = selectedCourse.substring(
                                selectedCourse.indexOf("(") + 1,
                                selectedCourse.indexOf(")")
                        );
                        deleteCourse(courseId);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void deleteCourse(String courseId) {
        db.collection("courses").document(courseId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Course deleted", Toast.LENGTH_SHORT).show();
                    recreate(); // Refresh the activity
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error deleting course", Toast.LENGTH_SHORT).show());
    }
}