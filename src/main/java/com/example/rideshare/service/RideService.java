package com.example.rideshare.service;

import com.example.rideshare.dto.CreateRideRequest;
import com.example.rideshare.dto.RideDto;
import com.example.rideshare.exception.BusinessException;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.BookingMapper;
import com.example.rideshare.mapper.RideMapper;
import com.example.rideshare.mapper.RouteMapper;
import com.example.rideshare.model.*;
import com.example.rideshare.repository.RideRepository;
import com.example.rideshare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RideService {
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final RideMapper rideMapper;
    private final RouteMapper routeMapper;
    private final BookingMapper bookingMapper;

    @Transactional(readOnly = true)
    public List<RideDto> getAllRides() {
        return rideMapper.toDtoList(rideRepository.findAll());
    }

    @Transactional(readOnly = true)
    public RideDto getRideById(Long id) {
        Ride ride = rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + id));
        return rideMapper.toDto(ride);
    }

    @Transactional
    public RideDto createRide(CreateRideRequest request) {
        User driver = userRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        Ride ride = new Ride();
        ride.setDriver(driver);
        ride.setDepartureTime(request.getDepartureTime());
        ride.setAvailableSeats(request.getAvailableSeats());
        ride.setPrice(request.getPrice());
        ride.setStatus("SCHEDULED");

        Route route = routeMapper.toEntity(request.getRoute());
        ride.setRoute(route);

        Ride savedRide = rideRepository.save(ride);

        // Добавляем поездку в список поездок водителя
        driver.getRidesAsDriver().add(savedRide);

        return rideMapper.toDto(savedRide);
    }

    @Transactional
    public RideDto updateRide(Long id, CreateRideRequest request) {
        Ride existingRide = rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + id));

        // Проверяем, можно ли обновить поездку
        if (!"SCHEDULED".equals(existingRide.getStatus())) {
            throw new BusinessException("Cannot update ride that is not in SCHEDULED status");
        }

        existingRide.setDepartureTime(request.getDepartureTime());
        existingRide.setAvailableSeats(request.getAvailableSeats());
        existingRide.setPrice(request.getPrice());

        // Обновляем маршрут
        Route updatedRoute = routeMapper.toEntity(request.getRoute());
        updatedRoute.setId(existingRide.getRoute().getId());
        existingRide.setRoute(updatedRoute);

        Ride updatedRide = rideRepository.save(existingRide);
        return rideMapper.toDto(updatedRide);
    }

    @Transactional
    public void deleteRide(Long id) {
        Ride ride = rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + id));

        // Проверяем, можно ли удалить поездку
        if (!"SCHEDULED".equals(ride.getStatus())) {
            throw new BusinessException("Cannot delete ride that is not in SCHEDULED status");
        }

        // Удаляем связанные бронирования
        if (ride.getBookings() != null && !ride.getBookings().isEmpty()) {
            ride.getBookings().clear();
        }

        rideRepository.delete(ride);
    }

    @Transactional(readOnly = true)
    public List<RideDto> getUpcomingRidesWithDetails() {
        // Используем метод с @EntityGraph для решения N+1 проблемы
        List<Ride> rides = rideRepository.findUpcomingRidesWithDetails(LocalDateTime.now());
        return rideMapper.toDtoList(rides);
    }

    @Transactional(readOnly = true)
    public List<RideDto> getRidesInDateRange(LocalDateTime start, LocalDateTime end) {
        // Используем метод с JOIN FETCH для решения N+1 проблемы
        List<Ride> rides = rideRepository.findRidesInDateRangeWithAllDetails(start, end);
        return rideMapper.toDtoList(rides);
    }

    @Transactional
    public RideDto createRideWithBookings(Long rideId, List<Long> passengerIds) {
        // Демонстрация сохранения нескольких связанных сущностей
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found"));

        for (Long passengerId : passengerIds) {
            User passenger = userRepository.findById(passengerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Passenger not found: " + passengerId));

            Booking booking = new Booking();
            booking.setPassenger(passenger);
            booking.setRide(ride);
            booking.setSeats(1);
            booking.setStatus("PENDING");
            booking.setTotalPrice(ride.getPrice());

            ride.getBookings().add(booking);
            passenger.getBookings().add(booking);

            // Добавляем пассажира в список пассажиров поездки
            ride.getPassengers().add(passenger);
        }

        Ride savedRide = rideRepository.save(ride);
        return rideMapper.toDtoWithBookings(savedRide, bookingMapper);
    }
}