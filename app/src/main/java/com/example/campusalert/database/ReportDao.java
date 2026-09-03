package com.example.campusalert.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.campusalert.models.Report;

import java.util.List;

@Dao
public interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Report report);

    @Update
    void update(Report report);

    @Query("SELECT * FROM reports WHERE isArchived = 0 AND mergedIntoId IS NULL ORDER BY timestamp DESC")
    LiveData<List<Report>> getAllActiveReports();

    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    LiveData<List<Report>> getAllReports();

    @Query("SELECT * FROM reports WHERE userId = :userId AND isArchived = 0 ORDER BY timestamp DESC")
    LiveData<List<Report>> getReportsByStudent(String userId);

    @Query("SELECT * FROM reports WHERE staffId = :staffId AND isArchived = 0 ORDER BY timestamp DESC")
    LiveData<List<Report>> getReportsByStaff(String staffId);

    @Query("SELECT * FROM reports WHERE staffId = :staffId AND status = :status AND isArchived = 0 ORDER BY timestamp DESC")
    LiveData<List<Report>> getReportsByStaffAndStatus(String staffId, String status);

    @Query("SELECT * FROM reports WHERE status = :status AND isArchived = 0 ORDER BY timestamp DESC")
    LiveData<List<Report>> getReportsByStatus(String status);
    
    @Query("SELECT * FROM reports WHERE id = :reportId")
    LiveData<Report> getReportById(String reportId);

    @Query("SELECT * FROM reports WHERE id = :reportId")
    Report getReportByIdSync(String reportId);

    @Query("SELECT COUNT(*) FROM reports WHERE status != 'Completed' AND isArchived = 0")
    LiveData<Integer> getOpenReportsCount();

    @Query("SELECT COUNT(*) FROM reports")
    LiveData<Integer> getTotalReportsCount();

    @Query("SELECT category, COUNT(*) as count FROM reports WHERE isArchived = 0 GROUP BY category")
    LiveData<List<CategoryCount>> getReportsByCategory();

    public static class CategoryCount {
        public String category;
        public int count;
    }
}