package com.example.campusalert;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusalert.databinding.ActivityMainBinding;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SessionManager;
import com.example.campusalert.viewmodels.MainViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private MainViewModel viewModel;
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
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            UserRole role = sessionManager.getUserRole();
            if (role != null) {
                navigateToDashboard(role.name());
                return;
            } else {
                sessionManager.logout();
            }
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
        
        configureGoogleSignIn();
        setupObservers();
        
        binding.signInButton.setOnClickListener(v -> attemptLogin());
        binding.googleSignInButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            binding.progressBar.setVisibility(View.VISIBLE);
            viewModel.loginWithGoogle(account);
        } catch (ApiException e) {
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupObservers() {
        viewModel.getUserLiveData().observe(this, user -> {
            binding.progressBar.setVisibility(View.GONE);
            if (user != null) {
                UserRole role = user.getRoleEnum();
                sessionManager.createSession(user);
                navigateToDashboard(role.name());
            }
        });

        viewModel.getErrorLiveData().observe(this, errorMessage -> {
            binding.progressBar.setVisibility(View.GONE);
            if (errorMessage != null) {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
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
        if (role == null) return;
        
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
