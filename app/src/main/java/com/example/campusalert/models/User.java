package com.example.campusalert.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.firebase.database.Exclude;

@Entity(tableName = "users")
public class User {
    @PrimaryKey
    @NonNull
    private String email;
    private String name;
    private String role; 
    private String fcmToken;

    // No-argument constructor for Firebase
    public User() {
        this.email = "";
    }

    public User(@NonNull String email, String name, String role) {
        this.email = email;
        this.name = name;
        this.role = role;
    }

    @NonNull
    public String getEmail() { return email; }
    public void setEmail(@NonNull String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    /**
     * Helper to get the role as an enum for application logic.
     * Excluded from Firebase to avoid duplicate role fields.
     */
    @Exclude
    public UserRole getRoleEnum() {
        if (role == null) return UserRole.STUDENT;
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            if (role.equalsIgnoreCase("staff") || role.equalsIgnoreCase("maintenance")) 
                return UserRole.MAINTENANCE_STAFF;
            return UserRole.STUDENT;
        }
    }
}