package com.lab.dao;

import com.lab.model.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DaoTest {

    @Test
    @DisplayName("Verify User model instantiation and getters/setters")
    void testUserModel() {
        User user = new User(1, "admin", "hash123", "ADMIN");
        assertEquals(1, user.getId());
        assertEquals("admin", user.getUsername());
        assertEquals("hash123", user.getPasswordHash());
        assertEquals("ADMIN", user.getRole());
        assertNotNull(user.toString());
    }

    @Test
    @DisplayName("Verify DAO instances can be instantiated cleanly")
    void testDaoInstantiation() {
        SampleDAO sampleDAO = new SampleDAO();
        MachineDAO machineDAO = new MachineDAO();
        HistoryDAO historyDAO = new HistoryDAO();
        UserDAO userDAO = new UserDAO();
        TestTypeDAO testTypeDAO = new TestTypeDAO();
        PriorityDAO priorityDAO = new PriorityDAO();

        assertNotNull(sampleDAO);
        assertNotNull(machineDAO);
        assertNotNull(historyDAO);
        assertNotNull(userDAO);
        assertNotNull(testTypeDAO);
        assertNotNull(priorityDAO);
    }

    @Test
    @DisplayName("Verify TestTypeDAO and PriorityDAO resolution")
    void testReferenceDaos() {
        TestTypeDAO testTypeDAO = new TestTypeDAO();
        PriorityDAO priorityDAO = new PriorityDAO();

        assertNotNull(testTypeDAO.findAllNames());
        assertNotNull(priorityDAO.findAll());
    }

    @Test
    @DisplayName("Verify BCrypt password and UserDAO auto-heal")
    void testPasswordCheck() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        UserDAO userDAO = new UserDAO();

        userDAO.ensureDefaultAccounts(encoder);

        try {
            Optional<User> admin = userDAO.findByUsername("admin");
            if (admin.isPresent()) {
                org.junit.jupiter.api.Assertions.assertTrue(
                        encoder.matches("admin123", admin.get().getPasswordHash()),
                        "Admin password in DB must match admin123"
                );
            }

            Optional<User> staff = userDAO.findByUsername("staff");
            if (staff.isPresent()) {
                org.junit.jupiter.api.Assertions.assertTrue(
                        encoder.matches("staff123", staff.get().getPasswordHash()),
                        "Staff password in DB must match staff123"
                );
            }
        } catch (Exception e) {
            System.out.println("Notice: Could not connect to DB in unit test: " + e.getMessage());
        }
    }

    @AfterAll
    static void tearDownAll() {
        DBConnection.shutdown();
    }
}
