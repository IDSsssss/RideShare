package com.example.rideshare.service.impl;

import com.example.rideshare.model.dto.BookingRequestDto;
import com.example.rideshare.model.dto.TaskStatusResponse;
import com.example.rideshare.service.AsyncBookingService;
import com.example.rideshare.utils.BookingTaskTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncBookingServiceImpl implements AsyncBookingService {

    private final BookingTaskTracker taskTracker;
    private final AsyncBookingExecutionService asyncExecutionService;

    @Override
    public String processBookingsAsync(List<BookingRequestDto> bookingRequests) {
        String taskId = taskTracker.generateAndCreateTask(bookingRequests.size());

        asyncExecutionService.doAsyncProcessing(taskId, bookingRequests);

        return taskId;
    }

    @Override
    public TaskStatusResponse getTaskStatus(String taskId) {
        return taskTracker.toResponse(taskId);
    }
}