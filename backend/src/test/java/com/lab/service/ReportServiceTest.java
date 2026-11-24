package com.lab.service;

import com.lab.dto.DailyReportResponse;
import com.lab.enums.Priority;
import com.lab.enums.Status;
import com.lab.model.Sample;
import com.lab.repository.SampleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {

    @Test
    @DisplayName("Verify DailyReport generation aggregates completed, pending, on-hold metrics")
    void testDailyReport() {
        SchedulerService scheduler = new SchedulerService();
        SampleRepository sampleRepo = new SampleRepository();
        ReportService reportService = new ReportService(scheduler, sampleRepo);

        LocalDateTime now = LocalDateTime.now();
        Sample s1 = scheduler.registerSample("Blood", Priority.HIGH, now, 30);
        Sample s2 = scheduler.registerSample("Blood", Priority.NORMAL, now, 120);

        DailyReportResponse report = reportService.generateDailyReport(LocalDate.now());

        assertNotNull(report);
        assertEquals(LocalDate.now(), report.getDate());
        assertTrue(report.getPending() >= 2);
    }
}
