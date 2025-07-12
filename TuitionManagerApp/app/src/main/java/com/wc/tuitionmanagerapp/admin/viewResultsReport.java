package com.wc.tuitionmanagerapp.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.wc.tuitionmanagerapp.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class viewResultsReport extends AppCompatActivity {
    private static final String TAG = "ResultsReport";
    private BarChart resultsChart;
    private FirebaseFirestore db;
    private TextInputEditText studentIdEditText;

    // Grade to numerical value mapping
    private static final Map<String, Float> GRADE_VALUES = new HashMap<String, Float>() {{
        put("A+", 13f); put("A", 12f); put("A-", 11f);
        put("B+", 10f); put("B", 9f); put("B-", 8f);
        put("C+", 7f); put("C", 6f); put("C-", 5f);
        put("D+", 4f); put("D", 3f); put("D-", 2f);
        put("F", 1f);
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_results_report);

        resultsChart = findViewById(R.id.resultsChart);
        studentIdEditText = findViewById(R.id.studentIdEditText);
        Button searchButton = findViewById(R.id.searchButton);
        db = FirebaseFirestore.getInstance();

        // Basic chart setup
        setupChart();

        searchButton.setOnClickListener(v -> {
            String studentId = studentIdEditText.getText().toString().trim();
            if (!studentId.isEmpty()) {
                fetchResultsData(studentId);
            } else {
                Toast.makeText(this, "Please enter a Student ID", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupChart() {
        resultsChart.getDescription().setEnabled(true);
        resultsChart.getDescription().setText("Results Report");
        resultsChart.setDrawGridBackground(false);
        resultsChart.setDrawBarShadow(false);
        resultsChart.setNoDataText("Enter a Student ID and click Search");

        // Configure Y-axis for grades
        YAxis yAxis = resultsChart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(14f);
        yAxis.setGranularity(1f);
        yAxis.setValueFormatter(new GradeAxisFormatter());
        resultsChart.getAxisRight().setEnabled(false);
    }

    private void fetchResultsData(String studentId) {
        db.collection("submitted_assignments")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Map<String, String> courseGrades = new HashMap<>();
                        List<String> courseNames = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String courseId = document.getString("courseId");
                            String grade = document.getString("grade");

                            if (courseId != null && grade != null && GRADE_VALUES.containsKey(grade)) {
                                // First get course name
                                db.collection("courses").document(courseId)
                                        .get()
                                        .addOnSuccessListener(courseDoc -> {
                                            String courseName = courseDoc.getString("courseName");
                                            if (courseName == null) courseName = courseId;

                                            courseGrades.put(courseName, grade);
                                            courseNames.add(courseName);

                                            // If we've processed all documents, update chart
                                            if (courseGrades.size() == task.getResult().size()) {
                                                updateResultsChart(courseGrades, courseNames);
                                            }
                                        });
                            }
                        }

                        if (courseGrades.isEmpty()) {
                            Toast.makeText(this, "No results found for this student", Toast.LENGTH_SHORT).show();
                            resultsChart.clear();
                            resultsChart.setNoDataText("No results found");
                        }
                    } else {
                        Log.e(TAG, "Error getting documents", task.getException());
                        Toast.makeText(this, "Error fetching results", Toast.LENGTH_SHORT).show();
                        resultsChart.setNoDataText("Error loading data");
                    }
                });
    }

    private void updateResultsChart(Map<String, String> courseGrades, List<String> courseNames) {
        List<BarEntry> entries = new ArrayList<>();

        for (int i = 0; i < courseNames.size(); i++) {
            String grade = courseGrades.get(courseNames.get(i));
            if (grade != null && GRADE_VALUES.containsKey(grade)) {
                entries.add(new BarEntry(i, GRADE_VALUES.get(grade)));
            }
        }

        if (entries.isEmpty()) {
            resultsChart.clear();
            resultsChart.setNoDataText("No valid grade data available");
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Grades");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);

        // Configure X-axis
        XAxis xAxis = resultsChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(courseNames));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(courseNames.size());
        xAxis.setLabelRotationAngle(-45);

        resultsChart.setData(barData);
        resultsChart.setFitBars(true);
        resultsChart.animateY(1000);
        resultsChart.invalidate();
    }

    // Custom formatter to display grades on Y-axis
    private static class GradeAxisFormatter extends ValueFormatter {
        private static final String[] GRADES = {
                "", "F", "D-", "D", "D+", "C-", "C", "C+", "B-", "B", "B+", "A-", "A", "A+"
        };

        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            int index = (int) value;
            if (index >= 0 && index < GRADES.length) {
                return GRADES[index];
            }
            return "";
        }
    }
}