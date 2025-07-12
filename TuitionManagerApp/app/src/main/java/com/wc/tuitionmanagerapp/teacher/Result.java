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
    private String CurrentcourseId;

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

    private void getCourseIdFromName() {
        String nameCourses = courseSpinner.getSelectedItem().toString();
        firestoreDB.collection("courses")
                .whereEqualTo("courseName", nameCourses)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0); // Get the first match
                        CurrentcourseId = doc.getId(); // Document ID is your courseId
                        Log.d("CourseQuery", "Course ID for " + nameCourses + ": " + CurrentcourseId);

                        // Use courseId here (e.g., pass to another method)
                    } else {
                        Toast.makeText(this, "Course not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Error fetching courseId: ", e);
                    Toast.makeText(this, "Failed to get course ID", Toast.LENGTH_SHORT).show();
                });
    }


    // Method to handle submit button click (if you have one)
    public void onSubmitClick(View view) {
        if (courseSpinner.getSelectedItem() == null || stdSpinner.getSelectedItem() == null || marksSpinner.getSelectedItem() == null) {
            Toast.makeText(this, "Please select all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String courseName = courseSpinner.getSelectedItem().toString();
        String studentId = stdSpinner.getSelectedItem().toString();
        String selectedGrade = marksSpinner.getSelectedItem().toString();

        // Step 1: Get courseId from courseName
        firestoreDB.collection("courses")
                .whereEqualTo("courseName", courseName)
                .get()
                .addOnSuccessListener(courseQuery -> {
                    if (!courseQuery.isEmpty()) {
                        DocumentSnapshot courseDoc = courseQuery.getDocuments().get(0);
                        String courseId = courseDoc.getId();  // Get courseId from document ID

                        // Step 2: Query submitted_assignments for ungraded submission
                        firestoreDB.collection("submitted_assignments")
                                .whereEqualTo("studentId", studentId)
                                .whereEqualTo("courseId", courseId)
                                .whereEqualTo("grade", "") // grade not yet assigned
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    if (!querySnapshot.isEmpty()) {
                                        DocumentSnapshot submissionDoc = querySnapshot.getDocuments().get(0);

                                        // Step 3: Update grade field
                                        firestoreDB.collection("submitted_assignments")
                                                .document(submissionDoc.getId())
                                                .update("grade", selectedGrade, "status", "graded")
                                                .addOnSuccessListener(unused -> {
                                                    Toast.makeText(this, "Grade submitted successfully", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(this, "Failed to update grade", Toast.LENGTH_SHORT).show();
                                                    Log.e("FirestoreUpdate", "Error updating grade: ", e);
                                                });
                                    } else {
                                        Toast.makeText(this, "No ungraded submission found for this student", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to find submitted assignment", Toast.LENGTH_SHORT).show();
                                    Log.e("FirestoreQuery", "Error querying submitted_assignments: ", e);
                                });

                    } else {
                        Toast.makeText(this, "Course not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch course ID", Toast.LENGTH_SHORT).show();
                    Log.e("FirestoreCourse", "Error fetching courseId: ", e);
                });
    }

}