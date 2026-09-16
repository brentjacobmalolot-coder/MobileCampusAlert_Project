package com.example.campusalert;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campusalert.adapters.TaskAdapter;
import com.example.campusalert.databinding.DashboardMaintenanceBinding;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.TaskAssignment;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SessionManager;
import com.example.campusalert.viewmodels.ReportViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MaintenanceDashboardActivity extends AppCompatActivity {
    private DashboardMaintenanceBinding binding;
    private ReportViewModel reportViewModel;
    private SessionManager sessionManager;
    private TaskAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration assignmentListener;
    private final Set<String> shownTasks = new HashSet<>();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Notifications are disabled. You might miss important alerts.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
        // Security check
        if (!sessionManager.isLoggedIn() || sessionManager.getUserRole() != UserRole.MAINTENANCE_STAFF) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        binding = DashboardMaintenanceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Personalize the dashboard with the staff's name
        binding.collapsingToolbar.setTitle(sessionManager.getUserName());

        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        
        setupRecyclerView();
        setupTabs();
        setupListeners();
        
        updateFilter(getString(R.string.assigned));
        setupNotificationBadge();
        requestNotificationPermission();

        // Start listening for new task assignments
        String userEmail = sessionManager.getUserEmail();
        if (userEmail != null) {
            startAssignmentListener(userEmail);
        }
    }

    private void startAssignmentListener(String userEmail) {
        assignmentListener = db.collection("assignments").document(userEmail).collection("tasks")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("MaintenanceDashboard", "Assignment listener failed: " + error.getMessage());
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
                                // If task is no longer pending, allow it to be shown again if it becomes pending later
                                shownTasks.remove(taskId);
                            }
                        }
                    }
                });
    }

    private void showAssignmentPrompt(TaskAssignment task, String taskId) {
        String userEmail = sessionManager.getUserEmail();
        new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                .setTitle("🛠️ New Task Assigned")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setMessage("You have been assigned a new report.\n\n" +
                        "Title: " + task.getTitle() + "\n" +
                        "Location: " + task.getDescription()) // TaskAssignment might have location in description or similar
                .setCancelable(false)
                .setPositiveButton("Accept & View", (dialog, which) -> {
                    // Update task status in Firestore
                    if (userEmail != null) {
                        db.collection("assignments").document(userEmail).collection("tasks")
                                .document(taskId).update("status", "Accepted");
                    }
                    
                    // Open Report Detail
                    Intent intent = new Intent(this, ReportDetailActivity.class);
                    intent.putExtra("REPORT_ID", task.getId());
                    startActivity(intent);
                    
                    dialog.dismiss();
                })
                .setNegativeButton("Dismiss", (dialog, which) -> {
                    if (userEmail != null) {
                        db.collection("assignments").document(userEmail).collection("tasks")
                                .document(taskId).update("status", "Dismissed");
                    }
                    dialog.dismiss();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (assignmentListener != null) {
            assignmentListener.remove();
        }
    }

    private void setupRecyclerView() {
        binding.recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter();
        binding.recyclerTasks.setAdapter(adapter);

        adapter.setOnTaskActionListener(new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onView(Report report) {
                Intent intent = new Intent(MaintenanceDashboardActivity.this, ReportDetailActivity.class);
                intent.putExtra("REPORT_ID", report.getId());
                startActivity(intent);
            }

            @Override
            public void onComplete(Report report) {
                report.setStatus("Completed");
                report.setStaffId(sessionManager.getUserEmail());
                reportViewModel.update(report);
                
                // Notify Student
                reportViewModel.sendNotification(report.getUserId(), 
                        "Task Completed", 
                        "The maintenance issue has been resolved: " + report.getTitle());
                
                Toast.makeText(MaintenanceDashboardActivity.this, R.string.report_updated, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onManage(Report report) {
                showManagementMenu(report);
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
                        case 0: // Flagging
                            reportViewModel.flagReport(report.getId(), !report.isFlagged());
                            Toast.makeText(this, report.isFlagged() ? "Flag removed" : "Report flagged", Toast.LENGTH_SHORT).show();
                            break;
                        case 1: // Merging
                            showMergeDialog(report.getId());
                            break;
                        case 2: // Archiving
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
                .setMessage("Enter the ID of the master report to merge this duplicate into.")
                .setView(input)
                .setPositiveButton("Merge", (dialog, which) -> {
                    String masterId = input.getText().toString().trim();
                    if (!masterId.isEmpty()) {
                        reportViewModel.mergeReports(duplicateId, masterId);
                        Toast.makeText(this, "Reports merged", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Master ID cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                CharSequence text = tab.getText();
                if (text != null) {
                    updateFilter(text.toString());
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupListeners() {
        binding.btnSettings.setOnClickListener(v -> 
            startActivity(new Intent(this, SettingsActivity.class)));

        binding.btnNotifications.setOnClickListener(v -> 
            startActivity(new Intent(this, NotificationsActivity.class)));
    }

    private void setupNotificationBadge() {
        String email = sessionManager.getUserEmail();
        if (email == null) return;

        db.collection("notifications")
                .whereEqualTo("recipientEmail", email)
                .whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        int count = value.size();
                        binding.notificationBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void updateFilter(String statusTitle) {
        String dbStatus = "Pending";
        boolean showAllActive = false;

        if (getString(R.string.assigned).equals(statusTitle)) {
            showAllActive = true;
        } else if (getString(R.string.in_progress).equals(statusTitle)) {
            dbStatus = "In Progress";
        } else if (getString(R.string.completed).equals(statusTitle)) {
            dbStatus = "Completed";
        }

        LiveData<List<Report>> reportsLiveData;
        if (showAllActive) {
            reportsLiveData = reportViewModel.getReportsByStaffActive(sessionManager.getUserEmail());
        } else {
            reportsLiveData = reportViewModel.getReportsByStaffAndStatus(sessionManager.getUserEmail(), dbStatus);
        }

        // Personalized task list: Only show reports assigned to this specific staff member
        reportsLiveData.observe(this, reports -> {
            if (reports != null) {
                adapter.submitList(reports);
                binding.textTaskCount.setText(getString(R.string.task_count_format, statusTitle, reports.size()));
            }
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}