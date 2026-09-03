package com.example.campusalert;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class MaintenanceDashboardActivity extends AppCompatActivity {
    private DashboardMaintenanceBinding binding;
    private ReportViewModel reportViewModel;
    private SessionManager sessionManager;
    private TaskAdapter adapter;
    private DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://mobile-application-db-321ff-default-rtdb.firebaseio.com/";

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

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        // Personalize the dashboard with the staff's name
        binding.collapsingToolbar.setTitle(sessionManager.getUserName());

        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        
        setupRecyclerView();
        setupTabs();
        setupListeners();
        
        updateFilter(getString(R.string.assigned));

        // Start listening for new task assignments
        String userEmail = sessionManager.getUserEmail();
        if (userEmail != null) {
            startAssignmentListener(sanitizeKey(userEmail));
        }
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
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("MaintenanceDashboard", "Assignment listener failed: " + error.getMessage());
                    }
                });
    }

    private void showAssignmentPrompt(TaskAssignment task, DatabaseReference ref) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("🛠️ New Task Assigned")
                .setMessage("You have been assigned to: " + task.getTitle() + "\n\nDescription: " + task.getDescription())
                .setCancelable(false)
                .setPositiveButton("Accept & View", (dialog, which) -> {
                    // Update task status in Firebase
                    ref.child("status").setValue("Accepted");
                    
                    // Open Report Detail
                    Intent intent = new Intent(this, ReportDetailActivity.class);
                    intent.putExtra("REPORT_ID", task.getId());
                    startActivity(intent);
                    
                    dialog.dismiss();
                })
                .setNegativeButton("Dismiss", (dialog, which) -> {
                    ref.child("status").setValue("Dismissed");
                    dialog.dismiss();
                })
                .show();
    }

    private String sanitizeKey(String key) {
        if (key == null) return null;
        return key.replace(".", ",")
                  .replace("#", "_")
                  .replace("$", "_")
                  .replace("[", "_")
                  .replace("]", "_");
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
            
        binding.profileImageSmall.setOnClickListener(v -> 
            startActivity(new Intent(this, SettingsActivity.class)));

        binding.btnNotifications.setOnClickListener(v -> 
            startActivity(new Intent(this, NotificationsActivity.class)));
    }

    private void updateFilter(String statusTitle) {
        String dbStatus = "Pending";
        if (getString(R.string.in_progress).equals(statusTitle)) {
            dbStatus = "In Progress";
        } else if (getString(R.string.completed).equals(statusTitle)) {
            dbStatus = "Completed";
        }

        // Personalized task list: Only show reports assigned to this specific staff member
        reportViewModel.getReportsByStaffAndStatus(sessionManager.getUserEmail(), dbStatus).observe(this, reports -> {
            if (reports != null) {
                adapter.submitList(reports);
                binding.textTaskCount.setText(getString(R.string.task_count_format, statusTitle, reports.size()));
            }
        });
    }
}
