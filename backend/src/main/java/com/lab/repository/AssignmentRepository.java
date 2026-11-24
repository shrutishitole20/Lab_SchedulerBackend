package com.lab.repository;

import com.lab.model.SampleAssignment;
import com.lab.util.DBConnection;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AssignmentRepository {

    private static volatile boolean tableInitialized = false;

    public AssignmentRepository() {
        ensureTable();
    }

    public synchronized void ensureTable() {
        if (tableInitialized) return;
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return;
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(conn.getCatalog(), null, "sample_assignments", new String[]{"TABLE"})) {
                if (!rs.next()) {
                    String createSql = "CREATE TABLE IF NOT EXISTS sample_assignments (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "sample_id VARCHAR(50) NOT NULL, " +
                            "machine_id VARCHAR(50) NOT NULL, " +
                            "assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                            "started_at DATETIME NULL, " +
                            "completed_at DATETIME NULL, " +
                            "status ENUM('ASSIGNED', 'PROCESSING', 'COMPLETED', 'CANCELLED', 'REASSIGNED') NOT NULL DEFAULT 'ASSIGNED', " +
                            "notes VARCHAR(255) NULL, " +
                            "CONSTRAINT fk_assign_sample FOREIGN KEY (sample_id) REFERENCES samples (id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                            "CONSTRAINT fk_assign_machine FOREIGN KEY (machine_id) REFERENCES machines (id) ON DELETE RESTRICT ON UPDATE CASCADE, " +
                            "INDEX idx_assign_sample (sample_id), " +
                            "INDEX idx_assign_machine (machine_id), " +
                            "INDEX idx_assign_status (status)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate(createSql);
                        System.out.println("[DATABASE] Successfully ensured table 'sample_assignments'.");
                    }
                }
            }
            tableInitialized = true;
        } catch (SQLException e) {
            System.err.println("Notice ensuring sample_assignments table: " + e.getMessage());
        }
    }

    public boolean recordAssignment(String sampleId, String machineId, String status, String notes) {
        ensureTable();
        String sql = "INSERT INTO sample_assignments (sample_id, machine_id, assigned_at, status, notes) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sampleId);
            stmt.setString(2, machineId);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(4, status != null ? status : "ASSIGNED");
            stmt.setString(5, notes);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("AssignmentRepository.recordAssignment error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStatus(String sampleId, String machineId, String newStatus, LocalDateTime startedAt, LocalDateTime completedAt) {
        ensureTable();
        String sql = "UPDATE sample_assignments SET status = ?, started_at = COALESCE(?, started_at), completed_at = COALESCE(?, completed_at) " +
                     "WHERE sample_id = ? AND machine_id = ? AND status IN ('ASSIGNED', 'PROCESSING')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus);
            if (startedAt != null) {
                stmt.setTimestamp(2, Timestamp.valueOf(startedAt));
            } else {
                stmt.setNull(2, Types.TIMESTAMP);
            }

            if (completedAt != null) {
                stmt.setTimestamp(3, Timestamp.valueOf(completedAt));
            } else {
                stmt.setNull(3, Types.TIMESTAMP);
            }

            stmt.setString(4, sampleId);
            stmt.setString(5, machineId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("AssignmentRepository.updateStatus error: " + e.getMessage());
            return false;
        }
    }

    public List<SampleAssignment> findBySampleId(String sampleId) {
        ensureTable();
        List<SampleAssignment> list = new ArrayList<>();
        String sql = "SELECT id, sample_id, machine_id, assigned_at, started_at, completed_at, status, notes " +
                     "FROM sample_assignments WHERE sample_id = ? ORDER BY assigned_at ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sampleId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp assignTs = rs.getTimestamp("assigned_at");
                    Timestamp startTs = rs.getTimestamp("started_at");
                    Timestamp compTs = rs.getTimestamp("completed_at");

                    list.add(new SampleAssignment(
                            rs.getInt("id"),
                            rs.getString("sample_id"),
                            rs.getString("machine_id"),
                            assignTs != null ? assignTs.toLocalDateTime() : null,
                            startTs != null ? startTs.toLocalDateTime() : null,
                            compTs != null ? compTs.toLocalDateTime() : null,
                            rs.getString("status"),
                            rs.getString("notes")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("AssignmentRepository.findBySampleId error: " + e.getMessage());
        }
        return list;
    }
}
