# Implementation Plan - Ensure Assigned Reports Appear on Staff Dashboard

The goal is to ensure that reports assigned to a maintenance staff member correctly appear on their dashboard. Currently, there is a status mismatch: an Admin assigning a staff member sets the report to "In Progress," but the staff dashboard's "Assigned" tab specifically filters for "Pending" status.

## Proposed Changes

### Database Layer

#### [MODIFY] [ReportDao.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/database/ReportDao.java)
- Add a new query `getReportsByStaffActive` that retrieves reports assigned to a staff member with a status that is NOT "Completed". This will be used for the "Assigned" tab.

### Repository and ViewModel

#### [MODIFY] [ReportRepository.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/database/ReportRepository.java)
- Add `getReportsByStaffActive(String staffId)` to expose the new DAO method.

#### [MODIFY] [ReportViewModel.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/viewmodels/ReportViewModel.java)
- Add `getReportsByStaffActive(String staffId)` to expose the repository method to the UI.

### UI Layer

#### [MODIFY] [MaintenanceDashboardActivity.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/MaintenanceDashboardActivity.java)
- Update the `updateFilter` method. When the "Assigned" tab is selected, use `reportViewModel.getReportsByStaffActive` instead of `getReportsByStaffAndStatus(..., "Pending")`. This ensures that reports assigned to the staff will appear in the first tab regardless of whether they are "Pending" or "In Progress".

#### [MODIFY] [ReportDetailActivity.java](file:///C:/Users/HomePC/AndroidStudioProjects/CampusAlert/app/src/main/java/com/example/campusalert/ReportDetailActivity.java)
- (Optional) Refine the assignment logic to ensure the status change is appropriate, but the dashboard fix addresses the immediate visibility issue.

## Verification Plan

### Manual Verification
1.  Log in as an Admin.
2.  Assign a report to a specific Maintenance Staff member.
3.  Log in as that Maintenance Staff member.
4.  Verify that the report appears in the "Assigned" tab of the Maintenance Dashboard.
5.  Move the report to "In Progress" in the Report Detail view and verify it still appears in the "Assigned" tab (as it's an active task) and also in the "In Progress" tab.
6.  Mark the report as "Completed" and verify it moves to the "Completed" tab and disappears from the "Assigned" tab.
