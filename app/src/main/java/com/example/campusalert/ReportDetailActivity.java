package com.example.campusalert;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusalert.databinding.ActivityReportDetailBinding;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SessionManager;
import com.example.campusalert.viewmodels.ReportViewModel;

public class ReportDetailActivity extends AppCompatActivity {
    private ActivityReportDetailBinding binding;
    private ReportViewModel reportViewModel;
    private SessionManager sessionManager;
    private int reportId;
    private Report currentReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityReportDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);

        reportId = getIntent().getIntExtra("REPORT_ID", -1);
        if (reportId == -1) {
            finish();
            return;
        }

        setupToolbar();
        setupStaffAction();

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
            binding.btnSaveUpdate.setOnClickListener(v -> updateReport());
        } else {
            binding.cardStaffAction.setVisibility(View.GONE);
        }
    }

    private void populateData(Report report) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Report ID: #" + report.getId());
        }
        
        binding.detailTitle.setText(report.getTitle());
        binding.detailBuilding.setText(report.getBuilding());
        binding.detailDescription.setText(report.getDescription());
        binding.badgeStatus.setText(report.getStatus().toUpperCase());
        
        binding.textStaffComment.setText(report.getStaffComment() != null && !report.getStaffComment().isEmpty() 
                ? report.getStaffComment() : "No formal response yet.");

        // Load evidence photo if available
        if (report.getImageUri() != null && !report.getImageUri().isEmpty()) {
            binding.cardEvidencePhoto.setVisibility(View.VISIBLE);
            try {
                binding.detailImage.setImageURI(Uri.parse(report.getImageUri()));
            } catch (Exception e) {
                binding.cardEvidencePhoto.setVisibility(View.GONE);
            }
        } else {
            binding.cardEvidencePhoto.setVisibility(View.GONE);
        }

        updateStatusUI(report.getStatus());
        
        if (sessionManager.getUserRole() == UserRole.MAINTENANCE_STAFF) {
            binding.staffCommentEdit.setText(report.getStaffComment());
            if ("Pending".equals(report.getStatus())) binding.toggleStatus.check(R.id.btn_status_pending);
            else if ("In Progress".equals(report.getStatus())) binding.toggleStatus.check(R.id.btn_status_progress);
            else if ("Completed".equals(report.getStatus())) binding.toggleStatus.check(R.id.btn_status_completed);
        }
    }

    private void updateStatusUI(String status) {
        // Reset steps
        binding.dotProgress.setBackgroundResource(R.drawable.circle_gray);
        binding.dotCompleted.setBackgroundResource(R.drawable.circle_gray);
        binding.line1.setBackgroundColor(0xFFDDDDDD);
        binding.line2.setBackgroundColor(0xFFDDDDDD);

        if ("In Progress".equals(status) || "Completed".equals(status)) {
            binding.dotProgress.setBackgroundResource(R.drawable.circle_green);
            binding.line1.setBackgroundResource(R.color.forest_green);
        }
        if ("Completed".equals(status)) {
            binding.dotCompleted.setBackgroundResource(R.drawable.circle_green);
            binding.line2.setBackgroundResource(R.color.forest_green);
        }
    }

    private void updateReport() {
        int checkedId = binding.toggleStatus.getCheckedButtonId();
        String newStatus = "Pending";
        if (checkedId == R.id.btn_status_progress) newStatus = "In Progress";
        else if (checkedId == R.id.btn_status_completed) newStatus = "Completed";

        String comment = binding.staffCommentEdit.getText().toString().trim();

        currentReport.setStatus(newStatus);
        currentReport.setStaffComment(comment);
        currentReport.setStaffId(sessionManager.getUserEmail());

        reportViewModel.update(currentReport);
        Toast.makeText(this, "Report updated successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}