package com.wc.tuitionmanagerapp.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.wc.tuitionmanagerapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class viewAttendanceReport extends AppCompatActivity {
    private static final String TAG = "AttendanceReport";
    private BarChart attendanceChart;
    private FirebaseFirestore db;
    private TextInputEditText userIdEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_attendance_report);

        attendanceChart = findViewById(R.id.attendanceChart);
        userIdEditText = findViewById(R.id.userIdEditText);
        Button searchButton = findViewById(R.id.searchButton);
        db = FirebaseFirestore.getInstance();

        // Basic chart setup
        attendanceChart.getDescription().setEnabled(true);
        attendanceChart.getDescription().setText("Attendance Report");
        attendanceChart.setDrawGridBackground(false);
        attendanceChart.setDrawBarShadow(false);

        searchButton.setOnClickListener(v -> {
            String userId = userIdEditText.getText().toString().trim();
            if (!userId.isEmpty()) {
                fetchAttendanceData(userId);
            } else {
                Toast.makeText(this, "Please enter a User ID", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchAttendanceData(String userId) {
        db.collection("courses")
                .whereArrayContains("students", userId)
                .get()
                .addOnCompleteListener(coursesTask -> {
                    if (coursesTask.isSuccessful() && coursesTask.getResult() != null) {
                        List<String> courseNames = new ArrayList<>();
                        AtomicInteger pendingQueries = new AtomicInteger(coursesTask.getResult().size());
                        AtomicReference<List<BarEntry>> entriesRef = new AtomicReference<>(new ArrayList<>());

                        if (pendingQueries.get() == 0) {
                            runOnUiThread(() -> {
                                Toast.makeText(this, "User is not registered in any courses", Toast.LENGTH_SHORT).show();
                                attendanceChart.clear();
                                attendanceChart.setNoDataText("No courses found for this user");
                            });
                            return;
                        }

                        for (QueryDocumentSnapshot courseDoc : coursesTask.getResult()) {
                            String courseId = courseDoc.getId();
                            String courseName = courseDoc.getString("courseName");
                            if (courseName == null) courseName = courseId;
                            final int currentIndex = courseNames.size();
                            courseNames.add(courseName);

                            db.collection("attendance")
                                    .whereEqualTo("userId", userId)
                                    .whereEqualTo("courseId", courseId)
                                    .whereEqualTo("status", "Present")
                                    .get()
                                    .addOnCompleteListener(attendanceTask -> {
                                        int presentCount = attendanceTask.isSuccessful() ? attendanceTask.getResult().size() : 0;
                                        List<BarEntry> entries = entriesRef.get();
                                        entries.add(new BarEntry(currentIndex, presentCount));
                                        entriesRef.set(entries);

                                        if (pendingQueries.decrementAndGet() == 0) {
                                            runOnUiThread(() -> updateAttendanceChart(entriesRef.get(), courseNames));
                                        }
                                    });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Log.e(TAG, "Error getting courses", coursesTask.getException());
                            Toast.makeText(this, "Error fetching course data", Toast.LENGTH_SHORT).show();
                            attendanceChart.setNoDataText("Error loading data");
                        });
                    }
                });
    }

    private void updateAttendanceChart(List<BarEntry> entries, List<String> courseNames) {
        if (entries.isEmpty()) {
            attendanceChart.clear();
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

        // Configure Y-axis
        attendanceChart.getAxisLeft().setGranularity(1f); // Show integer values only
        attendanceChart.getAxisRight().setEnabled(false); // Disable right axis

        attendanceChart.setData(barData);
        attendanceChart.setFitBars(true);
        attendanceChart.animateY(1000);
        attendanceChart.invalidate();
    }
}