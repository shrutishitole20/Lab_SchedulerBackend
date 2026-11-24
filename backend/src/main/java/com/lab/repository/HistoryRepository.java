package com.lab.repository;

import com.lab.model.HistoryEntry;
import com.lab.util.DBConnection;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class HistoryRepository {

    public boolean save(HistoryEntry entry) {
        String sql = "INSERT INTO sample_history (sample_id, action, details, created_at) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, entry.getSampleId());
            stmt.setString(2, entry.getAction());
            stmt.setString(3, entry.getDetails());
            stmt.setTimestamp(4, entry.getTimestamp() != null ? Timestamp.valueOf(entry.getTimestamp()) : Timestamp.valueOf(LocalDateTime.now()));

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("HistoryRepository.save error for sample " + entry.getSampleId() + ": " + e.getMessage());
            return false;
        }
    }

    public boolean update(int id, HistoryEntry entry) {
        String sql = "UPDATE sample_history SET action = ?, details = ?, created_at = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, entry.getAction());
            stmt.setString(2, entry.getDetails());
            stmt.setTimestamp(3, entry.getTimestamp() != null ? Timestamp.valueOf(entry.getTimestamp()) : Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(4, id);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("HistoryRepository.update error for id " + id + ": " + e.getMessage());
            return false;
        }
    }

    public boolean update(HistoryEntry entry) {
        String sql = "UPDATE sample_history SET action = ?, details = ?, created_at = ? WHERE sample_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, entry.getAction());
            stmt.setString(2, entry.getDetails());
            stmt.setTimestamp(3, entry.getTimestamp() != null ? Timestamp.valueOf(entry.getTimestamp()) : Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(4, entry.getSampleId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("HistoryRepository.update error for sample " + entry.getSampleId() + ": " + e.getMessage());
            return false;
        }
    }

    public Optional<HistoryEntry> findById(int id) {
        String sql = "SELECT id, sample_id, action, details, created_at FROM sample_history WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToHistory(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("HistoryRepository.findById error for id " + id + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<HistoryEntry> findAll() {
        List<HistoryEntry> list = new ArrayList<>();
        String sql = "SELECT id, sample_id, action, details, created_at FROM sample_history ORDER BY created_at ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToHistory(rs));
            }
        } catch (SQLException e) {
            System.err.println("HistoryRepository.findAll error: " + e.getMessage());
        }
        return list;
    }

    public List<HistoryEntry> findByStatus(String status) {
        List<HistoryEntry> list = new ArrayList<>();
        String sql = "SELECT id, sample_id, action, details, created_at FROM sample_history WHERE action = ? ORDER BY created_at ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToHistory(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("HistoryRepository.findByStatus error for status " + status + ": " + e.getMessage());
        }
        return list;
    }

    public List<HistoryEntry> findBySampleId(String sampleId) {
        List<HistoryEntry> list = new ArrayList<>();
        String sql = "SELECT id, sample_id, action, details, created_at FROM sample_history WHERE sample_id = ? ORDER BY created_at ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sampleId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToHistory(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("HistoryRepository.findBySampleId error for sample " + sampleId + ": " + e.getMessage());
        }
        return list;
    }

    private HistoryEntry mapRowToHistory(ResultSet rs) throws SQLException {
        String sampleId = rs.getString("sample_id");
        String action = rs.getString("action");
        String details = rs.getString("details");
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime timestamp = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        return new HistoryEntry(sampleId, timestamp, action, details);
    }
}
