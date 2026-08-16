package com.example.campusalert.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey
    @NonNull
    private String email;
    private String password;
    private String name;
    private String role; // Changed to String for Firebase compatibility

    // No-argument constructor for Firebase
    public User() {
    }

    public User(@NonNull String email, String password, String name, String role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    @NonNull
    public String getEmail() { return email; }
    public void setEmail(@NonNull String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    /**
     * Helper to get the role as an enum for application logic.
     */
    public UserRole getRoleEnum() {
        if (role == null) return UserRole.STUDENT;
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            if (role.equalsIgnoreCase("staff")) return UserRole.MAINTENANCE_STAFF;
            return UserRole.STUDENT;
        }
    }
}