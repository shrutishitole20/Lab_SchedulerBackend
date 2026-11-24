package com.lab.model;

import java.time.LocalDateTime;

public class MachineDowntime {

    private int id;
    private String machineId;
    private LocalDateTime downStart;
    private LocalDateTime downEnd;
    private String reason;
    private String reportedBy;

    public MachineDowntime() {
    }

    public MachineDowntime(String machineId, LocalDateTime downStart, String reason, String reportedBy) {
        this.machineId = machineId;
        this.downStart = downStart;
        this.reason = reason;
        this.reportedBy = reportedBy != null ? reportedBy : "SYSTEM";
    }

    public MachineDowntime(int id, String machineId, LocalDateTime downStart, LocalDateTime downEnd, String reason, String reportedBy) {
        this.id = id;
        this.machineId = machineId;
        this.downStart = downStart;
        this.downEnd = downEnd;
        this.reason = reason;
        this.reportedBy = reportedBy;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public LocalDateTime getDownStart() {
        return downStart;
    }

    public void setDownStart(LocalDateTime downStart) {
        this.downStart = downStart;
    }

    public LocalDateTime getDownEnd() {
        return downEnd;
    }

    public void setDownEnd(LocalDateTime downEnd) {
        this.downEnd = downEnd;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(String reportedBy) {
        this.reportedBy = reportedBy;
    }

    public boolean isActive() {
        return downEnd == null;
    }

    @Override
    public String toString() {
        return "MachineDowntime{" +
                "id=" + id +
                ", machineId='" + machineId + '\'' +
                ", downStart=" + downStart +
                ", downEnd=" + downEnd +
                ", reason='" + reason + '\'' +
                ", reportedBy='" + reportedBy + '\'' +
                '}';
    }
}
