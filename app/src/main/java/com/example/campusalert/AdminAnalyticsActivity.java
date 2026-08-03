package com.example.campusalert;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campusalert.adapters.ReportAdapter;
import com.example.campusalert.databinding.ActivityAdminAnalyticsBinding;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SessionManager;
import com.example.campusalert.viewmodels.ReportViewModel;

public class AdminAnalyticsActivity extends AppCompatActivity {
    private ActivityAdminAnalyticsBinding binding;
    private ReportViewModel reportViewModel;
    private SessionManager sessionManager;
    private ReportAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
        // Security Guard
        if (!sessionManager.isLoggedIn() || sessionManager.getUserRole() != UserRole.ADMIN) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        binding = ActivityAdminAnalyticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupRecyclerView();
        setupChartsAndStats();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        
        binding.btnSettings.setOnClickListener(v -> 
            startActivity(new Intent(this, SettingsActivity.class)));
            
        binding.profileImageAdmin.setOnClickListener(v -> 
            startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void setupRecyclerView() {
        binding.recyclerAllReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter();
        binding.recyclerAllReports.setAdapter(adapter);
        
        adapter.setOnItemClickListener(report -> {
            Intent intent = new Intent(this, ReportDetailActivity.class);
            intent.putExtra("REPORT_ID", report.getId());
            startActivity(intent);
        });
    }

    private void setupChartsAndStats() {
        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        
        // Real-time KPI updates
        reportViewModel.getOpenReportsCount().observe(this, count -> 
            binding.totalOpenText.setText(String.valueOf(count)));

        reportViewModel.getAllReports().observe(this, reports -> {
            adapter.submitList(reports);
            binding.avgResolutionText.setText(String.valueOf(reports.size()));
        });
        
        // Dynamic Chart scaling
        reportViewModel.getReportsByCategory().observe(this, categories -> {
            for (int i = 0; i < Math.min(categories.size(), 7); i++) {
                int resId = getResources().getIdentifier("bar_" + (i + 1), "id", getPackageName());
                View bar = findViewById(resId);
                if (bar != null) {
                    ViewGroup.LayoutParams params = bar.getLayoutParams();
                    params.height = Math.max(20, Math.min(categories.get(i).count * 40, 400));
                    bar.setLayoutParams(params);
                }
            }
        });
    }
}