package com.lab.model;

import com.lab.enums.Priority;
import com.lab.enums.Status;
import java.time.LocalDateTime;

public class Sample {
    private String id;
    private String sampleType;
    private String testType;
    private String testCategory;
    private String processingType;
    private Priority priority;
    private LocalDateTime receivedTime;
    private int targetMinutes;
    private Status status;
    private String assignedMachineId;
    private LocalDateTime startTime;
    private LocalDateTime completionTime;

    public Sample() {
        this.status = Status.RECEIVED;
        this.sampleType = "Blood";
        this.testCategory = "General";
        this.processingType = "Routine";
    }

    public Sample(String id, String testType, Priority priority, LocalDateTime receivedTime, int targetMinutes) {
        this(id, inferSampleType(testType), testType, inferTestCategory(testType),
             (priority == Priority.EMERGENCY ? "STAT" : "Routine"), priority, receivedTime, targetMinutes);
    }

    public Sample(String id, String testType, Priority priority, LocalDateTime receivedTime) {
        this(id, testType, priority, receivedTime, priority != null ? priority.getDefaultTargetMinutes() : 0);
    }

    public Sample(String id, String sampleType, String testType, String testCategory, String processingType,
                  Priority priority, LocalDateTime receivedTime, int targetMinutes) {
        this.id = id;
        this.testType = (testType != null) ? testType.trim() : "";
        this.sampleType = (sampleType != null && !sampleType.trim().isEmpty()) ? sampleType.trim() : inferSampleType(this.testType);
        this.testCategory = (testCategory != null && !testCategory.trim().isEmpty()) ? testCategory.trim() : inferTestCategory(this.testType);
        this.processingType = (processingType != null && !processingType.trim().isEmpty()) ? processingType.trim() : (priority == Priority.EMERGENCY ? "STAT" : "Routine");
        this.priority = (priority != null) ? priority : Priority.NORMAL;
        this.receivedTime = receivedTime;
        this.targetMinutes = targetMinutes;
        this.status = Status.RECEIVED;
    }

    public Sample(String id, String testType, Priority priority, LocalDateTime receivedTime,
                  int targetMinutes, Status status, String assignedMachineId,
                  LocalDateTime startTime, LocalDateTime completionTime) {
        this(id, inferSampleType(testType), testType, inferTestCategory(testType),
             (priority == Priority.EMERGENCY ? "STAT" : "Routine"),
             priority, receivedTime, targetMinutes, status, assignedMachineId, startTime, completionTime);
    }

    public Sample(String id, String sampleType, String testType, String testCategory, String processingType,
                  Priority priority, LocalDateTime receivedTime,
                  int targetMinutes, Status status, String assignedMachineId,
                  LocalDateTime startTime, LocalDateTime completionTime) {
        this.id = id;
        this.testType = (testType != null) ? testType.trim() : "";
        this.sampleType = (sampleType != null && !sampleType.trim().isEmpty()) ? sampleType.trim() : inferSampleType(this.testType);
        this.testCategory = (testCategory != null && !testCategory.trim().isEmpty()) ? testCategory.trim() : inferTestCategory(this.testType);
        this.processingType = (processingType != null && !processingType.trim().isEmpty()) ? processingType.trim() : (priority == Priority.EMERGENCY ? "STAT" : "Routine");
        this.priority = (priority != null) ? priority : Priority.NORMAL;
        this.receivedTime = receivedTime;
        this.targetMinutes = targetMinutes;
        this.status = (status != null) ? status : Status.RECEIVED;
        this.assignedMachineId = assignedMachineId;
        this.startTime = startTime;
        this.completionTime = completionTime;
    }

    public static String inferSampleType(String testType) {
        if (testType == null) return "Blood";
        String lower = testType.toLowerCase().trim();
        if (lower.contains("urine")) return "Urine";
        if (lower.contains("swab") || lower.contains("covid")) return "Swab";
        if (lower.contains("saliva")) return "Saliva";
        if (lower.contains("tissue") || lower.contains("biopsy")) return "Tissue";
        return "Blood";
    }

    public static String inferTestCategory(String testType) {
        if (testType == null) return "General";
        String lower = testType.toLowerCase().trim();
        if (lower.contains("blood") || lower.contains("cbc") || lower.contains("hemat")) return "Hematology";
        if (lower.contains("urine") || lower.contains("glucose") || lower.contains("chem")) return "Biochemistry";
        if (lower.contains("thyroid") || lower.contains("endo") || lower.contains("hormone")) return "Endocrinology";
        if (lower.contains("covid") || lower.contains("pcr") || lower.contains("dna")) return "Molecular";
        return "General";
    }

    public boolean canTransitionTo(Status newStatus) {
        if (newStatus == null || this.status == newStatus) return true;
        if (this.status == null) return true;
        switch (this.status) {
            case RECEIVED:
                return newStatus == Status.QUEUED || newStatus == Status.ON_HOLD;
            case QUEUED:
                return newStatus == Status.PROCESSING || newStatus == Status.ON_HOLD;
            case PROCESSING:
                return newStatus == Status.COMPLETED || newStatus == Status.ON_HOLD || newStatus == Status.QUEUED;
            case ON_HOLD:
                return newStatus == Status.QUEUED || newStatus == Status.RECEIVED;
            case COMPLETED:
            default:
                return false;
        }
    }

    public LocalDateTime getDeadline() {
        return (receivedTime != null) ? receivedTime.plusMinutes(targetMinutes) : null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public String getTestCategory() {
        return testCategory;
    }

    public void setTestCategory(String testCategory) {
        this.testCategory = testCategory;
    }

    public String getProcessingType() {
        return processingType;
    }

    public void setProcessingType(String processingType) {
        this.processingType = processingType;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public LocalDateTime getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(LocalDateTime receivedTime) {
        this.receivedTime = receivedTime;
    }

    public int getTargetMinutes() {
        return targetMinutes;
    }

    public void setTargetMinutes(int targetMinutes) {
        this.targetMinutes = targetMinutes;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        if (this.status != null && !canTransitionTo(status)) {
            throw new IllegalStateException("Invalid status transition from " + this.status + " to " + status + " for sample " + this.id);
        }
        this.status = status;
    }

    public void forceStatus(Status status) {
        this.status = status;
    }

    public String getAssignedMachineId() {
        return assignedMachineId;
    }

    public void setAssignedMachineId(String assignedMachineId) {
        this.assignedMachineId = assignedMachineId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(LocalDateTime completionTime) {
        this.completionTime = completionTime;
    }

    @Override
    public String toString() {
        return "Sample{" +
                "id='" + id + '\'' +
                ", sampleType='" + sampleType + '\'' +
                ", testType='" + testType + '\'' +
                ", testCategory='" + testCategory + '\'' +
                ", processingType='" + processingType + '\'' +
                ", priority=" + (priority != null ? priority.name() : "null") +
                ", receivedTime=" + receivedTime +
                ", targetMinutes=" + targetMinutes +
                ", deadline=" + getDeadline() +
                ", status=" + status +
                (assignedMachineId != null ? ", assignedMachineId='" + assignedMachineId + '\'' : "") +
                (startTime != null ? ", startTime=" + startTime : "") +
                (completionTime != null ? ", completionTime=" + completionTime : "") +
                '}';
    }
}
