package com.example.campusalert.database;

import android.util.Log;
import androidx.room.TypeConverter;
import com.example.campusalert.models.UserRole;

public class Converters {
    @TypeConverter
    public static String fromUserRole(UserRole role) {
        return role == null ? null : role.name();
    }

    @TypeConverter
    public static UserRole toUserRole(String role) {
        if (role == null) return null;
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            Log.e("Converters", "Unknown role: " + role);
            return UserRole.STUDENT; // Safe fallback
        }
    }
}