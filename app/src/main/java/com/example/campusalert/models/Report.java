package com.example.campusalert.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
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
    private String imageUri; // URI or path of the evidence photo

    public Report() {
        this.id = ""; // Required for Firebase
    }

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
    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }
}