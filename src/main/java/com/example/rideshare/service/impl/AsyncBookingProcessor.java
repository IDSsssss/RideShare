package com.example.rideshare.service.impl;

import com.example.rideshare.model.dto.BookingRequestDto;
import com.example.rideshare.model.dto.BookingResponseDto;
import com.example.rideshare.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncBookingProcessor {

    private final BookingService bookingService;

    @Transactional
    public void processSingleBooking(BookingRequestDto request) {
        BookingResponseDto created = bookingService.createBooking(request);
        bookingService.confirmBooking(created.getId());
    }
}
