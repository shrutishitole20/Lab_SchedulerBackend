package com.lab.model;

import java.time.LocalDateTime;

public class SampleAssignment {

    private int id;
    private String sampleId;
    private String machineId;
    private LocalDateTime assignedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String status;
    private String notes;

    public SampleAssignment() {
    }

    public SampleAssignment(String sampleId, String machineId, LocalDateTime assignedAt, String status, String notes) {
        this.sampleId = sampleId;
        this.machineId = machineId;
        this.assignedAt = assignedAt != null ? assignedAt : LocalDateTime.now();
        this.status = status != null ? status : "ASSIGNED";
        this.notes = notes;
    }

    public SampleAssignment(int id, String sampleId, String machineId, LocalDateTime assignedAt, LocalDateTime startedAt, LocalDateTime completedAt, String status, String notes) {
        this.id = id;
        this.sampleId = sampleId;
        this.machineId = machineId;
        this.assignedAt = assignedAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.status = status;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "SampleAssignment{" +
                "id=" + id +
                ", sampleId='" + sampleId + '\'' +
                ", machineId='" + machineId + '\'' +
                ", assignedAt=" + assignedAt +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                ", status='" + status + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}
