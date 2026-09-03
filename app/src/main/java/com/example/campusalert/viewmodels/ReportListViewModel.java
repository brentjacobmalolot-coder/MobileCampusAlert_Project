package com.example.campusalert.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.campusalert.models.Report;
import com.example.campusalert.models.UserRole;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReportListViewModel extends ViewModel {
    private static final String DATABASE_URL = "https://mobile-application-db-321ff-default-rtdb.firebaseio.com/";
    private final DatabaseReference mDatabase;
    
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
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference().child("reports");
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
            query = mDatabase;
        } else {
            // Firebase prefix search on 'title' field
            query = mDatabase.orderByChild("title").startAt(keyword).endAt(keyword + "\uf8ff");
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Report> list = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Report report = data.getValue(Report.class);
                    if (report != null) {
                        if (report.getId() == null || report.getId().isEmpty()) {
                            report.setId(data.getKey());
                        }
                        list.add(report);
                    }
                }
                allFetchedReports = list;
                applyFilters();
                _isLoading.setValue(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                _isLoading.setValue(false);
            }
        });
    }

    public void setFilters(String category, String location) {
        this.selectedCategory = category;
        this.selectedLocation = location;
        applyFilters();
    }

    private void applyFilters() {
        List<Report> filteredList = allFetchedReports.stream()
            .filter(r -> !r.isArchived() && r.getMergedIntoId() == null) // Queue Integrity: Hide archived/merged
            .filter(r -> {
                // If student, only show their own reports
                if (userRole == UserRole.STUDENT && userEmail != null) {
                    return userEmail.equalsIgnoreCase(r.getUserId());
                }
                return true;
            })
            .filter(r -> selectedCategory == null || selectedCategory.equalsIgnoreCase(r.getCategory()))
            .filter(r -> selectedLocation == null || (r.getBuilding() != null && r.getBuilding().equalsIgnoreCase(selectedLocation)))
            .collect(Collectors.toList());
        _reports.setValue(filteredList);
    }
}
