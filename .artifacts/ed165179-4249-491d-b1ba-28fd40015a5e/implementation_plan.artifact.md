# Implementation Plan - Fix Popup, Analytics, and Notifications

Fix the issue where assigned reports don't pop up on the staff dashboard, improve the analytics design, and implement a comprehensive notification system for all user roles.

## Proposed Changes

### [Dashboard & Popup]

#### [MODIFY] [MaintenanceDashboardActivity.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/MaintenanceDashboardActivity.java)
- Ensure the `startAssignmentListener` correctly triggers for new assignments.
- Add a check to prevent popups for assignments that were already "Accepted" or "Dismissed" in previous sessions if they appear as `ADDED` on initial load.
- Improve the `showAssignmentPrompt` UI.

### [Analytics Design]

#### [MODIFY] [activity_admin_analytics.xml](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/res/layout/activity_admin_analytics.xml)
- Redesign KPI cards for better visual hierarchy.
- Add icons and better spacing to the analytics sections.
- Improve the color palette to match the "Professional" theme mentioned in the layout comments.

#### [MODIFY] [AdminAnalyticsActivity.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/AdminAnalyticsActivity.java)
- Update chart styling to be cleaner.
- Add more granular stats if applicable.

### [Notification System]

#### [MODIFY] [ReportRepository.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/database/ReportRepository.java)
- Add a method `notifyAdmins(String title, String message, String reportId)` to send notifications to all users with the `ADMIN` role.
- Ensure `sendNotification` works reliably for all roles.

#### [MODIFY] [ReportSubmissionActivity.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/ReportSubmissionActivity.java)
- Replace hardcoded admin email with a call to `notifyAdmins`.

#### [MODIFY] [ReportDetailActivity.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/ReportDetailActivity.java)
- When staff updates status: Notify the Student (reporter) and all Admins.
- When admin assigns staff: Notify the Staff member (already partially done, ensure it's robust).

## Verification Plan

### Automated Tests
- N/A (Manual verification on device/emulator is more effective for UI/Popups/Notifications).

### Manual Verification
1. **Popup**: Log in as Staff, then log in as Admin in another instance (or use Firebase Console to add an assignment). Verify the popup appears immediately on the Staff dashboard.
2. **Analytics**: Open the Admin dashboard and verify the new design.
3. **Notifications**:
   - Submit a report as a Student -> Check if Admin gets a notification.
   - Assign a report as Admin -> Check if Staff gets a notification/popup.
   - Update a report as Staff -> Check if Student and Admin get notifications.
