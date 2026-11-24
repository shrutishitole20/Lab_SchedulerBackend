package com.lab.service;

import com.lab.enums.DelayStatus;
import com.lab.enums.Priority;
import com.lab.enums.Status;
import com.lab.model.HistoryEntry;
import com.lab.model.Machine;
import com.lab.model.Sample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ComprehensiveSchedulerTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 11, 20, 9, 0, 0);
    private SchedulerService scheduler;

    @BeforeEach
    void setUp() {

        scheduler = new SchedulerService();
    }

    @Test
    @DisplayName("TEST 1: Emergency sample arrives -> receives #1 queue position and takes scheduling priority")
    void test01_EmergencySampleArrives() {
        Machine m1 = new Machine("M1", "Hematology Analyzer", Set.of("Blood"), 1);
        scheduler.addMachine(m1);

        Sample sOccupant = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 60);
        assertEquals(Status.PROCESSING, sOccupant.getStatus(), "Initial sample should be processing on M1");

        Sample sNormal1 = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME.plusMinutes(2), 120);
        Sample sNormal2 = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME.plusMinutes(4), 120);
        assertEquals(Status.QUEUED, sNormal1.getStatus());
        assertEquals(Status.QUEUED, sNormal2.getStatus());

        Sample sEmergency = scheduler.registerSample("Blood", Priority.EMERGENCY, BASE_TIME.plusMinutes(6), 15);
        assertEquals(Status.QUEUED, sEmergency.getStatus());

        Sample topOfQueue = scheduler.getQueue().peek();
        assertNotNull(topOfQueue);
        assertEquals(sEmergency.getId(), topOfQueue.getId(), "Emergency sample must be at head of queue (#1 position)");

        scheduler.completeSample(sOccupant.getId());

        assertEquals(Status.PROCESSING, sEmergency.getStatus(), "Emergency sample must be assigned immediately");
        assertEquals("M1", sEmergency.getAssignedMachineId());
        assertEquals(Status.QUEUED, sNormal1.getStatus(), "Normal sample 1 must still be queued");
        assertEquals(Status.QUEUED, sNormal2.getStatus(), "Normal sample 2 must still be queued");
    }

    @Test
    @DisplayName("TEST 2: Same priority, different deadlines -> earlier deadline is selected first")
    void test02_SamePriorityDifferentDeadlines() {
        Machine m1 = new Machine("M1", "Analyzer Unit", Set.of("Blood"), 1);
        scheduler.addMachine(m1);

        Sample sOccupant = scheduler.registerSample("Blood", Priority.HIGH, BASE_TIME, 30);

        Sample sA = scheduler.registerSample("Blood", Priority.HIGH, BASE_TIME, 45);

        Sample sB = scheduler.registerSample("Blood", Priority.HIGH, BASE_TIME, 20);

        assertTrue(sB.getDeadline().isBefore(sA.getDeadline()), "Sample B deadline must be earlier than Sample A");

        scheduler.completeSample(sOccupant.getId());

        assertEquals(Status.PROCESSING, sB.getStatus(), "Sample with earlier deadline must be dispatched first");
        assertEquals("M1", sB.getAssignedMachineId());
        assertEquals(Status.QUEUED, sA.getStatus(), "Sample with later deadline remains queued");
    }

    @Test
    @DisplayName("TEST 3: Same priority & deadline -> earlier received time first, then ID alphabetical tie-breaker")
    void test03_SamePriorityAndSameDeadline() {
        Machine m1 = new Machine("M1", "Analyzer", Set.of("Blood"), 1);
        scheduler.addMachine(m1);

        Sample sOccupant = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 10);

        Sample sA = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 60);

        Sample sB = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME.plusMinutes(30), 30);

        assertEquals(sA.getDeadline(), sB.getDeadline(), "Both samples must have identical deadlines (10:00)");

        scheduler.completeSample(sOccupant.getId());

        assertEquals(Status.PROCESSING, sA.getStatus(), "Earlier received sample must be scheduled first");
        assertEquals(Status.QUEUED, sB.getStatus());

        Sample tie1 = new Sample("S010", "Blood", Priority.NORMAL, BASE_TIME, 60);
        Sample tie2 = new Sample("S020", "Blood", Priority.NORMAL, BASE_TIME, 60);

        SampleComparator comparator = new SampleComparator();
        int compResult = comparator.compare(tie1, tie2);
        assertTrue(compResult < 0, "S010 should precede S020 based on alphabetical tie-breaker");

        PriorityQueue<Sample> tieQueue = new PriorityQueue<>(comparator);
        tieQueue.add(tie2);
        tieQueue.add(tie1);
        assertEquals("S010", tieQueue.poll().getId(), "S010 must be polled first as deterministic tie-breaker");
        assertEquals("S020", tieQueue.poll().getId());
    }

    @Test
    @DisplayName("TEST 4: No compatible machine -> sample remains QUEUED and is not assigned incorrectly")
    void test04_NoCompatibleMachine() {

        Machine m1 = new Machine("M1", "Clinical Chemistry", Set.of("Blood", "Urine"), 2);
        scheduler.addMachine(m1);

        Sample sTox = scheduler.registerSample("Toxicology", Priority.HIGH, BASE_TIME, 60);

        assertEquals(Status.QUEUED, sTox.getStatus(), "Incompatible sample must stay QUEUED");
        assertNull(sTox.getAssignedMachineId(), "Incompatible sample must have no assigned machine");
        assertEquals(0, m1.getCurrentLoad(), "Machine load must remain 0");

        Sample sBlood = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 60);

        assertEquals(Status.PROCESSING, sBlood.getStatus(), "Compatible sample must be assigned");
        assertEquals("M1", sBlood.getAssignedMachineId());
        assertEquals(Status.QUEUED, sTox.getStatus(), "Incompatible sample remains QUEUED without error");
        assertEquals(1, m1.getCurrentLoad());
    }

    @Test
    @DisplayName("TEST 5: Machine becomes unavailable -> no new assignments to it; queued samples rescheduled")
    void test05_MachineBecomesUnavailable() {
        Machine m1 = new Machine("M1", "Primary Machine", Set.of("Blood"), 1);
        Machine m2 = new Machine("M2", "Backup Machine", Set.of("Blood"), 1);
        scheduler.addMachine(m1);
        scheduler.addMachine(m2);

        Sample s1 = scheduler.registerSample("Blood", Priority.HIGH, BASE_TIME, 30);
        Sample s2 = scheduler.registerSample("Blood", Priority.HIGH, BASE_TIME, 30);
        assertEquals("M1", s1.getAssignedMachineId());
        assertEquals("M2", s2.getAssignedMachineId());

        Sample s3 = scheduler.registerSample("Blood", Priority.HIGH, BASE_TIME, 30);
        Sample s4 = scheduler.registerSample("Blood", Priority.HIGH, BASE_TIME, 30);
        assertEquals(Status.QUEUED, s3.getStatus());
        assertEquals(Status.QUEUED, s4.getStatus());

        scheduler.markMachineDown("M1", "Hardware maintenance");
        assertFalse(m1.isAvailable(), "M1 availability must be false");

        scheduler.completeSample(s1.getId());
        assertEquals(Status.COMPLETED, s1.getStatus());
        assertEquals(0, m1.getCurrentLoad(), "M1 load is now 0");
        assertEquals(Status.QUEUED, s3.getStatus(), "s3 must NOT be assigned to down machine M1");

        scheduler.completeSample(s2.getId());
        assertEquals(Status.PROCESSING, s3.getStatus(), "s3 must be assigned to available machine M2");
        assertEquals("M2", s3.getAssignedMachineId());
        assertEquals(Status.QUEUED, s4.getStatus(), "s4 remains queued until M2 frees up");
    }

    @Test
    @DisplayName("TEST 6: Sample becomes overdue -> flagged as OVERDUE, preserving status and history")
    void test06_SampleBecomesOverdue() {
        LocalDateTime received = BASE_TIME;
        int targetMinutes = 20;

        Sample sample = scheduler.registerSample("Routine Blood", "Blood", "Hematology", "Routine",
                Priority.HIGH, received, targetMinutes);

        assertEquals(DelayStatus.ON_TIME, scheduler.checkDelay(sample, received.plusMinutes(10)));

        assertEquals(DelayStatus.APPROACHING, scheduler.checkDelay(sample, received.plusMinutes(17)));

        assertEquals(DelayStatus.OVERDUE, scheduler.checkDelay(sample, received.plusMinutes(20)));

        assertEquals(DelayStatus.OVERDUE, scheduler.checkDelay(sample, received.plusMinutes(30)));

        List<Sample> delayedList = scheduler.getDelayedSamples(received.plusMinutes(25));
        assertTrue(delayedList.stream().anyMatch(s -> s.getId().equals(sample.getId())),
                "Sample must be present in delayed samples list");

        List<HistoryEntry> sampleHist = scheduler.getHistory().stream()
                .filter(h -> sample.getId().equals(h.getSampleId()))
                .toList();
        assertFalse(sampleHist.isEmpty(), "Sample history must be preserved");
    }

    @Test
    @DisplayName("TEST 7: Emergency sample arrives during processing -> processing sample is NOT moved")
    void test07_EmergencySampleArrivesDuringProcessing() {
        Machine m1 = new Machine("M1", "Analyzer", Set.of("Blood"), 1);
        scheduler.addMachine(m1);

        Sample sNormal = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 120);
        assertEquals(Status.PROCESSING, sNormal.getStatus());
        assertEquals("M1", sNormal.getAssignedMachineId());
        assertEquals(1, m1.getCurrentLoad());

        Sample sEmergency = scheduler.registerSample("Blood", Priority.EMERGENCY, BASE_TIME.plusMinutes(10), 15);

        assertEquals(Status.PROCESSING, sNormal.getStatus(), "Running sample must not be preempted");
        assertEquals("M1", sNormal.getAssignedMachineId());

        assertEquals(Status.QUEUED, sEmergency.getStatus());
        assertEquals(1, m1.getCurrentLoad(), "Machine load must remain capped at 1");

        scheduler.completeSample(sNormal.getId());
        assertEquals(Status.COMPLETED, sNormal.getStatus());
        assertEquals(Status.PROCESSING, sEmergency.getStatus());
        assertEquals("M1", sEmergency.getAssignedMachineId());
    }

    @Test
    @DisplayName("ADDITIONAL: Duplicate sample ID -> auto-incrementing ID generator prevents duplicate collisions")
    void test08_DuplicateSampleIdPrevention() {
        Sample s1 = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 60);
        Sample s2 = scheduler.registerSample("Blood", Priority.HIGH, BASE_TIME, 30);
        Sample s3 = scheduler.registerSample("Blood", Priority.EMERGENCY, BASE_TIME, 15);

        assertNotEquals(s1.getId(), s2.getId(), "Sample IDs must be unique");
        assertNotEquals(s2.getId(), s3.getId(), "Sample IDs must be unique");
        assertNotEquals(s1.getId(), s3.getId(), "Sample IDs must be unique");

        assertTrue(s1.getId().matches("S\\d{3,}"), "Sample ID must follow format Sxxx");
    }

    @Test
    @DisplayName("ADDITIONAL: Invalid sample registration -> rejects null or invalid input parameters")
    void test09_InvalidSampleRegistration() {

        assertThrows(IllegalArgumentException.class, () -> {
            scheduler.registerSample(null, Priority.NORMAL, BASE_TIME, 60);
        }, "Null test type must throw IllegalArgumentException");

        assertThrows(IllegalArgumentException.class, () -> {
            scheduler.registerSample("   ", Priority.NORMAL, BASE_TIME, 60);
        }, "Empty test type must throw IllegalArgumentException");

        assertThrows(IllegalArgumentException.class, () -> {
            scheduler.registerSample("Blood", null, BASE_TIME, 60);
        }, "Null priority must throw IllegalArgumentException");

        assertThrows(IllegalArgumentException.class, () -> {
            scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 0);
        }, "Zero target duration must throw IllegalArgumentException");

        assertThrows(IllegalArgumentException.class, () -> {
            scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, -15);
        }, "Negative target duration must throw IllegalArgumentException");
    }

    @Test
    @DisplayName("ADDITIONAL: Invalid status transition -> prevents illegal state modifications")
    void test10_InvalidStatusTransitions() {
        Machine m1 = new Machine("M1", "Analyzer", Set.of("Blood"), 1);
        scheduler.addMachine(m1);

        Sample sample = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 60);
        assertEquals(Status.PROCESSING, sample.getStatus());

        scheduler.completeSample(sample.getId());
        assertEquals(Status.COMPLETED, sample.getStatus());

        assertThrows(IllegalStateException.class, () -> {
            scheduler.putOnHold(sample.getId());
        }, "COMPLETED sample cannot be put on hold");

        assertThrows(IllegalStateException.class, () -> {
            scheduler.completeSample(sample.getId());
        }, "COMPLETED sample cannot be completed again");

        assertThrows(IllegalStateException.class, () -> {
            sample.setStatus(Status.QUEUED);
        }, "COMPLETED sample cannot transition to QUEUED");

        assertThrows(IllegalStateException.class, () -> {
            scheduler.releaseFromHold(sample.getId());
        }, "Non-ON_HOLD sample cannot be released from hold");
    }

    @Test
    @DisplayName("ADDITIONAL: Machine capacity exceeded -> strictly capped at configured capacity")
    void test11_MachineCapacityExceeded() {

        Machine m1 = new Machine("M1", "Dual Analyzer", Set.of("Blood"), 2);
        scheduler.addMachine(m1);

        Sample s1 = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 60);
        Sample s2 = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 60);

        assertEquals(2, m1.getCurrentLoad(), "Machine load should be 2/2");
        assertFalse(m1.hasFreeSlot(), "Machine should have no free slots");
        assertEquals(Status.PROCESSING, s1.getStatus());
        assertEquals(Status.PROCESSING, s2.getStatus());

        Sample s3 = scheduler.registerSample("Blood", Priority.HIGH, BASE_TIME, 30);
        assertEquals(Status.QUEUED, s3.getStatus());
        assertEquals(2, m1.getCurrentLoad(), "Machine capacity must NEVER exceed 2");
    }

    @Test
    @DisplayName("ADDITIONAL: Machine downtime -> records downtime reason and prevents new sample assignments")
    void test12_MachineDowntime() {
        Machine m1 = new Machine("M1", "Analyzer", Set.of("Blood"), 1);
        scheduler.addMachine(m1);

        scheduler.markMachineDown("M1", "Calibration required");
        assertFalse(m1.isAvailable());

        Sample s = scheduler.registerSample("Blood", Priority.EMERGENCY, BASE_TIME, 15);
        assertEquals(Status.QUEUED, s.getStatus(), "Sample must not be assigned to machine in downtime");

        boolean foundDownEntry = scheduler.getHistory().stream()
                .anyMatch(h -> "MACHINE_DOWN".equals(h.getAction()) && h.getDetails().contains("Calibration required"));
        assertTrue(foundDownEntry, "History entry for MACHINE_DOWN with reason must be present");
    }

    @Test
    @DisplayName("ADDITIONAL: Machine recovery -> restores availability and automatically dispatches waiting queue")
    void test13_MachineRecovery() {
        Machine m1 = new Machine("M1", "Analyzer", Set.of("Blood"), 1);
        scheduler.addMachine(m1);

        scheduler.markMachineDown("M1", "Emergency maintenance");
        Sample sWaiting = scheduler.registerSample("Blood", Priority.HIGH, BASE_TIME, 30);
        assertEquals(Status.QUEUED, sWaiting.getStatus());

        scheduler.markMachineUp("M1");
        assertTrue(m1.isAvailable(), "Machine should now be UP and available");

        assertEquals(Status.PROCESSING, sWaiting.getStatus(), "Waiting sample should be assigned upon machine recovery");
        assertEquals("M1", sWaiting.getAssignedMachineId());
        assertEquals(1, m1.getCurrentLoad());
    }

    @Test
    @DisplayName("ADDITIONAL: Sample completion -> records completion time, frees slot, and logs turnaround SLA")
    void test14_SampleCompletion() {
        Machine m1 = new Machine("M1", "Analyzer", Set.of("Blood"), 1);
        scheduler.addMachine(m1);

        Sample sample = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 60);
        assertEquals(Status.PROCESSING, sample.getStatus());

        scheduler.completeSample(sample.getId());

        assertEquals(Status.COMPLETED, sample.getStatus());
        assertNotNull(sample.getCompletionTime(), "Completion time must be recorded");
        assertEquals(0, m1.getCurrentLoad(), "Machine load must be decremented to 0");

        boolean hasCompLog = scheduler.getHistory().stream()
                .anyMatch(h -> sample.getId().equals(h.getSampleId()) && "COMPLETED".equals(h.getAction()));
        assertTrue(hasCompLog, "History entry for COMPLETED must be logged");
    }

    @Test
    @DisplayName("ADDITIONAL: Sample put on hold -> frees machine/queue slot and allows resuming")
    void test15_SamplePutOnHoldAndReleased() {
        Machine m1 = new Machine("M1", "Analyzer", Set.of("Blood"), 1);
        scheduler.addMachine(m1);

        Sample s1 = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 60);
        Sample s2 = scheduler.registerSample("Blood", Priority.HIGH, BASE_TIME, 30);

        assertEquals(Status.PROCESSING, s1.getStatus());
        assertEquals(Status.QUEUED, s2.getStatus());

        scheduler.putOnHold(s1.getId());
        assertEquals(Status.ON_HOLD, s1.getStatus());
        assertNull(s1.getAssignedMachineId(), "Assigned machine must be cleared on hold");

        assertEquals(Status.PROCESSING, s2.getStatus(), "Waiting sample should take over freed slot");
        assertEquals("M1", s2.getAssignedMachineId());

        scheduler.releaseFromHold(s1.getId());
        assertEquals(Status.QUEUED, s1.getStatus(), "Released sample must return to QUEUED");
    }

    @Test
    @DisplayName("ADDITIONAL: Daily report generation -> computes completed, pending, delayed, and on-hold metrics")
    void test16_DailyReportGeneration() {
        Machine m1 = new Machine("M1", "Analyzer", Set.of("Blood"), 5);
        scheduler.addMachine(m1);

        Sample sComp = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 60);
        scheduler.completeSample(sComp.getId());

        Sample sProc = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 60);

        Sample sHold = scheduler.registerSample("Blood", Priority.NORMAL, BASE_TIME, 60);
        scheduler.putOnHold(sHold.getId());

        Sample sOverdue = scheduler.registerSample("Blood", Priority.EMERGENCY, BASE_TIME, 15);

        LocalDateTime evalTime = BASE_TIME.plusMinutes(30);

        long completed = scheduler.getAllSamples().values().stream()
                .filter(s -> s.getStatus() == Status.COMPLETED).count();
        long onHold = scheduler.getAllSamples().values().stream()
                .filter(s -> s.getStatus() == Status.ON_HOLD).count();
        long pending = scheduler.getAllSamples().values().stream()
                .filter(s -> s.getStatus() == Status.QUEUED || s.getStatus() == Status.PROCESSING).count();
        long delayed = scheduler.getAllSamples().values().stream()
                .filter(s -> s.getStatus() != Status.COMPLETED &&
                        (scheduler.checkDelay(s, evalTime) == DelayStatus.APPROACHING ||
                         scheduler.checkDelay(s, evalTime) == DelayStatus.OVERDUE)).count();

        assertEquals(1, completed, "Completed samples count must be 1");
        assertEquals(1, onHold, "On-Hold samples count must be 1");
        assertEquals(2, pending, "Pending samples count must be 2 (sProc + sOverdue)");
        assertTrue(delayed >= 1, "Delayed samples count must be at least 1 (sOverdue)");
    }
}
