package com.example.campusalert;

import android.os.Bundle;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusalert.utils.InactivityManager;
import com.example.campusalert.utils.SessionManager;


public abstract class BaseActivity extends AppCompatActivity {

    protected SessionManager sessionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            InactivityManager.getInstance().onActivityResumed(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            InactivityManager.getInstance().onActivityPaused();
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            InactivityManager.getInstance().userInteracted(this);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            InactivityManager.getInstance().userInteracted(this);
        }
        return super.dispatchTouchEvent(ev);
    }
}
