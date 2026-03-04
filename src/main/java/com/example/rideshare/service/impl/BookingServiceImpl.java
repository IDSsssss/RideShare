package com.example.rideshare.service.impl;

import com.example.rideshare.model.dto.BookingRequestDto;
import com.example.rideshare.model.dto.BookingResponseDto;
import com.example.rideshare.model.entity.Booking;
import com.example.rideshare.model.entity.Ride;
import com.example.rideshare.model.entity.User;
import com.example.rideshare.model.enums.BookingStatus;
import com.example.rideshare.exception.BusinessException;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.BookingMapper;
import com.example.rideshare.repository.BookingRepository;
import com.example.rideshare.repository.RideRepository;
import com.example.rideshare.repository.UserRepository;
import com.example.rideshare.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto request) {
        log.debug("Creating booking for ride: {}, passenger: {}", request.getRideId(), request.getPassengerId());

        Ride ride = rideRepository.findById(request.getRideId())
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + request.getRideId()));


        if (!"SCHEDULED".equals(ride.getStatus())) {
            throw new BusinessException("Cannot book ride that is not in SCHEDULED status");
        }

        Integer bookedSeats = bookingRepository.getTotalBookedSeatsForRide(request.getRideId());
        if (bookedSeats == null) {
            bookedSeats = 0;
        }

        if (bookedSeats + request.getSeats() > ride.getAvailableSeats()) {
            throw new BusinessException("Not enough available seats. Requested: "
                    + request.getSeats() + ", Available: " + (ride.getAvailableSeats() - bookedSeats));
        }

        if (bookingRepository.existsByPassengerIdAndRideIdAndStatusIn(
                request.getPassengerId(), request.getRideId(),
                List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING))) {
            throw new BusinessException("Passenger already has a booking for this ride");
        }

        User passenger = userRepository.findById(request.getPassengerId()).orElseThrow(() ->
                new ResourceNotFoundException("Passenger not found with id: " + request.getPassengerId()));

        Booking booking = bookingMapper.toEntity(request);
        booking.setRide(ride);
        booking.setPassenger(passenger);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalPrice(ride.getPrice() * request.getSeats());

        Booking savedBooking = bookingRepository.save(booking);

        ride.getBookings().add(savedBooking);
        passenger.getBookings().add(savedBooking);
        ride.getPassengers().add(passenger);

        log.info("Booking created successfully with id: {}", savedBooking.getId());
        return bookingMapper.toResponseDto(savedBooking);
    }

    @Override
    @Transactional
    public BookingResponseDto cancelBooking(Long bookingId) {
        log.debug("Cancelling booking with id: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Booking is already cancelled");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException("Cannot cancel completed booking");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking cancelledBooking = bookingRepository.save(booking);

        log.info("Booking cancelled successfully with id: {}", bookingId);
        return bookingMapper.toResponseDto(cancelledBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getBookingsByUser(Long userId) {
        log.debug("Fetching bookings for user: {}", userId);

        if (userId == null) {
            throw new BusinessException("User ID cannot be null");
        }

        List<Booking> bookings = bookingRepository.findByPassengerId(userId);
        return bookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getBookingsByRide(Long rideId) {
        log.debug("Fetching bookings for ride: {}", rideId);

        if (rideId == null) {
            throw new BusinessException("Ride ID cannot be null");
        }

        List<Booking> bookings = bookingRepository.findByRideId(rideId);
        return bookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional
    public BookingResponseDto confirmBooking(Long bookingId) {
        log.debug("Confirming booking with id: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Only pending bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking confirmedBooking = bookingRepository.save(booking);

        log.info("Booking confirmed successfully with id: {}", bookingId);
        return bookingMapper.toResponseDto(confirmedBooking);
    }
}