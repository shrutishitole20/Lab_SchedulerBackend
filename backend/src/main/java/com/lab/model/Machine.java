package com.lab.model;

import java.util.HashSet;
import java.util.Set;

public class Machine {
    private String id;
    private String name;
    private Set<String> supportedTestTypes;
    private int capacity;
    private int currentLoad;
    private boolean available;

    public Machine() {
        this.supportedTestTypes = new HashSet<>();
        this.available = true;
    }

    public Machine(String id, String name, Set<String> supportedTestTypes, int capacity) {
        this.id = id;
        this.name = name;
        this.supportedTestTypes = (supportedTestTypes != null) ? supportedTestTypes : new HashSet<>();
        this.capacity = capacity;
        this.currentLoad = 0;
        this.available = true;
    }

    public Machine(String id, String name, Set<String> supportedTestTypes, int capacity, int currentLoad, boolean available) {
        this.id = id;
        this.name = name;
        this.supportedTestTypes = (supportedTestTypes != null) ? supportedTestTypes : new HashSet<>();
        this.capacity = capacity;
        this.currentLoad = currentLoad;
        this.available = available;
    }

    public boolean canProcess(String testType) {
        if (supportedTestTypes == null || testType == null) {
            return false;
        }
        return supportedTestTypes.stream().anyMatch(t -> t.equalsIgnoreCase(testType.trim()));
    }

    public boolean hasFreeSlot() {
        return available && (currentLoad < capacity);
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

    public int getCurrentLoad() {
        return currentLoad;
    }

    public void setCurrentLoad(int currentLoad) {
        this.currentLoad = currentLoad;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return "Machine{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", supportedTestTypes=" + supportedTestTypes +
                ", capacity=" + capacity +
                ", currentLoad=" + currentLoad +
                ", available=" + available +
                ", hasFreeSlot=" + hasFreeSlot() +
                '}';
    }
}
