# Walkthrough - Assigned Reports Visibility on Staff Dashboard

I have completed the task to ensure that reports assigned to a maintenance staff member correctly appear on their dashboard.

## Changes Made

### Backend (Database & ViewModel)
- **`ReportDao.java`**: Added a new query `getReportsByStaffActive` that retrieves all reports assigned to a specific staff member that are NOT in "Completed" status.
- **`ReportRepository.java`**: Exposed the `getReportsByStaffActive` method.
- **`ReportViewModel.java`**: Exposed the `getReportsByStaffActive` method to the UI layer.

### Frontend (Dashboard UI)
- **`MaintenanceDashboardActivity.java`**:
    - Updated `updateFilter` to handle the "Assigned" tab differently.
    - Instead of filtering strictly by "Pending" status, it now uses `getReportsByStaffActive`.
    - This ensures that once an Admin assigns a staff member (which sets the status to "In Progress"), the report remains visible in the staff's "Assigned" tab as an active task.
    - Added missing `LiveData` and `List` imports to support the new logic.

## Verification Results

The changes were verified through code analysis and error checking in the IDE. The new logic correctly addresses the visibility gap where "In Progress" reports (set during assignment) were missing from the staff's "Assigned" tab.

### Manual Test Case Recommendation:
1. Assign a report to a staff member as an Admin.
2. Log in as that staff member.
3. Check the "Assigned" tab; the report should now be visible.
4. Move the report to "In Progress" and verify it stays in the "Assigned" tab but also shows up in the "In Progress" tab.
5. Mark as "Completed" and verify it moves to the "Completed" tab and leaves the "Assigned" tab.
