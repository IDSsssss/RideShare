package com.example.rideshare.service;

import com.example.rideshare.model.dto.BookingRequestDto;
import com.example.rideshare.model.dto.BookingResponseDto;
import java.util.List;

public interface BookingService {
    BookingResponseDto createBooking(BookingRequestDto request);

    BookingResponseDto cancelBooking(Long bookingId);

    List<BookingResponseDto> getBookingsByUser(Long userId);

    List<BookingResponseDto> getBookingsByRide(Long rideId);

    BookingResponseDto confirmBooking(Long bookingId);
}