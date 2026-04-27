package com.example.rideshare.service.impl;

import com.example.rideshare.model.dto.BookingRequestDto;
import com.example.rideshare.model.entity.Booking;
import com.example.rideshare.model.entity.Ride;
import com.example.rideshare.model.entity.User;
import com.example.rideshare.model.enums.BookingStatus;
import com.example.rideshare.repository.BookingRepository;
import com.example.rideshare.repository.RideRepository;
import com.example.rideshare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncBookingProcessor {

    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;

    @Transactional
    public void processSingleBooking(BookingRequestDto request) {
        log.info("Processing booking for rideId: {}, passengerId: {}",
                request.getRideId(), request.getPassengerId());

        Ride ride = rideRepository.findById(request.getRideId())
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + request.getRideId()));

        Integer bookedSeats = bookingRepository.getTotalBookedSeatsForRide(request.getRideId());
        if (bookedSeats == null) {
            bookedSeats = 0;
        }

        if (bookedSeats + request.getSeats() > ride.getAvailableSeats()) {
            throw new BusinessException("Not enough seats. Requested: " + request.getSeats()
                    + ", Available: " + (ride.getAvailableSeats() - bookedSeats));
        }

        User passenger = userRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: "
                        + request.getPassengerId()));

        Booking booking = new Booking();
        booking.setRide(ride);
        booking.setPassenger(passenger);
        booking.setSeats(request.getSeats());
        booking.setStatus(BookingStatus.CONFIRMED);

        bookingRepository.save(booking);
        log.info("Booking created successfully with id: {}", booking.getId());
    }
}