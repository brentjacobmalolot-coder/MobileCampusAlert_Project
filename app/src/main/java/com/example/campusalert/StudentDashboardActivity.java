package com.example.campusalert;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campusalert.adapters.ReportAdapter;
import com.example.campusalert.databinding.DashboardStudentBinding;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SessionManager;
import com.example.campusalert.viewmodels.ReportViewModel;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class StudentDashboardActivity extends AppCompatActivity {
    private DashboardStudentBinding binding;
    private ReportViewModel reportViewModel;
    private SessionManager sessionManager;
    private ReportAdapter adapter;
    private List<Report> allReports = new ArrayList<>();
    private FirebaseFirestore db;

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
        if (!sessionManager.isLoggedIn() || sessionManager.getUserRole() != UserRole.STUDENT) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        binding = DashboardStudentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        // Dynamic Greeting
        binding.textWelcome.setText("Welcome, " + sessionManager.getUserName());

        setupRecyclerView();
        setupFilters();
        setupListeners();
        
        observeData();
        setupNotificationBadge();
        requestNotificationPermission();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
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
            
        binding.btnViewAll.setOnClickListener(v -> {
            binding.chipAll.setChecked(true);
            filterReports();
        });

        binding.btnNotifications.setOnClickListener(v -> 
            startActivity(new Intent(this, NotificationsActivity.class)));
    }
}