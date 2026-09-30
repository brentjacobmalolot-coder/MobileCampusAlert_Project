package com.example.campusalert;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.campusalert.utils.NotificationHelper;
import com.example.campusalert.utils.SessionManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        String title = null;
        String message = null;
        String reportId = null;

        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            title = remoteMessage.getData().get("title");
            message = remoteMessage.getData().get("message");
            reportId = remoteMessage.getData().get("reportId");
        }

        if (remoteMessage.getNotification() != null) {
            if (title == null) title = remoteMessage.getNotification().getTitle();
            if (message == null) message = remoteMessage.getNotification().getBody();
        }

        if (title == null) title = "Campus Alert";
        if (message == null) message = "You have a new update.";

        NotificationHelper.showPopUpNotification(this, title, message, reportId);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        updateTokenInFirestore(token);
    }

    private void updateTokenInFirestore(String token) {
        SessionManager sessionManager = new SessionManager(this);
        String email = sessionManager.getUserEmail();
        if (email != null) {
            FirebaseFirestore.getInstance().collection("users")
                    .document(email)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token updated successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating FCM token", e));
        }
    }
}
