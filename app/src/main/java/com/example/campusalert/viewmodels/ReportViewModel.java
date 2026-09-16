package com.example.campusalert.viewmodels;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.campusalert.database.ReportRepository;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.User;
import com.example.campusalert.database.ReportDao;
import com.example.campusalert.utils.PriorityClassifier;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.List;

public class ReportViewModel extends AndroidViewModel {
    private final ReportRepository repository;
    private final PriorityClassifier priorityClassifier;
    private static final String TAG = "ReportViewModel";

    public ReportViewModel(@NonNull Application application) {
        super(application);
        repository = new ReportRepository(application);
        priorityClassifier = new PriorityClassifier(application);
    }

    public LiveData<List<Report>> getAllReports() {
        return repository.getAllReports();
    }

    public LiveData<List<Report>> getReportsByStudent(String userId) {
        return repository.getReportsByStudent(userId);
    }

    public LiveData<List<Report>> getReportsByStaff(String staffId) {
        return repository.getReportsByStaff(staffId);
    }

    public LiveData<List<Report>> getReportsByStaffAndStatus(String staffId, String status) {
        return repository.getReportsByStaffAndStatus(staffId, status);
    }

    public LiveData<List<Report>> getReportsByStaffActive(String staffId) {
        return repository.getReportsByStaffActive(staffId);
    }

    public LiveData<List<Report>> getReportsByStatus(String status) {
        return repository.getReportsByStatus(status);
    }

    public LiveData<Report> getReportById(String id) {
        return repository.getReportById(id);
    }

    public LiveData<Integer> getOpenReportsCount() {
        return repository.getOpenReportsCount();
    }

    public LiveData<Integer> getTotalReportsCount() {
        return repository.getTotalReportsCount();
    }

    public LiveData<List<ReportDao.CategoryCount>> getReportsByCategory() {
        return repository.getReportsByCategory();
    }

    public LiveData<List<User>> getStaffMembers() {
        return repository.getStaffMembers();
    }

    public void insert(Report report, Uri imageUri, ReportRepository.OnUploadCompleteListener listener) {
        repository.insert(report, imageUri, listener);
    }

    public void update(Report report) {
        repository.update(report);
    }

    public void archiveReport(String reportId) {
        repository.archiveReport(reportId);
    }

    public void flagReport(String reportId, boolean isFlagged) {
        repository.flagReport(reportId, isFlagged);
    }

    public void mergeReports(String duplicateId, String masterId) {
        repository.mergeReports(duplicateId, masterId);
    }

    public void incrementViewCount(String reportId) {
        repository.incrementViewCount(reportId);
    }

    public void sendNotification(String email, String title, String message) {
        repository.sendNotification(email, title, message);
    }

    public void sendNotification(String email, String title, String message, String reportId) {
        repository.sendNotification(email, title, message, reportId);
    }

    public void notifyAdmins(String title, String message, String reportId) {
        repository.notifyAdmins(title, message, reportId);
    }

    public String classifyPriority(String description) {
        return priorityClassifier.classify(description);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (priorityClassifier != null) {
            priorityClassifier.close();
        }
    }

    /**
     * Retrieves the FCM token and saves it to the database for the current user.
     * @param userId The ID of the currently logged-in user.
     */
    public void updateFcmToken(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    repository.updateFcmToken(userId, token);
                });
    }
}
