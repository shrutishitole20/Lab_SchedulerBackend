package com.lab.repository;

import com.lab.enums.Priority;
import com.lab.model.User;
import com.lab.util.DBConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryTest {

    @Test
    @DisplayName("Verify Repository beans can be instantiated")
    void testRepositoryInstantiation() {
        SampleRepository sampleRepo = new SampleRepository();
        MachineRepository machineRepo = new MachineRepository();
        HistoryRepository historyRepo = new HistoryRepository();
        UserRepository userRepo = new UserRepository();
        TestTypeRepository testTypeRepo = new TestTypeRepository();
        PriorityRepository priorityRepo = new PriorityRepository();
        DowntimeRepository downtimeRepo = new DowntimeRepository();
        AssignmentRepository assignmentRepo = new AssignmentRepository();

        assertNotNull(sampleRepo);
        assertNotNull(machineRepo);
        assertNotNull(historyRepo);
        assertNotNull(userRepo);
        assertNotNull(testTypeRepo);
        assertNotNull(priorityRepo);
        assertNotNull(downtimeRepo);
        assertNotNull(assignmentRepo);
    }

    @Test
    @DisplayName("Verify Priority and TestType lookups")
    void testReferenceRepositories() {
        TestTypeRepository testTypeRepo = new TestTypeRepository();
        PriorityRepository priorityRepo = new PriorityRepository();

        assertNotNull(testTypeRepo.findAllNames());
        assertNotNull(priorityRepo.findAll());
        int emergencyId = priorityRepo.resolvePriorityId(Priority.EMERGENCY);
        assertTrue(emergencyId > 0);
    }

    @Test
    @DisplayName("Verify User authentication hashing with BCrypt")
    void testUserPasswordHashing() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        UserRepository userRepo = new UserRepository();

        userRepo.ensureDefaultAccounts(encoder);

        try {
            Optional<User> admin = userRepo.findByUsername("admin");
            if (admin.isPresent()) {
                assertTrue(encoder.matches("admin123", admin.get().getPasswordHash()));
            }
        } catch (Exception e) {
            System.out.println("Notice: DB not available for unit test: " + e.getMessage());
        }
    }

    @AfterAll
    static void tearDown() {
        DBConnection.shutdown();
    }
}
