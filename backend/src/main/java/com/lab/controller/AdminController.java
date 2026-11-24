package com.lab.controller;

import com.lab.dto.ApiResponse;
import com.lab.dto.PriorityRequest;
import com.lab.dto.TestTypeRequest;
import com.lab.dto.UserRequest;
import com.lab.dto.UserResponse;
import com.lab.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AdminController {

    private final AdminService adminService;

    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/priorities")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPriorities() {
        List<Map<String, Object>> priorities = adminService.getAllPriorities();
        return ResponseEntity.ok(ApiResponse.ok("Priorities retrieved successfully", priorities));
    }

    @PostMapping("/priorities")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPriority(@Valid @RequestBody PriorityRequest request) {
        Map<String, Object> data = adminService.createPriority(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Priority created successfully (Admin)", data));
    }

    @PutMapping("/priorities/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePriority(@PathVariable int id,
                                                                          @Valid @RequestBody PriorityRequest request) {
        Map<String, Object> data = adminService.updatePriority(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Priority updated successfully (Admin)", data));
    }

    @DeleteMapping("/priorities/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deletePriority(@PathVariable int id) {
        adminService.deletePriority(id);
        return ResponseEntity.ok(ApiResponse.ok("Priority tier deleted successfully", "Deleted ID: " + id));
    }

    @GetMapping("/test-types")
    public ResponseEntity<ApiResponse<List<String>>> getTestTypes() {
        List<String> types = adminService.getAllTestTypes();
        return ResponseEntity.ok(ApiResponse.ok("Test types retrieved successfully", types));
    }

    @PostMapping("/test-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTestType(@Valid @RequestBody TestTypeRequest request) {
        Map<String, Object> data = adminService.createTestType(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Test Type registered successfully (Admin)", data));
    }

    @PutMapping("/test-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateTestType(@PathVariable int id,
                                                                          @Valid @RequestBody TestTypeRequest request) {
        Map<String, Object> data = adminService.updateTestType(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Test Type updated successfully (Admin)", data));
    }

    @DeleteMapping("/test-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteTestType(@PathVariable int id) {
        adminService.deleteTestType(id);
        return ResponseEntity.ok(ApiResponse.ok("Test category deleted successfully", "Deleted ID: " + id));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers() {
        List<UserResponse> safeUsers = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved successfully", safeUsers));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String role = request.get("role");

        UserResponse user = adminService.createUser(username, password, role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User created successfully", user));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable int id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User deleted successfully", "Deleted ID: " + id));
    }
}
