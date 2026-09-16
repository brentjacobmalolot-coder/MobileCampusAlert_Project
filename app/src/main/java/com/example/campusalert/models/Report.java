package com.example.campusalert.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "reports")
public class Report {
    @PrimaryKey
    @NonNull
    private String id; // Use Firebase Push Key as Primary Key
    
    private String title;
    private String description;
    private String building;
    private String category;
    private String priority; // Low, Medium, High
    private String status;   // Pending, In Progress, Completed
    private long timestamp;
    private String userId;   // Email of the student who reported it
    private String staffId;  // Email of the staff assigned
    private String staffComment;
    private String imageUrl; // Can be a URL from Storage or a Base64 string
    private long viewCount = 0;

    // Queue Integrity Fields
    private boolean isArchived = false;
    private boolean isFlagged = false;
    private String mergedIntoId = null;

    public Report() {
        this.id = ""; // Required for Firebase
    }

    @Ignore
    public Report(String title, String description, String building, String category, String priority, String status, long timestamp, String userId) {
        this.id = ""; // Initialized empty, set later by Firebase/Repository
        this.title = title;
        this.description = description;
        this.building = building;
        this.category = category;
        this.priority = priority;
        this.status = status;
        this.timestamp = timestamp;
        this.userId = userId;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }
    public String getStaffComment() { return staffComment; }
    public void setStaffComment(String staffComment) { this.staffComment = staffComment; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }

    public double getEngagementScore() {
        int priorityWeight = 1;
        if ("High".equalsIgnoreCase(priority)) priorityWeight = 3;
        else if ("Medium".equalsIgnoreCase(priority) || "Med".equalsIgnoreCase(priority)) priorityWeight = 2;
        return viewCount * priorityWeight;
    }
    
    // Legacy support for older code using getImageUri
    public String getImageUri() { return imageUrl; }
    public void setImageUri(String imageUri) { this.imageUrl = imageUri; }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }
    public boolean isFlagged() { return isFlagged; }
    public void setFlagged(boolean flagged) { isFlagged = flagged; }
    public String getMergedIntoId() { return mergedIntoId; }
    public void setMergedIntoId(String mergedIntoId) { this.mergedIntoId = mergedIntoId; }
}