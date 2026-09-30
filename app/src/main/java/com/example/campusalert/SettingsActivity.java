package com.example.campusalert;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusalert.databinding.UserSettingsBinding;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SessionManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends BaseActivity {
    private UserSettingsBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        binding.btnLogout.setOnClickListener(v -> performLogout());
    }

    private void performLogout() {
        // 1. CLEAR EVERYTHING IMMEDIATELY
        // Clear Firebase Auth session
        FirebaseAuth.getInstance().signOut();
        // Clear local storage session (synchronized)
        sessionManager.logout();

        // 2. Prepare Google Sign-out Client
        // It's important to use the same configuration used for login
        String webClientId = getString(R.string.default_web_client_id);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        
        // 3. Clear Google Account and THEN navigate
        googleSignInClient.signOut().addOnCompleteListener(this, task -> navigateToLogin());
        
        // Safety Fallback: Force navigation if Google API is slow (prevents hanging)
        binding.btnLogout.postDelayed(this::navigateToLogin, 800);
    }

    private void navigateToLogin() {
        if (isFinishing()) return;
        
        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        // CRITICAL: Clear all activities (Dashboards) from the background
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // Explicitly signal to MainActivity that this is a fresh start after logout
        intent.putExtra("IS_LOGGING_OUT", true);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}