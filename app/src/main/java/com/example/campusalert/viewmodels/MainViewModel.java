package com.example.campusalert.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import com.example.campusalert.database.AppDatabase;
import com.example.campusalert.database.ReportRepository;
import com.example.campusalert.models.Report;
import com.example.campusalert.models.User;
import com.example.campusalert.models.UserRole;
import com.example.campusalert.utils.SecurityUtils;

public class MainViewModel extends AndroidViewModel {
    private final ReportRepository repository;

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new ReportRepository(application);
        seedDataIfNecessary();
    }

    private void seedDataIfNecessary() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Check if seeding is needed
            if (repository.getUserCount() == 0) {
                String hashedPassword = SecurityUtils.hashPassword("password");
                
                // Create Default Accounts
                repository.insertUser(new User("student@school.edu", hashedPassword, "John Student", UserRole.STUDENT));
                repository.insertUser(new User("staff@school.edu", hashedPassword, "Mike Staff", UserRole.MAINTENANCE_STAFF));
                repository.insertUser(new User("admin@school.edu", hashedPassword, "Admin User", UserRole.ADMIN));

                // Create Sample Reports for the Showcase
                repository.insert(new Report("Broken AC - Room 204", "The air conditioner is making a loud rattling noise and not cooling.", "St. Therese Bldg", "Facilities", "High", "Pending", System.currentTimeMillis(), "student@school.edu"));
                repository.insert(new Report("Loose Wiring - Library", "Exposed wires in the East Wing reading area.", "Library", "Electrical", "High", "In Progress", System.currentTimeMillis() - 3600000, "student@school.edu"));
                repository.insert(new Report("Water Leakage - Gym", "Water dripping from the ceiling near the basketball court.", "Sports Complex", "Plumbing", "Medium", "Completed", System.currentTimeMillis() - 86400000, "student@school.edu"));
            }
        });
    }

    public User login(String email, String password) {
        String hashedPassword = SecurityUtils.hashPassword(password);
        return repository.login(email, hashedPassword);
    }
}
