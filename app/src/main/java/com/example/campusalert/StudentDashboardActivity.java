package com.example.campusalert;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campusalert.adapters.ReportAdapter;
import com.example.campusalert.databinding.DashboardStudentBinding;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SessionManager;
import com.example.campusalert.viewmodels.ReportViewModel;

import java.util.ArrayList;
import java.util.List;

public class StudentDashboardActivity extends AppCompatActivity {
    private DashboardStudentBinding binding;
    private ReportViewModel reportViewModel;
    private SessionManager sessionManager;
    private ReportAdapter adapter;
    private List<Report> allReports = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
        // Security check
        if (!sessionManager.isLoggedIn() || sessionManager.getUserRole() != UserRole.STUDENT) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        binding = DashboardStudentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Dynamic Greeting
        binding.textWelcome.setText("Welcome, " + sessionManager.getUserName());

        setupRecyclerView();
        setupFilters();
        setupListeners();
        
        observeData();
    }

    private void setupRecyclerView() {
        binding.recyclerReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter();
        binding.recyclerReports.setAdapter(adapter);

        adapter.setOnItemClickListener(report -> {
            Intent intent = new Intent(this, ReportDetailActivity.class);
            intent.putExtra("REPORT_ID", report.getId());
            startActivity(intent);
        });
    }

    private void setupFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            filterReports();
        });
    }

    private void observeData() {
        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        reportViewModel.getReportsByStudent(sessionManager.getUserEmail()).observe(this, reports -> {
            if (reports != null) {
                allReports = reports;
                filterReports();
            }
        });
    }

    private void filterReports() {
        int checkedId = binding.chipGroupFilter.getCheckedChipId();
        List<Report> filtered = new ArrayList<>();
        
        String targetStatus = "";
        if (checkedId == R.id.chip_pending) targetStatus = "Pending";
        else if (checkedId == R.id.chip_in_progress) targetStatus = "In Progress";
        else if (checkedId == R.id.chip_completed) targetStatus = "Completed";

        if (targetStatus.isEmpty()) {
            filtered.addAll(allReports);
        } else {
            for (Report r : allReports) {
                if (targetStatus.equalsIgnoreCase(r.getStatus())) {
                    filtered.add(r);
                }
            }
        }
        
        adapter.submitList(filtered);
    }

    private void setupListeners() {
        binding.fabReport.setOnClickListener(v -> 
            startActivity(new Intent(this, ReportSubmissionActivity.class)));

        binding.btnSettings.setOnClickListener(v -> 
            startActivity(new Intent(this, SettingsActivity.class)));
            
        binding.profileImage.setOnClickListener(v -> 
            startActivity(new Intent(this, SettingsActivity.class)));
            
        binding.btnViewAll.setOnClickListener(v -> {
            binding.chipAll.setChecked(true);
            filterReports();
        });

        binding.btnNotifications.setOnClickListener(v -> 
            startActivity(new Intent(this, NotificationsActivity.class)));
    }
}