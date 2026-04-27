package com.example.rideshare.service.impl;

import com.example.rideshare.model.dto.BookingRequestDto;
import com.example.rideshare.model.dto.TaskStatusResponse;
import com.example.rideshare.model.entity.Booking;
import com.example.rideshare.model.entity.Ride;
import com.example.rideshare.model.entity.User;
import com.example.rideshare.model.enums.BookingStatus;
import com.example.rideshare.repository.BookingRepository;
import com.example.rideshare.repository.RideRepository;
import com.example.rideshare.repository.UserRepository;
import com.example.rideshare.service.AsyncBookingService;
import com.example.rideshare.utils.BookingTaskTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncBookingServiceImpl implements AsyncBookingService {

    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final BookingTaskTracker taskTracker;

    @Override
    public String processBookingsAsync(List<BookingRequestDto> bookingRequests) {
        String taskId = taskTracker.generateAndCreateTask(bookingRequests.size());
        log.info("✅ Generated taskId: {} for {} bookings", taskId, bookingRequests.size());

        doAsyncProcessing(taskId, bookingRequests);

        return taskId;
    }

    @Async("bookingExecutor")
    public void doAsyncProcessing(String taskId, List<BookingRequestDto> bookingRequests) {
        taskTracker.updateStatus(taskId, "PROCESSING");
        log.info("🔄 Started async processing for task {}", taskId);

        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < bookingRequests.size(); i++) {
            BookingRequestDto request = bookingRequests.get(i);
            try {
                processSingleBooking(request);
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
                taskTracker.addError(taskId, error);  // Добавляем ошибку в список
            }
        }

        if (failCount > 0) {
            log.warn("⚠️ Task {} completed with {} successes and {} failures", taskId, successCount, failCount);
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

    @Transactional
    protected void processSingleBooking(BookingRequestDto request) {
        Ride ride = rideRepository.findById(request.getRideId())
                .orElseThrow(() -> new RuntimeException("Ride not found with id: " + request.getRideId()));

        User passenger = userRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getPassengerId()));

        Integer bookedSeats = bookingRepository.getTotalBookedSeatsForRide(request.getRideId());
        if (bookedSeats == null) bookedSeats = 0;

        if (bookedSeats + request.getSeats() > ride.getAvailableSeats()) {
            throw new RuntimeException("Not enough seats. Requested: " + request.getSeats() +
                    ", Available: " + (ride.getAvailableSeats() - bookedSeats));
        }

        Booking booking = new Booking();
        booking.setRide(ride);
        booking.setPassenger(passenger);
        booking.setSeats(request.getSeats());
        booking.setStatus(BookingStatus.CONFIRMED);

        bookingRepository.save(booking);
    }

    @Override
    public TaskStatusResponse getTaskStatus(String taskId) {
        return taskTracker.toResponse(taskId);
    }
}