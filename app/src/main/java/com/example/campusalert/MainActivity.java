package com.example.campusalert;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusalert.databinding.ActivityMainBinding;
import com.example.campusalert.utils.SessionManager;
import com.example.campusalert.viewmodels.MainViewModel;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            navigateToDashboard(sessionManager.getUserRole().name());
            return;
        }

        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        
        setupObservers();
        
        binding.signInButton.setOnClickListener(v -> attemptLogin());
    }

    private void setupObservers() {
        viewModel.getUserLiveData().observe(this, user -> {
            binding.progressBar.setVisibility(View.GONE);
            if (user != null) {
                sessionManager.createSession(user);
                navigateToDashboard(user.getRole().name());
            }
        });

        viewModel.getErrorLiveData().observe(this, errorMessage -> {
            binding.progressBar.setVisibility(View.GONE);
            Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
        });
    }

    private void attemptLogin() {
        String email = binding.emailEdit.getText().toString().trim();
        String password = binding.passwordEdit.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.login(email, password);
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        switch (role) {
            case "ADMIN":
                intent = new Intent(this, AdminAnalyticsActivity.class);
                break;
            case "MAINTENANCE_STAFF":
                intent = new Intent(this, MaintenanceDashboardActivity.class);
                break;
            default:
                intent = new Intent(this, StudentDashboardActivity.class);
                break;
        }
        startActivity(intent);
        finish();
    }
}