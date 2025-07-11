package com.wc.tuitionmanagerapp.teacher;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.wc.tuitionmanagerapp.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Result extends AppCompatActivity {

    private String teacherUID;
    private FirebaseFirestore firestoreDB;
    private List<String> courseNames = new ArrayList<>();
    private List<String> studentList = new ArrayList<>();
    private Spinner stdSpinner;
    private Spinner courseSpinner;
    private Spinner marksSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_result);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firestore
        firestoreDB = FirebaseFirestore.getInstance();

        // Get teacher UID from shared preferences
        teacherUID = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("userId", null);

        // Initialize spinners
        courseSpinner = findViewById(R.id.spinnerCourse);
        stdSpinner = findViewById(R.id.spinnerStudent);
        marksSpinner = findViewById(R.id.spinnerMarks);

        // Setup marks spinner
        setupMarksSpinner();

        // Load courses for this teacher
        getCourseNames();
    }

    private void setupMarksSpinner() {
        List<String> marksList = Arrays.asList("A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "D-", "F");
        ArrayAdapter<String> marksAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                marksList
        );
        marksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        marksSpinner.setAdapter(marksAdapter);
    }

    private void getCourseNames() {
        firestoreDB.collection("courses")
                .whereArrayContains("teachers", teacherUID)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        courseNames.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String courseName = doc.getString("courseName");
                            if (courseName != null) {
                                courseNames.add(courseName);
                            }
                        }
                        addCoursesToSpinner(courseNames);
                        Log.d("CourseList", "Courses for teacher: " + courseNames);
                    } else {
                        Log.d("CourseList", "No courses found for this teacher.");
                        Toast.makeText(this, "No courses assigned to you", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Error fetching courses: ", e);
                    Toast.makeText(this, "Failed to load courses", Toast.LENGTH_SHORT).show();
                });
    }

    private void addCoursesToSpinner(List<String> courses) {
        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                courses
        );
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        courseSpinner.setAdapter(courseAdapter);

        // Set up course selection listener
        courseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // When a course is selected, load its students
                getStudentsInCourse();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void getStudentsInCourse() {
        if (courseSpinner.getSelectedItem() == null) {
            return;
        }

        String courseName = courseSpinner.getSelectedItem().toString();
        firestoreDB.collection("courses")
                .whereEqualTo("courseName", courseName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot courseDoc = queryDocumentSnapshots.getDocuments().get(0);
                        List<String> students = (List<String>) courseDoc.get("students");

                        if (students != null && !students.isEmpty()) {
                            studentList.clear();
                            studentList.addAll(students);
                            addStudentsToSpinner(studentList);
                        } else {
                            Log.d("StudentList", "No students enrolled in this course");
                            Toast.makeText(this, "No students in this course", Toast.LENGTH_SHORT).show();
                            // Clear student spinner
                            studentList.clear();
                            addStudentsToSpinner(studentList);
                        }
                    } else {
                        Log.d("CourseLookup", "No course found with name: " + courseName);
                        Toast.makeText(this, "Course not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Error fetching students: ", e);
                    Toast.makeText(this, "Failed to load students", Toast.LENGTH_SHORT).show();
                });
    }

    private void addStudentsToSpinner(List<String> students) {
        ArrayAdapter<String> studentAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                students
        );
        studentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stdSpinner.setAdapter(studentAdapter);
    }

    // Method to handle submit button click (if you have one)
    public void onSubmitClick(View view) {
        // Implement your submit logic here
        if (courseSpinner.getSelectedItem() == null || stdSpinner.getSelectedItem() == null || marksSpinner.getSelectedItem() == null) {
            Toast.makeText(this, "Please select all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String course = courseSpinner.getSelectedItem().toString();
        String student = stdSpinner.getSelectedItem().toString();
        String mark = marksSpinner.getSelectedItem().toString();

        // TODO: Implement your Firestore update logic here
        Toast.makeText(this, "Result submitted for " + student + ": " + mark, Toast.LENGTH_SHORT).show();
    }
}