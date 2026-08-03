package com.example.campusalert;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campusalert.adapters.ReportAdapter;
import com.example.campusalert.databinding.DashboardStudentBinding;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SessionManager;
import com.example.campusalert.viewmodels.ReportViewModel;

public class StudentDashboardActivity extends AppCompatActivity {
    private DashboardStudentBinding binding;
    private ReportViewModel reportViewModel;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
        // RBAC Security Guard
        if (!sessionManager.isLoggedIn() || sessionManager.getUserRole() != UserRole.STUDENT) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        binding = DashboardStudentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();
        setupListeners();
    }

    private void setupRecyclerView() {
        binding.recyclerReports.setLayoutManager(new LinearLayoutManager(this));
        ReportAdapter adapter = new ReportAdapter();
        binding.recyclerReports.setAdapter(adapter);

        adapter.setOnItemClickListener(report -> {
            Intent intent = new Intent(this, ReportDetailActivity.class);
            intent.putExtra("REPORT_ID", report.getId());
            startActivity(intent);
        });
        
        // Observe Data
        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        reportViewModel.getReportsByStudent(sessionManager.getUserEmail()).observe(this, adapter::submitList);
    }

    private void setupListeners() {
        binding.fabReport.setOnClickListener(v -> 
            startActivity(new Intent(this, ReportSubmissionActivity.class)));

        binding.btnSettings.setOnClickListener(v -> 
            startActivity(new Intent(this, SettingsActivity.class)));
            
        binding.profileImage.setOnClickListener(v -> 
            startActivity(new Intent(this, SettingsActivity.class)));
    }
}