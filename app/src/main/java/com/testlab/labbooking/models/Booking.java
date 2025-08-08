package com.testlab.labbooking.models;

import com.testlab.labbooking.utils.DateTimeUtils;

public class Booking {
    private String id;
    private String labId;
    private String userId;
    private String labName; // Denormalized for easier queries
    private String userName; // Denormalized for easier queries
    private String date; // Format: YYYY-MM-DD
    private String startTime; // Format: HH:MM
    private String endTime; // Format: HH:MM
    private String status; // "pending", "approved", "rejected", "cancelled"
    private String purpose;
    private long createdAt;
    private long updatedAt;
    private String adminNotes;
    private int durationMinutes;

    public Booking() {
        // Empty constructor for Firestore
    }

    public Booking(String labId, String userId, String labName, String userName,
                   String date, String startTime, String endTime, String purpose) {
        this.labId = labId;
        this.userId = userId;
        this.labName = labName;
        this.userName = userName;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
        this.status = "pending";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.durationMinutes = DateTimeUtils.getTimeDifferenceInMinutes(startTime, endTime);
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabId() {
        return labId;
    }

    public void setLabId(String labId) {
        this.labId = labId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLabName() {
        return labName;
    }

    public void setLabName(String labName) {
        this.labName = labName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public int getDurationMinutes() {
        return DateTimeUtils.getTimeDifferenceInMinutes(startTime, endTime);
    }
}