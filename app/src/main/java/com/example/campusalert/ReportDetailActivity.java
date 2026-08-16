package com.example.campusalert;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.campusalert.databinding.ActivityReportDetailBinding;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.User;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SessionManager;
import com.example.campusalert.viewmodels.ReportViewModel;

import java.util.ArrayList;
import java.util.List;

public class ReportDetailActivity extends AppCompatActivity {
    private ActivityReportDetailBinding binding;
    private ReportViewModel reportViewModel;
    private SessionManager sessionManager;
    private String reportId;
    private Report currentReport;
    private List<User> staffList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityReportDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);

        reportId = getIntent().getStringExtra("REPORT_ID");
        if (reportId == null || reportId.isEmpty()) {
            finish();
            return;
        }

        setupToolbar();
        setupStaffAction();
        setupAdminAssignment();

        reportViewModel.getReportById(reportId).observe(this, report -> {
            if (report != null) {
                currentReport = report;
                populateData(report);
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupStaffAction() {
        if (sessionManager.getUserRole() == UserRole.MAINTENANCE_STAFF) {
            binding.cardStaffAction.setVisibility(View.VISIBLE);
            binding.btnSaveUpdate.setOnClickListener(v -> updateReportStatus());
        } else {
            binding.cardStaffAction.setVisibility(View.GONE);
        }
    }

    private void setupAdminAssignment() {
        if (sessionManager.getUserRole() == UserRole.ADMIN) {
            binding.cardAdminAssignment.setVisibility(View.VISIBLE);
            
            reportViewModel.getStaffMembers().observe(this, users -> {
                if (users != null) {
                    this.staffList = users;
                    List<String> names = new ArrayList<>();
                    for (User u : users) names.add(u.getName());
                    
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                            android.R.layout.simple_dropdown_item_1line, names);
                    binding.staffSpinner.setAdapter(adapter);
                }
            });

            binding.btnAssignStaff.setOnClickListener(v -> assignStaff());
        } else {
            binding.cardAdminAssignment.setVisibility(View.GONE);
        }
    }

    private void populateData(Report report) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Report Details");
        }
        
        binding.detailTitle.setText(report.getTitle());
        binding.detailBuilding.setText(report.getBuilding());
        binding.detailDescription.setText(report.getDescription());
        binding.badgeStatus.setText(report.getStatus().toUpperCase());
        
        binding.textStaffComment.setText(report.getStaffComment() != null && !report.getStaffComment().isEmpty() 
                ? report.getStaffComment() : getString(R.string.no_comments_yet));

        if (report.getImageUri() != null && !report.getImageUri().isEmpty()) {
            binding.cardEvidencePhoto.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(report.getImageUri())
                    .placeholder(android.R.drawable.progress_horizontal)
                    .error(android.R.drawable.stat_notify_error)
                    .into(binding.detailImage);
        } else {
            binding.cardEvidencePhoto.setVisibility(View.GONE);
        }

        updateStatusUI(report.getStatus());
        
        if (sessionManager.getUserRole() == UserRole.MAINTENANCE_STAFF) {
            binding.staffCommentEdit.setText(report.getStaffComment());
            if ("Pending".equalsIgnoreCase(report.getStatus())) binding.toggleStatus.check(R.id.btn_status_pending);
            else if ("In Progress".equalsIgnoreCase(report.getStatus())) binding.toggleStatus.check(R.id.btn_status_progress);
            else if ("Completed".equalsIgnoreCase(report.getStatus())) binding.toggleStatus.check(R.id.btn_status_completed);
        }
    }

    private void updateStatusUI(String status) {
        binding.dotProgress.setBackgroundResource(R.drawable.circle_gray);
        binding.dotCompleted.setBackgroundResource(R.drawable.circle_gray);
        binding.line1.setBackgroundColor(0xFFDDDDDD);
        binding.line2.setBackgroundColor(0xFFDDDDDD);

        if ("In Progress".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
            binding.dotProgress.setBackgroundResource(R.drawable.circle_green);
            binding.line1.setBackgroundResource(R.color.forest_green);
        }
        if ("Completed".equalsIgnoreCase(status)) {
            binding.dotCompleted.setBackgroundResource(R.drawable.circle_green);
            binding.line2.setBackgroundResource(R.color.forest_green);
        }
    }

    private void assignStaff() {
        if (currentReport == null) return;

        String selectedName = binding.staffSpinner.getText().toString();
        User selectedStaff = null;
        for (User u : staffList) {
            if (u.getName().equals(selectedName)) {
                selectedStaff = u;
                break;
            }
        }

        if (selectedStaff != null) {
            currentReport.setStaffId(selectedStaff.getEmail());
            currentReport.setStatus("In Progress");
            reportViewModel.update(currentReport);

            // Notify Staff
            reportViewModel.sendNotification(selectedStaff.getEmail(), 
                    "New Task Assigned", 
                    "You have been assigned to: " + currentReport.getTitle());
            
            // Notify Student
            reportViewModel.sendNotification(currentReport.getUserId(), 
                    "Report Update", 
                    "Your report is now being handled by " + selectedStaff.getName());

            Toast.makeText(this, "Staff assigned and notified", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please select a staff member", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateReportStatus() {
        if (currentReport == null) return;

        int checkedId = binding.toggleStatus.getCheckedButtonId();
        String newStatus = "Pending";
        if (checkedId == R.id.btn_status_progress) newStatus = "In Progress";
        else if (checkedId == R.id.btn_status_completed) newStatus = "Completed";

        String comment = binding.staffCommentEdit.getText().toString().trim();

        currentReport.setStatus(newStatus);
        currentReport.setStaffComment(comment);
        
        reportViewModel.update(currentReport);
        
        // Notify Student
        reportViewModel.sendNotification(currentReport.getUserId(), 
                "Report Update", 
                "Status changed to: " + newStatus);

        Toast.makeText(this, R.string.report_updated, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}