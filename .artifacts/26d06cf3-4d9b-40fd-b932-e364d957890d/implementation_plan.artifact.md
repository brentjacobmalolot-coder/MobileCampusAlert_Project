# Migration Plan: Firebase Realtime Database to Cloud Firestore

This plan outlines the steps to migrate the CampusAlert project from Firebase Realtime Database (RTDB) to Cloud Firestore. The migration will enhance querying capabilities for analytics while maintaining the Room database as the primary source of truth for the UI.

## User Review Required

> [!IMPORTANT]
> The data structure in Firestore will slightly differ from RTDB. We will be using collections instead of a hierarchical tree.
> Specifically, "reports", "users", "notifications", and "assignments" will be top-level collections.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/gradle/libs.versions.toml)
- Add `firebase-firestore` to the `libraries` section using the existing `firebase-bom`.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/build.gradle.kts)
- Replace `libs.firebase.database` with `libs.firebase.firestore`.
- Keep `firebase-messaging` as it is not part of the RTDB/Firestore migration directly but relies on the BOM.

---

### Data Layer

#### [MODIFY] [ReportRepository.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/database/ReportRepository.java)
- Replace `DatabaseReference` with `FirebaseFirestore`.
- Remove `DATABASE_URL`.
- Refactor `insert()` to use Firestore's `collection("reports").add()` or `document(id).set()`.
- Refactor `syncReportsFromFirebase()` to use `addSnapshotListener` on collections.
- Update `update()`, `archiveReport()`, `flagReport()`, `mergeReports()`, and `incrementViewCount()` to use Firestore document updates.
- Refactor `updateFcmToken()` and `sendNotification()` to use Firestore collections.
- Remove RTDB-specific sanitization (`sanitizeKey`) if no longer needed (Firestore handles periods in keys/IDs better).

---

### UI Layer

#### [MODIFY] [MaintenanceDashboardActivity.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/MaintenanceDashboardActivity.java)
- Replace `DatabaseReference` with `FirebaseFirestore`.
- Update `startAssignmentListener` to use Firestore snapshots on the `assignments` collection.

#### [MODIFY] [NotificationsActivity.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/NotificationsActivity.java)
- Replace `DatabaseReference` with `FirebaseFirestore`.
- Update `loadNotifications` and `startAssignmentListener` to use Firestore.

#### [MODIFY] [ReportDetailActivity.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/ReportDetailActivity.java)
- Update `setupStaffSpinner` to fetch from Firestore's `users` or `permissions` collection.
- Update `assignStaff` to write the assignment to Firestore.

## Verification Plan

### Automated Tests
- Run existing Room unit tests (if any) to ensure local persistence still works.
- Perform a `gradle build` to verify dependency changes.

### Manual Verification
1. Deploy the app to a device/emulator.
2. Submit a new report and verify it appears in both Firestore and the local Room database.
3. Assign a staff member to a report and verify the notification/assignment prompt appears on the staff device.
4. Update a report status and verify the real-time sync reflects the change in the UI.
