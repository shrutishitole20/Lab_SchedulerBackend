package com.lab;

import com.lab.enums.DelayStatus;
import com.lab.enums.Priority;
import com.lab.enums.Status;
import com.lab.model.Machine;
import com.lab.model.Sample;
import com.lab.service.SampleComparator;
import com.lab.service.SchedulerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @Test
    @DisplayName("Verify Priority rank and default targets")
    void testPriorityEnum() {
        assertEquals(1, Priority.EMERGENCY.getRank());
        assertEquals(15, Priority.EMERGENCY.getDefaultTargetMinutes());

        assertEquals(2, Priority.HIGH.getRank());
        assertEquals(30, Priority.HIGH.getDefaultTargetMinutes());

        assertEquals(3, Priority.NORMAL.getRank());
        assertEquals(120, Priority.NORMAL.getDefaultTargetMinutes());

        assertEquals(4, Priority.LOW.getRank());
        assertEquals(240, Priority.LOW.getDefaultTargetMinutes());
    }

    @Test
    @DisplayName("Verify SampleComparator priority order")
    void testSampleComparatorOrdering() {
        LocalDateTime base = LocalDateTime.of(2026, 11, 18, 9, 0);

        Sample s1 = new Sample("S001", "Blood", Priority.HIGH, base, 30);
        Sample s2 = new Sample("S002", "Urine", Priority.NORMAL, base, 120);
        Sample s3 = new Sample("S003", "Blood", Priority.EMERGENCY, base, 15);
        Sample s4 = new Sample("S004", "Thyroid", Priority.NORMAL, base, 180);
        Sample s5 = new Sample("S005", "Blood", Priority.HIGH, base, 45);

        PriorityQueue<Sample> queue = new PriorityQueue<>(new SampleComparator());
        queue.add(s1);
        queue.add(s2);
        queue.add(s3);
        queue.add(s4);
        queue.add(s5);

        List<String> polledIds = new ArrayList<>();
        while (!queue.isEmpty()) {
            polledIds.add(queue.poll().getId());
        }

        assertEquals(List.of("S003", "S001", "S005", "S002", "S004"), polledIds);
    }

    @Test
    @DisplayName("Verify markMachineDown and markMachineUp")
    void testMachineDownAndUp() {
        SchedulerService scheduler = new SchedulerService();
        Machine m1 = new Machine("M1", "M1", Set.of("Blood"), 1);
        scheduler.addMachine(m1);

        LocalDateTime now = LocalDateTime.now();
        Sample s1 = scheduler.registerSample("Blood", Priority.HIGH, now, 30);
        assertEquals(Status.PROCESSING, s1.getStatus());

        scheduler.markMachineDown("M1");
        assertFalse(m1.isAvailable());

        assertEquals(Status.PROCESSING, s1.getStatus());

        Sample s2 = scheduler.registerSample("Blood", Priority.HIGH, now, 30);
        assertEquals(Status.QUEUED, s2.getStatus());

        scheduler.markMachineUp("M1");
        assertTrue(m1.isAvailable());

        scheduler.completeSample(s1.getId());
        assertEquals(Status.COMPLETED, s1.getStatus());
        assertEquals(Status.PROCESSING, s2.getStatus());
    }

    @Test
    @DisplayName("Verify putOnHold and releaseFromHold")
    void testHoldAndRelease() {
        SchedulerService scheduler = new SchedulerService();

        LocalDateTime now = LocalDateTime.now();
        Sample s1 = scheduler.registerSample("Blood", Priority.HIGH, now, 30);
        assertEquals(Status.QUEUED, s1.getStatus());
        assertEquals(1, scheduler.getQueue().size());

        scheduler.putOnHold(s1.getId());
        assertEquals(Status.ON_HOLD, s1.getStatus());
        assertEquals(0, scheduler.getQueue().size());

        scheduler.releaseFromHold(s1.getId());
        assertEquals(Status.QUEUED, s1.getStatus());
        assertEquals(1, scheduler.getQueue().size());
    }

    @Test
    @DisplayName("Verify checkDelay: ON_TIME, APPROACHING, and OVERDUE")
    void testCheckDelay() {
        SchedulerService scheduler = new SchedulerService();
        LocalDateTime base = LocalDateTime.of(2026, 11, 18, 9, 0);
        Sample s = new Sample("S001", "Blood", Priority.HIGH, base, 30);

        assertEquals(DelayStatus.ON_TIME, scheduler.checkDelay(s, base.plusMinutes(10)));

        assertEquals(DelayStatus.APPROACHING, scheduler.checkDelay(s, base.plusMinutes(24)));

        assertEquals(DelayStatus.APPROACHING, scheduler.checkDelay(s, base.plusMinutes(29)));

        assertEquals(DelayStatus.OVERDUE, scheduler.checkDelay(s, base.plusMinutes(30)));

        assertEquals(DelayStatus.OVERDUE, scheduler.checkDelay(s, base.plusMinutes(40)));
    }

    @Test
    @DisplayName("Verify getDelayedSamples filters approaching and overdue samples")
    void testGetDelayedSamples() {
        SchedulerService scheduler = new SchedulerService();
        LocalDateTime base = LocalDateTime.of(2026, 11, 18, 9, 0);

        Sample s1 = scheduler.registerSample("Blood", Priority.EMERGENCY, base, 15);
        Sample s2 = scheduler.registerSample("Urine", Priority.NORMAL, base, 120);

        List<Sample> delayed = scheduler.getDelayedSamples(base.plusMinutes(13));
        assertEquals(1, delayed.size());
        assertEquals("S001", delayed.get(0).getId());

        delayed = scheduler.getDelayedSamples(base.plusMinutes(16));
        assertEquals(1, delayed.size());
        assertEquals("S001", delayed.get(0).getId());
    }

    @Test
    @DisplayName("Verify displayQueue and displayMachineWorkload execute without errors")
    void testDisplayMethods() {
        SchedulerService scheduler = new SchedulerService();
        scheduler.addMachine(new Machine("M1", "M1", Set.of("Blood"), 2));
        assertDoesNotThrow(scheduler::displayQueue);
        assertDoesNotThrow(scheduler::displayMachineWorkload);
    }

    @Test
    @DisplayName("Verify Main execution runs cleanly")
    void testMainExecution() {
        assertDoesNotThrow(() -> Main.main(new String[]{}));
    }
}
