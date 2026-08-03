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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusalert.databinding.ActivityReportSubmissionBinding;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SessionManager;
import com.example.campusalert.viewmodels.ReportViewModel;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportSubmissionActivity extends AppCompatActivity {
    private ActivityReportSubmissionBinding binding;
    private ReportViewModel reportViewModel;
    private SessionManager sessionManager;
    private Uri currentPhotoUri;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
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
            getSupportActionBar().setTitle("");
        }
    }

    private void setupDropdowns() {
        String[] buildings = {"St. Therese Bldg", "Sports Complex", "Library", "Engineering Hall", "Student Center", "SCC Gym"};
        binding.buildingDropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, buildings));

        String[] categories = {"Plumbing", "Electrical", "Furniture", "Facilities", "Security", "Other"};
        binding.categoryDropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories));
    }

    private void setupListeners() {
        binding.btnPickPhoto.setOnClickListener(v -> {
            String[] options = {"Take Photo", "Choose from Gallery"};
            new AlertDialog.Builder(this)
                    .setTitle("Select Image")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) checkCameraPermission();
                        else mGetContent.launch("image/*");
                    }).show();
        });

        binding.submitButton.setOnClickListener(v -> submitReport());
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
            // Use the hardcoded authority that matches manifest for reliability
            currentPhotoUri = FileProvider.getUriForFile(this, "com.example.mylog_in.fileprovider", photoFile);
            mTakePicture.launch(currentPhotoUri);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitReport() {
        String building = binding.buildingDropdown.getText().toString();
        String category = binding.categoryDropdown.getText().toString();
        String desc = binding.descriptionEdit.getText().toString().trim();

        if (building.isEmpty() || category.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String priority = "Medium";
        if (binding.priorityToggleGroup.getCheckedButtonId() == R.id.btn_low) priority = "Low";
        else if (binding.priorityToggleGroup.getCheckedButtonId() == R.id.btn_high) priority = "High";

        Report report = new Report(category + " issue", desc, building, category, priority, "Pending", System.currentTimeMillis(), sessionManager.getUserEmail());
        if (currentPhotoUri != null) report.setImageUri(currentPhotoUri.toString());

        reportViewModel.insert(report);
        Toast.makeText(this, "Report submitted successfully", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}