package com.example.campusalert;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.campusalert.databinding.ActivityReportDetailBinding;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.TaskAssignment;
import com.example.campusalert.models.User;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SessionManager;
import com.example.campusalert.viewmodels.ReportViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ReportDetailActivity extends AppCompatActivity {
    private ActivityReportDetailBinding binding;
    private ReportViewModel reportViewModel;
    private SessionManager sessionManager;
    private String reportId;
    private Report currentReport;
    private final List<User> staffList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityReportDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        db = FirebaseFirestore.getInstance();
        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);

        reportId = getIntent().getStringExtra("REPORT_ID");
        if (reportId == null || reportId.isEmpty()) {
            finish();
            return;
        }

        setupToolbar();
        applyUserPermissions();

        reportViewModel.getReportById(reportId).observe(this, report -> {
            if (report != null) {
                currentReport = report;
                populateData(report);
                invalidateOptionsMenu();
            }
        });
    }

    private void applyUserPermissions() {
        UserRole role = sessionManager.getUserRole();
        binding.cardAdminAssignment.setVisibility(role == UserRole.ADMIN ? View.VISIBLE : View.GONE);
        binding.cardStaffAction.setVisibility(role == UserRole.MAINTENANCE_STAFF ? View.VISIBLE : View.GONE);

        if (role == UserRole.ADMIN) setupAdminAssignment();
        else if (role == UserRole.MAINTENANCE_STAFF) setupStaffAction();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Report Details");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (sessionManager.getUserRole() == UserRole.MAINTENANCE_STAFF || sessionManager.getUserRole() == UserRole.ADMIN) {
            getMenuInflater().inflate(R.menu.menu_report_detail, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_manage) {
            if (currentReport != null) showManagementMenu(currentReport);
            return true;
        }
        return super.onOptionsItemSelected(item);
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
                            finish(); // Finish as it's no longer "active"
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
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupStaffAction() {
        binding.btnSaveUpdate.setOnClickListener(v -> updateReportStatus());
    }

    private void setupAdminAssignment() {
        setupStaffSpinner();
        binding.btnAssignStaff.setOnClickListener(v -> assignStaff());
    }

    private void setupStaffSpinner() {
        // Query users collection for maintenance staff
        db.collection("users").addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("ReportDetail", "Error fetching staff: " + error.getMessage());
                return;
            }
            if (value != null) {
                List<String> displayList = new ArrayList<>();
                displayList.add("Select Maintenance Staff");
                staffList.clear();

                for (QueryDocumentSnapshot doc : value) {
                    User user = doc.toObject(User.class);
                    String role = user.getRole();
                    if (role != null && (role.equalsIgnoreCase("maintenance") || 
                                       role.equalsIgnoreCase("staff") || 
                                       role.equalsIgnoreCase("MAINTENANCE_STAFF"))) {
                        
                        String displayName = (user.getName() != null && !user.getName().isEmpty()) 
                                ? user.getName() : user.getEmail();
                        displayList.add(displayName);
                        staffList.add(user);
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        ReportDetailActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        displayList
                );
                binding.staffSpinner.setAdapter(adapter);
            }
        });
    }

    private void populateData(Report report) {
        binding.detailTitle.setText(report.getTitle());
        binding.detailBuilding.setText(report.getBuilding());
        binding.detailDescription.setText(report.getDescription());
        binding.badgeStatus.setText(report.getStatus().toUpperCase());
        
        binding.textViewCount.setText(report.getViewCount() + " views");
        
        binding.textStaffComment.setText(report.getStaffComment() != null && !report.getStaffComment().isEmpty() 
                ? report.getStaffComment() : "No comments yet from staff.");

        String imageStr = report.getImageUrl();
        if (imageStr != null && !imageStr.isEmpty()) {
            binding.cardEvidencePhoto.setVisibility(View.VISIBLE);
            
            if (imageStr.startsWith("data:image")) {
                try {
                    String pureBase64 = imageStr.substring(imageStr.indexOf(",") + 1);
                    byte[] decodedString = Base64.decode(pureBase64, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    binding.detailImage.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    Log.e("ReportDetail", "Error decoding Base64 image", e);
                }
            } else {
                Glide.with(this).load(imageStr).into(binding.detailImage);
            }
            
            binding.detailImage.setOnClickListener(v -> showFullImage(imageStr));
        } else {
            binding.cardEvidencePhoto.setVisibility(View.GONE);
        }

        updateStatusUI(report.getStatus());
        
        if (sessionManager.getUserRole() == UserRole.MAINTENANCE_STAFF) {
            binding.staffCommentEdit.setText(report.getStaffComment());
            if ("In Progress".equals(report.getStatus())) {
                binding.toggleStatus.check(R.id.btn_status_progress);
            } else if ("Completed".equals(report.getStatus())) {
                binding.toggleStatus.check(R.id.btn_status_completed);
            } else {
                binding.toggleStatus.check(R.id.btn_status_pending);
            }
        }
    }

    private void showFullImage(String imageStr) {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView imageView = new ImageView(this);
        
        if (imageStr.startsWith("data:image")) {
            try {
                String pureBase64 = imageStr.substring(imageStr.indexOf(",") + 1);
                byte[] decodedString = Base64.decode(pureBase64, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                imageView.setImageBitmap(decodedByte);
            } catch (Exception e) {
                Log.e("ReportDetail", "Error decoding Base64 for full view", e);
            }
        } else {
            Glide.with(this).load(imageStr).into(imageView);
        }
        
        dialog.setContentView(imageView);
        imageView.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateStatusUI(String status) {
        int grayColor = 0xFFDDDDDD;
        int greenColor = ContextCompat.getColor(this, R.color.forest_green);
        
        binding.dotProgress.setBackgroundResource(R.drawable.circle_gray);
        binding.dotCompleted.setBackgroundResource(R.drawable.circle_gray);
        binding.line1.setBackgroundColor(grayColor);
        binding.line2.setBackgroundColor(grayColor);

        if ("In Progress".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
            binding.dotProgress.setBackgroundResource(R.drawable.circle_green);
            binding.line1.setBackgroundColor(greenColor);
        }
        if ("Completed".equalsIgnoreCase(status)) {
            binding.dotCompleted.setBackgroundResource(R.drawable.circle_green);
            binding.line2.setBackgroundColor(greenColor);
        }
    }

    private void assignStaff() {
        if (currentReport == null) return;

        String selectedValue = binding.staffSpinner.getText().toString();
        User selectedStaff = null;
        
        for (User u : staffList) {
            if ((u.getName() != null && u.getName().equals(selectedValue)) || u.getEmail().equals(selectedValue)) {
                selectedStaff = u;
                break;
            }
        }

        if (selectedStaff != null) {
            currentReport.setStaffId(selectedStaff.getEmail());
            currentReport.setStatus("In Progress");
            reportViewModel.update(currentReport);

            // Create TaskAssignment for the immediate pop-up system
            String taskId = currentReport.getId();
            TaskAssignment assignment = new TaskAssignment(
                    taskId,
                    currentReport.getTitle(),
                    currentReport.getDescription(),
                    selectedStaff.getEmail(),
                    sessionManager.getUserEmail(),
                    System.currentTimeMillis(),
                    "Pending"
            );
            
            // Write to Firestore "assignments" collection
            db.collection("assignments").document(selectedStaff.getEmail())
                    .collection("tasks").document(taskId).set(assignment);

            reportViewModel.sendNotification(selectedStaff.getEmail(), "New Task", "Assigned: " + currentReport.getTitle(), taskId);
            reportViewModel.sendNotification(currentReport.getUserId(), "Status Update", "Issue is now handled by " + selectedStaff.getName(), taskId);
            
            // Notify other Admins about the assignment
            reportViewModel.notifyAdmins("Staff Assigned", 
                    "Staff member " + selectedStaff.getName() + " has been assigned to: " + currentReport.getTitle(),
                    taskId);

            Toast.makeText(this, "Staff assigned successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please select a valid staff member", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateReportStatus() {
        if (currentReport == null) return;
        int checkedId = binding.toggleStatus.getCheckedButtonId();
        String newStatus = "Pending";
        if (checkedId == R.id.btn_status_progress) newStatus = "In Progress";
        else if (checkedId == R.id.btn_status_completed) newStatus = "Completed";

        currentReport.setStatus(newStatus);
        currentReport.setStaffComment(binding.staffCommentEdit.getText().toString().trim());
        reportViewModel.update(currentReport);
        
        // Notify Student
        reportViewModel.sendNotification(currentReport.getUserId(), "Status Update", "Status changed to: " + newStatus, currentReport.getId());
        
        // Notify Admins about the update
        reportViewModel.notifyAdmins("Report Updated", 
                "Staff (" + sessionManager.getUserName() + ") updated status to " + newStatus + " for: " + currentReport.getTitle(),
                currentReport.getId());

        Toast.makeText(this, "Report updated", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}