package com.example.campusalert.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.campusalert.database.ReportRepository;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.ReportDao;
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

    public LiveData<Report> getReportById(int id) {
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

    public void insert(Report report) {
        repository.insert(report);
    }

    public void update(Report report) {
        repository.update(report);
    }
}