package com.lab.scheduler;

import com.lab.enums.DelayStatus;
import com.lab.enums.Priority;
import com.lab.enums.Status;
import com.lab.model.Machine;
import com.lab.model.Sample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SchedulerEngineTest {

    private SchedulerEngine engine;
    private final LocalDateTime base = LocalDateTime.of(2026, 11, 20, 10, 0);

    @BeforeEach
    void setUp() {
        engine = new SchedulerEngine();
    }

    @Test
    @DisplayName("Verify PriorityQueue ordering with Comparator")
    void testQueuePriority() {
        Sample normal = new Sample("S001", "Blood", Priority.NORMAL, base, 120);
        Sample urgent = new Sample("S002", "Blood", Priority.EMERGENCY, base, 15);
        Sample high = new Sample("S003", "Blood", Priority.HIGH, base, 30);

        engine.enqueue(normal);
        engine.enqueue(urgent);
        engine.enqueue(high);

        assertEquals(3, engine.queueSize());
        assertEquals("S002", engine.getQueue().poll().getId());
        assertEquals("S003", engine.getQueue().poll().getId());
        assertEquals("S001", engine.getQueue().poll().getId());
    }

    @Test
    @DisplayName("Verify machine assignment algorithm")
    void testMachineAssignment() {
        Map<String, Machine> machines = new LinkedHashMap<>();
        Machine m1 = new Machine("M1", "Hematology Analyzer", Set.of("Blood"), 1);
        machines.put(m1.getId(), m1);

        Sample s1 = new Sample("S001", "Blood", Priority.HIGH, base, 30);
        Sample s2 = new Sample("S002", "Blood", Priority.NORMAL, base, 120);
        s1.setStatus(Status.QUEUED);
        s2.setStatus(Status.QUEUED);
        engine.enqueue(s1);
        engine.enqueue(s2);

        List<SchedulerEngine.AssignmentResult> results = engine.assignSamples(machines);

        assertEquals(1, results.size());
        assertEquals("S001", results.get(0).getSample().getId());
        assertEquals(Status.PROCESSING, s1.getStatus());
        assertEquals("M1", s1.getAssignedMachineId());
        assertEquals(1, m1.getCurrentLoad());
        assertEquals(1, engine.queueSize());
        assertEquals("S002", engine.getQueue().peek().getId());
    }

    @Test
    @DisplayName("Verify SLA delay status transitions")
    void testCheckDelay() {
        Sample s = new Sample("S001", "Blood", Priority.HIGH, base, 30);

        assertEquals(DelayStatus.ON_TIME, engine.checkDelay(s, base.plusMinutes(10)));
        assertEquals(DelayStatus.APPROACHING, engine.checkDelay(s, base.plusMinutes(24)));
        assertEquals(DelayStatus.OVERDUE, engine.checkDelay(s, base.plusMinutes(30)));
    }
}
