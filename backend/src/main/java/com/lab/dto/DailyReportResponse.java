package com.lab.dto;

import com.lab.model.Sample;

import java.time.LocalDate;
import java.util.List;

public class DailyReportResponse {

    private LocalDate date;
    private long completed;
    private long pending;
    private long delayed;
    private long onHold;
    private List<Sample> samples;

    public DailyReportResponse() {
    }

    public DailyReportResponse(LocalDate date, long completed, long pending, long delayed, long onHold, List<Sample> samples) {
        this.date = date;
        this.completed = completed;
        this.pending = pending;
        this.delayed = delayed;
        this.onHold = onHold;
        this.samples = samples;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getCompleted() {
        return completed;
    }

    public void setCompleted(long completed) {
        this.completed = completed;
    }

    public long getPending() {
        return pending;
    }

    public void setPending(long pending) {
        this.pending = pending;
    }

    public long getDelayed() {
        return delayed;
    }

    public void setDelayed(long delayed) {
        this.delayed = delayed;
    }

    public long getOnHold() {
        return onHold;
    }

    public void setOnHold(long onHold) {
        this.onHold = onHold;
    }

    public List<Sample> getSamples() {
        return samples;
    }

    public void setSamples(List<Sample> samples) {
        this.samples = samples;
    }
}
