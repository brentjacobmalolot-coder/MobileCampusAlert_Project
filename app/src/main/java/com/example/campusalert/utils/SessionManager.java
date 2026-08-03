package com.example.campusalert.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import com.example.campusalert.models.User;
import com.example.campusalert.models.UserRole;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class SessionManager {
    private static final String TAG = "SessionManager";
    private static final String PREF_NAME = "CampusAlertSession";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_NAME = "name";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            pref = EncryptedSharedPreferences.create(
                    PREF_NAME,
                    masterKeyAlias,
                    context.getApplicationContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            editor = pref.edit();
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "EncryptedSharedPreferences failed, falling back to standard prefs", e);
            pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            editor = pref.edit();
        }
    }

    public void createSession(User user) {
        if (user == null || user.getRole() == null) return;
        
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_ROLE, user.getRole().name());
        editor.putString(KEY_NAME, user.getName());
        editor.apply();
    }

    public String getUserEmail() {
        return pref.getString(KEY_EMAIL, null);
    }

    public UserRole getUserRole() {
        String roleStr = pref.getString(KEY_ROLE, null);
        if (roleStr == null) return null;
        try {
            return UserRole.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid role stored in prefs: " + roleStr);
            return null;
        }
    }

    public String getUserName() {
        return pref.getString(KEY_NAME, "User");
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }

    public boolean isLoggedIn() {
        return getUserEmail() != null && getUserRole() != null;
    }
}