package com.example.rideshare.service;

import com.example.rideshare.model.dto.BookingRequestDto;
import com.example.rideshare.model.dto.BookingResponseDto;

import java.util.List;

public interface BookingService {
    BookingResponseDto createBooking(BookingRequestDto request);

    BookingResponseDto cancelBooking(Long bookingId);

    BookingResponseDto confirmBooking(Long bookingId);

    List<BookingResponseDto> getAllBookings();

    List<BookingResponseDto> getBookingsByUser(Long userId);

    List<BookingResponseDto> getBookingsByRide(Long rideId);
}