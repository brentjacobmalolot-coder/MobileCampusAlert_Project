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
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SecurityUtils;
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
        // Connect to the specific Realtime Database instance provided in the screenshot
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();
    }

    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    /**
     * Performs a login by querying the 'users' node for a matching 'email' field.
     * Maps the Firebase data (lowercase roles, plain-text passwords) to the app model.
     */
    public void login(String email, String password) {
        Log.d(TAG, "Login attempt for: " + email);

        // The database uses numerical IDs as keys, so we query by the 'email' property
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
                        
                        // Check if password matches (Plain text from screenshot or SHA-256 hash)
                        if (dbPassword.equals(password) || dbPassword.equals(hashedInput)) {
                            User user = new User();
                            user.setEmail(dbEmail != null ? dbEmail : email);
                            user.setPassword(dbPassword);
                            user.setName(dbName != null ? dbName : "Campus User");

                            // Map roles: 'student' -> STUDENT, 'admin' -> ADMIN, 'staff' -> MAINTENANCE_STAFF
                            if (dbRole != null) {
                                switch (dbRole.toLowerCase()) {
                                    case "admin":
                                        user.setRole(UserRole.ADMIN);
                                        break;
                                    case "staff":
                                        user.setRole(UserRole.MAINTENANCE_STAFF);
                                        break;
                                    case "student":
                                    default:
                                        user.setRole(UserRole.STUDENT);
                                        break;
                                }
                            } else {
                                user.setRole(UserRole.STUDENT);
                            }

                            Log.d(TAG, "Login success: " + user.getName() + " [" + user.getRole() + "]");
                            
                            // Synchronize session with local database
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
}
