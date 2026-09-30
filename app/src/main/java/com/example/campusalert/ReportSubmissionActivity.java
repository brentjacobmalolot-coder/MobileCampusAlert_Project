package com.example.campusalert;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusalert.databinding.ActivityReportSubmissionBinding;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SecurityUtils;
import com.example.campusalert.utils.SessionManager;
import com.example.campusalert.viewmodels.ReportViewModel;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Optimized Activity for report submission.
 * Supports camera capture, gallery picking, and Firebase integration with Admin notifications.
 */
public class ReportSubmissionActivity extends BaseActivity {
    private ActivityReportSubmissionBinding binding;
    private ReportViewModel reportViewModel;
    private SessionManager sessionManager;
    private Uri currentPhotoUri;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    currentPhotoUri = uri;
                    binding.imgPreview.setImageURI(uri);
                    binding.cardPhotoPreview.setVisibility(View.VISIBLE);
                }
            }
    );

    private final ActivityResultLauncher<Uri> mTakePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success) {
                    binding.imgPreview.setImageURI(currentPhotoUri);
                    binding.cardPhotoPreview.setVisibility(View.VISIBLE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
        String userEmail = sessionManager.getUserEmail();
        if (!sessionManager.isLoggedIn() || sessionManager.getUserRole() != UserRole.STUDENT || !SecurityUtils.isInstitutionalStudentEmail(userEmail)) {
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
            getSupportActionBar().setTitle("");
        }
    }

    private void setupDropdowns() {
        String[] buildings = {"St. Therese Bldg", "Library", "Law Library", "SCC Gym", "Canteen"};
        binding.buildingDropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, buildings));

        String[] categories = {"Plumbing", "Electrical", "Furniture", "Facilities", "Security", "Other"};
        binding.categoryDropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories));
    }

    private void setupListeners() {
        binding.btnPickPhoto.setOnClickListener(v -> showPhotoOptions());
        
        binding.btnRemovePhoto.setOnClickListener(v -> {
            currentPhotoUri = null;
            binding.imgPreview.setImageDrawable(null);
            binding.cardPhotoPreview.setVisibility(View.GONE);
        });

        binding.submitButton.setOnClickListener(v -> submitReport());
    }

    private void showPhotoOptions() {
        String[] options = {getString(R.string.take_photo), getString(R.string.choose_gallery)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.evidence_photo_source)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            checkCameraPermission();
                            break;
                        case 1:
                            mGetContent.launch("image/*");
                            break;
                    }
                }).show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            File photoFile = File.createTempFile(
                    "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + "_",
                    ".jpg",
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            );
            // Authority must match AndroidManifest.xml exactly to avoid INSTALL_FAILED_CONFLICTING_PROVIDER
            currentPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".unique.campus.fileprovider", photoFile);
            mTakePicture.launch(currentPhotoUri);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitReport() {
        String building = binding.buildingDropdown.getText().toString();
        String category = binding.categoryDropdown.getText().toString();
        
        String descRaw = binding.descriptionEdit.getText() != null ? binding.descriptionEdit.getText().toString() : "";
        String desc = descRaw.trim();

        if (building.isEmpty() || category.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        String priority = reportViewModel.classifyPriority(desc);

        // UI Loading State
        binding.submitButton.setEnabled(false);
        binding.submitButton.setText("");
        binding.uploadProgress.setVisibility(View.VISIBLE);

        Report report = new Report(category + " issue", desc, building, category, priority, "Pending", System.currentTimeMillis(), sessionManager.getUserEmail());

        reportViewModel.insert(report, currentPhotoUri, success -> runOnUiThread(() -> {
            binding.uploadProgress.setVisibility(View.GONE);
            binding.submitButton.setEnabled(true);
            binding.submitButton.setText(R.string.submit_official_report);

            if (success) {
                // Notify Student
                reportViewModel.sendNotification(sessionManager.getUserEmail(),
                        "Report Received",
                        "Your report for " + building + " has been submitted and is awaiting review.",
                        report.getId());
                
                // Notify All Admins
                reportViewModel.notifyAdmins(
                        "New Report Submitted", 
                        "A new " + category + " issue was reported at " + building,
                        report.getId());
                        
                Toast.makeText(this, R.string.report_submitted, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Submission failed. Please check your connection.", Toast.LENGTH_LONG).show();
            }
        }));
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
