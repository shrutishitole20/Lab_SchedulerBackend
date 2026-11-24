package com.lab.service;

import com.lab.dto.DailyReportResponse;
import com.lab.enums.DelayStatus;
import com.lab.enums.Status;
import com.lab.model.Sample;
import com.lab.repository.SampleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final SchedulerService schedulerService;
    private final SampleRepository sampleRepository;

    @Autowired
    public ReportService(SchedulerService schedulerService, SampleRepository sampleRepository) {
        this.schedulerService = schedulerService;
        this.sampleRepository = (sampleRepository != null) ? sampleRepository : new SampleRepository();
    }

    public DailyReportResponse generateDailyReport(LocalDate date) {
        LocalDate reportDate = (date != null) ? date : LocalDate.now();
        LocalDateTime refTime = LocalDateTime.now();

        Map<String, Sample> combinedPool = new LinkedHashMap<>();
        try {
            List<Sample> dbSamples = sampleRepository.findAll();
            for (Sample s : dbSamples) {
                combinedPool.put(s.getId(), s);
            }
        } catch (Exception ignored) {
        }

        for (Sample s : schedulerService.getAllSamples().values()) {
            combinedPool.put(s.getId(), s);
        }

        long completedCount = 0;
        long pendingCount = 0;
        long delayedCount = 0;
        long onHoldCount = 0;
        List<Sample> relevantSamples = new ArrayList<>();

        for (Sample s : combinedPool.values()) {
            LocalDate receivedDate = (s.getReceivedTime() != null) ? s.getReceivedTime().toLocalDate() : null;
            LocalDate completionDate = (s.getCompletionTime() != null) ? s.getCompletionTime().toLocalDate() : null;

            if (s.getStatus() == Status.COMPLETED) {
                if (reportDate.equals(completionDate) || reportDate.equals(receivedDate)) {
                    completedCount++;
                    relevantSamples.add(s);
                }
            } else if (s.getStatus() == Status.ON_HOLD) {
                if (reportDate.equals(receivedDate)) {
                    onHoldCount++;
                    relevantSamples.add(s);
                }
            } else if (s.getStatus() == Status.QUEUED || s.getStatus() == Status.PROCESSING || s.getStatus() == Status.RECEIVED) {
                if (reportDate.equals(receivedDate)) {
                    pendingCount++;
                    relevantSamples.add(s);

                    DelayStatus delayStatus = schedulerService.checkDelay(s, refTime);
                    if (delayStatus == DelayStatus.APPROACHING || delayStatus == DelayStatus.OVERDUE) {
                        delayedCount++;
                    }
                }
            }
        }

        return new DailyReportResponse(
                reportDate,
                completedCount,
                pendingCount,
                delayedCount,
                onHoldCount,
                relevantSamples
        );
    }
}
