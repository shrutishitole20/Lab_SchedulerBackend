package com.lab.model;

import java.time.LocalDateTime;

public class HistoryEntry {
    private String sampleId;
    private LocalDateTime timestamp;
    private String action;
    private String details;

    public HistoryEntry() {
        this.timestamp = LocalDateTime.now();
    }

    public HistoryEntry(String sampleId, String action, String details) {
        this.sampleId = sampleId;
        this.timestamp = LocalDateTime.now();
        this.action = action;
        this.details = details;
    }

    public HistoryEntry(String sampleId, LocalDateTime timestamp, String action, String details) {
        this.sampleId = sampleId;
        this.timestamp = timestamp;
        this.action = action;
        this.details = details;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return "HistoryEntry{" +
                "sampleId='" + sampleId + '\'' +
                ", timestamp=" + timestamp +
                ", action='" + action + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}
