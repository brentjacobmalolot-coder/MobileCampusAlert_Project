package com.example.campusalert;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campusalert.adapters.ReportAdapter;
import com.example.campusalert.databinding.ActivityAdminAnalyticsBinding;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SessionManager;
import com.example.campusalert.viewmodels.ReportViewModel;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminAnalyticsActivity extends AppCompatActivity {
    private ActivityAdminAnalyticsBinding binding;
    private ReportViewModel reportViewModel;
    private SessionManager sessionManager;
    private ReportAdapter adapter;
    private List<Report> currentReports = new ArrayList<>();
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
        // Security Guard
        if (!sessionManager.isLoggedIn() || sessionManager.getUserRole() != UserRole.ADMIN) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        binding = ActivityAdminAnalyticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        db = FirebaseFirestore.getInstance();

        setupToolbar();
        setupRecyclerView();
        setupChartsAndStats();
        setupNotificationBadge();
        requestNotificationPermission();
        
        binding.btnSeeAll.setOnClickListener(v -> {
            // Logic to see all reports, perhaps another activity or expanding this one
            Toast.makeText(this, "Viewing all reports", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Admin: " + sessionManager.getUserName());
        }
        
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_analytics, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_export_csv) {
            if (!currentReports.isEmpty()) {
                exportToCSV(currentReports);
            } else {
                Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        
        // Total Reports KPI
        reportViewModel.getTotalReportsCount().observe(this, count -> 
            binding.textTotalCount.setText(String.valueOf(count)));

        // Pending Reports KPI
        reportViewModel.getReportsByStatus("Pending").observe(this, pendingReports -> {
            if (pendingReports != null) {
                binding.textPendingCount.setText(String.valueOf(pendingReports.size()));
            }
        });

        reportViewModel.getAllReports().observe(this, reports -> {
            if (reports != null) {
                this.currentReports = reports;
                
                // Calculate Total Engagement
                long totalEngagement = 0;
                for (Report r : reports) {
                    totalEngagement += (long) r.getEngagementScore();
                }
                binding.textTotalEngagement.setText(String.valueOf(totalEngagement));

                // Sort by Engagement Score (Descending)
                List<Report> sortedReports = new ArrayList<>(reports);
                Collections.sort(sortedReports, (r1, r2) -> 
                        Double.compare(r2.getEngagementScore(), r1.getEngagementScore()));
                
                adapter.submitList(sortedReports.size() > 5 ? sortedReports.subList(0, 5) : sortedReports);
                updateBarChart(reports);
            }
        });
    }

    private void updateBarChart(List<Report> reports) {
        Map<String, Integer> categoryCounts = new HashMap<>();
        for (Report report : reports) {
            String category = report.getCategory();
            if (category == null || category.isEmpty()) category = "Uncategorized";
            categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        
        // Sorting keys for consistency
        List<String> sortedCategories = new ArrayList<>(categoryCounts.keySet());
        Collections.sort(sortedCategories);

        for (String category : sortedCategories) {
            Integer count = categoryCounts.get(category);
            if (count != null) {
                entries.add(new BarEntry(index, count));
                labels.add(category);
                index++;
            }
        }

        BarDataSet dataSet = new BarDataSet(entries, "Reports by Category");
        
        // Use a more professional color palette
        int[] colors = {
            0xFF2E7D32, // Forest Green
            0xFF1976D2, // Blue
            0xFFFBC02D, // Yellow
            0xFFD32F2F, // Red
            0xFF7B1FA2, // Purple
            0xFF0097A7  // Cyan
        };
        dataSet.setColors(colors);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(0xFF757575);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);
        binding.barChartReports.setData(barData);
        
        // Styling the chart
        binding.barChartReports.getDescription().setEnabled(false);
        binding.barChartReports.getLegend().setEnabled(false);
        binding.barChartReports.setFitBars(true);
        binding.barChartReports.setDrawGridBackground(false);
        binding.barChartReports.setDrawBarShadow(false);
        
        XAxis xAxis = binding.barChartReports.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(0xFF757575);
        xAxis.setTextSize(10f);
        
        binding.barChartReports.getAxisLeft().setDrawGridLines(true);
        binding.barChartReports.getAxisLeft().setGridColor(0xFFEEEEEE);
        binding.barChartReports.getAxisLeft().setTextColor(0xFF757575);
        binding.barChartReports.getAxisRight().setEnabled(false);
        
        binding.barChartReports.animateY(1200);
        binding.barChartReports.invalidate();
    }

    private void exportToCSV(List<Report> reports) {
        StringBuilder csvString = new StringBuilder();
        csvString.append("ID,Title,Facility,Category,Status,Timestamp,User\n");
        
        for (Report report : reports) {
            csvString.append(report.getId()).append(",")
                    .append(escapeCsv(report.getTitle())).append(",")
                    .append(escapeCsv(report.getBuilding())).append(",")
                    .append(escapeCsv(report.getCategory())).append(",")
                    .append(report.getStatus()).append(",")
                    .append(report.getTimestamp()).append(",")
                    .append(report.getUserId()).append("\n");
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "CampusAlert_Export_" + System.currentTimeMillis() + ".csv");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
        if (uri != null) {
            try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
                if (stream != null) {
                    stream.write(csvString.toString().getBytes());
                    Toast.makeText(this, "CSV Exported to Downloads", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}
