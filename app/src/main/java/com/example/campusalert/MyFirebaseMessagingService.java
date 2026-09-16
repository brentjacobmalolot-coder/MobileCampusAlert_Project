package com.example.campusalert;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.campusalert.utils.SessionManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "task_alerts";
    private static final String CHANNEL_NAME = "Task Alerts";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        // Handle data payload if present
        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            String title = remoteMessage.getData().get("title");
            String message = remoteMessage.getData().get("message");
            if (title != null && message != null) {
                showNotification(title, message);
            }
        }

        // Handle notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            showNotification(title != null ? title : "New Alert",
                           body != null ? body : "Check your campus alerts.");
        }
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
            String encodedEmail = email.replace(".", ",");
            FirebaseFirestore.getInstance().collection("users")
                    .document(encodedEmail)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token updated successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating FCM token", e));
        }
    }

    private void showNotification(String title, String messageBody) {
        Intent intent = new Intent(this, NotificationsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create Notification Channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.mainscclogo) // Using existing icon from manifest
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        // Use a random ID to prevent overwriting multiple notifications
        notificationManager.notify(new Random().nextInt(), notificationBuilder.build());
    }
}
