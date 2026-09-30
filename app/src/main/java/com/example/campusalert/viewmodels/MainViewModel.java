package com.example.campusalert.viewmodels;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.campusalert.R;
import com.example.campusalert.database.AppDatabase;
import com.example.campusalert.database.ReportRepository;
import com.example.campusalert.models.User;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SecurityUtils;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainViewModel extends AndroidViewModel {
    private static final String TAG = "MainViewModel";
    private final ReportRepository repository;
    private final FirebaseFirestore db;

    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new ReportRepository(application);
        db = FirebaseFirestore.getInstance();
    }

    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public void loginWithGoogle(GoogleSignInAccount account) {
        if (account == null) {
            errorLiveData.postValue("Google sign-in failed.");
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        syncUserRole(account.getEmail(), account.getDisplayName());
                    } else {
                        errorLiveData.postValue("Google Auth failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    public void syncUserRole(String email, String defaultName) {
        if (email == null) return;

        Log.d(TAG, "Syncing permissions for: " + email);
        
        db.collection("users").document(email).get()
                .addOnCompleteListener(task -> {
                    User user = new User();
                    user.setEmail(email);

                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        DocumentSnapshot doc = task.getResult();
                        String roleStr = doc.getString("role");
                        String dbName = doc.getString("name");
                        user.setName((dbName != null && !dbName.isEmpty()) ? dbName : (defaultName != null ? defaultName : "User"));
                        
                        if ("admin".equalsIgnoreCase(roleStr)) {
                            user.setRole("ADMIN");
                        } else if (roleStr != null && (roleStr.toLowerCase().contains("staff") || roleStr.toLowerCase().contains("maintenance"))) {
                            user.setRole("MAINTENANCE_STAFF");
                        } else {
                            user.setRole("STUDENT");
                        }
                    } else {
                        // User not found in Firestore, default to Student
                        user.setName(defaultName != null ? defaultName : "New Student");
                        user.setRole("STUDENT");
                    }
                    
                    // Verify that student accounts are using the institutional email ending with sccpag.edu.ph
                    if (user.getRoleEnum() == UserRole.STUDENT) {
                        if (!SecurityUtils.isInstitutionalStudentEmail(email)) {
                            FirebaseAuth.getInstance().signOut();
                            errorLiveData.postValue(getApplication().getString(R.string.institutional_email_required));
                            return;
                        }
                    }

                    finalizeLogin(user);
                });
    }

    private void finalizeLogin(User user) {
        if (user.getEmail() != null) {
            db.collection("users").document(user.getEmail()).set(user)
                    .addOnCompleteListener(task -> {
                        AppDatabase.databaseWriteExecutor.execute(() -> repository.insertUser(user));
                        userLiveData.postValue(user);
                    });
        } else {
            userLiveData.postValue(user);
        }
    }
}