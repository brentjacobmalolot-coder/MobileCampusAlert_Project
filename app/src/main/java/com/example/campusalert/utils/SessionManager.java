package com.example.campusalert.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.example.campusalert.models.User;
import com.example.campusalert.models.UserRole;

public class SessionManager {
    private static final String TAG = "SessionManager";
    private static final String PREF_NAME = "CampusAlertSession_v3";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_NAME = "name";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createSession(User user) {
        if (user == null) return;
        
        // Use the getRoleEnum() helper to get the normalized enum name
        String normalizedRole = user.getRoleEnum().name();
        
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_ROLE, normalizedRole);
        editor.putString(KEY_NAME, user.getName());
        editor.commit(); 
        Log.d(TAG, "Session created for: " + user.getEmail() + " with normalized role: " + normalizedRole);
    }

    public String getUserEmail() {
        return pref.getString(KEY_EMAIL, null);
    }

    public UserRole getUserRole() {
        String roleStr = pref.getString(KEY_ROLE, null);
        if (roleStr == null) return null;
        
        try {
            return UserRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid role stored: " + roleStr);
            // Comprehensive fallback for common database variations
            String normalized = roleStr.toUpperCase();
            if (normalized.contains("ADMIN")) return UserRole.ADMIN;
            if (normalized.contains("MAINTENANCE") || normalized.contains("STAFF")) return UserRole.MAINTENANCE_STAFF;
            if (normalized.contains("STUDENT")) return UserRole.STUDENT;
            return null;
        }
    }

    public String getUserName() {
        return pref.getString(KEY_NAME, "User");
    }

    public void logout() {
        editor.clear();
        editor.commit();
        Log.d(TAG, "Session cleared.");
    }

    public boolean isLoggedIn() {
        UserRole role = getUserRole();
        return getUserEmail() != null && role != null;
    }
}
