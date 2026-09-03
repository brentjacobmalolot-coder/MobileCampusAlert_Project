package com.example.campusalert;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusalert.databinding.ActivityMainBinding;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SessionManager;
import com.example.campusalert.viewmodels.MainViewModel;
import com.example.campusalert.viewmodels.ReportViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private ReportViewModel reportViewModel;
    private SessionManager sessionManager;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    handleSignInResult(task);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Hardware acceleration is disabled for this activity in Manifest
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        
        try {
            sessionManager = new SessionManager(this);

            // Check if we need to redirect immediately
            if (sessionManager.isLoggedIn() && !getIntent().getBooleanExtra("IS_LOGGING_OUT", false)) {
                UserRole role = sessionManager.getUserRole();
                if (role != null) {
                    navigateToDashboard(role.name());
                    return;
                }
            }

            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            
            viewModel = new ViewModelProvider(this).get(MainViewModel.class);
            reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);
            
            configureGoogleSignIn();
            setupObservers();
            
            binding.googleSignInButton.setOnClickListener(v -> signInWithGoogle());
            binding.signInButton.setOnClickListener(v -> handleManualSignIn());

        } catch (Exception e) {
            Log.e(TAG, "Critical startup failure", e);
            Toast.makeText(this, "Startup error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // Don't finish(), allow user to see error or try manual reload if possible
        }
    }

    private void handleManualSignIn() {
        if (binding.emailEditText.getText() == null || binding.passwordEditText.getText() == null) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();
        
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        
        binding.progressBar.setVisibility(View.VISIBLE);
        
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null && user.getEmail() != null) {
                        viewModel.syncUserRole(user.getEmail(), user.getDisplayName());
                    }
                } else {
                    binding.progressBar.setVisibility(View.GONE);
                    handleAuthError(task.getException());
                }
            });
    }

    private void handleAuthError(Exception e) {
        String message;
        if (e instanceof FirebaseAuthInvalidUserException) message = getString(R.string.account_not_found);
        else if (e instanceof FirebaseAuthInvalidCredentialsException) message = getString(R.string.invalid_password);
        else message = (e != null) ? e.getMessage() : "Authentication failed.";
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) 
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signInWithGoogle() {
        googleSignInLauncher.launch(mGoogleSignInClient.getSignInIntent());
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                binding.progressBar.setVisibility(View.VISIBLE);
                viewModel.loginWithGoogle(account);
            }
        } catch (ApiException e) {
            Log.e(TAG, "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(this, "Google login failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupObservers() {
        viewModel.getUserLiveData().observe(this, user -> {
            if (user != null) {
                binding.progressBar.setVisibility(View.GONE);
                sessionManager.createSession(user);
                String encodedEmail = user.getEmail().replace(".", ",");
                reportViewModel.updateFcmToken(encodedEmail);
                navigateToDashboard(user.getRoleEnum().name());
            }
        });

        viewModel.getErrorLiveData().observe(this, errorMessage -> {
            if (errorMessage != null) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToDashboard(String role) {
        Log.d(TAG, "Navigating to dashboard: " + role);
        Intent intent;
        if ("ADMIN".equals(role)) intent = new Intent(this, AdminAnalyticsActivity.class);
        else if ("MAINTENANCE_STAFF".equals(role)) intent = new Intent(this, MaintenanceDashboardActivity.class);
        else intent = new Intent(this, StudentDashboardActivity.class);
        
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
