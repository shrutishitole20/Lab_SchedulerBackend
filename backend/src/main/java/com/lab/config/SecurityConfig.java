package com.lab.config;

import com.lab.model.User;
import com.lab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Spring Security Configuration with Role-Based Access Control:
 * - Public: Static assets, /api/auth/login, /api/auth/me
 * - ADMIN: Full access (create/update/delete machines, priorities, test types, and manage users)
 * - STAFF: Daily operations (registering samples, viewing queue/machines, completing/holding samples)
 *
 * CORS allowed origins are configured via the {@code cors.allowed.origins} property in
 * application.properties, which supports the {@code CORS_ALLOWED_ORIGINS} environment variable.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Comma-separated allowed CORS origins injected from application.properties.
     * Override at runtime with the CORS_ALLOWED_ORIGINS environment variable.
     * Example: CORS_ALLOWED_ORIGINS=http://localhost:3000,https://myapp.example.com
     */
    @Value("${cors.allowed.origins:http://localhost:3000,http://127.0.0.1:3000}")
    private String corsAllowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/", "/index.html", "/*.html", "/*.css", "/*.js", "/css/**", "/js/**", "/favicon.ico", "/static/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()

                // ADMIN-only endpoints
                .requestMatchers(HttpMethod.POST, "/api/machines").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/machines/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/machines/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/priorities/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/priorities/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/priorities/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/test-types/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/test-types/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/test-types/**").hasRole("ADMIN")
                .requestMatchers("/api/users/**").hasRole("ADMIN")

                // STAFF & ADMIN shared access
                .requestMatchers(HttpMethod.GET, "/api/priorities/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.GET, "/api/test-types/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/api/**").hasAnyRole("ADMIN", "STAFF")

                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse comma-separated origins from application.properties / CORS_ALLOWED_ORIGINS env var
        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository, PasswordEncoder encoder) {
        try {
            userRepository.ensureDefaultAccounts(encoder);
        } catch (Exception e) {
            System.err.println("[SECURITY] Could not verify default accounts on startup: " + e.getMessage());
        }

        return username -> {
            try {
                Optional<User> dbUser = userRepository.findByUsername(username);
                if (dbUser.isPresent()) {
                    User u = dbUser.get();
                    String role = (u.getRole() != null) ? u.getRole().replace("ROLE_", "") : "STAFF";
                    if ("ADMIN".equalsIgnoreCase(role)) {
                        return org.springframework.security.core.userdetails.User.builder()
                                .username(u.getUsername())
                                .password(u.getPasswordHash())
                                .roles("ADMIN", "STAFF")
                                .build();
                    } else {
                        return org.springframework.security.core.userdetails.User.builder()
                                .username(u.getUsername())
                                .password(u.getPasswordHash())
                                .roles("STAFF")
                                .build();
                    }
                }
            } catch (Exception e) {
                System.err.println("Notice: Could not load user from DB, falling back to in-memory: " + e.getMessage());
            }

            String envAdminPass = System.getenv("ADMIN_PASSWORD");
            String envStaffPass = System.getenv("STAFF_PASSWORD");
            String adminPlain = (envAdminPass != null && !envAdminPass.isBlank()) ? envAdminPass : "admin123";
            String staffPlain = (envStaffPass != null && !envStaffPass.isBlank()) ? envStaffPass : "staff123";

            if ("admin".equalsIgnoreCase(username)) {
                return org.springframework.security.core.userdetails.User.builder()
                        .username("admin")
                        .password(encoder.encode(adminPlain))
                        .roles("ADMIN", "STAFF")
                        .build();
            } else if ("staff".equalsIgnoreCase(username) || "staff1".equalsIgnoreCase(username)) {
                return org.springframework.security.core.userdetails.User.builder()
                        .username(username)
                        .password(encoder.encode(staffPlain))
                        .roles("STAFF")
                        .build();
            }

            throw new UsernameNotFoundException("User not found: " + username);
        };
    }
}
