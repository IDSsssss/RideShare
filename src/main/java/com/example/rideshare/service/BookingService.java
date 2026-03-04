package com.example.rideshare.service;

import com.example.rideshare.dto.BookingDto;
import com.example.rideshare.exception.BusinessException;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.BookingMapper;
import com.example.rideshare.model.Booking;
import com.example.rideshare.model.Ride;
import com.example.rideshare.model.User;
import com.example.rideshare.repository.BookingRepository;
import com.example.rideshare.repository.RideRepository;
import com.example.rideshare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;

    @Transactional
    public BookingDto createBooking(Long rideId, Long passengerId, Integer seats) {
        Ride ride = rideRepository.findById(rideId).orElseThrow(() ->
                new ResourceNotFoundException("Ride not found"));

        Integer bookedSeats = bookingRepository.getTotalBookedSeatsForRide(rideId);
        if (bookedSeats == null) {
            bookedSeats = 0;
        }

        if (bookedSeats + seats > ride.getAvailableSeats()) {
            throw new BusinessException("Not enough available seats");
        }

        User passenger = userRepository.findById(passengerId).orElseThrow(() ->
                new ResourceNotFoundException("Passenger not found"));
        Booking booking = new Booking();
        booking.setRide(ride);
        booking.setPassenger(passenger);
        booking.setSeats(seats);
        booking.setStatus("CONFIRMED");
        booking.setTotalPrice(ride.getPrice() * seats);

        Booking savedBooking = bookingRepository.save(booking);

        ride.getBookings().add(savedBooking);
        passenger.getBookings().add(savedBooking);

        return bookingMapper.toDto(savedBooking);
    }

    @Transactional
    public BookingDto cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() ->
                new ResourceNotFoundException("Booking not found"));

        booking.setStatus("CANCELLED");
        Booking cancelledBooking = bookingRepository.save(booking);

        return bookingMapper.toDto(cancelledBooking);
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getBookingsByUser(Long userId) {
        List<Booking> bookings = bookingRepository.findByPassengerId(userId);

        return bookingMapper.toDtoList(bookings);
    }
}