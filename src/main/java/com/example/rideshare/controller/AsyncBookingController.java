package com.example.rideshare.controller;

import com.example.rideshare.model.dto.AsyncTaskResponse;
import com.example.rideshare.model.dto.BookingRequestDto;
import com.example.rideshare.model.dto.TaskStatusResponse;
import com.example.rideshare.service.AsyncBookingService;

import com.example.rideshare.service.impl.CounterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/async")
@RequiredArgsConstructor
@Tag(name = "Async Booking Controller", description = "API for asynchronous booking processing")
public class AsyncBookingController {

    private final AsyncBookingService asyncBookingService;
    private final CounterService counterService;

    @PostMapping("/bookings")
    @Operation(summary = "Create bookings asynchronously", description = "Returns task ID for status tracking")
    public ResponseEntity<AsyncTaskResponse> createBookingsAsync(
            @RequestBody List<BookingRequestDto> bookingRequests
    ) {
        String taskId = asyncBookingService.processBookingsAsync(bookingRequests);

        return ResponseEntity.accepted().body(
                new AsyncTaskResponse(taskId, "ACCEPTED", "Task queued for processing")
        );
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "Get task status", description = "Returns current status of async task")
    public ResponseEntity<TaskStatusResponse> getTaskStatus(@PathVariable String taskId) {

        TaskStatusResponse status = asyncBookingService.getTaskStatus(taskId);

        if (status == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(status);
    }

    @PostMapping("/race-demo/unsafe")
    @Operation(summary = "Demonstrate race condition", description = "Shows race condition with unsafe counter")
    public ResponseEntity<CounterService.RaceConditionResult> demonstrateRaceCondition(
            @RequestParam(defaultValue = "50") int threads,
            @RequestParam(defaultValue = "1000") int operations
    ) {
        CounterService.RaceConditionResult result = counterService.demonstrateRaceCondition(threads, operations);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/race-demo/safe")
    @Operation(summary = "Demonstrate solution", description = "Shows solution with atomic counter")
    public ResponseEntity<CounterService.RaceConditionResult> demonstrateSolution(
            @RequestParam(defaultValue = "50") int threads,
            @RequestParam(defaultValue = "1000") int operations
    ) {
        CounterService.RaceConditionResult result = counterService.demonstrateSolution(threads, operations);

        return ResponseEntity.ok(result);
    }
}