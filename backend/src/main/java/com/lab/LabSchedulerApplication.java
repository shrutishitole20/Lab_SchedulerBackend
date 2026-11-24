package com.lab;

import com.lab.model.Machine;
import com.lab.enums.Priority;
import com.lab.service.SchedulerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.util.Set;

@SpringBootApplication
public class LabSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabSchedulerApplication.class, args);
    }

    @Bean
    public CommandLineRunner initDefaultData(SchedulerService scheduler) {
        return args -> {

            if (scheduler.getMachines().isEmpty()) {
                scheduler.addMachine(new Machine("M1", "Hematology & Chemistry Analyzer", Set.of("Blood", "Urine"), 2));
                scheduler.addMachine(new Machine("M2", "Endocrine & Immunoassay Analyzer", Set.of("Blood", "Thyroid"), 1));
                System.out.println("[LabScheduler] Auto-seeded default machines: M1, M2");
            }

            if (scheduler.getAllSamples().isEmpty()) {
                LocalDateTime baseTime = LocalDateTime.of(2026, 11, 18, 9, 0, 0);
                scheduler.registerSample("Blood", Priority.HIGH, baseTime, 30);
                scheduler.registerSample("Urine", Priority.NORMAL, baseTime, 120);
                scheduler.registerSample("Blood", Priority.EMERGENCY, baseTime, 15);
                scheduler.registerSample("Thyroid", Priority.NORMAL, baseTime, 180);
                scheduler.registerSample("Blood", Priority.HIGH, baseTime, 45);
                System.out.println("[LabScheduler] Auto-seeded default samples: S001 - S005");
            }
        };
    }
}

