package com.lab;

import com.lab.dao.DBConnection;
import com.lab.enums.DelayStatus;
import com.lab.enums.Priority;
import com.lab.enums.Status;
import com.lab.model.Machine;
import com.lab.model.Sample;
import com.lab.service.SchedulerService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class Main {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public static void main(String[] args) {
        System.out.println("=========================================================================================");
        System.out.println("            LAB SAMPLE SCHEDULER - DATABASE-SYNCHRONIZED SERVICE                         ");
        System.out.println("=========================================================================================\n");

        boolean dbConnected = DBConnection.testConnection();
        if (dbConnected) {
            System.out.println("[DATABASE] Successfully connected to MySQL using config.properties.");
            System.out.println("[DATABASE] Startup sync: Auto-loading machines and unfinished samples from DB...\n");
        } else {
            System.out.println("[DATABASE] MySQL not reachable at localhost:3306 (or invalid password in config.properties).");
            System.out.println("[DATABASE] Scheduler running in local memory mode.\n");
        }

        SchedulerService scheduler = new SchedulerService(dbConnected);

        if (!scheduler.getMachines().containsKey("M1")) {
            scheduler.addMachine(new Machine("M1", "Hematology Analyzer", Set.of("Blood", "Urine"), 2));
        }
        if (!scheduler.getMachines().containsKey("M2")) {
            scheduler.addMachine(new Machine("M2", "Endocrine Analyzer", Set.of("Blood", "Thyroid"), 1));
        }

        System.out.println("Current Machines in Service:");
        scheduler.displayMachineWorkload();

        LocalDateTime baseTime = LocalDateTime.of(2026, 11, 18, 9, 0, 0);

        if (scheduler.getAllSamples().isEmpty()) {
            System.out.println(">>> Registering samples S001 - S005 into Scheduler (persisted to MySQL)...");
            scheduler.registerSample("Blood", Priority.HIGH, baseTime, 30);
            scheduler.registerSample("Urine", Priority.NORMAL, baseTime, 120);
            scheduler.registerSample("Blood", Priority.EMERGENCY, baseTime, 15);
            scheduler.registerSample("Thyroid", Priority.NORMAL, baseTime, 180);
            scheduler.registerSample("Blood", Priority.HIGH, baseTime, 45);
        } else {
            System.out.printf("Loaded %d sample(s) directly from MySQL on startup.%n", scheduler.getAllSamples().size());
        }

        scheduler.displayMachineWorkload();
        scheduler.displayQueue();

        LocalDateTime timeAt9_25 = baseTime.plusMinutes(25);
        System.out.println(">>> SLA Status Check at 09:25 (25 min elapsed):");
        for (Sample s : scheduler.getAllSamples().values()) {
            DelayStatus delayStatus = scheduler.checkDelay(s, timeAt9_25);
            String deadlineStr = (s.getDeadline() != null) ? s.getDeadline().format(TIME_FMT) : "N/A";
            String priorityStr = (s.getPriority() != null) ? s.getPriority().name() : "NORMAL";
            System.out.printf("  - Sample %-5s (%-22s | %-9s | Target: %3dm | Deadline: %s) -> Status: %s%n",
                    s.getId(), s.getTestType(), priorityStr, s.getTargetMinutes(),
                    deadlineStr, delayStatus);
        }

        Sample firstProcessing = scheduler.getAllSamples().values().stream()
                .filter(s -> s.getStatus() == Status.PROCESSING)
                .findFirst()
                .orElse(null);

        if (firstProcessing != null) {
            System.out.printf("%n>>> Completing processing for %s on machine %s...%n",
                    firstProcessing.getId(), firstProcessing.getAssignedMachineId());
            scheduler.completeSample(firstProcessing.getId());
            scheduler.displayMachineWorkload();
            scheduler.displayQueue();
        }

        System.out.println("=========================================================================================");
        System.out.println("                       SCHEDULER RUN COMPLETED SUCCESSFULLY                              ");
        System.out.println("=========================================================================================");
        DBConnection.shutdown();
    }
}
