package com.example.campusalert.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages unique tracking of events within a single app session.
 * This implementation uses an in-memory HashSet to track report views.
 */
public class TrackingManager {
    private static TrackingManager instance;
    private final Set<String> viewedReportIds;

    private TrackingManager() {
        viewedReportIds = new HashSet<>();
    }

    public static synchronized TrackingManager getInstance() {
        if (instance == null) {
            instance = new TrackingManager();
        }
        return instance;
    }

    /**
     * Checks if a report view should be tracked for the current session.
     * @param reportId The ID of the report being viewed.
     * @return true if the report has not been viewed in this session yet, false otherwise.
     */
    public boolean shouldTrackView(String reportId) {
        if (reportId == null || reportId.isEmpty()) {
            return false;
        }
        
        if (viewedReportIds.contains(reportId)) {
            return false;
        } else {
            viewedReportIds.add(reportId);
            return true;
        }
    }
    
    /**
     * Resets the session tracking data.
     */
    public void resetSession() {
        viewedReportIds.clear();
    }
}
