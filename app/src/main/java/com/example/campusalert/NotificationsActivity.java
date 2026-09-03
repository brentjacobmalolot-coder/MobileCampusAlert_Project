package com.example.campusalert;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {
    private ActivityNotificationsBinding binding;
    private NotificationAdapter adapter;
    private SessionManager sessionManager;
    private ReportViewModel reportViewModel;
    private DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://mobile-application-db-321ff-default-rtdb.firebaseio.com/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();
        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);

        setupToolbar();
        setupRecyclerView();
        loadNotifications();

        if (sessionManager.getUserRole() == UserRole.MAINTENANCE_STAFF) {
            String email = sessionManager.getUserEmail();
            if (email != null) {
                startAssignmentListener(sanitizeKey(email));
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

    private void startAssignmentListener(String encodedEmail) {
        mDatabase.child("assignments").child(encodedEmail)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                        TaskAssignment task = snapshot.getValue(TaskAssignment.class);
                        if (task != null && "Pending".equals(task.getStatus())) {
                            showAssignmentPrompt(task, snapshot.getRef());
                        }
                    }
                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}
                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void showAssignmentPrompt(TaskAssignment task, DatabaseReference ref) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("🛠️ New Task Assigned")
                .setMessage("Task: " + task.getTitle())
                .setCancelable(false)
                .setPositiveButton("Accept", (dialog, which) -> {
                    ref.child("status").setValue("Accepted");
                    Intent intent = new Intent(this, ReportDetailActivity.class);
                    intent.putExtra("REPORT_ID", task.getId());
                    startActivity(intent);
                })
                .setNegativeButton("Dismiss", (dialog, which) -> ref.child("status").setValue("Dismissed"))
                .show();
    }

    private String sanitizeKey(String key) {
        if (key == null) return null;
        return key.replace(".", ",").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");
    }

    private void loadNotifications() {
        String email = sessionManager.getUserEmail();
        if (email == null) return;
        
        String encodedEmail = sanitizeKey(email);
        mDatabase.child("notifications").child(encodedEmail)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Notification> notifications = new ArrayList<>();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Notification n = data.getValue(Notification.class);
                            if (n != null) {
                                notifications.add(n);
                                if (!n.isRead()) {
                                    data.getRef().child("read").setValue(true);
                                }
                            }
                        }
                        Collections.reverse(notifications);
                        adapter.submitList(notifications);
                        binding.textNoNotifications.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
