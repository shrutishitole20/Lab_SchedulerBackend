package com.lab.repository;

import com.lab.model.Machine;
import com.lab.util.DBConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class MachineRepository {

    private final TestTypeRepository testTypeRepository;

    public MachineRepository() {
        this(new TestTypeRepository());
    }

    @Autowired
    public MachineRepository(TestTypeRepository testTypeRepository) {
        this.testTypeRepository = (testTypeRepository != null) ? testTypeRepository : new TestTypeRepository();
    }

    public boolean save(Machine machine) {
        String insertMachineSql = "INSERT INTO machines (id, name, capacity, is_available) VALUES (?, ?, ?, ?) " +
                                  "ON DUPLICATE KEY UPDATE name = VALUES(name), capacity = VALUES(capacity), is_available = VALUES(is_available)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertMachineSql)) {

            stmt.setString(1, machine.getId());
            stmt.setString(2, machine.getName());
            stmt.setInt(3, machine.getCapacity());
            stmt.setBoolean(4, machine.isAvailable());
            stmt.executeUpdate();

            saveMachineTestTypes(conn, machine);
            return true;
        } catch (SQLException e) {
            System.err.println("MachineRepository.save error for machine " + machine.getId() + ": " + e.getMessage());
            return false;
        }
    }

    public boolean update(Machine machine) {
        String updateSql = "UPDATE machines SET name = ?, capacity = ?, is_available = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {

            stmt.setString(1, machine.getName());
            stmt.setInt(2, machine.getCapacity());
            stmt.setBoolean(3, machine.isAvailable());
            stmt.setString(4, machine.getId());

            int affected = stmt.executeUpdate();
            saveMachineTestTypes(conn, machine);
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("MachineRepository.update error for machine " + machine.getId() + ": " + e.getMessage());
            return false;
        }
    }

    public boolean delete(String id) {
        String sql = "DELETE FROM machines WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("MachineRepository.delete error for machine " + id + ": " + e.getMessage());
            return false;
        }
    }

    public Optional<Machine> findById(String id) {
        String sql = "SELECT m.id, m.name, m.capacity, m.is_available, tt.name AS test_type_name " +
                     "FROM machines m " +
                     "LEFT JOIN machine_test_types mtt ON m.id = mtt.machine_id " +
                     "LEFT JOIN test_types tt ON mtt.test_type_id = tt.id " +
                     "WHERE m.id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                Machine machine = null;
                Set<String> supported = new HashSet<>();

                while (rs.next()) {
                    if (machine == null) {
                        machine = new Machine(
                                rs.getString("id"),
                                rs.getString("name"),
                                new HashSet<>(),
                                rs.getInt("capacity"),
                                0,
                                rs.getBoolean("is_available")
                        );
                    }
                    String testName = rs.getString("test_type_name");
                    if (testName != null) {
                        supported.add(testName);
                    }
                }

                if (machine != null) {
                    machine.setSupportedTestTypes(supported);
                    return Optional.of(machine);
                }
            }
        } catch (SQLException e) {
            System.err.println("MachineRepository.findById error for id " + id + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Machine> findAll() {
        return queryMachines("SELECT m.id, m.name, m.capacity, m.is_available, tt.name AS test_type_name " +
                             "FROM machines m " +
                             "LEFT JOIN machine_test_types mtt ON m.id = mtt.machine_id " +
                             "LEFT JOIN test_types tt ON mtt.test_type_id = tt.id " +
                             "ORDER BY m.id ASC", null);
    }

    public List<Machine> findByStatus(boolean isAvailable) {
        return queryMachines("SELECT m.id, m.name, m.capacity, m.is_available, tt.name AS test_type_name " +
                             "FROM machines m " +
                             "LEFT JOIN machine_test_types mtt ON m.id = mtt.machine_id " +
                             "LEFT JOIN test_types tt ON mtt.test_type_id = tt.id " +
                             "WHERE m.is_available = ? " +
                             "ORDER BY m.id ASC", isAvailable ? 1 : 0);
    }

    public List<Machine> findByStatus(String status) {
        boolean isAvailable = "AVAILABLE".equalsIgnoreCase(status) || "UP".equalsIgnoreCase(status) || "TRUE".equalsIgnoreCase(status);
        return findByStatus(isAvailable);
    }

    private List<Machine> queryMachines(String sql, Integer statusFilter) {
        Map<String, Machine> machineMap = new LinkedHashMap<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (statusFilter != null) {
                stmt.setInt(1, statusFilter);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    Machine machine = machineMap.computeIfAbsent(id, k -> {
                        try {
                            return new Machine(
                                    k,
                                    rs.getString("name"),
                                    new HashSet<>(),
                                    rs.getInt("capacity"),
                                    0,
                                    rs.getBoolean("is_available")
                            );
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    String testName = rs.getString("test_type_name");
                    if (testName != null) {
                        machine.getSupportedTestTypes().add(testName);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("MachineRepository query error: " + e.getMessage());
        }

        return new ArrayList<>(machineMap.values());
    }

    private void saveMachineTestTypes(Connection conn, Machine machine) {
        if (machine.getSupportedTestTypes() == null || machine.getSupportedTestTypes().isEmpty()) {
            return;
        }

        String deleteSql = "DELETE FROM machine_test_types WHERE machine_id = ?";
        String insertSql = "INSERT IGNORE INTO machine_test_types (machine_id, test_type_id) VALUES (?, ?)";

        boolean originalAutoCommit = true;
        try {
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try (PreparedStatement delStmt = conn.prepareStatement(deleteSql);
                 PreparedStatement insStmt = conn.prepareStatement(insertSql)) {

                delStmt.setString(1, machine.getId());
                delStmt.executeUpdate();

                for (String testType : machine.getSupportedTestTypes()) {
                    int typeId = testTypeRepository.findOrInsert(testType);
                    insStmt.setString(1, machine.getId());
                    insStmt.setInt(2, typeId);
                    insStmt.addBatch();
                }
                insStmt.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("Rollback error: " + rollbackEx.getMessage());
            }
            System.err.println("Warning saving machine test types (rolled back): " + e.getMessage());
        } finally {
            try {
                conn.setAutoCommit(originalAutoCommit);
            } catch (SQLException ignored) {
            }
        }
    }
}
