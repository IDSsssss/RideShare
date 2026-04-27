package com.example.rideshare.service;

import com.example.rideshare.model.dto.BookingRequestDto;
import com.example.rideshare.model.dto.TaskStatusResponse;
import java.util.List;

public interface AsyncBookingService {

    String processBookingsAsync(List<BookingRequestDto> bookingRequests);

    TaskStatusResponse getTaskStatus(String taskId);
}