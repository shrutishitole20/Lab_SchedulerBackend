package com.lab.dto;

import jakarta.validation.constraints.NotBlank;

public class TestTypeRequest {

    @NotBlank(message = "Test type name is required")
    private String name;

    public TestTypeRequest() {
    }

    public TestTypeRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
