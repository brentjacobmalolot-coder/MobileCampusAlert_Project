package com.example.campusalert.database;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.campusalert.models.Notification;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.User;
import com.example.campusalert.models.UserRole;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReportRepository {
    private final UserDao userDao;
    private final ReportDao reportDao;
    private final LiveData<List<Report>> allReports;
    private final DatabaseReference mDatabase;
    private final FirebaseStorage mStorage;
    private static final String DATABASE_URL = "https://mobile-application-db-321ff-default-rtdb.firebaseio.com/";

    public ReportRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        userDao = db.userDao();
        reportDao = db.reportDao();
        allReports = reportDao.getAllReports();
        
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();
        mStorage = FirebaseStorage.getInstance();
        
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

    public LiveData<Report> getReportById(String id) {
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

    public LiveData<List<User>> getStaffMembers() {
        return userDao.getUsersByRole("MAINTENANCE_STAFF");
    }

    public void insert(Report report, Uri localImageUri, OnUploadCompleteListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            String key = mDatabase.child("reports").push().getKey();
            if (key == null) {
                if (listener != null) listener.onComplete(false);
                return;
            }
            
            report.setId(key);

            if (localImageUri != null) {
                StorageReference storageRef = mStorage.getReference().child("reports/" + key + ".jpg");
                storageRef.putFile(localImageUri)
                        .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    report.setImageUri(uri.toString());
                                    finalizeInsert(report, key, listener);
                                }))
                        .addOnFailureListener(e -> {
                            Log.e("ReportRepository", "Image upload failed: " + e.getMessage());
                            finalizeInsert(report, key, listener);
                        });
            } else {
                finalizeInsert(report, key, listener);
            }
        });
    }

    private void finalizeInsert(Report report, String key, OnUploadCompleteListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            reportDao.insert(report);
            mDatabase.child("reports").child(key).setValue(report)
                    .addOnCompleteListener(task -> {
                        if (listener != null) listener.onComplete(task.isSuccessful());
                    });
        });
    }

    public void update(Report report) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            reportDao.update(report);
            if (report.getId() != null && !report.getId().isEmpty()) {
                mDatabase.child("reports").child(report.getId()).setValue(report);
            }
        });
    }

    public void sendNotification(String recipientEmail, String title, String message) {
        String id = UUID.randomUUID().toString();
        Notification notification = new Notification(id, title, message, System.currentTimeMillis(), recipientEmail);
        String encodedEmail = recipientEmail.replace(".", ",");
        mDatabase.child("notifications").child(encodedEmail).child(id).setValue(notification);
    }

    private void syncReportsFromFirebase() {
        mDatabase.child("reports").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        Report report = data.getValue(Report.class);
                        if (report != null) {
                            if (report.getId() == null || report.getId().isEmpty()) {
                                report.setId(data.getKey());
                            }
                            reportDao.insert(report);
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ReportRepository", "Firebase sync failed: " + error.getMessage());
            }
        });
        
        mDatabase.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        User user = data.getValue(User.class);
                        if (user != null) {
                            userDao.insert(user);
                        }
                    }
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void insertUser(User user) {
        AppDatabase.databaseWriteExecutor.execute(() -> userDao.insert(user));
    }

    public User getUserByEmail(String email) {
        return userDao.login(email);
    }

    public int getUserCount() {
        return userDao.getUserCount();
    }

    public interface OnUploadCompleteListener {
        void onComplete(boolean success);
    }
}
