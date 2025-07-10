package com.wc.tuitionmanagerapp.student;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.wc.tuitionmanagerapp.R;
import java.util.ArrayList;
import java.util.List;

public class ViewAttendance extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AttendanceAdapter adapter;
    private List<AttendanceRecord> attendanceList;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_attendance);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewAttendance);
        tvEmpty = findViewById(R.id.tvEmpty);
        attendanceList = new ArrayList<>();

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter(attendanceList);
        recyclerView.setAdapter(adapter);

        // Fetch attendance data from Firestore
        fetchAttendance();
    }

    private void fetchAttendance() {
        String studentId = "st123"; // Test with hardcoded ID first
        Log.d("ATTENDANCE", "Fetching data for student: " + studentId);

        FirebaseFirestore.getInstance()
                .collection("attendance")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("ATTENDANCE", "Got " + task.getResult().size() + " records");
                        attendanceList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d("ATTENDANCE", document.getId() + " => " + document.getData());
                            AttendanceRecord record = document.toObject(AttendanceRecord.class);
                            attendanceList.add(record);
                        }
                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(attendanceList.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        Log.e("ATTENDANCE", "Error loading attendance", task.getException());
                        Toast.makeText(this, "Error loading attendance", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}