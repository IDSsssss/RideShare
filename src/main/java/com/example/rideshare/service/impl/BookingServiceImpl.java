package com.example.rideshare.service.impl;

import com.example.rideshare.exception.ConflictException;
import com.example.rideshare.model.dto.BookingRequestDto;
import com.example.rideshare.model.dto.BookingResponseDto;
import com.example.rideshare.model.entity.Booking;
import com.example.rideshare.model.entity.Ride;
import com.example.rideshare.model.entity.User;
import com.example.rideshare.model.enums.BookingStatus;
import com.example.rideshare.exception.BusinessException;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.BookingMapper;
import com.example.rideshare.model.RideEffectiveStatuses;
import com.example.rideshare.model.enums.RideStatus;
import com.example.rideshare.repository.BookingRepository;
import com.example.rideshare.repository.RideRepository;
import com.example.rideshare.repository.UserRepository;
import com.example.rideshare.security.CurrentUserAccessor;
import com.example.rideshare.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;
    private final CurrentUserAccessor currentUserAccessor;

    private static final String RIDE_NOT_FOUND = "Ride not found with id: ";
    private static final String RIDE_WITH_STATUS = "Cannot book seats in ride with status: ";
    private static final String HAS_BOOKING = "Passenger already has an active booking for this ride";
    private static final String NOT_ENOUGH_SEATS = "Not enough seats available. Requested: %d, available: %d";
    private static final String PASSENGER_NOT_FOUND = "Passenger not found with id: ";
    private static final String BOOKING_NOT_FOUND = "Booking not found with id: ";
    private static final String CANNOT_CANCEL = "Cannot cancel completed booking";
    private static final String ALREADY_CANCELLED = "Booking is already cancelled";
    private static final String CANNOT_CONFIRM = "Only pending bookings can be confirmed";
    private static final String USER_NOT_FOUND = "User not found with id: ";

    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto request) {
        Ride ride = rideRepository.findById(request.getRideId())
                .orElseThrow(() -> new ResourceNotFoundException(RIDE_NOT_FOUND + request.getRideId()));

        LocalDateTime now = LocalDateTime.now();
        if (RideEffectiveStatuses.calculate(ride, now) != RideStatus.SCHEDULED) {
            throw new BusinessException(RIDE_WITH_STATUS + RideEffectiveStatuses.calculate(ride, now));
        }

        boolean alreadyBooked = bookingRepository.existsByPassengerIdAndRideIdAndStatusIn(
                request.getPassengerId(),
                request.getRideId(),
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        );

        if (alreadyBooked) {
            throw new ConflictException(HAS_BOOKING);
        }

        Integer bookedSeats = bookingRepository.getTotalBookedSeatsForRide(request.getRideId());
        if (bookedSeats == null) {
            bookedSeats = 0;
        }

        if (bookedSeats + request.getSeats() > ride.getAvailableSeats()) {
            throw new BusinessException(
                    String.format(NOT_ENOUGH_SEATS, request.getSeats(), ride.getAvailableSeats() - bookedSeats)
            );
        }

        User passenger = userRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new ResourceNotFoundException(PASSENGER_NOT_FOUND + request.getPassengerId()));

        Booking booking = new Booking();
        booking.setRide(ride);
        booking.setPassenger(passenger);
        booking.setSeats(request.getSeats());
        booking.setStatus(BookingStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);

        ride.getBookings().add(savedBooking);
        passenger.getBookings().add(savedBooking);

        return bookingMapper.toResponseDto(savedBooking);
    }

    @Override
    @Transactional
    public BookingResponseDto cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(BOOKING_NOT_FOUND + bookingId));

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ConflictException(CANNOT_CANCEL);
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException(ALREADY_CANCELLED);
        }

        Long passengerId = booking.getPassenger() != null ? booking.getPassenger().getId() : null;
        Long driverUserId = booking.getRide() != null && booking.getRide().getDriver() != null
                ? booking.getRide().getDriver().getId()
                : null;
        currentUserAccessor.requireAdminOrPassengerOrDriver(passengerId, driverUserId);

        booking.setStatus(BookingStatus.CANCELLED);
        Booking cancelledBooking = bookingRepository.save(booking);

        return bookingMapper.toResponseDto(cancelledBooking);
    }

    @Override
    @Transactional
    public BookingResponseDto confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(BOOKING_NOT_FOUND + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ConflictException(CANNOT_CONFIRM);
        }

        Long driverUserId = booking.getRide() != null && booking.getRide().getDriver() != null
                ? booking.getRide().getDriver().getId()
                : null;
        currentUserAccessor.requireAdminOrDriver(driverUserId);

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking confirmedBooking = bookingRepository.save(booking);

        return bookingMapper.toResponseDto(confirmedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAll();
        return bookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getBookingsByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException(USER_NOT_FOUND + userId);
        }

        List<Booking> bookings = bookingRepository.findByPassengerId(userId);
        return bookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getBookingsByRide(Long rideId) {
        if (!rideRepository.existsById(rideId)) {
            throw new ResourceNotFoundException(RIDE_NOT_FOUND + rideId);
        }

        List<Booking> bookings = bookingRepository.findByRideId(rideId);
        return bookingMapper.toResponseDtoList(bookings);
    }
}