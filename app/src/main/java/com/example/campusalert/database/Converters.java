package com.example.campusalert.database;

import androidx.room.TypeConverter;
import com.example.campusalert.models.UserRole;

public class Converters {
    @TypeConverter
    public static String fromUserRole(UserRole role) {
        return role == null ? null : role.name();
    }

    @TypeConverter
    public static UserRole toUserRole(String role) {
        return role == null ? null : UserRole.valueOf(role);
    }
}