package com.lab.service;

import com.lab.enums.DelayStatus;
import com.lab.enums.Priority;
import com.lab.enums.Status;
import com.lab.model.Machine;
import com.lab.model.Sample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class SchedulerServiceTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 11, 18, 9, 0, 0);

    @Test
    @DisplayName("Case 1: Emergency sample arrives while normal samples are waiting -> emergency is assigned first")
    void testEmergencyAssignedBeforeNormalWaitingSamples() {
        SchedulerService scheduler = new SchedulerService();
        Machine m1 = new Machine("M1", "Hematology Analyzer", Set.of("Blood"), 1);
        scheduler.addMachine(m1);

        Sample sInitial = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 60);
        assertEquals(Status.PROCESSING, sInitial.getStatus());

        Sample sNormal = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME.plusMinutes(2), 120);
        assertEquals(Status.QUEUED, sNormal.getStatus());

        Sample sEmergency = scheduler.registerSample("Blood", Priority.EMERGENCY, BASE_TIME.plusMinutes(5), 15);
        assertEquals(Status.QUEUED, sEmergency.getStatus());

        scheduler.completeSample(sInitial.getId());

        assertEquals(Status.PROCESSING, sEmergency.getStatus());
        assertEquals("M1", sEmergency.getAssignedMachineId());
        assertEquals(Status.QUEUED, sNormal.getStatus());
    }

    @Test
    @DisplayName("Case 2: Two samples with same priority but different deadlines -> earlier deadline first")
    void testSamePriorityEarlierDeadlineAssignedFirst() {
        SchedulerService scheduler = new SchedulerService();
        Machine m1 = new Machine("M1", "Analyzer", Set.of("Blood"), 1);
        scheduler.addMachine(m1);

        Sample sInitial = scheduler.registerSample("Blood", Priority.HIGH, BASE_TIME, 30);
        assertEquals(Status.PROCESSING, sInitial.getStatus());

        Sample sA = scheduler.registerSample("Blood", Priority.HIGH, BASE_TIME, 45);

        Sample sB = scheduler.registerSample("Blood", Priority.HIGH, BASE_TIME, 20);

        assertEquals(Status.QUEUED, sA.getStatus());
        assertEquals(Status.QUEUED, sB.getStatus());

        scheduler.completeSample(sInitial.getId());

        assertEquals(Status.PROCESSING, sB.getStatus());
        assertEquals("M1", sB.getAssignedMachineId());
        assertEquals(Status.QUEUED, sA.getStatus());
    }

    @Test
    @DisplayName("Case 3: Two samples with same priority and same deadline -> earlier received time first")
    void testSamePriorityAndDeadlineEarlierReceivedTimeFirst() {
        SchedulerService scheduler = new SchedulerService();
        Machine m1 = new Machine("M1", "Analyzer", Set.of("Blood"), 1);
        scheduler.addMachine(m1);

        Sample sInitial = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 10);
        assertEquals(Status.PROCESSING, sInitial.getStatus());

        Sample sA = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 60);

        Sample sB = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME.plusMinutes(30), 30);

        assertEquals(sA.getDeadline(), sB.getDeadline());
        assertEquals(Status.QUEUED, sA.getStatus());
        assertEquals(Status.QUEUED, sB.getStatus());

        scheduler.completeSample(sInitial.getId());

        assertEquals(Status.PROCESSING, sA.getStatus());
        assertEquals("M1", sA.getAssignedMachineId());
        assertEquals(Status.QUEUED, sB.getStatus());
    }

    @Test
    @DisplayName("Case 4: No compatible machine is available -> sample stays QUEUED")
    void testNoCompatibleMachineKeepsSampleQueued() {
        SchedulerService scheduler = new SchedulerService();

        Machine m1 = new Machine("M1", "Hematology Analyzer", Set.of("Blood", "Urine"), 2);
        scheduler.addMachine(m1);

        Sample sThyroid = scheduler.registerSample("Thyroid", Priority.HIGH, BASE_TIME, 60);

        assertEquals(Status.QUEUED, sThyroid.getStatus());
        assertNull(sThyroid.getAssignedMachineId());
        assertEquals(0, m1.getCurrentLoad());
        assertEquals(1, scheduler.getQueue().size());
    }

    @Test
    @DisplayName("Case 5: A machine becomes unavailable while samples are waiting -> they go to another machine")
    void testMachineUnavailableRoutesWaitingSamplesToAnotherMachine() {
        SchedulerService scheduler = new SchedulerService();
        Machine m1 = new Machine("M1", "Primary Analyzer", Set.of("Blood"), 1);
        Machine m2 = new Machine("M2", "Backup Analyzer", Set.of("Blood"), 1);
        scheduler.addMachine(m1);
        scheduler.addMachine(m2);

        Sample s1 = scheduler.registerSample("Blood", Priority.HIGH, BASE_TIME, 30);
        Sample s2 = scheduler.registerSample("Blood", Priority.HIGH, BASE_TIME, 30);
        assertEquals("M1", s1.getAssignedMachineId());
        assertEquals("M2", s2.getAssignedMachineId());

        Sample s3 = scheduler.registerSample("Blood", Priority.HIGH, BASE_TIME, 30);
        assertEquals(Status.QUEUED, s3.getStatus());

        scheduler.markMachineDown("M1");
        assertFalse(m1.isAvailable());

        scheduler.completeSample(s1.getId());
        assertEquals(Status.QUEUED, s3.getStatus());

        scheduler.completeSample(s2.getId());

        assertEquals(Status.PROCESSING, s3.getStatus());
        assertEquals("M2", s3.getAssignedMachineId());
    }

    @Test
    @DisplayName("Case 6: A sample exceeds its processing target -> checkDelay returns OVERDUE")
    void testSampleExceedingTargetReturnsOverdue() {
        SchedulerService scheduler = new SchedulerService();
        LocalDateTime received = BASE_TIME;
        int targetMinutes = 30;

        Sample s = new Sample("S001", "Blood", Priority.HIGH, received, targetMinutes);

        assertEquals(DelayStatus.ON_TIME, scheduler.checkDelay(s, received.plusMinutes(10)));

        assertEquals(DelayStatus.APPROACHING, scheduler.checkDelay(s, received.plusMinutes(24)));

        assertEquals(DelayStatus.OVERDUE, scheduler.checkDelay(s, received.plusMinutes(30)));

        assertEquals(DelayStatus.OVERDUE, scheduler.checkDelay(s, received.plusMinutes(35)));
    }

    @Test
    @DisplayName("Case 7: A higher-priority sample arrives while another is PROCESSING -> processing sample is NOT moved")
    void testHigherPriorityArrivalDoesNotMoveProcessingSample() {
        SchedulerService scheduler = new SchedulerService();
        Machine m1 = new Machine("M1", "Analyzer", Set.of("Blood"), 1);
        scheduler.addMachine(m1);

        Sample sNormal = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 120);
        assertEquals(Status.PROCESSING, sNormal.getStatus());
        assertEquals("M1", sNormal.getAssignedMachineId());
        assertEquals(1, m1.getCurrentLoad());

        Sample sEmergency = scheduler.registerSample("Blood", Priority.EMERGENCY, BASE_TIME.plusMinutes(10), 15);

        assertEquals(Status.PROCESSING, sNormal.getStatus());
        assertEquals("M1", sNormal.getAssignedMachineId());

        assertEquals(Status.QUEUED, sEmergency.getStatus());
        assertNull(sEmergency.getAssignedMachineId());

        assertEquals(1, m1.getCurrentLoad());
    }

    @Test
    @DisplayName("Case 8: Low priority support and ordering behind Normal priority")
    void testLowPriorityOrderedBehindNormal() {
        SchedulerService scheduler = new SchedulerService();
        Machine m1 = new Machine("M1", "Analyzer", Set.of("Blood"), 1);
        scheduler.addMachine(m1);

        Sample sInitial = scheduler.registerSample("Blood", Priority.HIGH, BASE_TIME, 30);
        assertEquals(Status.PROCESSING, sInitial.getStatus());

        Sample sLow = scheduler.registerSample("Blood", Priority.LOW, BASE_TIME, 240);

        Sample sNormal = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME.plusMinutes(5), 120);

        scheduler.completeSample(sInitial.getId());

        assertEquals(Status.PROCESSING, sNormal.getStatus());
        assertEquals(Status.QUEUED, sLow.getStatus());
    }

    @Test
    @DisplayName("Case 9: Prevent invalid status transitions")
    void testInvalidStatusTransitions() {
        SchedulerService scheduler = new SchedulerService();
        Machine m1 = new Machine("M1", "Analyzer", Set.of("Blood"), 1);
        scheduler.addMachine(m1);

        Sample s = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 60);
        assertEquals(Status.PROCESSING, s.getStatus());

        scheduler.completeSample(s.getId());
        assertEquals(Status.COMPLETED, s.getStatus());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> {
            scheduler.putOnHold(s.getId());
        });

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> {
            scheduler.completeSample(s.getId());
        });

        org.junit.jupiter.api.Assertions.assertFalse(s.canTransitionTo(Status.QUEUED));
        org.junit.jupiter.api.Assertions.assertFalse(s.canTransitionTo(Status.PROCESSING));
    }

    @Test
    @DisplayName("Case 10: Unique sample ID generation and extended fields")
    void testUniqueIdGenerationAndExtendedFields() {
        SchedulerService scheduler = new SchedulerService();
        Sample s1 = scheduler.registerSample("Saliva Swab", "COVID", "Molecular", "STAT", Priority.EMERGENCY, BASE_TIME, 20);

        assertEquals("Saliva Swab", s1.getSampleType());
        assertEquals("COVID", s1.getTestType());
        assertEquals("Molecular", s1.getTestCategory());
        assertEquals("STAT", s1.getProcessingType());
        org.junit.jupiter.api.Assertions.assertTrue(s1.getId().startsWith("S"));

        Sample s2 = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 60);
        org.junit.jupiter.api.Assertions.assertNotEquals(s1.getId(), s2.getId());
    }
}
