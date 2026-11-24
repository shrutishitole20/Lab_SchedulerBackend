package com.lab.scheduler;

import com.lab.enums.DelayStatus;
import com.lab.enums.Status;
import com.lab.model.Machine;
import com.lab.model.Sample;
import com.lab.util.DateTimeUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Core Scheduler Engine handling PriorityQueue management, machine matching,
 * workload balancing, and SLA delay analysis.
 */
public class SchedulerEngine {

    private final PriorityQueue<Sample> queue;

    public SchedulerEngine() {
        this.queue = new PriorityQueue<>(new SampleComparator());
    }

    public synchronized PriorityQueue<Sample> getQueue() {
        return queue;
    }

    public synchronized void enqueue(Sample sample) {
        if (sample != null) {
            if (sample.getStatus() == Status.RECEIVED) {
                sample.setStatus(Status.QUEUED);
            }
            queue.add(sample);
        }
    }

    public synchronized boolean remove(Sample sample) {
        return queue.remove(sample);
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    public synchronized int queueSize() {
        return queue.size();
    }

    public synchronized void clear() {
        queue.clear();
    }

    /**
     * Matches eligible queued samples to available and compatible machines.
     * Returns a list of newly assigned sample-machine pairs.
     */
    public synchronized List<AssignmentResult> assignSamples(Map<String, Machine> machines) {
        List<AssignmentResult> assignedResults = new ArrayList<>();
        List<Sample> unassigned = new ArrayList<>();

        while (!queue.isEmpty()) {
            Sample sample = queue.poll();

            if (sample.getStatus() == Status.PROCESSING) {
                continue;
            }

            Machine targetMachine = null;
            for (Machine m : machines.values()) {
                if (m.isAvailable() && m.canProcess(sample.getTestType()) && m.hasFreeSlot()) {
                    targetMachine = m;
                    break;
                }
            }

            if (targetMachine != null) {
                sample.setStatus(Status.PROCESSING);
                LocalDateTime now = LocalDateTime.now();
                sample.setStartTime(now);
                sample.setAssignedMachineId(targetMachine.getId());
                targetMachine.setCurrentLoad(targetMachine.getCurrentLoad() + 1);

                assignedResults.add(new AssignmentResult(sample, targetMachine, now));
            } else {
                unassigned.add(sample);
            }
        }

        queue.addAll(unassigned);
        return assignedResults;
    }

    /**
     * Evaluates sample SLA status: ON_TIME, APPROACHING (>=80% time elapsed), or OVERDUE.
     */
    public DelayStatus checkDelay(Sample s, LocalDateTime now) {
        if (s == null || s.getReceivedTime() == null || now == null) {
            return DelayStatus.ON_TIME;
        }

        LocalDateTime deadline = s.getDeadline();
        if (deadline == null) {
            return DelayStatus.ON_TIME;
        }

        if (!now.isBefore(deadline)) {
            return DelayStatus.OVERDUE;
        }

        long totalSeconds = (long) s.getTargetMinutes() * 60;
        long elapsedSeconds = Duration.between(s.getReceivedTime(), now).getSeconds();

        if (elapsedSeconds >= (totalSeconds * 0.8)) {
            return DelayStatus.APPROACHING;
        }

        return DelayStatus.ON_TIME;
    }

    /**
     * Returns list of non-completed samples that are either APPROACHING SLA or OVERDUE.
     */
    public List<Sample> getDelayedSamples(Collection<Sample> samples, LocalDateTime now) {
        List<Sample> delayed = new ArrayList<>();
        for (Sample s : samples) {
            if (s.getStatus() != Status.COMPLETED) {
                DelayStatus delayStatus = checkDelay(s, now);
                if (delayStatus == DelayStatus.APPROACHING || delayStatus == DelayStatus.OVERDUE) {
                    delayed.add(s);
                }
            }
        }
        return delayed;
    }

    public void displayQueue() {
        System.out.println("=========================================================================================");
        System.out.printf("                              WAITING QUEUE (%d SAMPLES)                                 %n", queue.size());
        System.out.println("=========================================================================================");
        if (queue.isEmpty()) {
            System.out.println("  (No samples currently waiting in queue)");
            System.out.println("-----------------------------------------------------------------------------------------\n");
            return;
        }

        System.out.printf("%-5s | %-6s | %-10s | %-16s | %-10s | %-16s | %-16s%n",
                "Pos", "ID", "Test Type", "Priority", "Target", "Received", "Deadline");
        System.out.println("-----------------------------------------------------------------------------------------");

        PriorityQueue<Sample> copyQueue = new PriorityQueue<>(queue);
        int pos = 1;
        while (!copyQueue.isEmpty()) {
            Sample s = copyQueue.poll();
            System.out.printf("#%-4d | %-6s | %-10s | %-16s | %-3d min   | %-16s | %-16s%n",
                    pos++,
                    s.getId(),
                    s.getTestType(),
                    (s.getPriority() != null ? s.getPriority().name() + " (R" + s.getPriority().getRank() + ")" : "NORMAL"),
                    s.getTargetMinutes(),
                    (s.getReceivedTime() != null ? s.getReceivedTime().format(DateTimeUtil.DATE_TIME_FORMATTER) : "N/A"),
                    (s.getDeadline() != null ? s.getDeadline().format(DateTimeUtil.DATE_TIME_FORMATTER) : "N/A")
            );
        }
        System.out.println("-----------------------------------------------------------------------------------------\n");
    }

    public void displayMachineWorkload(Map<String, Machine> machines) {
        System.out.println("=========================================================================================");
        System.out.println("                               MACHINE WORKLOAD STATUS                                   ");
        System.out.println("=========================================================================================");
        System.out.printf("%-6s | %-32s | %-12s | %-10s | %-10s | %-20s%n",
                "ID", "Name", "Workload", "Free Slots", "Status", "Supported Tests");
        System.out.println("-----------------------------------------------------------------------------------------");

        for (Machine m : machines.values()) {
            int freeSlots = m.isAvailable() ? Math.max(0, m.getCapacity() - m.getCurrentLoad()) : 0;
            String statusStr = m.isAvailable()
                    ? (m.getCurrentLoad() >= m.getCapacity() ? "FULL" : "AVAILABLE")
                    : "DOWN";

            System.out.printf("%-6s | %-32s | %2d / %-2d %-4s | %-10d | %-10s | %-20s%n",
                    m.getId(),
                    m.getName(),
                    m.getCurrentLoad(),
                    m.getCapacity(),
                    (m.getCurrentLoad() >= m.getCapacity() && m.isAvailable() ? "[BUSY]" : ""),
                    freeSlots,
                    statusStr,
                    m.getSupportedTestTypes()
            );
        }
        System.out.println("-----------------------------------------------------------------------------------------\n");
    }

    public static class AssignmentResult {
        private final Sample sample;
        private final Machine machine;
        private final LocalDateTime assignedTime;

        public AssignmentResult(Sample sample, Machine machine, LocalDateTime assignedTime) {
            this.sample = sample;
            this.machine = machine;
            this.assignedTime = assignedTime;
        }

        public Sample getSample() { return sample; }
        public Machine getMachine() { return machine; }
        public LocalDateTime getAssignedTime() { return assignedTime; }
    }
}
