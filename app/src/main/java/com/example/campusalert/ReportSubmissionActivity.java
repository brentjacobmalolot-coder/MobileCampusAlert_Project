package com.example.campusalert;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusalert.databinding.ActivityReportSubmissionBinding;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SessionManager;
import com.example.campusalert.viewmodels.ReportViewModel;

public class ReportSubmissionActivity extends AppCompatActivity {
    private ActivityReportSubmissionBinding binding;
    private ReportViewModel reportViewModel;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn() || sessionManager.getUserRole() != UserRole.STUDENT) {
            finish();
            return;
        }

        binding = ActivityReportSubmissionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);

        setupToolbar();
        setupDropdowns();
        setupListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupDropdowns() {
        String[] buildings = {"St. Therese Bldg", "Sports Complex", "Library", "Engineering Hall", "Student Center"};
        ArrayAdapter<String> buildingAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, buildings);
        binding.buildingDropdown.setAdapter(buildingAdapter);

        String[] categories = {"Plumbing", "Electrical", "Furniture", "Facilities", "Security", "Other"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories);
        binding.categoryDropdown.setAdapter(categoryAdapter);
    }

    private void setupListeners() {
        binding.submitButton.setOnClickListener(v -> submitReport());
    }

    private void submitReport() {
        String building = binding.buildingDropdown.getText().toString();
        String category = binding.categoryDropdown.getText().toString();
        String description = binding.descriptionEdit.getText().toString().trim();
        
        // Get Priority from Toggle Group
        String priority = "Medium"; // Default
        int checkedId = binding.priorityToggleGroup.getCheckedButtonId();
        if (checkedId == R.id.btn_low) priority = "Low";
        else if (checkedId == R.id.btn_high) priority = "High";

        if (building.isEmpty() || category.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        Report report = new Report(
                category + " issue at " + building,
                description,
                building,
                category,
                priority,
                "Pending",
                System.currentTimeMillis(),
                sessionManager.getUserEmail()
        );

        reportViewModel.insert(report);
        Toast.makeText(this, R.string.report_submitted, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}