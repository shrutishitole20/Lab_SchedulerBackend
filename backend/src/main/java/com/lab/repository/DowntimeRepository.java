package com.lab.repository;

import com.lab.model.MachineDowntime;
import com.lab.util.DBConnection;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class DowntimeRepository {

    private static volatile boolean tableInitialized = false;

    public DowntimeRepository() {
        ensureTable();
    }

    public synchronized void ensureTable() {
        if (tableInitialized) return;
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return;
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(conn.getCatalog(), null, "machine_downtime", new String[]{"TABLE"})) {
                if (!rs.next()) {
                    String createSql = "CREATE TABLE IF NOT EXISTS machine_downtime (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "machine_id VARCHAR(50) NOT NULL, " +
                            "down_start DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                            "down_end DATETIME NULL, " +
                            "reason VARCHAR(255) NOT NULL, " +
                            "reported_by VARCHAR(50) NULL DEFAULT 'SYSTEM', " +
                            "CONSTRAINT fk_downtime_machine FOREIGN KEY (machine_id) REFERENCES machines (id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                            "INDEX idx_downtime_machine (machine_id), " +
                            "INDEX idx_downtime_active (machine_id, down_end)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate(createSql);
                        System.out.println("[DATABASE] Successfully ensured table 'machine_downtime'.");
                    }
                }
            }
            tableInitialized = true;
        } catch (SQLException e) {
            System.err.println("Notice ensuring machine_downtime table: " + e.getMessage());
        }
    }

    public boolean recordDowntime(String machineId, String reason, String reportedBy) {
        ensureTable();
        String sql = "INSERT INTO machine_downtime (machine_id, down_start, reason, reported_by) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, machineId);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(3, reason != null ? reason : "Unspecified downtime / maintenance");
            stmt.setString(4, reportedBy != null ? reportedBy : "SYSTEM");

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("DowntimeRepository.recordDowntime error for machine " + machineId + ": " + e.getMessage());
            return false;
        }
    }

    public boolean resolveDowntime(String machineId) {
        ensureTable();
        String sql = "UPDATE machine_downtime SET down_end = ? WHERE machine_id = ? AND down_end IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(2, machineId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("DowntimeRepository.resolveDowntime error for machine " + machineId + ": " + e.getMessage());
            return false;
        }
    }

    public Optional<MachineDowntime> findActiveByMachineId(String machineId) {
        ensureTable();
        String sql = "SELECT id, machine_id, down_start, down_end, reason, reported_by FROM machine_downtime " +
                     "WHERE machine_id = ? AND down_end IS NULL ORDER BY down_start DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, machineId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToDowntime(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("DowntimeRepository.findActiveByMachineId error: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<MachineDowntime> findAll() {
        ensureTable();
        List<MachineDowntime> list = new ArrayList<>();
        String sql = "SELECT id, machine_id, down_start, down_end, reason, reported_by FROM machine_downtime ORDER BY down_start DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToDowntime(rs));
            }
        } catch (SQLException e) {
            System.err.println("DowntimeRepository.findAll error: " + e.getMessage());
        }
        return list;
    }

    private MachineDowntime mapRowToDowntime(ResultSet rs) throws SQLException {
        Timestamp startTs = rs.getTimestamp("down_start");
        Timestamp endTs = rs.getTimestamp("down_end");

        return new MachineDowntime(
                rs.getInt("id"),
                rs.getString("machine_id"),
                startTs != null ? startTs.toLocalDateTime() : LocalDateTime.now(),
                endTs != null ? endTs.toLocalDateTime() : null,
                rs.getString("reason"),
                rs.getString("reported_by")
        );
    }
}
