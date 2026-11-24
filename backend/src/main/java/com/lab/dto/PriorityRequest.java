package com.lab.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PriorityRequest {

    @NotBlank(message = "Priority name is required (e.g., 'URGENT')")
    private String name;

    @NotNull(message = "Rank order is required")
    @Min(value = 1, message = "Rank order must be at least 1")
    private Integer rankOrder;

    @NotNull(message = "Target minutes is required")
    @Min(value = 1, message = "Target minutes must be at least 1")
    private Integer targetMinutes;

    public PriorityRequest() {
    }

    public PriorityRequest(String name, Integer rankOrder, Integer targetMinutes) {
        this.name = name;
        this.rankOrder = rankOrder;
        this.targetMinutes = targetMinutes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getRankOrder() {
        return rankOrder;
    }

    public void setRankOrder(Integer rankOrder) {
        this.rankOrder = rankOrder;
    }

    public Integer getTargetMinutes() {
        return targetMinutes;
    }

    public void setTargetMinutes(Integer targetMinutes) {
        this.targetMinutes = targetMinutes;
    }
}
