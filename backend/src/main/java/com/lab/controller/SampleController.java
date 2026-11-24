package com.lab.controller;

import com.lab.dto.ApiResponse;
import com.lab.dto.SampleRequest;
import com.lab.enums.Status;
import com.lab.exception.ResourceNotFoundException;
import com.lab.model.HistoryEntry;
import com.lab.model.Sample;
import com.lab.repository.HistoryRepository;
import com.lab.service.SchedulerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/samples")
public class SampleController {

    private final SchedulerService schedulerService;
    private final HistoryRepository historyRepository;

    @Autowired
    public SampleController(SchedulerService schedulerService, HistoryRepository historyRepository) {
        this.schedulerService = schedulerService;
        this.historyRepository = historyRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Sample>>> getAllSamples(@RequestParam(required = false) Status status) {
        List<Sample> list = new ArrayList<>(schedulerService.getAllSamples().values());
        if (status != null) {
            list = list.stream().filter(s -> s.getStatus() == status).toList();
        }
        return ResponseEntity.ok(ApiResponse.ok("Samples retrieved successfully", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Sample>> getSampleById(@PathVariable String id) {
        Sample sample = schedulerService.getAllSamples().get(id);
        if (sample == null) {
            throw new ResourceNotFoundException("Sample not found with ID: " + id);
        }
        return ResponseEntity.ok(ApiResponse.ok("Sample retrieved successfully", sample));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Sample>> registerSample(@Valid @RequestBody SampleRequest request) {
        LocalDateTime receivedTime = request.getReceivedTime() != null ? request.getReceivedTime() : LocalDateTime.now();
        int targetMinutes = request.getTargetMinutes() != null ? request.getTargetMinutes() : request.getPriority().getDefaultTargetMinutes();

        Sample created = schedulerService.registerSample(
                request.getSampleType(),
                request.getTestType(),
                request.getTestCategory(),
                request.getProcessingType(),
                request.getPriority(),
                receivedTime,
                targetMinutes
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Sample registered successfully", created));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<Sample>> completeSample(@PathVariable String id) {
        Sample sample = schedulerService.getAllSamples().get(id);
        if (sample == null) {
            throw new ResourceNotFoundException("Sample not found with ID: " + id);
        }

        schedulerService.completeSample(id);
        return ResponseEntity.ok(ApiResponse.ok("Sample " + id + " marked as COMPLETED", sample));
    }

    @PostMapping("/{id}/hold")
    public ResponseEntity<ApiResponse<Sample>> putOnHold(@PathVariable String id) {
        Sample sample = schedulerService.getAllSamples().get(id);
        if (sample == null) {
            throw new ResourceNotFoundException("Sample not found with ID: " + id);
        }

        schedulerService.putOnHold(id);
        return ResponseEntity.ok(ApiResponse.ok("Sample " + id + " placed ON_HOLD", sample));
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<ApiResponse<Sample>> releaseFromHold(@PathVariable String id) {
        Sample sample = schedulerService.getAllSamples().get(id);
        if (sample == null) {
            throw new ResourceNotFoundException("Sample not found with ID: " + id);
        }

        schedulerService.releaseFromHold(id);
        return ResponseEntity.ok(ApiResponse.ok("Sample " + id + " released from hold and re-queued", sample));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<HistoryEntry>>> getSampleHistory(@PathVariable String id) {
        Sample sample = schedulerService.getAllSamples().get(id);
        if (sample == null) {
            throw new ResourceNotFoundException("Sample not found with ID: " + id);
        }

        List<HistoryEntry> historyList = (historyRepository != null) ? historyRepository.findBySampleId(id) : List.of();

        if (historyList.isEmpty()) {
            historyList = schedulerService.getHistory().stream()
                    .filter(h -> id.equalsIgnoreCase(h.getSampleId()))
                    .toList();
        }

        return ResponseEntity.ok(ApiResponse.ok("History for sample " + id, historyList));
    }
}
