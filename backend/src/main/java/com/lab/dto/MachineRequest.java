package com.lab.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public class MachineRequest {

    @NotBlank(message = "Machine ID is required (e.g., 'M1', 'M2')")
    private String id;

    @NotBlank(message = "Machine Name is required")
    private String name;

    @NotEmpty(message = "At least one supported test type must be provided")
    private Set<String> supportedTestTypes;

    @Min(value = 1, message = "Machine capacity must be at least 1")
    private int capacity;

    private boolean available = true;

    public MachineRequest() {
    }

    public MachineRequest(String id, String name, Set<String> supportedTestTypes, int capacity, boolean available) {
        this.id = id;
        this.name = name;
        this.supportedTestTypes = supportedTestTypes;
        this.capacity = capacity;
        this.available = available;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getSupportedTestTypes() {
        return supportedTestTypes;
    }

    public void setSupportedTestTypes(Set<String> supportedTestTypes) {
        this.supportedTestTypes = supportedTestTypes;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
