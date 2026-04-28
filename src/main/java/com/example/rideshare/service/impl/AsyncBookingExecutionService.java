package com.example.rideshare.service.impl;

import com.example.rideshare.model.dto.BookingRequestDto;
import com.example.rideshare.utils.BookingTaskTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncBookingExecutionService {

    private final BookingTaskTracker taskTracker;
    private final AsyncBookingProcessor bookingProcessor;

    @Async("bookingExecutor")
    public void doAsyncProcessing(String taskId, List<BookingRequestDto> bookingRequests) {
        taskTracker.updateStatus(taskId, "PROCESSING");

        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            taskTracker.failTask(taskId, "Task was interrupted");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (BookingRequestDto request : bookingRequests) {
            try {
                bookingProcessor.processSingleBooking(request);
                taskTracker.incrementProcessed(taskId);
                successCount++;
            } catch (Exception e) {
                failCount++;
                String error = String.format("Ride %d, User %d: %s",
                        request.getRideId(), request.getPassengerId(), e.getMessage());
                taskTracker.addError(taskId, error);
            }
        }

        if (failCount > 0) {
            if (successCount == 0) {
                taskTracker.failTask(taskId, "All bookings failed");
            } else {
                taskTracker.completeTask(taskId);
            }
        } else {
            taskTracker.completeTask(taskId);
        }
    }
}