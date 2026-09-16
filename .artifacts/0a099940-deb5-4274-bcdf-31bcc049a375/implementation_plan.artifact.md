# Unique-per-session View Tracking and Engagement Score Implementation

Implement a system to track unique views of reports within an app session and calculate an engagement score based on priority and views.

## User Review Required

> [!IMPORTANT]
> The Room database version will be bumped. Since `AppDatabase` uses `fallbackToDestructiveMigration()`, all local data will be cleared and re-synced from Firebase upon the first launch after this update.

## Proposed Changes

### Data Model

#### [MODIFY] [Report.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/models/Report.java)
- Add `viewCount` field.
- Add `getEngagementScore()` helper method.
- Add getter and setter for `viewCount`.

### Persistence & Database

#### [MODIFY] [AppDatabase.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/database/AppDatabase.java)
- Bump version to 48.

#### [MODIFY] [ReportDao.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/database/ReportDao.java)
- Add `incrementViewCount` query.

#### [MODIFY] [ReportRepository.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/database/ReportRepository.java)
- Implement `incrementViewCount` to update Room and Firebase.

### Tracking Logic

#### [NEW] [TrackingManager.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/utils/TrackingManager.java)
- Implement session-based unique view tracking using a `HashSet`.

### UI & ViewModel

#### [MODIFY] [ReportViewModel.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/viewmodels/ReportViewModel.java)
- Expose `incrementViewCount` from repository.

#### [MODIFY] [activity_report_detail.xml](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/res/layout/activity_report_detail.xml)
- Add a TextView to display `viewCount`.

#### [MODIFY] [ReportDetailActivity.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/ReportDetailActivity.java)
- Trigger view increment logic if unique to the session.
- Display `viewCount` in UI.

### Analytics

#### [MODIFY] [activity_admin_analytics.xml](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/res/layout/activity_admin_analytics.xml)
- Add "Total Engagement" KPI card.

#### [MODIFY] [AdminAnalyticsActivity.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/AdminAnalyticsActivity.java)
- Calculate and display total engagement.
- Sort "Recent Reports" by engagement score.

## Verification Plan

### Automated Tests
- N/A (Manual verification on device)

### Manual Verification
- Launch the app and view a report. Verify the view count increments (check Logcat or UI).
- View the same report again in the same session. Verify the view count does NOT increment.
- Restart the app and view the report. Verify the view count increments.
- As Admin, check the Analytics dashboard. Verify "Total Engagement" matches the sum of views and reports are sorted by engagement score.
