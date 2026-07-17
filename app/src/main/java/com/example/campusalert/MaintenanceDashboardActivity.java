package com.example.campusalert;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campusalert.adapters.TaskAdapter;
import com.example.campusalert.databinding.DashboardMaintenanceBinding;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SessionManager;
import com.example.campusalert.viewmodels.ReportViewModel;
import com.google.android.material.tabs.TabLayout;

public class MaintenanceDashboardActivity extends AppCompatActivity {
    private DashboardMaintenanceBinding binding;
    private ReportViewModel reportViewModel;
    private SessionManager sessionManager;
    private TaskAdapter adapter;

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

        setupRecyclerView();
        setupTabs();
        setupListeners();
        
        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        updateFilter("Assigned");
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
                Toast.makeText(MaintenanceDashboardActivity.this, R.string.completed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateFilter(tab.getText().toString());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupListeners() {
        binding.btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void updateFilter(String status) {
        String dbStatus = status;
        if (getString(R.string.assigned).equals(status)) dbStatus = "Pending";
        else if (getString(R.string.in_progress).equals(status)) dbStatus = "In Progress";
        else if (getString(R.string.completed).equals(status)) dbStatus = "Completed";

        reportViewModel.getReportsByStatus(dbStatus).observe(this, reports -> {
            adapter.submitList(reports);
            binding.textTaskCount.setText(status + " (" + reports.size() + ")");
        });
    }
}