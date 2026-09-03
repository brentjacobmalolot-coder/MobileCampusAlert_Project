package com.example.campusalert.models;

public class Notification {
    private String id;
    private String title;
    private String message;
    private long timestamp;
    private boolean read;
    private String recipientEmail;
    private String reportId; // Linked report for management actions

    public Notification() {}

    public Notification(String id, String title, String message, long timestamp, String recipientEmail) {
        this(id, title, message, timestamp, recipientEmail, null);
    }

    public Notification(String id, String title, String message, long timestamp, String recipientEmail, String reportId) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.recipientEmail = recipientEmail;
        this.read = false;
        this.reportId = reportId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
}