package com.example.campusalert.database;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.User;
import java.util.List;

public class ReportRepository {
    private final UserDao userDao;
    private final ReportDao reportDao;
    private final LiveData<List<Report>> allReports;

    public ReportRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        userDao = db.userDao();
        reportDao = db.reportDao();
        allReports = reportDao.getAllReports();
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
        AppDatabase.databaseWriteExecutor.execute(() -> reportDao.insert(report));
    }

    public void update(Report report) {
        AppDatabase.databaseWriteExecutor.execute(() -> reportDao.update(report));
    }

    public void insertUser(User user) {
        AppDatabase.databaseWriteExecutor.execute(() -> userDao.insert(user));
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
