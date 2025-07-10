package com.wc.tuitionmanagerapp.student;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.FirebaseFirestore;
import com.wc.tuitionmanagerapp.R;
import com.wc.tuitionmanagerapp.adapters.AttendanceAdapter;
import com.wc.tuitionmanagerapp.models.Attendance;

import java.util.List;

public class ViewAttendance extends AppCompatActivity {
    private RecyclerView recyclerView;
    private AttendanceAdapter adapter;
    private FirebaseFirestore db;
    private SwipeRefreshLayout swipeRefresh;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_attendance);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewAttendance);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Get studentId from intent
        studentId = getIntent().getStringExtra("studentId");
        if (studentId == null) {
            Toast.makeText(this, "Student ID not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        fetchAttendanceData();

        // Pull-to-refresh
        swipeRefresh.setOnRefreshListener(this::fetchAttendanceData);
    }

    private void fetchAttendanceData() {
        swipeRefresh.setRefreshing(true);
        db.collection("attendance")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnCompleteListener(task -> {
                    swipeRefresh.setRefreshing(false);
                    if (task.isSuccessful()) {
                        List<Attendance> attendanceList = task.getResult().toObjects(Attendance.class);
                        if (attendanceList.isEmpty()) {
                            findViewById(R.id.tvEmpty).setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            findViewById(R.id.tvEmpty).setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            adapter = new AttendanceAdapter(attendanceList);
                            recyclerView.setAdapter(adapter);
                        }
                    } else {
                        Toast.makeText(this, "Error: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}