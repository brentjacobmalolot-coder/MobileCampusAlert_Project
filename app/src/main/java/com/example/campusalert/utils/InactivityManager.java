package com.example.campusalert.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.campusalert.MainActivity;


public class InactivityManager {
    private static final String TAG = "InactivityManager";

    // 2.5 minutes in milliseconds (within the 2-3 minute specification)
    public static final long INACTIVITY_TIMEOUT_MS = (long) (2.5 * 60 * 1000);

    private static InactivityManager instance;
    private final Handler handler;
    private Runnable logoutRunnable;
    private long lastInteractionTime;
    private boolean isTimerRunning = false;

    private InactivityManager() {
        handler = new Handler(Looper.getMainLooper());
        lastInteractionTime = System.currentTimeMillis();
    }

    public static synchronized InactivityManager getInstance() {
        if (instance == null) {
            instance = new InactivityManager();
        }
        return instance;
    }

    /**
     * Resets the inactivity timer upon any user interaction (touch, click, key, scroll).
     */
    public synchronized void userInteracted(Context context) {
        lastInteractionTime = System.currentTimeMillis();
        if (isTimerRunning) {
            scheduleTimer(context, INACTIVITY_TIMEOUT_MS);
        }
    }


    public synchronized void onActivityResumed(Context context) {
        long elapsed = System.currentTimeMillis() - lastInteractionTime;
        if (elapsed >= INACTIVITY_TIMEOUT_MS) {
            Log.d(TAG, "Inactivity threshold reached in background. Performing silent logout.");
            performSilentLogout(context);
        } else {
            long remaining = INACTIVITY_TIMEOUT_MS - elapsed;
            isTimerRunning = true;
            scheduleTimer(context, remaining);
        }
    }

    /**
     * Called when an Activity pauses. Cancels current pending Runnable.
     */
    public synchronized void onActivityPaused() {
        cancelTimer();
    }

    private void scheduleTimer(Context context, long delayMs) {
        cancelTimer();
        final Context appContext = context.getApplicationContext();
        logoutRunnable = () -> performSilentLogout(appContext);
        handler.postDelayed(logoutRunnable, Math.max(0, delayMs));
        isTimerRunning = true;
        Log.d(TAG, "Inactivity timer scheduled for " + delayMs + " ms");
    }

    private void cancelTimer() {
        if (logoutRunnable != null) {
            handler.removeCallbacks(logoutRunnable);
            logoutRunnable = null;
        }
        isTimerRunning = false;
    }


    public void performSilentLogout(Context context) {
        cancelTimer();
        
        SessionManager sessionManager = new SessionManager(context);
        if (sessionManager.isLoggedIn()) {
            sessionManager.clearActiveSession();
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("IS_AUTO_LOGOUT", true);
        context.startActivity(intent);
    }
}
