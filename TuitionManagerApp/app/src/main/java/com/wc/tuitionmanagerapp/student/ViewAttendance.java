package com.wc.tuitionmanagerapp.student;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.wc.tuitionmanagerapp.R;

import java.util.ArrayList;
import java.util.List;

public class ViewAttendance extends AppCompatActivity {

    private ListView listView;
    private FirebaseFirestore db;
    private SwipeRefreshLayout swipeRefresh;
    private String studentId;
    private String courseIdString;
    private List<String> dateList = new ArrayList<>();
    private String studentName;
    private Spinner spinnerCourses;
    private List<String> courseList = new ArrayList<>();
    private ArrayAdapter<String> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_attendance);

        listView = findViewById(R.id.listViewAttendance);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        spinnerCourses = findViewById(R.id.spinnerCourses);

        // Initialize the adapter for the ListView
        listAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                dateList);
        listView.setAdapter(listAdapter);

        studentId = getIntent().getStringExtra("studentId");
        if (studentId == null) {
            Toast.makeText(this, "Student ID not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        studentName = getSharedPreferences("student_prefs", MODE_PRIVATE)
                .getString("username", null);

        Log.d("name", studentName);

        db = FirebaseFirestore.getInstance();
        getCourseNames();

        swipeRefresh.setOnRefreshListener(() -> {
            if (spinnerCourses.getSelectedItem() != null) {
                String selectedCourse = spinnerCourses.getSelectedItem().toString();
                Log.d("SwipeRefresh", "Refreshing attendance for course: " + selectedCourse);
                fetchCourseId(selectedCourse, () -> {
                    fetchAttendanceDatesByStudentNameAndCourseId(studentName, courseIdString);
                });
            }
        });
    }

    private void getCourseNames() {
        db.collection("courses")
                .whereArrayContains("students", studentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    courseList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String courseName = doc.getString("courseName");
                            if (courseName != null && !courseName.isEmpty()) {
                                courseList.add(courseName);
                            }
                        }
                        Log.d("CourseList", "Fetched courses: " + courseList.toString());

                        if (courseList.isEmpty()) {
                            showNoCoursesMessage();
                        } else {
                            setupCourseSpinner();
                        }
                    } else {
                        showNoCoursesMessage();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Error fetching courses: ", e);
                    Toast.makeText(this, "Failed to load courses", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupCourseSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, courseList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourses.setAdapter(spinnerAdapter);

        spinnerCourses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCourse = courseList.get(position);
                Log.d("CourseSelection", "Selected course: " + selectedCourse);
                fetchCourseId(selectedCourse, () -> {
                    Log.d("CourseID", "Fetched course ID: " + courseIdString);
                    fetchAttendanceDatesByStudentNameAndCourseId(studentName, courseIdString);
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void showNoCoursesMessage() {
        Toast.makeText(this, "No courses found for this student", Toast.LENGTH_SHORT).show();
        findViewById(R.id.tvEmpty).setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
    }

    private void fetchCourseId(String courseName, Runnable onComplete) {
        db.collection("courses")
                .whereEqualTo("courseName", courseName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                            courseIdString = document.getId();
                            Log.d("CourseInfo", "Document ID: " + courseIdString);

                            if (onComplete != null) {
                                onComplete.run();
                            }
                        } else {
                            findViewById(R.id.tvEmpty).setVisibility(View.VISIBLE);
                            listView.setVisibility(View.GONE);
                        }
                    } else {
                        Toast.makeText(this, "Error: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchAttendanceDatesByStudentNameAndCourseId(String studentName, String courseId) {
        Log.d("AttendanceFetch", "Starting to fetch attendance dates for student: " + studentName + ", course: " + courseId);
        swipeRefresh.setRefreshing(true);

        db.collection("attendance")
                .whereEqualTo("username", studentName)
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnCompleteListener(task -> {
                    swipeRefresh.setRefreshing(false);

                    if (task.isSuccessful()) {
                        Log.d("AttendanceFetch", "Query successful for " + studentName + ", course: " + courseId);
                        dateList.clear(); // Clear existing data
                        int documentCount = 0;

                        for (DocumentSnapshot document : task.getResult()) {
                            documentCount++;
                            String date = document.getString("date");
                            if (date != null && !date.isEmpty()) {
                                dateList.add(date);
                                Log.v("AttendanceFetch", "Processed document #" + documentCount +
                                        ", ID: " + document.getId() +
                                        ", Date: " + date);
                            } else {
                                Log.w("AttendanceFetch", "Missing or empty date in document #" +
                                        documentCount + ", ID: " + document.getId());
                            }
                        }

                        Log.d("AttendanceFetch", "Total documents processed: " + documentCount +
                                ", Valid dates found: " + dateList.size());

                        if (dateList.isEmpty()) {
                            Log.i("AttendanceFetch", "No attendance dates found for " +
                                    studentName + " in course " + courseId);
                            findViewById(R.id.tvEmpty).setVisibility(View.VISIBLE);
                            listView.setVisibility(View.GONE);
                        } else {
                            Log.i("AttendanceFetch", "Displaying " + dateList.size() +
                                    " attendance dates");
                            findViewById(R.id.tvEmpty).setVisibility(View.GONE);
                            listView.setVisibility(View.VISIBLE);

                            // Update the adapter with new data
                            listAdapter.notifyDataSetChanged();
                        }
                    } else {
                        String errorMsg = "Error loading attendance dates: " + task.getException();
                        Log.e("AttendanceFetch", errorMsg, task.getException());
                        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}