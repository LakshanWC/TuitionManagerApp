package com.wc.tuitionmanagerapp.student;

public class AttendanceRecord {
    private String date;
    private String status; //present or absent
    private String courseName;

    // Empty constructor for Firestore
    public AttendanceRecord() {}

    // Getters and setters

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
}
