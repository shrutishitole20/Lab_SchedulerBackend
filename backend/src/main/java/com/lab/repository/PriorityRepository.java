package com.lab.repository;

import com.lab.enums.Priority;
import com.lab.util.DBConnection;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PriorityRepository {

    public int resolvePriorityId(Priority priority) {
        if (priority == null) {
            return 3;
        }
        return findOrInsert(priority.name(), priority.getRank(), priority.getDefaultTargetMinutes());
    }

    public int findOrInsert(String name, int rankOrder, int targetMinutes) {
        if (name == null || name.trim().isEmpty()) {
            return 3;
        }
        String cleanName = name.trim().toUpperCase();

        String selectSql = "SELECT id FROM priorities WHERE UPPER(name) = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSql)) {

            stmt.setString(1, cleanName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }

            String insertSql = "INSERT INTO priorities (name, rank_order, target_minutes) VALUES (?, ?, ?)";
            try (PreparedStatement insStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insStmt.setString(1, cleanName);
                insStmt.setInt(2, rankOrder);
                insStmt.setInt(3, targetMinutes);
                insStmt.executeUpdate();
                try (ResultSet keys = insStmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("PriorityRepository error resolving '" + name + "': " + e.getMessage());
        }
        return rankOrder;
    }

    public boolean create(String name, int rankOrder, int targetMinutes) {
        String sql = "INSERT INTO priorities (name, rank_order, target_minutes) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name.trim().toUpperCase());
            stmt.setInt(2, rankOrder);
            stmt.setInt(3, targetMinutes);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("PriorityRepository.create error: " + e.getMessage());
            return false;
        }
    }

    public boolean update(int id, String name, int rankOrder, int targetMinutes) {
        String sql = "UPDATE priorities SET name = ?, rank_order = ?, target_minutes = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name.trim().toUpperCase());
            stmt.setInt(2, rankOrder);
            stmt.setInt(3, targetMinutes);
            stmt.setInt(4, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("PriorityRepository.update error for id " + id + ": " + e.getMessage());
            return false;
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM priorities WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("PriorityRepository.delete error for id " + id + ": " + e.getMessage());
            return false;
        }
    }

    public Optional<Map<String, Object>> findById(int id) {
        String sql = "SELECT id, name, rank_order, target_minutes FROM priorities WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("name", rs.getString("name"));
                    row.put("rankOrder", rs.getInt("rank_order"));
                    row.put("targetMinutes", rs.getInt("target_minutes"));
                    return Optional.of(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("PriorityRepository.findById error: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Map<String, Object>> findAll() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT id, name, rank_order, target_minutes FROM priorities ORDER BY rank_order ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("name", rs.getString("name"));
                row.put("rankOrder", rs.getInt("rank_order"));
                row.put("targetMinutes", rs.getInt("target_minutes"));
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println("PriorityRepository.findAll error: " + e.getMessage());
        }
        return list;
    }
}
