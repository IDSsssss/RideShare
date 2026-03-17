package com.example.rideshare.service.impl;

import com.example.rideshare.mapper.UserMapper;
import com.example.rideshare.model.dto.BookingRequestDto;
import com.example.rideshare.model.dto.BookingResponseDto;
import com.example.rideshare.model.entity.Booking;
import com.example.rideshare.model.entity.Ride;
import com.example.rideshare.model.entity.User;
import com.example.rideshare.model.enums.BookingStatus;
import com.example.rideshare.exception.BusinessException;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.BookingMapper;
import com.example.rideshare.model.enums.RideStatus;
import com.example.rideshare.repository.BookingRepository;
import com.example.rideshare.repository.RideRepository;
import com.example.rideshare.repository.UserRepository;
import com.example.rideshare.service.BookingService;
import com.example.rideshare.model.dto.UserResponseDto;
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
    private final UserMapper userMapper;

    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto request) {
        Ride ride = rideRepository.findById(request.getRideId())
                .orElseThrow(() -> new ResourceNotFoundException("Поездка не найдена с ID: " + request.getRideId()));

        if (RideStatus.SCHEDULED != ride.getStatus()) {
            throw new BusinessException("Нельзя бронировать места в поездке со статусом: " + ride.getStatus());
        }

        boolean alreadyBooked = bookingRepository.existsByPassengerIdAndRideIdAndStatusIn(
                request.getPassengerId(),
                request.getRideId(),
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        );

        if (alreadyBooked) {
            throw new BusinessException("Пассажир уже имеет активное бронирование на эту поездку");
        }

        Integer bookedSeats = bookingRepository.getTotalBookedSeatsForRide(request.getRideId());
        if (bookedSeats == null) {
            bookedSeats = 0;
        }

        if (bookedSeats + request.getSeats() > ride.getAvailableSeats()) {
            throw new BusinessException(
                    String.format("Недостаточно мест. Запрошено: %d, свободно: %d",
                            request.getSeats(), ride.getAvailableSeats() - bookedSeats)
            );
        }

        User passenger = userRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new ResourceNotFoundException("Пассажир не найден с ID: "
                        + request.getPassengerId()));

        Booking booking = new Booking();
        booking.setRide(ride);
        booking.setPassenger(passenger);
        booking.setSeats(request.getSeats());
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalPrice(ride.getPrice() * request.getSeats());

        Booking savedBooking = bookingRepository.save(booking);

        ride.getBookings().add(savedBooking);
        passenger.getBookings().add(savedBooking);

        return bookingMapper.toResponseDto(savedBooking);
    }

    @Override
    @Transactional
    public BookingResponseDto cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено с ID: " + bookingId));

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException("Нельзя отменить завершенное бронирование");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Бронирование уже отменено");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking cancelledBooking = bookingRepository.save(booking);

        return bookingMapper.toResponseDto(cancelledBooking);
    }

    @Override
    @Transactional
    public BookingResponseDto confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено с ID: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Можно подтвердить только бронирования со статусом PENDING");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking confirmedBooking = bookingRepository.save(booking);

        return bookingMapper.toResponseDto(confirmedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getBookingsByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Пользователь не найден с ID: " + userId);
        }

        List<Booking> bookings = bookingRepository.findByPassengerId(userId);
        return bookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getBookingsByRide(Long rideId) {
        if (!rideRepository.existsById(rideId)) {
            throw new ResourceNotFoundException("Поездка не найдена с ID: " + rideId);
        }

        List<Booking> bookings = bookingRepository.findByRideId(rideId);
        return bookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getPassengersByRide(Long rideId) {
        List<User> passengers = bookingRepository.findPassengersByRideId(rideId);
        return userMapper.toResponseDtoList(passengers);
    }
}