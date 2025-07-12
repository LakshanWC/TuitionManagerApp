package com.wc.tuitionmanagerapp.student;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.wc.tuitionmanagerapp.R;

import java.util.ArrayList;
import java.util.List;

public class checkNotification extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private NotificationAdapter adapter;
    private List<Notification> notifications = new ArrayList<>();
    private FirebaseFirestore db;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_notification);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                Log.d("IntentDebug", key + " = " + extras.get(key));
            }
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewNotifications);
        tvEmpty = findViewById(R.id.tvEmpty);

        // Get studentId from intent
        studentId = getIntent().getStringExtra("studentId");
        if (studentId == null || studentId.isEmpty()) {
            Toast.makeText(this, "Student not identified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notifications, this::markAsRead);
        recyclerView.setAdapter(adapter);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        loadUnreadNotifications();
    }

    private void loadUnreadNotifications() {
        Log.d("NotificationDebug", "Loading notifications for studentId: " + studentId);

        db.collection("notifications")
                .whereEqualTo("studentId", studentId)
                .get() // First get all notifications for this student
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("NotificationDebug", "Found " + task.getResult().size() + " notifications");
                        notifications.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Log.d("NotificationDebug", "Processing document: " + document.getId());

                                String id = document.getId();
                                String title = document.getString("title");
                                String message = document.getString("message");
                                Object isReadObj = document.get("isRead");

                                // Improved isRead handling
                                boolean isRead = false;
                                if (isReadObj != null) {
                                    if (isReadObj instanceof Boolean) {
                                        isRead = (Boolean) isReadObj;
                                    } else if (isReadObj instanceof String) {
                                        String isReadStr = ((String) isReadObj).toLowerCase().trim();
                                        isRead = isReadStr.equals("true") || isReadStr.equals("1");
                                    }
                                }

                                Log.d("NotificationDebug", "Title: " + title + ", isRead: " + isRead);

                                if (!isRead) {
                                    String timestamp = document.getString("timestamp");
                                    notifications.add(new Notification(id, title, message, timestamp));
                                    Log.d("NotificationDebug", "Added notification: " + title);
                                }
                            } catch (Exception e) {
                                Log.e("NotificationError", "Error processing document", e);
                            }
                        }

                        Log.d("NotificationDebug", "Total unread: " + notifications.size());
                        updateUI();
                    } else {
                        Log.e("NotificationError", "Error loading notifications", task.getException());
                        showError("Error loading notifications");
                    }
                });
    }

    private void markAsRead(String notificationId, int position) {
        DocumentReference docRef = db.collection("notifications").document(notificationId);
        docRef.update("isRead", true)
                .addOnSuccessListener(aVoid -> {
                    // Remove from local list and update UI
                    notifications.remove(position);
                    adapter.notifyItemRemoved(position);
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to mark as read", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI() {
        if (notifications.isEmpty()) {
            tvEmpty.setText("No unread notifications");
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        tvEmpty.setText(message);
        tvEmpty.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    // Notification model class
    private static class Notification {
        String id;
        String title;
        String message;
        String timestamp;

        public Notification(String id, String title, String message, String timestamp) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.timestamp = timestamp;
        }
    }

    // Adapter class
    private static class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
        private List<Notification> notifications;
        private final OnMarkReadListener markReadListener;

        interface OnMarkReadListener {
            void onMarkRead(String notificationId, int position);
        }

        public NotificationAdapter(List<Notification> notifications, OnMarkReadListener markReadListener) {
            this.notifications = notifications;
            this.markReadListener = markReadListener;
        }

        @NonNull
        @Override
        public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new NotificationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
            Notification notification = notifications.get(position);
            holder.tvTitle.setText(notification.title);
            holder.tvMessage.setText(notification.message);
            holder.tvTimestamp.setText(notification.timestamp != null ? notification.timestamp : "");

            holder.btnRead.setOnClickListener(v -> {
                markReadListener.onMarkRead(notification.id, position);
            });
        }

        @Override
        public int getItemCount() {
            return notifications.size();
        }

        static class NotificationViewHolder extends RecyclerView.ViewHolder {
            CardView cardView;
            TextView tvTitle, tvMessage, tvTimestamp;
            Button btnRead;

            public NotificationViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.cardNotification);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvMessage = itemView.findViewById(R.id.tvMessage);
                tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
                btnRead = itemView.findViewById(R.id.btnRead);
            }
        }
    }
}