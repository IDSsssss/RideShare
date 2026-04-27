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
        log.info("🔄 Started async processing for task {}", taskId);

        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < bookingRequests.size(); i++) {
            BookingRequestDto request = bookingRequests.get(i);
            try {
                // Вызываем через отдельный сервис - @Transactional сработает!
                bookingProcessor.processSingleBooking(request);
                taskTracker.incrementProcessed(taskId);
                successCount++;
                log.info("📝 Task {}: processed {}/{} - Ride {} User {} Seats {}",
                        taskId, i + 1, bookingRequests.size(),
                        request.getRideId(), request.getPassengerId(), request.getSeats());
            } catch (Exception e) {
                failCount++;
                String error = String.format("Ride %d, User %d: %s",
                        request.getRideId(), request.getPassengerId(), e.getMessage());
                log.error("❌ Failed: {}", error);
                taskTracker.addError(taskId, error);
            }
        }

        if (failCount > 0) {
            log.warn("⚠️ Task {} completed with {} successes and {} failures",
                    taskId, successCount, failCount);
            if (successCount == 0) {
                taskTracker.failTask(taskId, "All bookings failed");
            } else {
                taskTracker.completeTask(taskId);
            }
        } else {
            taskTracker.completeTask(taskId);
            log.info("✅ Task {} completed successfully. Processed {}/{} bookings",
                    taskId, successCount, bookingRequests.size());
        }
    }
}