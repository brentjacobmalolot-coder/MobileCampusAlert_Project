package com.example.campusalert;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campusalert.adapters.NotificationAdapter;
import com.example.campusalert.databinding.ActivityNotificationsBinding;
import com.example.campusalert.models.Notification;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.TaskAssignment;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SessionManager;
import com.example.campusalert.viewmodels.ReportViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationsActivity extends AppCompatActivity {
    private ActivityNotificationsBinding binding;
    private NotificationAdapter adapter;
    private SessionManager sessionManager;
    private ReportViewModel reportViewModel;
    private FirebaseFirestore db;
    private ListenerRegistration notificationListener;
    private ListenerRegistration assignmentListener;
    private final Set<String> shownTasks = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        db = FirebaseFirestore.getInstance();
        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);

        setupToolbar();
        setupRecyclerView();
        loadNotifications();

        if (sessionManager.getUserRole() == UserRole.MAINTENANCE_STAFF) {
            String email = sessionManager.getUserEmail();
            if (email != null) {
                startAssignmentListener(email);
            }
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        binding.recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter();
        binding.recyclerNotifications.setAdapter(adapter);

        adapter.setOnNotificationActionListener(new NotificationAdapter.OnNotificationActionListener() {
            @Override
            public void onClick(Notification notification) {
                // Mark as read when clicked
                if (!notification.isRead()) {
                    db.collection("notifications").document(notification.getId()).update("read", true);
                }
                
                if (notification.getReportId() != null) {
                    Intent intent = new Intent(NotificationsActivity.this, ReportDetailActivity.class);
                    intent.putExtra("REPORT_ID", notification.getReportId());
                    startActivity(intent);
                }
            }

            @Override
            public void onLongClick(Notification notification) {
                // If the notification is linked to a report, allow management
                if (notification.getReportId() != null && sessionManager.getUserRole() == UserRole.MAINTENANCE_STAFF) {
                    fetchReportAndShowMenu(notification.getReportId());
                }
            }
        });
    }

    private void fetchReportAndShowMenu(String reportId) {
        // Use a one-shot observer to avoid leaking or multiple triggers
        reportViewModel.getReportById(reportId).observe(this, new Observer<Report>() {
            @Override
            public void onChanged(Report report) {
                if (report != null) {
                    showManagementMenu(report);
                }
                reportViewModel.getReportById(reportId).removeObserver(this);
            }
        });
    }

    private void showManagementMenu(Report report) {
        String flagAction = report.isFlagged() ? "Remove Flag" : "Flag as Important";
        String[] options = {flagAction, "Mark as Duplicate (Merge)", "Archive Report"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Manage Report: " + report.getTitle())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            reportViewModel.flagReport(report.getId(), !report.isFlagged());
                            Toast.makeText(this, !report.isFlagged() ? "Report flagged" : "Flag removed", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            showMergeDialog(report.getId());
                            break;
                        case 2:
                            reportViewModel.archiveReport(report.getId());
                            Toast.makeText(this, "Report archived", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    private void showMergeDialog(String duplicateId) {
        final EditText input = new EditText(this);
        input.setHint("Enter Master Report ID");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Merge Report")
                .setMessage("Enter the ID of the master report.")
                .setView(input)
                .setPositiveButton("Merge", (dialog, which) -> {
                    String masterId = input.getText().toString().trim();
                    if (!masterId.isEmpty()) {
                        reportViewModel.mergeReports(duplicateId, masterId);
                        Toast.makeText(this, "Reports merged", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startAssignmentListener(String userEmail) {
        assignmentListener = db.collection("assignments").document(userEmail).collection("tasks")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Notifications", "Assignment listener failed: " + error.getMessage());
                        return;
                    }
                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            TaskAssignment task = dc.getDocument().toObject(TaskAssignment.class);
                            String taskId = dc.getDocument().getId();
                            
                            if (task != null && "Pending".equals(task.getStatus())) {
                                if (dc.getType() == DocumentChange.Type.ADDED || dc.getType() == DocumentChange.Type.MODIFIED) {
                                    if (!shownTasks.contains(taskId)) {
                                        shownTasks.add(taskId);
                                        showAssignmentPrompt(task, taskId);
                                    }
                                }
                            } else {
                                shownTasks.remove(taskId);
                            }
                        }
                    }
                });
    }

    private void showAssignmentPrompt(TaskAssignment task, String taskId) {
        String userEmail = sessionManager.getUserEmail();
        new MaterialAlertDialogBuilder(this)
                .setTitle("🛠️ New Task Assigned")
                .setMessage("Task: " + task.getTitle())
                .setCancelable(false)
                .setPositiveButton("Accept", (dialog, which) -> {
                    if (userEmail != null) {
                        db.collection("assignments").document(userEmail).collection("tasks")
                                .document(taskId).update("status", "Accepted");
                    }
                    Intent intent = new Intent(this, ReportDetailActivity.class);
                    intent.putExtra("REPORT_ID", task.getId());
                    startActivity(intent);
                })
                .setNegativeButton("Dismiss", (dialog, which) -> {
                    if (userEmail != null) {
                        db.collection("assignments").document(userEmail).collection("tasks")
                                .document(taskId).update("status", "Dismissed");
                    }
                })
                .show();
    }

    private void loadNotifications() {
        String email = sessionManager.getUserEmail();
        if (email == null) return;
        
        notificationListener = db.collection("notifications")
                .whereEqualTo("recipientEmail", email)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Notifications", "Error loading notifications: " + error.getMessage());
                        return;
                    }
                    if (value != null) {
                        List<Notification> notifications = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            Notification n = doc.toObject(Notification.class);
                            if (n != null) {
                                notifications.add(n);
                            }
                        }
                        adapter.submitList(notifications);
                        binding.textNoNotifications.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) notificationListener.remove();
        if (assignmentListener != null) assignmentListener.remove();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}