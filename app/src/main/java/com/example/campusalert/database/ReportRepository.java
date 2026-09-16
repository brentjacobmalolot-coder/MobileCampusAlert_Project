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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
    private final FirebaseFirestore db;
    private final Context context;

    public ReportRepository(Application application) {
        this.context = application.getApplicationContext();
        AppDatabase database = AppDatabase.getDatabase(application);
        userDao = database.userDao();
        reportDao = database.reportDao();
        allReports = reportDao.getAllReports();
        allActiveReports = reportDao.getAllActiveReports();
        
        db = FirebaseFirestore.getInstance();
        
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

    public LiveData<List<Report>> getReportsByStaffActive(String staffId) {
        return reportDao.getReportsByStaffActive(staffId);
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
            String key = db.collection("reports").document().getId();
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
                db.collection("reports").document(key).set(report)
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
                    db.collection("reports").document(report.getId()).set(report);
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
                updates.put("archived", true);
                updates.put("status", "Archived");
                db.collection("reports").document(reportId).update(updates);
            }
        });
    }

    public void flagReport(String reportId, boolean isFlagged) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Report report = reportDao.getReportByIdSync(reportId);
            if (report != null) {
                report.setFlagged(isFlagged);
                reportDao.update(report);
                db.collection("reports").document(reportId).update("flagged", isFlagged);
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
                updates.put("archived", true);
                db.collection("reports").document(duplicateId).update(updates);
            }
        });
    }

    public void incrementViewCount(String reportId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                reportDao.incrementViewCount(reportId);
                if (reportId != null && !reportId.isEmpty()) {
                    db.collection("reports").document(reportId)
                            .update("viewCount", FieldValue.increment(1));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error incrementing view count: " + e.getMessage());
            }
        });
    }

    public void insertUser(User user) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                userDao.insert(user);
                // Also update Firestore to keep it in sync
                if (user.getEmail() != null) {
                    db.collection("users").document(user.getEmail()).set(user);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error inserting user: " + e.getMessage());
            }
        });
    }

    public void updateFcmToken(String userId, String token) {
        if (userId != null && token != null) {
            db.collection("users").document(userId).update("fcmToken", token)
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
        if (recipientEmail != null) {
            db.collection("notifications").document(id).set(notification);
        }
    }

    public void notifyAdmins(String title, String message, String reportId) {
        db.collection("users").whereEqualTo("role", "ADMIN").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User admin = doc.toObject(User.class);
                        if (admin.getEmail() != null) {
                            sendNotification(admin.getEmail(), title, message, reportId);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error notifying admins: " + e.getMessage()));
    }

    private void syncReportsFromFirebase() {
        db.collection("reports").addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Firestore reports sync failed: " + error.getMessage());
                return;
            }
            if (value != null) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    try {
                        for (QueryDocumentSnapshot doc : value) {
                            Report report = doc.toObject(Report.class);
                            if (report != null) {
                                if (report.getId() == null || report.getId().isEmpty()) {
                                    report.setId(doc.getId());
                                }
                                reportDao.insert(report);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error during reports sync: " + e.getMessage());
                    }
                });
            }
        });

        db.collection("users").addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Firestore users sync failed: " + error.getMessage());
                return;
            }
            if (value != null) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    try {
                        for (QueryDocumentSnapshot doc : value) {
                            User user = doc.toObject(User.class);
                            if (user != null) {
                                userDao.insert(user);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error during users sync: " + e.getMessage());
                    }
                });
            }
        });
    }

    public interface OnUploadCompleteListener {
        void onComplete(boolean success);
    }
}