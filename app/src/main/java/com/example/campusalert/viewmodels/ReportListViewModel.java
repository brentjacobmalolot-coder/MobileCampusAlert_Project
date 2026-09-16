package com.example.campusalert.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.campusalert.models.Report;
import com.example.campusalert.models.UserRole;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReportListViewModel extends ViewModel {
    private final FirebaseFirestore db;
    
    private final MutableLiveData<List<Report>> _reports = new MutableLiveData<>();
    public LiveData<List<Report>> reports = _reports;
    
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private List<Report> allFetchedReports = new ArrayList<>();
    private String selectedCategory = null;
    private String selectedLocation = null;
    private String userEmail = null;
    private UserRole userRole = null;

    public ReportListViewModel() {
        db = FirebaseFirestore.getInstance();
        searchReports("");
    }

    public void setUserContext(String email, UserRole role) {
        this.userEmail = email;
        this.userRole = role;
        applyFilters();
    }

    public void searchReports(String keyword) {
        _isLoading.setValue(true);
        
        Query query;
        if (keyword == null || keyword.isEmpty()) {
            query = db.collection("reports");
        } else {
            // Firestore prefix search
            query = db.collection("reports").orderBy("title").startAt(keyword).endAt(keyword + "\uf8ff");
        }

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Report> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Report report = doc.toObject(Report.class);
                    if (report != null) {
                        if (report.getId() == null || report.getId().isEmpty()) {
                            report.setId(doc.getId());
                        }
                        list.add(report);
                    }
                }
                allFetchedReports = list;
                applyFilters();
            }
            _isLoading.setValue(false);
        });
    }

    public void setFilters(String category, String location) {
        this.selectedCategory = category;
        this.selectedLocation = location;
        applyFilters();
    }

    private void applyFilters() {
        List<Report> filteredList = allFetchedReports.stream()
            .filter(r -> !r.isArchived() && r.getMergedIntoId() == null)
            .filter(r -> {
                if (userRole == UserRole.STUDENT && userEmail != null) {
                    return userEmail.equalsIgnoreCase(r.getUserId());
                }
                return true;
            })
            .filter(r -> selectedCategory == null || selectedCategory.equalsIgnoreCase(r.getCategory()))
            .filter(r -> selectedLocation == null || (r.getBuilding() != null && r.getBuilding().equalsIgnoreCase(selectedLocation)))
            .sorted((r1, r2) -> {
                int p1 = getPriorityWeight(r1.getPriority());
                int p2 = getPriorityWeight(r2.getPriority());
                if (p1 != p2) return Integer.compare(p1, p2);
                return Long.compare(r2.getTimestamp(), r1.getTimestamp());
            })
            .collect(Collectors.toList());
        _reports.setValue(filteredList);
    }

    private int getPriorityWeight(String priority) {
        if ("High".equalsIgnoreCase(priority)) return 1;
        if ("Medium".equalsIgnoreCase(priority) || "Med".equalsIgnoreCase(priority)) return 2;
        if ("Low".equalsIgnoreCase(priority)) return 3;
        return 4;
    }
}