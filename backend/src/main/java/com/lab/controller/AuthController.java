package com.lab.controller;

import com.lab.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || password == null) {
            throw new IllegalArgumentException("Username and password must be provided.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_STAFF");

        Map<String, Object> result = Map.of(
                "username", userDetails.getUsername(),
                "role", role,
                "message", "Login successful. Use HTTP Basic Auth header for API calls."
        );

        return ResponseEntity.ok(ApiResponse.ok("Authentication successful", result));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(ApiResponse.ok("Unauthenticated", Map.of("authenticated", false)));
        }

        Map<String, Object> profile = Map.of(
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList(),
                "authenticated", authentication.isAuthenticated()
        );
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved", profile));
    }
}
