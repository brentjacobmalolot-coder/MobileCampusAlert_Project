package com.example.campusalert.database;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class ReportRepository {
    private final UserDao userDao;
    private final ReportDao reportDao;
    private final LiveData<List<Report>> allReports;
    private final DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://mobile-application-db-321ff-default-rtdb.firebaseio.com/";

    public ReportRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        userDao = db.userDao();
        reportDao = db.reportDao();
        allReports = reportDao.getAllReports();
        
        // Initialize Firebase Reference
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();
        
        // Start syncing reports from Firebase to Local Room DB
        syncReportsFromFirebase();
    }

    public LiveData<List<Report>> getAllReports() {
        return allReports;
    }

    public LiveData<List<Report>> getReportsByStudent(String userId) {
        return reportDao.getReportsByStudent(userId);
    }

    public LiveData<List<Report>> getReportsByStatus(String status) {
        return reportDao.getReportsByStatus(status);
    }

    public LiveData<Report> getReportById(int id) {
        return reportDao.getReportById(id);
    }

    public LiveData<Integer> getOpenReportsCount() {
        return reportDao.getOpenReportsCount();
    }

    public LiveData<Integer> getTotalReportsCount() {
        return reportDao.getTotalReportsCount();
    }

    public LiveData<List<ReportDao.CategoryCount>> getReportsByCategory() {
        return reportDao.getReportsByCategory();
    }

    public void insert(Report report) {
        // 1. Insert into Room for offline availability
        AppDatabase.databaseWriteExecutor.execute(() -> {
            reportDao.insert(report);
            // 2. Sync to Firebase - Note: Room will generate ID, but for Firebase we might want to push and get a key
            String key = mDatabase.child("reports").push().getKey();
            if (key != null) {
                mDatabase.child("reports").child(key).setValue(report);
            }
        });
    }

    public void update(Report report) {
        // 1. Update local Room
        AppDatabase.databaseWriteExecutor.execute(() -> {
            reportDao.update(report);
            // 2. Update in Firebase
            // We need a way to find this report in Firebase. 
            // In a production app, we would store the Firebase push key in the Report model.
            // For now, we query by report ID or title/timestamp.
            mDatabase.child("reports").orderByChild("timestamp").equalTo(report.getTimestamp())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            child.getRef().setValue(report);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
        });
    }

    private void syncReportsFromFirebase() {
        mDatabase.child("reports").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        Report report = data.getValue(Report.class);
                        if (report != null) {
                            reportDao.insert(report); // insert onConflict REPLACE
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ReportRepository", "Firebase sync failed: " + error.getMessage());
            }
        });
    }

    public void insertUser(User user) {
        AppDatabase.databaseWriteExecutor.execute(() -> userDao.insert(user));
    }

    public User getUserByEmail(String email) {
        return userDao.login(email);
    }

    public User login(String email, String password) {
        User user = userDao.login(email);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    public int getUserCount() {
        return userDao.getUserCount();
    }
}
