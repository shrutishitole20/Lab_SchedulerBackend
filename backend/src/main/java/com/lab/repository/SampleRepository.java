package com.lab.repository;

import com.lab.enums.Priority;
import com.lab.enums.Status;
import com.lab.model.Sample;
import com.lab.util.DBConnection;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Optional;

@Repository
public class SampleRepository {

    private final TestTypeRepository testTypeRepository;
    private final PriorityRepository priorityRepository;

    private static volatile boolean extendedColumnsChecked = false;
    private static volatile boolean hasExtendedColumns = false;

    public SampleRepository() {
        this(new TestTypeRepository(), new PriorityRepository());
    }

    @Autowired
    public SampleRepository(TestTypeRepository testTypeRepository, PriorityRepository priorityRepository) {
        this.testTypeRepository = (testTypeRepository != null) ? testTypeRepository : new TestTypeRepository();
        this.priorityRepository = (priorityRepository != null) ? priorityRepository : new PriorityRepository();
    }

    private synchronized void ensureExtendedColumns(Connection conn) {
        if (extendedColumnsChecked) return;
        try {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "samples", "sample_type")) {
                if (rs.next()) {
                    hasExtendedColumns = true;
                } else {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("ALTER TABLE samples ADD COLUMN sample_type VARCHAR(50) NOT NULL DEFAULT 'Blood'");
                        stmt.executeUpdate("ALTER TABLE samples ADD COLUMN test_category VARCHAR(50) NOT NULL DEFAULT 'General'");
                        stmt.executeUpdate("ALTER TABLE samples ADD COLUMN processing_type VARCHAR(50) NOT NULL DEFAULT 'Routine'");
                        hasExtendedColumns = true;
                    } catch (SQLException ignored) {
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Notice checking extended columns: " + e.getMessage());
        }
        extendedColumnsChecked = true;
    }

    public boolean save(Sample sample) {
        if (sample == null || sample.getId() == null) {
            throw new IllegalArgumentException("Sample and ID cannot be null.");
        }

        String sql;
        try (Connection conn = DBConnection.getConnection()) {
            ensureExtendedColumns(conn);

            if (hasExtendedColumns) {
                sql = "INSERT INTO samples (id, test_type_id, priority_id, received_time, target_minutes, status, machine_id, start_time, completion_time, sample_type, test_category, processing_type) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                      "ON DUPLICATE KEY UPDATE " +
                      "test_type_id = VALUES(test_type_id), priority_id = VALUES(priority_id), target_minutes = VALUES(target_minutes), " +
                      "status = VALUES(status), machine_id = VALUES(machine_id), start_time = VALUES(start_time), completion_time = VALUES(completion_time), " +
                      "sample_type = VALUES(sample_type), test_category = VALUES(test_category), processing_type = VALUES(processing_type)";
            } else {
                sql = "INSERT INTO samples (id, test_type_id, priority_id, received_time, target_minutes, status, machine_id, start_time, completion_time) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                      "ON DUPLICATE KEY UPDATE " +
                      "test_type_id = VALUES(test_type_id), priority_id = VALUES(priority_id), target_minutes = VALUES(target_minutes), " +
                      "status = VALUES(status), machine_id = VALUES(machine_id), start_time = VALUES(start_time), completion_time = VALUES(completion_time)";
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setStatementParams(stmt, sample, hasExtendedColumns);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("SampleRepository.save error for sample " + sample.getId() + ": " + e.getMessage());
            return false;
        }
    }

    public boolean update(Sample sample) {
        if (sample == null || sample.getId() == null) {
            throw new IllegalArgumentException("Sample and ID cannot be null.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            ensureExtendedColumns(conn);

            String sql;
            if (hasExtendedColumns) {
                sql = "UPDATE samples SET test_type_id = ?, priority_id = ?, received_time = ?, target_minutes = ?, " +
                      "status = ?, machine_id = ?, start_time = ?, completion_time = ?, sample_type = ?, test_category = ?, processing_type = ? " +
                      "WHERE id = ?";
            } else {
                sql = "UPDATE samples SET test_type_id = ?, priority_id = ?, received_time = ?, target_minutes = ?, " +
                      "status = ?, machine_id = ?, start_time = ?, completion_time = ? WHERE id = ?";
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int typeId = testTypeRepository.findOrInsert(sample.getTestType());
                int priorityId = priorityRepository.resolvePriorityId(sample.getPriority());

                stmt.setInt(1, typeId);
                stmt.setInt(2, priorityId);
                stmt.setTimestamp(3, sample.getReceivedTime() != null ? Timestamp.valueOf(sample.getReceivedTime()) : Timestamp.valueOf(LocalDateTime.now()));
                stmt.setInt(4, sample.getTargetMinutes());
                stmt.setString(5, sample.getStatus() != null ? sample.getStatus().name() : Status.RECEIVED.name());

                if (sample.getAssignedMachineId() != null) {
                    stmt.setString(6, sample.getAssignedMachineId());
                } else {
                    stmt.setNull(6, Types.VARCHAR);
                }

                if (sample.getStartTime() != null) {
                    stmt.setTimestamp(7, Timestamp.valueOf(sample.getStartTime()));
                } else {
                    stmt.setNull(7, Types.TIMESTAMP);
                }

                if (sample.getCompletionTime() != null) {
                    stmt.setTimestamp(8, Timestamp.valueOf(sample.getCompletionTime()));
                } else {
                    stmt.setNull(8, Types.TIMESTAMP);
                }

                if (hasExtendedColumns) {
                    stmt.setString(9, sample.getSampleType() != null ? sample.getSampleType() : "Blood");
                    stmt.setString(10, sample.getTestCategory() != null ? sample.getTestCategory() : "General");
                    stmt.setString(11, sample.getProcessingType() != null ? sample.getProcessingType() : "Routine");
                    stmt.setString(12, sample.getId());
                } else {
                    stmt.setString(9, sample.getId());
                }

                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("SampleRepository.update error for sample " + sample.getId() + ": " + e.getMessage());
            return false;
        }
    }

    public boolean delete(String id) {
        if (id == null || id.trim().isEmpty()) return false;
        String sql = "DELETE FROM samples WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id.trim());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("SampleRepository.delete error for sample " + id + ": " + e.getMessage());
            return false;
        }
    }

    public Optional<Sample> findById(String id) {
        try (Connection conn = DBConnection.getConnection()) {
            ensureExtendedColumns(conn);
            String sql = buildSelectSql() + " WHERE s.id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRowToSample(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("SampleRepository.findById error for id " + id + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Sample> findAll() {
        List<Sample> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            ensureExtendedColumns(conn);
            String sql = buildSelectSql() + " ORDER BY s.received_time ASC";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToSample(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("SampleRepository.findAll error: " + e.getMessage());
        }
        return list;
    }

    public List<Sample> findByStatus(Status status) {
        return findByStatus(status != null ? status.name() : "");
    }

    public List<Sample> findByStatus(String status) {
        List<Sample> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            ensureExtendedColumns(conn);
            String sql = buildSelectSql() + " WHERE s.status = ? ORDER BY s.received_time ASC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, status);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRowToSample(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("SampleRepository.findByStatus error for status " + status + ": " + e.getMessage());
        }
        return list;
    }

    public int getMaxSampleIdNumber() {
        String sql = "SELECT id FROM samples";
        int maxId = 0;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                if (id != null) {
                    String digits = id.replaceAll("\\D+", "");
                    if (!digits.isEmpty()) {
                        try {
                            maxId = Math.max(maxId, Integer.parseInt(digits));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("SampleRepository.getMaxSampleIdNumber error: " + e.getMessage());
        }
        return maxId;
    }

    private String buildSelectSql() {
        if (hasExtendedColumns) {
            return "SELECT s.id, s.received_time, s.target_minutes, s.status, s.machine_id, s.start_time, s.completion_time, " +
                   "s.sample_type, s.test_category, s.processing_type, " +
                   "tt.name AS test_type_name, p.rank_order AS priority_rank " +
                   "FROM samples s " +
                   "LEFT JOIN test_types tt ON s.test_type_id = tt.id " +
                   "LEFT JOIN priorities p ON s.priority_id = p.id";
        }
        return "SELECT s.id, s.received_time, s.target_minutes, s.status, s.machine_id, s.start_time, s.completion_time, " +
               "tt.name AS test_type_name, p.rank_order AS priority_rank " +
               "FROM samples s " +
               "LEFT JOIN test_types tt ON s.test_type_id = tt.id " +
               "LEFT JOIN priorities p ON s.priority_id = p.id";
    }

    private void setStatementParams(PreparedStatement stmt, Sample sample, boolean includeExtended) throws SQLException {
        stmt.setString(1, sample.getId());
        stmt.setInt(2, testTypeRepository.findOrInsert(sample.getTestType()));
        stmt.setInt(3, priorityRepository.resolvePriorityId(sample.getPriority()));

        Timestamp receivedTs = sample.getReceivedTime() != null ?
                Timestamp.valueOf(sample.getReceivedTime()) :
                Timestamp.valueOf(LocalDateTime.now());
        stmt.setTimestamp(4, receivedTs);

        stmt.setInt(5, sample.getTargetMinutes());
        stmt.setString(6, sample.getStatus() != null ? sample.getStatus().name() : Status.RECEIVED.name());

        if (sample.getAssignedMachineId() != null) {
            stmt.setString(7, sample.getAssignedMachineId());
        } else {
            stmt.setNull(7, Types.VARCHAR);
        }

        if (sample.getStartTime() != null) {
            stmt.setTimestamp(8, Timestamp.valueOf(sample.getStartTime()));
        } else {
            stmt.setNull(8, Types.TIMESTAMP);
        }

        if (sample.getCompletionTime() != null) {
            stmt.setTimestamp(9, Timestamp.valueOf(sample.getCompletionTime()));
        } else {
            stmt.setNull(9, Types.TIMESTAMP);
        }

        if (includeExtended) {
            stmt.setString(10, sample.getSampleType() != null ? sample.getSampleType() : "Blood");
            stmt.setString(11, sample.getTestCategory() != null ? sample.getTestCategory() : "General");
            stmt.setString(12, sample.getProcessingType() != null ? sample.getProcessingType() : "Routine");
        }
    }

    private Sample mapRowToSample(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String testType = rs.getString("test_type_name");
        if (testType == null) {
            testType = "Blood";
        }

        int priorityRank = rs.getInt("priority_rank");
        Priority priority = Priority.fromRank(priorityRank);

        Timestamp receivedTs = rs.getTimestamp("received_time");
        LocalDateTime receivedTime = receivedTs != null ? receivedTs.toLocalDateTime() : LocalDateTime.now();

        int targetMinutes = rs.getInt("target_minutes");
        Sample sample = new Sample(id, testType, priority, receivedTime, targetMinutes);

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                sample.forceStatus(Status.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                sample.forceStatus(Status.RECEIVED);
            }
        }

        sample.setAssignedMachineId(rs.getString("machine_id"));

        Timestamp startTs = rs.getTimestamp("start_time");
        if (startTs != null) {
            sample.setStartTime(startTs.toLocalDateTime());
        }

        Timestamp compTs = rs.getTimestamp("completion_time");
        if (compTs != null) {
            sample.setCompletionTime(compTs.toLocalDateTime());
        }

        if (hasExtendedColumns) {
            try {
                String st = rs.getString("sample_type");
                if (st != null && !st.trim().isEmpty()) {
                    sample.setSampleType(st);
                }
                String tc = rs.getString("test_category");
                if (tc != null && !tc.trim().isEmpty()) {
                    sample.setTestCategory(tc);
                }
                String pt = rs.getString("processing_type");
                if (pt != null && !pt.trim().isEmpty()) {
                    sample.setProcessingType(pt);
                }
            } catch (SQLException ignored) {}
        }

        return sample;
    }
}
