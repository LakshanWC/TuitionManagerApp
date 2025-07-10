package com.wc.tuitionmanagerapp.models;

public class Attendance {
    private String date;
    private String status;

    public Attendance(){}

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
}
