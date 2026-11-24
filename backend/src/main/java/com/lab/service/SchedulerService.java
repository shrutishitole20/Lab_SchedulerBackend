package com.lab.service;

import com.lab.enums.DelayStatus;
import com.lab.enums.Priority;
import com.lab.enums.Status;
import com.lab.exception.InvalidStateTransitionException;
import com.lab.exception.ResourceNotFoundException;
import com.lab.model.HistoryEntry;
import com.lab.model.Machine;
import com.lab.model.Sample;
import com.lab.repository.AssignmentRepository;
import com.lab.repository.DowntimeRepository;
import com.lab.repository.HistoryRepository;
import com.lab.repository.MachineRepository;
import com.lab.repository.SampleRepository;
import com.lab.scheduler.SchedulerEngine;
import com.lab.util.DBConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Service
public class SchedulerService {

    private final SchedulerEngine schedulerEngine;
    private final Map<String, Machine> machines;
    private final Map<String, Sample> allSamples;
    private final List<HistoryEntry> history;
    private int idCounter;

    private final SampleRepository sampleRepository;
    private final MachineRepository machineRepository;
    private final HistoryRepository historyRepository;
    private final DowntimeRepository downtimeRepository;
    private final AssignmentRepository assignmentRepository;
    private final boolean persistenceEnabled;

    public SchedulerService() {
        this(false);
    }

    @Autowired
    public SchedulerService(SampleRepository sampleRepository,
                            MachineRepository machineRepository,
                            HistoryRepository historyRepository,
                            DowntimeRepository downtimeRepository,
                            AssignmentRepository assignmentRepository) {
        this(sampleRepository, machineRepository, historyRepository, downtimeRepository, assignmentRepository, DBConnection.testConnection());
    }

    public SchedulerService(SampleRepository sampleRepository,
                            MachineRepository machineRepository,
                            HistoryRepository historyRepository) {
        this(sampleRepository, machineRepository, historyRepository, new DowntimeRepository(), new AssignmentRepository(), DBConnection.testConnection());
    }

    public SchedulerService(boolean enablePersistence) {
        this(new SampleRepository(), new MachineRepository(), new HistoryRepository(), new DowntimeRepository(), new AssignmentRepository(), enablePersistence && DBConnection.testConnection());
    }

    public SchedulerService(SampleRepository sampleRepository,
                            MachineRepository machineRepository,
                            HistoryRepository historyRepository,
                            boolean enablePersistence) {
        this(sampleRepository, machineRepository, historyRepository, new DowntimeRepository(), new AssignmentRepository(), enablePersistence);
    }

    public SchedulerService(SampleRepository sampleRepository,
                            MachineRepository machineRepository,
                            HistoryRepository historyRepository,
                            DowntimeRepository downtimeRepository,
                            AssignmentRepository assignmentRepository,
                            boolean enablePersistence) {
        this.schedulerEngine = new SchedulerEngine();
        this.machines = new LinkedHashMap<>();
        this.allSamples = new LinkedHashMap<>();
        this.history = new ArrayList<>();
        this.idCounter = 1;

        this.sampleRepository = (sampleRepository != null) ? sampleRepository : new SampleRepository();
        this.machineRepository = (machineRepository != null) ? machineRepository : new MachineRepository();
        this.historyRepository = (historyRepository != null) ? historyRepository : new HistoryRepository();
        this.downtimeRepository = (downtimeRepository != null) ? downtimeRepository : new DowntimeRepository();
        this.assignmentRepository = (assignmentRepository != null) ? assignmentRepository : new AssignmentRepository();
        this.persistenceEnabled = enablePersistence;

        if (this.persistenceEnabled) {
            loadFromDatabase();
        }
    }

    public synchronized final void loadFromDatabase() {
        try {
            List<Machine> dbMachines = machineRepository.findAll();
            for (Machine m : dbMachines) {
                m.setCurrentLoad(0);
                machines.put(m.getId(), m);
            }

            int maxId = sampleRepository.getMaxSampleIdNumber();

            List<Sample> queuedSamples = sampleRepository.findByStatus(Status.QUEUED);
            List<Sample> processingSamples = sampleRepository.findByStatus(Status.PROCESSING);
            List<Sample> onHoldSamples = sampleRepository.findByStatus(Status.ON_HOLD);

            for (Sample s : processingSamples) {
                allSamples.put(s.getId(), s);
                maxId = Math.max(maxId, extractIdNumber(s.getId()));

                if (s.getAssignedMachineId() != null) {
                    Machine m = machines.get(s.getAssignedMachineId());
                    if (m != null) {
                        m.setCurrentLoad(m.getCurrentLoad() + 1);
                    }
                }
            }

            for (Sample s : onHoldSamples) {
                allSamples.put(s.getId(), s);
                maxId = Math.max(maxId, extractIdNumber(s.getId()));
            }

            for (Sample s : queuedSamples) {
                allSamples.put(s.getId(), s);
                schedulerEngine.enqueue(s);
                maxId = Math.max(maxId, extractIdNumber(s.getId()));
            }

            this.idCounter = maxId + 1;
            assignSamples();

        } catch (Exception e) {
            System.err.println("Warning: Error loading state from database: " + e.getMessage());
        }
    }

    private int extractIdNumber(String id) {
        if (id == null) return 0;
        try {
            String digits = id.replaceAll("\\D+", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public synchronized void addMachine(Machine m) {
        if (m == null || m.getId() == null) {
            throw new IllegalArgumentException("Machine and its ID cannot be null.");
        }
        machines.put(m.getId(), m);

        if (persistenceEnabled) {
            machineRepository.save(m);
        }
        assignSamples();
    }

    public synchronized void updateMachine(Machine m) {
        if (m == null || m.getId() == null) {
            throw new IllegalArgumentException("Machine and its ID cannot be null.");
        }
        machines.put(m.getId(), m);

        if (persistenceEnabled) {
            machineRepository.update(m);
        }
        assignSamples();
    }

    public synchronized Sample registerSample(String testType, Priority priority, LocalDateTime receivedTime) {
        if (priority == null) {
            throw new IllegalArgumentException("Priority cannot be null.");
        }
        return registerSample(null, testType, null, null, priority, receivedTime, priority.getDefaultTargetMinutes());
    }

    public synchronized Sample registerSample(String testType, Priority priority, LocalDateTime receivedTime, int targetMinutes) {
        return registerSample(null, testType, null, null, priority, receivedTime, targetMinutes);
    }

    public synchronized Sample registerSample(String sampleType, String testType, String testCategory, String processingType,
                                             Priority priority, LocalDateTime receivedTime, int targetMinutes) {
        if (testType == null || testType.trim().isEmpty()) {
            throw new IllegalArgumentException("Test type cannot be null or empty.");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Priority cannot be null.");
        }
        if (receivedTime == null) {
            receivedTime = LocalDateTime.now();
        }
        if (targetMinutes <= 0) {
            throw new IllegalArgumentException("Target turnaround duration must be greater than 0 minutes.");
        }

        while (allSamples.containsKey(String.format("S%03d", idCounter))) {
            idCounter++;
        }
        String id = String.format("S%03d", idCounter++);

        Sample sample = new Sample(id, sampleType, testType.trim(), testCategory, processingType, priority, receivedTime, targetMinutes);

        sample.forceStatus(Status.RECEIVED);
        HistoryEntry recEntry = new HistoryEntry(
                id,
                receivedTime,
                "RECEIVED",
                "Sample received in laboratory (Type: " + sample.getSampleType() + ", Priority: " + priority.name() + ", Target: " + targetMinutes + " min)"
        );
        history.add(recEntry);

        sample.setStatus(Status.QUEUED);
        int queuePosition = schedulerEngine.queueSize() + 1;
        HistoryEntry qEntry = new HistoryEntry(
                id,
                receivedTime,
                "QUEUED",
                "Sample placed in priority queue (Position: #" + queuePosition + ")"
        );
        history.add(qEntry);

        allSamples.put(id, sample);
        schedulerEngine.enqueue(sample);

        if (persistenceEnabled) {
            sampleRepository.save(sample);
            historyRepository.save(recEntry);
            historyRepository.save(qEntry);
        }

        assignSamples();
        return sample;
    }

    public synchronized void assignSamples() {
        List<SchedulerEngine.AssignmentResult> assignments = schedulerEngine.assignSamples(machines);

        for (SchedulerEngine.AssignmentResult res : assignments) {
            Sample sample = res.getSample();
            Machine machine = res.getMachine();
            LocalDateTime now = res.getAssignedTime();

            HistoryEntry procEntry = new HistoryEntry(
                    sample.getId(),
                    now,
                    "PROCESSING",
                    "Assigned to machine " + machine.getId() + " (" + machine.getName() + ")"
            );
            history.add(procEntry);

            if (persistenceEnabled) {
                sampleRepository.update(sample);
                machineRepository.update(machine);
                historyRepository.save(procEntry);
                assignmentRepository.recordAssignment(sample.getId(), machine.getId(), "PROCESSING", "Assigned to machine " + machine.getId());
            }
        }
    }

    public synchronized void completeSample(String id) {
        Sample sample = allSamples.get(id);
        if (sample == null) {
            throw new ResourceNotFoundException("Sample not found with ID: " + id);
        }
        if (sample.getStatus() != Status.PROCESSING) {
            throw new InvalidStateTransitionException("Sample " + id + " cannot be completed because it is in status: " + sample.getStatus());
        }

        sample.setStatus(Status.COMPLETED);
        LocalDateTime now = LocalDateTime.now();
        sample.setCompletionTime(now);

        String machineId = sample.getAssignedMachineId();
        Machine machine = machineId != null ? machines.get(machineId) : null;
        if (machine != null && machine.getCurrentLoad() > 0) {
            machine.setCurrentLoad(machine.getCurrentLoad() - 1);
        }

        DelayStatus delayStatus = checkDelay(sample, now);
        long elapsedMinutes = (sample.getReceivedTime() != null)
                ? Duration.between(sample.getReceivedTime(), now).toMinutes()
                : 0;

        HistoryEntry compEntry = new HistoryEntry(
                sample.getId(),
                now,
                "COMPLETED",
                "Processing completed on machine " + machineId + " (Duration: " + elapsedMinutes + " min, SLA: " + delayStatus + ")"
        );
        history.add(compEntry);

        if (persistenceEnabled) {
            sampleRepository.update(sample);
            if (machine != null) {
                machineRepository.update(machine);
            }
            historyRepository.save(compEntry);
            if (machineId != null) {
                assignmentRepository.updateStatus(sample.getId(), machineId, "COMPLETED", sample.getStartTime(), now);
            }
        }

        assignSamples();
    }

    public synchronized void markMachineDown(String machineId) {
        markMachineDown(machineId, "Machine " + machineId + " marked DOWN. Active processing samples remain untouched.");
    }

    public synchronized void markMachineDown(String machineId, String reason) {
        Machine machine = machines.get(machineId);
        if (machine == null) {
            throw new ResourceNotFoundException("Machine not found with ID: " + machineId);
        }

        machine.setAvailable(false);
        String details = (reason != null && !reason.trim().isEmpty()) ? reason : "Machine " + machineId + " marked DOWN.";
        HistoryEntry downEntry = new HistoryEntry(
                "SYSTEM",
                LocalDateTime.now(),
                "MACHINE_DOWN",
                details
        );
        history.add(downEntry);

        if (persistenceEnabled) {
            machineRepository.update(machine);
            historyRepository.save(downEntry);
            downtimeRepository.recordDowntime(machineId, details, "SYSTEM");
        }

        assignSamples();
    }

    public synchronized void markMachineUp(String machineId) {
        Machine machine = machines.get(machineId);
        if (machine == null) {
            throw new ResourceNotFoundException("Machine not found with ID: " + machineId);
        }

        machine.setAvailable(true);
        HistoryEntry upEntry = new HistoryEntry(
                "SYSTEM",
                LocalDateTime.now(),
                "MACHINE_UP",
                "Machine " + machineId + " marked UP/AVAILABLE."
        );
        history.add(upEntry);

        if (persistenceEnabled) {
            machineRepository.update(machine);
            historyRepository.save(upEntry);
            downtimeRepository.resolveDowntime(machineId);
        }

        assignSamples();
    }

    public synchronized void putOnHold(String sampleId) {
        Sample sample = allSamples.get(sampleId);
        if (sample == null) {
            throw new ResourceNotFoundException("Sample not found with ID: " + sampleId);
        }
        if (sample.getStatus() == Status.COMPLETED) {
            throw new InvalidStateTransitionException("Cannot place COMPLETED sample " + sampleId + " on hold.");
        }

        if (sample.getStatus() == Status.PROCESSING) {
            String machineId = sample.getAssignedMachineId();
            if (machineId != null) {
                Machine m = machines.get(machineId);
                if (m != null && m.getCurrentLoad() > 0) {
                    m.setCurrentLoad(m.getCurrentLoad() - 1);
                }
            }
            sample.setAssignedMachineId(null);
        } else if (sample.getStatus() == Status.QUEUED || sample.getStatus() == Status.RECEIVED) {
            schedulerEngine.remove(sample);
        }

        sample.setStatus(Status.ON_HOLD);
        HistoryEntry holdEntry = new HistoryEntry(
                sampleId,
                LocalDateTime.now(),
                "ON_HOLD",
                "Sample placed on hold and removed from priority queue."
        );
        history.add(holdEntry);

        if (persistenceEnabled) {
            sampleRepository.update(sample);
            historyRepository.save(holdEntry);
        }

        assignSamples();
    }

    public synchronized void releaseFromHold(String sampleId) {
        Sample sample = allSamples.get(sampleId);
        if (sample == null) {
            throw new ResourceNotFoundException("Sample not found with ID: " + sampleId);
        }
        if (sample.getStatus() != Status.ON_HOLD) {
            throw new InvalidStateTransitionException("Sample " + sampleId + " is not ON_HOLD (current: " + sample.getStatus() + ").");
        }

        sample.setStatus(Status.QUEUED);
        schedulerEngine.enqueue(sample);
        HistoryEntry relEntry = new HistoryEntry(
                sampleId,
                LocalDateTime.now(),
                "RELEASED",
                "Sample released from hold and returned to priority queue."
        );
        history.add(relEntry);

        if (persistenceEnabled) {
            sampleRepository.update(sample);
            historyRepository.save(relEntry);
        }

        assignSamples();
    }

    public DelayStatus checkDelay(Sample s, LocalDateTime now) {
        return schedulerEngine.checkDelay(s, now);
    }

    public List<Sample> getDelayedSamples(LocalDateTime now) {
        return schedulerEngine.getDelayedSamples(allSamples.values(), now);
    }

    public void displayQueue() {
        schedulerEngine.displayQueue();
    }

    public void displayMachineWorkload() {
        schedulerEngine.displayMachineWorkload(machines);
    }

    public PriorityQueue<Sample> getQueue() { return schedulerEngine.getQueue(); }
    public Map<String, Machine> getMachines() { return Collections.unmodifiableMap(machines); }
    public Map<String, Sample> getAllSamples() { return Collections.unmodifiableMap(allSamples); }
    public List<HistoryEntry> getHistory() { return Collections.unmodifiableList(history); }
    public boolean isPersistenceEnabled() { return persistenceEnabled; }
}
