package com.wc.tuitionmanagerapp.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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

public class courseDistributionReport extends AppCompatActivity {
    private static final String TAG = "CourseDistribution";
    private BarChart barChart;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_distribution_report);

        barChart = findViewById(R.id.barChart);
        db = FirebaseFirestore.getInstance();

        // Basic chart setup
        barChart.getDescription().setEnabled(true);
        barChart.getDescription().setText("Course Distribution");
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);

        fetchCourseData();
    }

    private void fetchCourseData() {
        db.collection("courses")  // Changed from "counters" to "courses"
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<BarEntry> entries = new ArrayList<>();
                        List<String> labels = new ArrayList<>();
                        int index = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                String courseName = document.getString("courseName");
                                List<String> students = (List<String>) document.get("students");
                                int studentCount = students != null ? students.size() : 0;

                                Log.d(TAG, "Course: " + courseName + ", Students: " + studentCount);

                                if (courseName != null && !courseName.isEmpty()) {
                                    entries.add(new BarEntry(index, studentCount));
                                    labels.add(courseName);
                                    index++;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing document", e);
                            }
                        }

                        if (entries.isEmpty()) {
                            Log.w(TAG, "No valid course data found");
                            barChart.clear();
                            barChart.setNoDataText("No course data available");
                        } else {
                            setupBarChart(entries, labels);
                        }
                    } else {
                        Log.e(TAG, "Error getting documents", task.getException());
                        barChart.setNoDataText("Error loading data");
                    }
                });
    }

    private void setupBarChart(List<BarEntry> entries, List<String> labels) {
        BarDataSet dataSet = new BarDataSet(entries, "Students per Course");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f); // Set custom bar width

        // Configure X-axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(labels.size());
        xAxis.setLabelRotationAngle(-45);

        // Configure Y-axis
        barChart.getAxisLeft().setGranularity(1f); // Show integer values only
        barChart.getAxisRight().setEnabled(false); // Disable right axis

        barChart.setData(barData);
        barChart.setFitBars(true);
        barChart.animateY(1000);
        barChart.invalidate(); // Refresh chart
    }
}