package com.lab.dao;

import com.lab.enums.Priority;
import com.lab.enums.Status;
import com.lab.model.HistoryEntry;
import com.lab.model.Machine;
import com.lab.model.MachineDowntime;
import com.lab.model.Sample;
import com.lab.model.SampleAssignment;
import com.lab.model.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseAuditTest {

    @Test
    @DisplayName("Verify Database Connectivity & Table Metadata")
    void testConnectionAndSchema() throws Exception {
        assertTrue(DBConnection.testConnection(), "Database connection must succeed");

        try (Connection conn = DBConnection.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("=== AUDIT: DATABASE METADATA ===");
            System.out.println("DBMS: " + meta.getDatabaseProductName() + " v" + meta.getDatabaseProductVersion());

            List<String> tables = new ArrayList<>();
            try (ResultSet rs = meta.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            System.out.println("Tables verified in database: " + tables);
            assertTrue(tables.contains("users"), "users table must exist");
            assertTrue(tables.contains("priorities"), "priorities table must exist");
            assertTrue(tables.contains("test_types"), "test_types table must exist");
            assertTrue(tables.contains("machines"), "machines table must exist");
            assertTrue(tables.contains("machine_test_types"), "machine_test_types table must exist");
            assertTrue(tables.contains("samples"), "samples table must exist");
            assertTrue(tables.contains("sample_history"), "sample_history table must exist");
        }
    }

    @Test
    @DisplayName("Verify UserDAO CRUD operations")
    void testUserCrud() {
        UserDAO userDAO = new UserDAO();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        userDAO.ensureDefaultAccounts(encoder);

        String testUsername = "audit_user_" + System.currentTimeMillis();
        User newUser = new User(0, testUsername, encoder.encode("pass123"), "STAFF");
        boolean saved = userDAO.save(newUser);
        assertTrue(saved, "UserDAO.save must succeed");
        assertTrue(newUser.getId() > 0, "Generated ID must be assigned");

        Optional<User> fetched = userDAO.findByUsername(testUsername);
        assertTrue(fetched.isPresent(), "User must be found by username");
        assertEquals("STAFF", fetched.get().getRole());

        User toUpdate = fetched.get();
        toUpdate.setRole("ADMIN");
        boolean updated = userDAO.update(toUpdate);
        assertTrue(updated, "UserDAO.update must succeed");

        Optional<User> updatedUser = userDAO.findById(toUpdate.getId());
        assertTrue(updatedUser.isPresent());
        assertEquals("ADMIN", updatedUser.get().getRole());

        boolean deleted = userDAO.delete(toUpdate.getId());
        assertTrue(deleted, "UserDAO.delete must succeed");
    }

    @Test
    @DisplayName("Verify PriorityDAO & TestTypeDAO dynamic resolution")
    void testReferenceDaos() {
        PriorityDAO priorityDAO = new PriorityDAO();
        TestTypeDAO testTypeDAO = new TestTypeDAO();

        int prioId = priorityDAO.resolvePriorityId(Priority.EMERGENCY);
        assertTrue(prioId > 0, "Priority ID must be resolved");
        List<Map<String, Object>> allPrios = priorityDAO.findAll();
        assertFalse(allPrios.isEmpty(), "Priorities must not be empty");

        int typeId = testTypeDAO.findOrInsert("Molecular PCR");
        assertTrue(typeId > 0, "Test type ID must be resolved or inserted");
        List<String> names = testTypeDAO.findAllNames();
        assertTrue(names.contains("Molecular PCR"));
    }

    @Test
    @DisplayName("Verify MachineDAO and Transactional Compatibility Bridge")
    void testMachineCrud() {
        MachineDAO machineDAO = new MachineDAO();
        String testMachineId = "TEST_M" + (System.currentTimeMillis() % 1000);

        Set<String> tests = new HashSet<>();
        tests.add("Blood");
        tests.add("Urine");

        Machine machine = new Machine(testMachineId, "Test Analyzer Unit", tests, 2, 0, true);
        boolean saved = machineDAO.save(machine);
        assertTrue(saved, "MachineDAO.save must succeed");

        Optional<Machine> fetched = machineDAO.findById(testMachineId);
        assertTrue(fetched.isPresent(), "Machine must be found");
        assertEquals("Test Analyzer Unit", fetched.get().getName());
        assertEquals(2, fetched.get().getCapacity());
        assertTrue(fetched.get().getSupportedTestTypes().contains("Blood"));

        machine.setName("Updated Analyzer Unit");
        machine.setCapacity(3);
        machine.getSupportedTestTypes().add("Thyroid");
        boolean updated = machineDAO.update(machine);
        assertTrue(updated, "MachineDAO.update must succeed");

        Optional<Machine> updatedFetched = machineDAO.findById(testMachineId);
        assertTrue(updatedFetched.isPresent());
        assertEquals(3, updatedFetched.get().getCapacity());
        assertTrue(updatedFetched.get().getSupportedTestTypes().contains("Thyroid"));

        boolean deleted = machineDAO.delete(testMachineId);
        assertTrue(deleted, "MachineDAO.delete must succeed");
    }

    @Test
    @DisplayName("Verify SampleDAO CRUD & Extended Columns")
    void testSampleCrud() {
        SampleDAO sampleDAO = new SampleDAO();
        String sampleId = "TEST_S" + (System.currentTimeMillis() % 10000);

        Sample sample = new Sample(sampleId, "Blood", Priority.HIGH, LocalDateTime.now(), 30);
        sample.setSampleType("Whole Blood");
        sample.setTestCategory("Hematology");
        sample.setProcessingType("Stat");

        boolean saved = sampleDAO.save(sample);
        assertTrue(saved, "SampleDAO.save must succeed");

        Optional<Sample> fetched = sampleDAO.findById(sampleId);
        assertTrue(fetched.isPresent(), "Sample must be found by ID");
        assertEquals(Status.RECEIVED, fetched.get().getStatus());
        assertEquals("Blood", fetched.get().getTestType());
        assertEquals(Priority.HIGH, fetched.get().getPriority());

        sample.setStatus(Status.QUEUED);
        sample.setStatus(Status.PROCESSING);
        sample.setAssignedMachineId("M1");
        sample.setStartTime(LocalDateTime.now());
        boolean updated = sampleDAO.update(sample);
        assertTrue(updated, "SampleDAO.update must succeed");

        Optional<Sample> updatedSample = sampleDAO.findById(sampleId);
        assertTrue(updatedSample.isPresent());
        assertEquals(Status.PROCESSING, updatedSample.get().getStatus());
        assertEquals("M1", updatedSample.get().getAssignedMachineId());

        boolean deleted = sampleDAO.delete(sampleId);
        assertTrue(deleted, "SampleDAO.delete must succeed");
    }

    @Test
    @DisplayName("Verify HistoryDAO audit logging")
    void testHistoryCrud() {
        HistoryDAO historyDAO = new HistoryDAO();
        String sampleId = "AUDIT_TEST_" + System.currentTimeMillis();

        HistoryEntry entry = new HistoryEntry(sampleId, LocalDateTime.now(), "AUDIT_CHECK", "Verification test entry");
        boolean saved = historyDAO.save(entry);
        assertTrue(saved, "HistoryDAO.save must succeed");

        List<HistoryEntry> entries = historyDAO.findBySampleId(sampleId);
        assertFalse(entries.isEmpty(), "Entries must be found for sample");
        assertEquals("AUDIT_CHECK", entries.get(0).getAction());
    }

    @Test
    @DisplayName("Verify DowntimeDAO tracking")
    void testDowntimeCrud() {
        DowntimeDAO downtimeDAO = new DowntimeDAO();
        String machineId = "M1";

        boolean recorded = downtimeDAO.recordDowntime(machineId, "Scheduled Maintenance Test", "AUDITOR");
        assertTrue(recorded, "DowntimeDAO.recordDowntime must succeed");

        Optional<MachineDowntime> active = downtimeDAO.findActiveByMachineId(machineId);
        assertTrue(active.isPresent(), "Active downtime must be found");
        assertEquals("M1", active.get().getMachineId());

        boolean resolved = downtimeDAO.resolveDowntime(machineId);
        assertTrue(resolved, "DowntimeDAO.resolveDowntime must succeed");

        Optional<MachineDowntime> afterResolve = downtimeDAO.findActiveByMachineId(machineId);
        assertFalse(afterResolve.isPresent(), "No active downtime should remain after resolution");
    }

    @Test
    @DisplayName("Verify AssignmentDAO tracking")
    void testAssignmentCrud() {
        SampleDAO sampleDAO = new SampleDAO();
        AssignmentDAO assignmentDAO = new AssignmentDAO();
        String sampleId = "S_ASSIGN_TEST";
        String machineId = "M1";

        Sample s = new Sample(sampleId, "Blood", Priority.NORMAL, LocalDateTime.now(), 60);
        sampleDAO.save(s);

        boolean recorded = assignmentDAO.recordAssignment(sampleId, machineId, "PROCESSING", "Test assignment");
        assertTrue(recorded, "AssignmentDAO.recordAssignment must succeed");

        boolean updated = assignmentDAO.updateStatus(sampleId, machineId, "COMPLETED", LocalDateTime.now().minusMinutes(10), LocalDateTime.now());
        assertTrue(updated, "AssignmentDAO.updateStatus must succeed");

        List<SampleAssignment> list = assignmentDAO.findBySampleId(sampleId);
        assertFalse(list.isEmpty(), "Assignment list must not be empty");
        assertEquals("COMPLETED", list.get(0).getStatus());

        sampleDAO.delete(sampleId);
    }

    @AfterAll
    static void tearDownAll() {
        DBConnection.shutdown();
    }
}
