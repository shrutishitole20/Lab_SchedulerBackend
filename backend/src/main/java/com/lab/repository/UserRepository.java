package com.lab.repository;

import com.lab.model.User;
import com.lab.util.DBConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    public boolean save(User user) {
        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getRole() != null ? user.getRole() : "STAFF");

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("UserRepository.save error for username " + user.getUsername() + ": " + e.getMessage());
        }
        return false;
    }

    public boolean update(User user) {
        String sql = "UPDATE users SET username = ?, password_hash = ?, role = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getRole());
            stmt.setInt(4, user.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserRepository.update error for user id " + user.getId() + ": " + e.getMessage());
            return false;
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserRepository.delete error for id " + id + ": " + e.getMessage());
            return false;
        }
    }

    public Optional<User> findById(int id) {
        String sql = "SELECT id, username, password_hash, role FROM users WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("UserRepository.findById error for id " + id + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, role FROM users WHERE username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("UserRepository.findByUsername error for " + username + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, password_hash, role FROM users ORDER BY id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("UserRepository.findAll error: " + e.getMessage());
        }
        return list;
    }

    public List<User> findByStatus(String status) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, password_hash, role FROM users WHERE role = ? ORDER BY id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("UserRepository.findByStatus error for role " + status + ": " + e.getMessage());
        }
        return list;
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("role")
        );
    }

    public void ensureDefaultAccounts(PasswordEncoder encoder) {
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return;

            String envAdminPass = System.getenv("ADMIN_PASSWORD");
            String envStaffPass = System.getenv("STAFF_PASSWORD");

            String adminPlain = (envAdminPass != null && !envAdminPass.isBlank()) ? envAdminPass : "admin123";
            String staffPlain = (envStaffPass != null && !envStaffPass.isBlank()) ? envStaffPass : "staff123";

            String adminHash = encoder.encode(adminPlain);
            String staffHash = encoder.encode(staffPlain);

            Optional<User> adminOpt = findByUsername("admin");
            if (adminOpt.isPresent()) {
                if (!encoder.matches(adminPlain, adminOpt.get().getPasswordHash())) {
                    try (PreparedStatement stmt = conn.prepareStatement("UPDATE users SET password_hash = ?, role = 'ADMIN' WHERE username = 'admin'")) {
                        stmt.setString(1, adminHash);
                        stmt.executeUpdate();
                        System.out.println("[SECURITY] Auto-healed admin password hash in MySQL.");
                    }
                }
            } else {
                try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password_hash, role) VALUES ('admin', ?, 'ADMIN')")) {
                    stmt.setString(1, adminHash);
                    stmt.executeUpdate();
                    System.out.println("[SECURITY] Seeded default admin user into MySQL.");
                }
            }

            Optional<User> staffOpt = findByUsername("staff");
            if (staffOpt.isPresent()) {
                if (!encoder.matches(staffPlain, staffOpt.get().getPasswordHash())) {
                    try (PreparedStatement stmt = conn.prepareStatement("UPDATE users SET password_hash = ?, role = 'STAFF' WHERE username = 'staff'")) {
                        stmt.setString(1, staffHash);
                        stmt.executeUpdate();
                        System.out.println("[SECURITY] Auto-healed staff password hash in MySQL.");
                    }
                }
            } else {
                try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password_hash, role) VALUES ('staff', ?, 'STAFF')")) {
                    stmt.setString(1, staffHash);
                    stmt.executeUpdate();
                    System.out.println("[SECURITY] Seeded default staff user into MySQL.");
                }
            }
        } catch (SQLException e) {
            System.err.println("UserRepository.ensureDefaultAccounts warning: " + e.getMessage());
        }
    }
}
