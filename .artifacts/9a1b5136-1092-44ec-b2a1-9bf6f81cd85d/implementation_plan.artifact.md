# Implement Background Notifications

This plan addresses the requirement to display notifications even when the application is not in use (backgrounded or closed). The primary issue identified is the missing runtime permission for notifications on Android 13+ and ensuring the Firebase Cloud Messaging (FCM) service is fully configured for background delivery.

## User Review Required

> [!IMPORTANT]
> This implementation assumes you are using Firebase Cloud Messaging (FCM) to trigger notifications. If you are only writing to Firestore and expecting the app to "know" about new data without a push server, we will need to implement a `WorkManager` for periodic background polling.

## Proposed Changes

### Core UI & Permissions

#### [MODIFY] [MainActivity.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/MainActivity.java)
- Add runtime permission request for `POST_NOTIFICATIONS` (required for Android 13+).
- Ensure the app asks for this permission upon startup if not already granted.

### Notification Service Enhancement

#### [MODIFY] [MyFirebaseMessagingService.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/MyFirebaseMessagingService.java)
- Improve `onMessageReceived` to ensure notifications are triggered even if the message contains only a data payload.
- Ensure the notification channel is configured for high importance to enable "popup" (heads-up) notifications.

## Verification Plan

### Automated Tests
- No specific automated tests; requires manual device testing.

### Manual Verification
1. Deploy the app to a device running Android 13 or higher.
2. Verify that the app requests "Allow Notifications" on the first launch.
3. Grant the permission.
4. Send a test FCM message (via Firebase Console) while the app is in the background or closed.
5. Verify the notification "pops up" as a heads-up notification.
