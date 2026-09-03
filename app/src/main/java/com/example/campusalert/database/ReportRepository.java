package com.example.campusalert.database;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.campusalert.models.Notification;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReportRepository {
    private static final String TAG = "ReportRepository";
    private final UserDao userDao;
    private final ReportDao reportDao;
    private final LiveData<List<Report>> allReports;
    private final LiveData<List<Report>> allActiveReports;
    private final DatabaseReference mDatabase;
    private final Context context;
    // URL without trailing slash for better consistency
    private static final String DATABASE_URL = "https://mobile-application-db-321ff-default-rtdb.firebaseio.com";

    public ReportRepository(Application application) {
        this.context = application.getApplicationContext();
        AppDatabase db = AppDatabase.getDatabase(application);
        userDao = db.userDao();
        reportDao = db.reportDao();
        allReports = reportDao.getAllReports();
        allActiveReports = reportDao.getAllActiveReports();
        
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();
        
        syncReportsFromFirebase();
    }

    public LiveData<List<Report>> getAllReports() {
        return allReports;
    }

    public LiveData<List<Report>> getAllActiveReports() {
        return allActiveReports;
    }

    public LiveData<List<Report>> getReportsByStudent(String userId) {
        return reportDao.getReportsByStudent(userId);
    }

    public LiveData<List<Report>> getReportsByStaff(String staffId) {
        return reportDao.getReportsByStaff(staffId);
    }

    public LiveData<List<Report>> getReportsByStaffAndStatus(String staffId, String status) {
        return reportDao.getReportsByStaffAndStatus(staffId, status);
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
                String base64Image = convertUriToBase64(localImageUri);
                if (base64Image != null) {
                    report.setImageUrl(base64Image);
                }
            }
            
            finalizeInsert(report, key, listener);
        });
    }

    private String convertUriToBase64(Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) return null;
            
            int maxWidth = 800;
            int maxHeight = 800;
            float ratio = Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight());
            int width = Math.round(ratio * bitmap.getWidth());
            int height = Math.round(ratio * bitmap.getHeight());
            
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
            byte[] byteArray = outputStream.toByteArray();
            
            return "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error converting image to Base64: " + e.getMessage());
            return null;
        }
    }

    private void finalizeInsert(Report report, String key, OnUploadCompleteListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                reportDao.insert(report);
                mDatabase.child("reports").child(key).setValue(report)
                        .addOnCompleteListener(task -> {
                            if (listener != null) listener.onComplete(task.isSuccessful());
                        });
            } catch (Exception e) {
                Log.e(TAG, "Error inserting report: " + e.getMessage());
                if (listener != null) listener.onComplete(false);
            }
        });
    }

    public void update(Report report) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                reportDao.update(report);
                if (report.getId() != null && !report.getId().isEmpty()) {
                    mDatabase.child("reports").child(report.getId()).setValue(report);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating report: " + e.getMessage());
            }
        });
    }

    public void archiveReport(String reportId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Report report = reportDao.getReportByIdSync(reportId);
            if (report != null) {
                report.setArchived(true);
                report.setStatus("Archived");
                reportDao.update(report);
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("isArchived", true);
                updates.put("status", "Archived");
                mDatabase.child("reports").child(reportId).updateChildren(updates);
            }
        });
    }

    public void flagReport(String reportId, boolean isFlagged) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Report report = reportDao.getReportByIdSync(reportId);
            if (report != null) {
                report.setFlagged(isFlagged);
                reportDao.update(report);
                mDatabase.child("reports").child(reportId).child("isFlagged").setValue(isFlagged);
            }
        });
    }

    public void mergeReports(String duplicateId, String masterId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Report report = reportDao.getReportByIdSync(duplicateId);
            if (report != null) {
                report.setMergedIntoId(masterId);
                report.setStatus("Merged/Duplicate");
                report.setArchived(true);
                reportDao.update(report);
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("mergedIntoId", masterId);
                updates.put("status", "Merged/Duplicate");
                updates.put("isArchived", true);
                mDatabase.child("reports").child(duplicateId).updateChildren(updates);
            }
        });
    }

    public void insertUser(User user) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                userDao.insert(user);
            } catch (Exception e) {
                Log.e(TAG, "Error inserting user: " + e.getMessage());
            }
        });
    }

    public void updateFcmToken(String userId, String token) {
        if (userId != null && token != null) {
            String sanitizedId = sanitizeKey(userId);
            mDatabase.child("users").child(sanitizedId).child("fcmToken").setValue(token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token updated for user: " + userId))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update FCM Token: " + e.getMessage()));
        }
    }

    public void sendNotification(String recipientEmail, String title, String message) {
        sendNotification(recipientEmail, title, message, null);
    }

    public void sendNotification(String recipientEmail, String title, String message, String reportId) {
        String id = UUID.randomUUID().toString();
        Notification notification = new Notification(id, title, message, System.currentTimeMillis(), recipientEmail, reportId);
        String encodedEmail = sanitizeKey(recipientEmail);
        if (encodedEmail != null) {
            mDatabase.child("notifications").child(encodedEmail).child(id).setValue(notification);
        }
    }

    private String sanitizeKey(String key) {
        if (key == null) return null;
        return key.replace(".", ",")
                  .replace("#", "_")
                  .replace("$", "_")
                  .replace("[", "_")
                  .replace("]", "_");
    }

    private void syncReportsFromFirebase() {
        mDatabase.child("reports").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    try {
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Report report = data.getValue(Report.class);
                            if (report != null) {
                                if (report.getId() == null || report.getId().isEmpty()) {
                                    report.setId(data.getKey());
                                }
                                reportDao.insert(report);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error during background sync: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase sync failed: " + error.getMessage());
            }
        });

        mDatabase.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    try {
                        for (DataSnapshot data : snapshot.getChildren()) {
                            User user = data.getValue(User.class);
                            if (user != null) {
                                userDao.insert(user);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error syncing users: " + e.getMessage());
                    }
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public interface OnUploadCompleteListener {
        void onComplete(boolean success);
    }
}