package com.example.campusalert;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campusalert.adapters.ReportAdapter;
import com.example.campusalert.databinding.ActivityReportListBinding;
import com.example.campusalert.databinding.LayoutFilterBottomSheetBinding;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SessionManager;
import com.example.campusalert.viewmodels.ReportListViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;

public class ReportListActivity extends AppCompatActivity {

    private ActivityReportListBinding binding;
    private ReportListViewModel viewModel;
    private ReportAdapter adapter;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(ReportListViewModel.class);

        // Initialize ViewModel with user context if needed
        if (sessionManager.getUserRole() == UserRole.STUDENT) {
            viewModel.setUserContext(sessionManager.getUserEmail(), UserRole.STUDENT);
        }

        setupToolbar();
        setupRecyclerView();
        setupObservers();

        binding.filterFab.setOnClickListener(v -> showFilterBottomSheet());
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle("Campus Reports");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new ReportAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(report -> {
            Intent intent = new Intent(ReportListActivity.this, ReportDetailActivity.class);
            intent.putExtra("REPORT_ID", report.getId());
            startActivity(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.report_list_menu, menu);
        
        MenuItem searchItem = menu.findItem(R.id.searchView);
        if (searchItem == null) {
            // If not in menu, check if it's in layout
            SearchView searchView = findViewById(R.id.searchView);
            if (searchView != null) {
                setupSearchView(searchView);
            }
        } else {
            SearchView searchView = (SearchView) searchItem.getActionView();
            setupSearchView(searchView);
        }
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_filter) {
            showFilterBottomSheet();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupSearchView(SearchView searchView) {
        searchView.setQueryHint("Search by title...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.searchReports(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    viewModel.searchReports("");
                }
                return true;
            }
        });
    }

    private void setupObservers() {
        viewModel.reports.observe(this, reports -> {
            adapter.submitList(reports);
            binding.recyclerView.setVisibility(reports.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void showFilterBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        LayoutFilterBottomSheetBinding sheetBinding = LayoutFilterBottomSheetBinding.inflate(getLayoutInflater());
        bottomSheetDialog.setContentView(sheetBinding.getRoot());

        sheetBinding.applyFilterButton.setOnClickListener(v -> {
            String category = null;
            int categoryId = sheetBinding.categoryChipGroup.getCheckedChipId();
            if (categoryId != View.NO_ID) {
                Chip chip = sheetBinding.categoryChipGroup.findViewById(categoryId);
                if (chip != null) category = chip.getText().toString();
            }

            String location = null;
            int locationId = sheetBinding.locationChipGroup.getCheckedChipId();
            if (locationId != View.NO_ID) {
                Chip chip = sheetBinding.locationChipGroup.findViewById(locationId);
                if (chip != null) location = chip.getText().toString();
            }

            viewModel.setFilters(category, location);
            bottomSheetDialog.dismiss();
        });

        sheetBinding.resetFilterButton.setOnClickListener(v -> {
            viewModel.setFilters(null, null);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }
}
