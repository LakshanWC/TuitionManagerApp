package com.wc.tuitionmanagerapp.student;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.wc.tuitionmanagerapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class studentViewAttendance extends AppCompatActivity {
    private static final String TAG = "StudentAttendance";
    private BarChart attendanceChart;
    private FirebaseFirestore db;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_view_attendance);

        attendanceChart = findViewById(R.id.attendanceChart);
        db = FirebaseFirestore.getInstance();

        // Get username from intent
        String username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            showError("User not identified");
            finish();
            return;
        }

        // Basic chart setup
        setupChart();

        // Find student ID based on username
        findStudentId(username);
    }

    private void setupChart() {
        attendanceChart.getDescription().setEnabled(true);
        attendanceChart.getDescription().setText("My Attendance");
        attendanceChart.setDrawGridBackground(false);
        attendanceChart.setDrawBarShadow(false);
        attendanceChart.setNoDataText("Loading your attendance...");

        // Configure Y-axis
        attendanceChart.getAxisLeft().setGranularity(1f); // Show integer values only
        attendanceChart.getAxisRight().setEnabled(false); // Disable right axis
    }

    private void findStudentId(String username) {
        db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        studentId = task.getResult().getDocuments().get(0).getString("userId");
                        if (studentId != null && !studentId.isEmpty()) {
                            fetchAttendanceData();
                        } else {
                            showError("Student ID not found");
                        }
                    } else {
                        showError("User not found in database");
                    }
                });
    }

    private void fetchAttendanceData() {
        db.collection("courses")
                .whereArrayContains("students", studentId)
                .get()
                .addOnCompleteListener(coursesTask -> {
                    if (coursesTask.isSuccessful() && coursesTask.getResult() != null) {
                        processCoursesData(coursesTask);
                    } else {
                        showError("Error fetching course data");
                    }
                });
    }

    private void processCoursesData(com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> coursesTask) {
        List<String> courseNames = new ArrayList<>();
        AtomicInteger pendingQueries = new AtomicInteger(coursesTask.getResult().size());
        AtomicReference<List<BarEntry>> entriesRef = new AtomicReference<>(new ArrayList<>());

        if (pendingQueries.get() == 0) {
            showNoCoursesMessage();
            return;
        }

        for (QueryDocumentSnapshot courseDoc : coursesTask.getResult()) {
            processSingleCourse(courseDoc, courseNames, entriesRef, pendingQueries);
        }
    }

    private void processSingleCourse(QueryDocumentSnapshot courseDoc,
                                     List<String> courseNames,
                                     AtomicReference<List<BarEntry>> entriesRef,
                                     AtomicInteger pendingQueries) {
        String courseId = courseDoc.getId();
        String courseName = courseDoc.getString("courseName");
        if (courseName == null) courseName = courseId;

        final int currentIndex = courseNames.size();
        courseNames.add(courseName);

        db.collection("attendance")
                .whereEqualTo("userId", studentId)
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("status", "Present")
                .get()
                .addOnCompleteListener(attendanceTask -> {
                    int presentCount = attendanceTask.isSuccessful() ? attendanceTask.getResult().size() : 0;
                    List<BarEntry> entries = entriesRef.get();
                    entries.add(new BarEntry(currentIndex, presentCount));
                    entriesRef.set(entries);

                    if (pendingQueries.decrementAndGet() == 0) {
                        updateAttendanceChart(entriesRef.get(), courseNames);
                    }
                });
    }

    private void updateAttendanceChart(List<BarEntry> entries, List<String> courseNames) {
        if (entries.isEmpty()) {
            attendanceChart.setNoDataText("No attendance data available");
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Present Days");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);

        // Configure X-axis
        XAxis xAxis = attendanceChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(courseNames));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(courseNames.size());
        xAxis.setLabelRotationAngle(-45);

        attendanceChart.setData(barData);
        attendanceChart.setFitBars(true);
        attendanceChart.animateY(1000);
        attendanceChart.invalidate();
    }

    private void showNoCoursesMessage() {
        runOnUiThread(() -> {
            attendanceChart.setNoDataText("You are not registered in any courses");
            Toast.makeText(this, "No courses found", Toast.LENGTH_SHORT).show();
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            attendanceChart.setNoDataText(message);
            Log.e(TAG, message);
        });
    }
}