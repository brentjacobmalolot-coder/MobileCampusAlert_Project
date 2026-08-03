package com.example.campusalert;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusalert.databinding.UserSettingsBinding;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SessionManager;

public class SettingsActivity extends AppCompatActivity {
    private UserSettingsBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize sessionManager inside onCreate to ensure context is valid
        sessionManager = new SessionManager(this);
        
        binding = UserSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        displayUserInfo();
        setupListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Title is also handled in XML, but we can set it here for clarity
            getSupportActionBar().setTitle(""); 
        }
    }

    private void displayUserInfo() {
        if (sessionManager != null) {
            binding.textUserName.setText(sessionManager.getUserName());
            binding.textUserEmail.setText(sessionManager.getUserEmail());
            
            UserRole role = sessionManager.getUserRole();
            if (role != null) {
                binding.chipUserRole.setText(role.name());
            } else {
                binding.chipUserRole.setText("User");
            }
        }
    }

    private void setupListeners() {
        binding.btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Using finish() instead of onBackPressed() for better navigation behavior here
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}