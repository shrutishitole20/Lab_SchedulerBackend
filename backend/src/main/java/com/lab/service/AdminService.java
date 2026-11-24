package com.lab.service;

import com.lab.dto.PriorityRequest;
import com.lab.dto.TestTypeRequest;
import com.lab.dto.UserResponse;
import com.lab.exception.DuplicateResourceException;
import com.lab.exception.ResourceNotFoundException;
import com.lab.model.User;
import com.lab.repository.PriorityRepository;
import com.lab.repository.TestTypeRepository;
import com.lab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final PriorityRepository priorityRepository;
    private final TestTypeRepository testTypeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AdminService(PriorityRepository priorityRepository,
                        TestTypeRepository testTypeRepository,
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder) {
        this.priorityRepository = (priorityRepository != null) ? priorityRepository : new PriorityRepository();
        this.testTypeRepository = (testTypeRepository != null) ? testTypeRepository : new TestTypeRepository();
        this.userRepository = (userRepository != null) ? userRepository : new UserRepository();
        this.passwordEncoder = passwordEncoder;
    }

    public List<Map<String, Object>> getAllPriorities() {
        return priorityRepository.findAll();
    }

    public Map<String, Object> createPriority(PriorityRequest request) {
        boolean success = priorityRepository.create(request.getName(), request.getRankOrder(), request.getTargetMinutes());
        if (!success) {
            throw new IllegalArgumentException("Failed to create priority: " + request.getName());
        }
        return Map.of(
                "name", request.getName().trim().toUpperCase(),
                "rankOrder", request.getRankOrder(),
                "targetMinutes", request.getTargetMinutes()
        );
    }

    public Map<String, Object> updatePriority(int id, PriorityRequest request) {
        boolean success = priorityRepository.update(id, request.getName(), request.getRankOrder(), request.getTargetMinutes());
        if (!success) {
            throw new ResourceNotFoundException("Priority with ID " + id + " not found or cannot be updated.");
        }
        return Map.of(
                "id", id,
                "name", request.getName().trim().toUpperCase(),
                "rankOrder", request.getRankOrder(),
                "targetMinutes", request.getTargetMinutes()
        );
    }

    public void deletePriority(int id) {
        boolean success = priorityRepository.delete(id);
        if (!success) {
            throw new ResourceNotFoundException("Priority with ID " + id + " not found.");
        }
    }

    public List<String> getAllTestTypes() {
        return testTypeRepository.findAllNames();
    }

    public Map<String, Object> createTestType(TestTypeRequest request) {
        int id = testTypeRepository.findOrInsert(request.getName().trim());
        return Map.of(
                "id", id,
                "name", request.getName().trim()
        );
    }

    public Map<String, Object> updateTestType(int id, TestTypeRequest request) {
        boolean success = testTypeRepository.update(id, request.getName().trim());
        if (!success) {
            throw new ResourceNotFoundException("Test Type with ID " + id + " not found.");
        }
        return Map.of("id", id, "name", request.getName().trim());
    }

    public void deleteTestType(int id) {
        boolean success = testTypeRepository.delete(id);
        if (!success) {
            throw new ResourceNotFoundException("Test category with ID " + id + " not found.");
        }
    }

    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserResponse> safeUsers = new ArrayList<>();
        for (User u : users) {
            safeUsers.add(new UserResponse(
                    u.getId(),
                    u.getUsername(),
                    u.getRole() != null ? u.getRole() : "STAFF"
            ));
        }
        return safeUsers;
    }

    public UserResponse createUser(String username, String password, String role) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Username and password are required.");
        }

        String cleanUsername = username.trim();
        if (userRepository.findByUsername(cleanUsername).isPresent()) {
            throw new DuplicateResourceException("Username '" + cleanUsername + "' is already taken.");
        }

        String cleanRole = (role != null && role.trim().toUpperCase().contains("ADMIN")) ? "ADMIN" : "STAFF";
        String encodedHash = (passwordEncoder != null) ? passwordEncoder.encode(password.trim()) : password.trim();

        User user = new User(cleanUsername, encodedHash, cleanRole);
        boolean saved = userRepository.save(user);
        if (!saved) {
            throw new IllegalArgumentException("Could not create user in database.");
        }

        return new UserResponse(user.getId(), user.getUsername(), user.getRole());
    }

    public void deleteUser(int id) {
        boolean deleted = userRepository.delete(id);
        if (!deleted) {
            throw new ResourceNotFoundException("User with ID " + id + " not found or cannot be deleted.");
        }
    }
}
