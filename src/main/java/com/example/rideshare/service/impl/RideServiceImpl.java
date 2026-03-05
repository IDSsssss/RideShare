package com.example.rideshare.service.impl;

import com.example.rideshare.model.dto.RideRequestDto;
import com.example.rideshare.model.dto.RideResponseDto;
import com.example.rideshare.model.entity.Ride;
import com.example.rideshare.model.entity.Route;
import com.example.rideshare.model.entity.User;
import com.example.rideshare.exception.BusinessException;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.RideMapper;
import com.example.rideshare.mapper.RouteMapper;
import com.example.rideshare.repository.BookingRepository;
import com.example.rideshare.repository.RideRepository;
import com.example.rideshare.repository.UserRepository;
import com.example.rideshare.service.RideService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final RideMapper rideMapper;
    private final RouteMapper routeMapper;

    private static final String RIDE_NOT_FOUND = "Ride not found with id: ";
    private static final String RIDE_ID_NULL = "Ride ID cannot be null";
    private static final String DRIVER_ID_NULL = "Driver ID cannot be null";
    private static final String DRIVER_NOT_FOUND = "Driver not found with id: ";
    private static final String INVALID_RIDE_STATUS = "Cannot %s ride that is not in SCHEDULED status";

    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final List<String> VALID_STATUSES = List.of(
            STATUS_SCHEDULED, STATUS_IN_PROGRESS, STATUS_COMPLETED, STATUS_CANCELLED
    );

    @Override
    @Transactional(readOnly = true)
    public List<RideResponseDto> getAllRides() {
        log.debug("Fetching all rides");
        return rideMapper.toResponseDtoList(rideRepository.findAllWithDetailsViaEntityGraph());
    }

    @Override
    @Transactional(readOnly = true)
    public RideResponseDto getRideById(Long id) {
        log.debug("Fetching ride by id: {}", id);

        if (id == null) {
            throw new BusinessException(RIDE_ID_NULL);
        }

        Ride ride = rideRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException(RIDE_NOT_FOUND + id));

        return rideMapper.toResponseDto(ride);
    }

    @Override
    //@Transactional
    public RideResponseDto createRide(RideRequestDto request) {
        log.debug("Creating new ride for driver id: {}", request.getDriverId());

        if (request.getDriverId() == null) {
            throw new BusinessException(DRIVER_ID_NULL);
        }

        User driver = userRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException(DRIVER_NOT_FOUND + request.getDriverId()));

        if (request.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Departure time must be in the future");
        }

        Ride ride = rideMapper.toEntity(request);
        ride.setDriver(driver);
        ride.setStatus(STATUS_SCHEDULED);
        ride.setBookings(new ArrayList<>());
        ride.setPassengers(new java.util.HashSet<>());

        Route route = routeMapper.toEntity(request.getRoute());
        ride.setRoute(route);

        Ride savedRide = rideRepository.save(ride);

        if (driver.getRidesAsDriver() == null) {
            driver.setRidesAsDriver(new ArrayList<>());
        }
        driver.getRidesAsDriver().add(savedRide);
        throw new RuntimeException("Симуляция ошибки для проверки Rollback");

        //return rideMapper.toResponseDto(savedRide);
    }

    @Override
    @Transactional
    public RideResponseDto updateRide(Long id, RideRequestDto request) {
        log.debug("Updating ride with id: {}", id);

        if (id == null) {
            throw new BusinessException(RIDE_ID_NULL);
        }

        Ride existingRide = rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RIDE_NOT_FOUND + id));

        if (!STATUS_SCHEDULED.equals(existingRide.getStatus())) {
            throw new BusinessException(String.format(INVALID_RIDE_STATUS, "update"));
        }

        existingRide.setDepartureTime(request.getDepartureTime());
        existingRide.setAvailableSeats(request.getAvailableSeats());
        existingRide.setPrice(request.getPrice());

        if (existingRide.getRoute() != null && request.getRoute() != null) {
            Route updatedRoute = routeMapper.toEntity(request.getRoute());
            updatedRoute.setId(existingRide.getRoute().getId());
            existingRide.setRoute(updatedRoute);
        }

        Ride updatedRide = rideRepository.save(existingRide);
        log.info("Ride updated successfully with id: {}", updatedRide.getId());

        return rideMapper.toResponseDto(updatedRide);
    }

    @Override
    @Transactional
    public void deleteRide(Long id) {
        log.debug("Deleting ride with id: {}", id);

        if (id == null) {
            throw new BusinessException(RIDE_ID_NULL);
        }

        Ride ride = rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RIDE_NOT_FOUND + id));

        if (!STATUS_SCHEDULED.equals(ride.getStatus())) {
            throw new BusinessException(String.format(INVALID_RIDE_STATUS, "delete"));
        }

        Integer bookedSeats = bookingRepository.getTotalBookedSeatsForRide(id);
        if (bookedSeats != null && bookedSeats > 0) {
            throw new BusinessException("Cannot delete ride with existing bookings");
        }

        rideRepository.delete(ride);
        log.info("Ride deleted successfully with id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideResponseDto> getUpcomingRidesWithDetails() {
        log.debug("Fetching upcoming rides with details");
        List<Ride> rides = rideRepository.findUpcomingRidesWithDetails(LocalDateTime.now());
        return rideMapper.toResponseDtoList(rides);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideResponseDto> getRidesInDateRange(LocalDateTime start, LocalDateTime end) {
        log.debug("Fetching rides in date range: {} - {}", start, end);

        if (start == null || end == null) {
            throw new BusinessException("Start and end dates must be provided");
        }

        if (start.isAfter(end)) {
            throw new BusinessException("Start date must be before end date");
        }

        List<Ride> rides = rideRepository.findRidesInDateRangeWithAllDetails(start, end);
        return rideMapper.toResponseDtoList(rides);
    }

    @Override
    @Transactional
    public RideResponseDto updateRideStatus(Long id, String status) {
        log.debug("Updating ride status for id: {} to {}", id, status);

        if (id == null) {
            throw new BusinessException(RIDE_ID_NULL);
        }

        if (status == null || status.trim().isEmpty()) {
            throw new BusinessException("Status cannot be null or empty");
        }

        Ride ride = rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RIDE_NOT_FOUND + id));

        if (!VALID_STATUSES.contains(status)) {
            throw new BusinessException("Invalid status: " + status);
        }

        if (STATUS_CANCELLED.equals(status)) {
            Integer bookedSeats = bookingRepository.getTotalBookedSeatsForRide(id);
            if (bookedSeats != null && bookedSeats > 0) {
                throw new BusinessException("Cannot cancel ride with existing bookings");
            }
        }

        ride.setStatus(status);
        Ride updatedRide = rideRepository.save(ride);
        log.info("Ride status updated successfully for id: {}", id);

        return rideMapper.toResponseDto(updatedRide);
    }
}