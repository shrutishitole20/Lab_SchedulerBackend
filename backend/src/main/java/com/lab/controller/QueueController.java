package com.lab.controller;

import com.lab.dto.ApiResponse;
import com.lab.model.Sample;
import com.lab.service.SchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    private final SchedulerService schedulerService;

    @Autowired
    public QueueController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Sample>>> getPriorityQueue() {
        PriorityQueue<Sample> copyQueue = new PriorityQueue<>(schedulerService.getQueue());
        List<Sample> orderedList = new ArrayList<>();
        while (!copyQueue.isEmpty()) {
            orderedList.add(copyQueue.poll());
        }

        return ResponseEntity.ok(ApiResponse.ok("Priority queue retrieved in scheduling order", orderedList));
    }

    @PostMapping("/dispatch")
    public ResponseEntity<ApiResponse<List<Sample>>> dispatchQueue() {
        schedulerService.assignSamples();

        PriorityQueue<Sample> copyQueue = new PriorityQueue<>(schedulerService.getQueue());
        List<Sample> orderedList = new ArrayList<>();
        while (!copyQueue.isEmpty()) {
            orderedList.add(copyQueue.poll());
        }

        return ResponseEntity.ok(ApiResponse.ok("Queue evaluated and eligible samples dispatched to machines", orderedList));
    }
}
