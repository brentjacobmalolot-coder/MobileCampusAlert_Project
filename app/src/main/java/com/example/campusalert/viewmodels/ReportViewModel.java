package com.example.campusalert.viewmodels;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.campusalert.database.ReportRepository;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.User;
import com.example.campusalert.database.ReportDao;
import java.util.List;

public class ReportViewModel extends AndroidViewModel {
    private final ReportRepository repository;

    public ReportViewModel(@NonNull Application application) {
        super(application);
        repository = new ReportRepository(application);
    }

    public LiveData<List<Report>> getAllReports() {
        return repository.getAllReports();
    }

    public LiveData<List<Report>> getReportsByStudent(String userId) {
        return repository.getReportsByStudent(userId);
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

    public void sendNotification(String email, String title, String message) {
        repository.sendNotification(email, title, message);
    }
}
