package com.lab.dto;

import com.lab.enums.Priority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class SampleRequest {

    @NotBlank(message = "testType is required (e.g., 'Blood', 'Urine', 'Thyroid')")
    private String testType;

    private String sampleType;

    private String testCategory;

    private String processingType;

    @NotNull(message = "priority is required (EMERGENCY, HIGH, NORMAL, LOW)")
    private Priority priority;

    private LocalDateTime receivedTime;

    @Min(value = 1, message = "targetMinutes must be greater than 0")
    private Integer targetMinutes;

    public SampleRequest() {
    }

    public SampleRequest(String testType, Priority priority, LocalDateTime receivedTime, Integer targetMinutes) {
        this.testType = testType;
        this.priority = priority;
        this.receivedTime = receivedTime;
        this.targetMinutes = targetMinutes;
    }

    public SampleRequest(String sampleType, String testType, String testCategory, String processingType,
                         Priority priority, LocalDateTime receivedTime, Integer targetMinutes) {
        this.sampleType = sampleType;
        this.testType = testType;
        this.testCategory = testCategory;
        this.processingType = processingType;
        this.priority = priority;
        this.receivedTime = receivedTime;
        this.targetMinutes = targetMinutes;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
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

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
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

    public Integer getTargetMinutes() {
        return targetMinutes;
    }

    public void setTargetMinutes(Integer targetMinutes) {
        this.targetMinutes = targetMinutes;
    }
}
