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
import com.example.campusalert.utils.SecurityUtils;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class MainViewModel extends AndroidViewModel {
    private static final String TAG = "MainViewModel";
    private final ReportRepository repository;
    private final DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://mobile-application-db-321ff-default-rtdb.firebaseio.com/";

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

    public void login(String email, String password) {
        Log.d(TAG, "Login attempt for: " + email);

        Query query = mDatabase.child("users").orderByChild("email").equalTo(email);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e(TAG, "No user found with email: " + email);
                    errorLiveData.postValue("Invalid email or password.");
                    return;
                }

                boolean foundValidUser = false;
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String dbEmail = userSnapshot.child("email").getValue(String.class);
                    String dbPassword = userSnapshot.child("password").getValue(String.class);
                    String dbName = userSnapshot.child("name").getValue(String.class);
                    String dbRole = userSnapshot.child("role").getValue(String.class);

                    if (dbPassword != null) {
                        String hashedInput = SecurityUtils.hashPassword(password);
                        
                        if (dbPassword.equals(password) || dbPassword.equals(hashedInput)) {
                            User user = new User();
                            user.setEmail(dbEmail != null ? dbEmail : email);
                            user.setPassword(dbPassword);
                            user.setName(dbName != null ? dbName : "Campus User");
                            
                            // Store role as string, normalized to uppercase
                            if (dbRole != null) {
                                String normalizedRole = dbRole.toUpperCase();
                                if (normalizedRole.equals("STAFF")) normalizedRole = "MAINTENANCE_STAFF";
                                user.setRole(normalizedRole);
                            } else {
                                user.setRole("STUDENT");
                            }

                            Log.d(TAG, "Login success: " + user.getName() + " [" + user.getRole() + "]");
                            
                            AppDatabase.databaseWriteExecutor.execute(() -> repository.insertUser(user));
                            userLiveData.postValue(user);
                            foundValidUser = true;
                            break;
                        }
                    }
                }

                if (!foundValidUser) {
                    Log.e(TAG, "Password mismatch for: " + email);
                    errorLiveData.postValue("Invalid email or password.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                errorLiveData.postValue("Network error. Please try again.");
            }
        });
    }

    public void loginWithGoogle(GoogleSignInAccount account) {
        if (account == null) {
            errorLiveData.postValue("Google sign-in failed.");
            return;
        }

        String email = account.getEmail();
        String name = account.getDisplayName();

        Log.d(TAG, "Google Login attempt for: " + email);

        Query query = mDatabase.child("users").orderByChild("email").equalTo(email);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = new User();
                user.setEmail(email);
                user.setName(name != null ? name : "Campus User");
                
                if (snapshot.exists()) {
                    // User exists, retrieve their role
                    DataSnapshot userSnapshot = snapshot.getChildren().iterator().next();
                    String dbRole = userSnapshot.child("role").getValue(String.class);
                    
                    if (dbRole != null) {
                        String normalizedRole = dbRole.toUpperCase();
                        if (normalizedRole.equals("STAFF")) normalizedRole = "MAINTENANCE_STAFF";
                        user.setRole(normalizedRole);
                    } else {
                        user.setRole("STUDENT");
                    }
                } else {
                    // New user via Google, default to STUDENT and save to DB
                    user.setRole("STUDENT");
                    String userId = email.replace(".", "_"); // Basic key generation
                    mDatabase.child("users").child(userId).setValue(user);
                }

                Log.d(TAG, "Google Login success: " + user.getName() + " [" + user.getRole() + "]");
                AppDatabase.databaseWriteExecutor.execute(() -> repository.insertUser(user));
                userLiveData.postValue(user);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                errorLiveData.postValue("Network error while syncing Google account.");
            }
        });
    }
}
