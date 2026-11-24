package com.lab.exception;

import com.lab.dto.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Verify ResourceNotFoundException returns 404 NOT_FOUND")
    void testResourceNotFound() {
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(new ResourceNotFoundException("Sample S999 not found"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("Sample S999 not found", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Verify InvalidStateTransitionException returns 409 CONFLICT")
    void testInvalidStateTransition() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidStateTransition(new InvalidStateTransitionException("Cannot complete COMPLETED sample"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Conflict / Invalid State", response.getBody().getError());
    }

    @Test
    @DisplayName("Verify DuplicateResourceException returns 409 CONFLICT")
    void testDuplicateResource() {
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateResource(new DuplicateResourceException("Machine M1 already exists"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Duplicate Resource", response.getBody().getError());
    }

    @Test
    @DisplayName("Verify AccessDeniedException returns 403 FORBIDDEN")
    void testAccessDenied() {
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(new AccessDeniedException("Access is denied"));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Forbidden", response.getBody().getError());
    }

    @Test
    @DisplayName("Verify IllegalArgumentException returns 400 BAD_REQUEST")
    void testIllegalArgument() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(new IllegalArgumentException("Invalid test type"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Bad Request", response.getBody().getError());
    }
}
