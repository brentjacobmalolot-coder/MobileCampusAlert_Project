package com.example.campusalert.viewmodels;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.campusalert.database.AppDatabase;
import com.example.campusalert.database.ReportRepository;
import com.example.campusalert.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MainViewModel extends AndroidViewModel {
    private static final String TAG = "MainViewModel";
    private final ReportRepository repository;
    private final DatabaseReference mDatabase;
    // URL without trailing slash for better consistency with Firebase SDK internals
    private static final String DATABASE_URL = "https://mobile-application-db-321ff-default-rtdb.firebaseio.com";

    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new ReportRepository(application);
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();
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

    private String sanitizeKey(String key) {
        if (key == null) return null;
        return key.replace(".", ",")
                  .replace("#", "_")
                  .replace("$", "_")
                  .replace("[", "_")
                  .replace("]", "_");
    }

    public void syncUserRole(String email, String defaultName) {
        if (email == null) return;

        Log.d(TAG, "Syncing permissions for: " + email);
        final String sanitizedEmail = sanitizeKey(email);
        
        mDatabase.child("permission").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot foundSnap = null;
                
                if (sanitizedEmail != null && snapshot.hasChild(sanitizedEmail)) {
                    foundSnap = snapshot.child(sanitizedEmail);
                } else {
                    String prefix = email.contains("@") ? email.split("@")[0] : email;
                    String sanitizedPrefix = sanitizeKey(prefix);
                    
                    if (sanitizedPrefix != null && snapshot.hasChild(sanitizedPrefix)) {
                        foundSnap = snapshot.child(sanitizedPrefix);
                    } else {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String dbEmail = child.child("email").getValue(String.class);
                            if (email.equalsIgnoreCase(dbEmail)) {
                                foundSnap = child;
                                break;
                            }
                        }
                    }
                }

                User user = new User();
                user.setEmail(email);

                if (foundSnap != null) {
                    String roleStr = foundSnap.child("role").getValue(String.class);
                    String dbName = foundSnap.child("name").getValue(String.class);
                    user.setName((dbName != null && !dbName.isEmpty()) ? dbName : (defaultName != null ? defaultName : "User"));
                    
                    if ("admin".equalsIgnoreCase(roleStr)) {
                        user.setRole("ADMIN");
                    } else if (roleStr != null && (roleStr.toLowerCase().contains("staff") || roleStr.toLowerCase().contains("maintenance"))) {
                        user.setRole("MAINTENANCE_STAFF");
                    } else {
                        user.setRole("STUDENT");
                    }
                } else {
                    user.setName(defaultName != null ? defaultName : "New Student");
                    user.setRole("STUDENT");
                }
                
                finalizeLogin(user);
            }

            private void finalizeLogin(User user) {
                String encoded = sanitizeKey(user.getEmail());
                if (encoded != null) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("name", user.getName());
                    updates.put("role", user.getRole());
                    updates.put("email", user.getEmail());
                    
                    mDatabase.child("users").child(encoded).updateChildren(updates)
                            .addOnCompleteListener(task -> {
                                AppDatabase.databaseWriteExecutor.execute(() -> repository.insertUser(user));
                                userLiveData.postValue(user);
                            });
                } else {
                    userLiveData.postValue(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Sync error: " + error.getMessage());
                User user = new User(email, defaultName != null ? defaultName : "User", "STUDENT");
                AppDatabase.databaseWriteExecutor.execute(() -> repository.insertUser(user));
                userLiveData.postValue(user);
            }
        });
    }
}
