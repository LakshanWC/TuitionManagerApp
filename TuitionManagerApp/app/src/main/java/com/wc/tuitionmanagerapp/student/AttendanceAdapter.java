package com.wc.tuitionmanagerapp.student;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wc.tuitionmanagerapp.R;

import java.util.List;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

    private List<AttendanceRecord> attendanceList;

    public AttendanceAdapter(List<AttendanceRecord> attendanceList) {
        this.attendanceList = attendanceList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceRecord record = attendanceList.get(position);
        holder.tvDate.setText(record.getDate());
        holder.tvCourse.setText(record.getCourseName());
        holder.tvStatus.setText(record.getStatus());
        holder.tvStatus.setTextColor(record.getStatus().equals("Present") ?
                Color.GREEN : Color.RED);
    }

    @Override
    public int getItemCount() { return attendanceList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvCourse, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvCourse = itemView.findViewById(R.id.tvCourse);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}