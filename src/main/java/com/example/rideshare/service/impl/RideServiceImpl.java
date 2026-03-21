package com.example.rideshare.service.impl;

import com.example.rideshare.model.dto.RideRequestDto;
import com.example.rideshare.model.dto.RideResponseDto;
import com.example.rideshare.model.dto.RideSearchCriteria;
import com.example.rideshare.model.dto.RideSearchRequest;
import com.example.rideshare.model.entity.Ride;
import com.example.rideshare.model.entity.Route;
import com.example.rideshare.model.entity.User;
import com.example.rideshare.exception.BusinessException;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.RideMapper;
import com.example.rideshare.mapper.RouteMapper;
import com.example.rideshare.model.enums.RideStatus;
import com.example.rideshare.repository.BookingRepository;
import com.example.rideshare.repository.RideRepository;
import com.example.rideshare.repository.UserRepository;
import com.example.rideshare.service.RideService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final RideMapper rideMapper;
    private final RouteMapper routeMapper;
    private final Map<RideSearchCriteria, Page<RideResponseDto>> searchCache = new HashMap<>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private final AtomicLong modificationCount = new AtomicLong(0);
    private long lastCacheModificationCount = -1;

    private static final String RIDE_NOT_FOUND = "Ride not found with id: ";
    private static final String RIDE_ID_NULL = "Ride ID cannot be null";
    private static final String DRIVER_ID_NULL = "Driver ID cannot be null";
    private static final String DRIVER_NOT_FOUND = "Driver not found with id: ";

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
        return rideMapper.toResponseDtoList(rideRepository.findAllWithDetailsViaEntityGraph());
    }

    @Override
    @Transactional(readOnly = true)
    public RideResponseDto getRideById(Long id) {
        if (id == null) {
            throw new BusinessException(RIDE_ID_NULL);
        }

        Ride ride = rideRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException(RIDE_NOT_FOUND + id));

        return rideMapper.toResponseDto(ride);
    }

    @Override
    @Transactional
    public RideResponseDto createRide(RideRequestDto request) {
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
        ride.setStatus(RideStatus.SCHEDULED);
        ride.setBookings(new ArrayList<>());

        Route route = routeMapper.toEntity(request.getRoute());
        ride.setRoute(route);

        Ride savedRide = rideRepository.save(ride);

        if (driver.getRidesAsDriver() == null) {
            driver.setRidesAsDriver(new ArrayList<>());
        }
        driver.getRidesAsDriver().add(savedRide);

        this.invalidateCache();

        return rideMapper.toResponseDto(savedRide);
    }

    @Override
    @Transactional
    public RideResponseDto updateRide(Long id, RideRequestDto request) {
        if (id == null) {
            throw new BusinessException(RIDE_ID_NULL);
        }

        Ride existingRide = rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RIDE_NOT_FOUND + id));

        existingRide.setDepartureTime(request.getDepartureTime());
        existingRide.setAvailableSeats(request.getAvailableSeats());
        existingRide.setPrice(request.getPrice());

        if (existingRide.getRoute() != null && request.getRoute() != null) {
            Route updatedRoute = routeMapper.toEntity(request.getRoute());
            updatedRoute.setId(existingRide.getRoute().getId());
            existingRide.setRoute(updatedRoute);
        }

        Ride updatedRide = rideRepository.save(existingRide);
        this.invalidateCache();

        return rideMapper.toResponseDto(updatedRide);
    }

    @Override
    @Transactional
    public void deleteRide(Long id) {
        if (id == null) {
            throw new BusinessException(RIDE_ID_NULL);
        }

        Ride ride = rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RIDE_NOT_FOUND + id));

        Integer bookedSeats = bookingRepository.getTotalBookedSeatsForRide(id);
        if (bookedSeats != null && bookedSeats > 0) {
            throw new BusinessException("Cannot delete ride with existing bookings");
        }

        rideRepository.delete(ride);

        this.invalidateCache();
    }

    @Override
    @Transactional
    public RideResponseDto updateRideStatus(Long id, String status) {
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

        ride.setStatus(RideStatus.valueOf(status));
        Ride updatedRide = rideRepository.save(ride);

        return rideMapper.toResponseDto(updatedRide);
    }

    @Transactional(readOnly = true)
    public Page<RideResponseDto> searchRides(RideSearchRequest request) {
        RideSearchCriteria cacheKey = new RideSearchCriteria(
                request.getStartPoint(),
                request.getEndPoint(),
                request.getFromDate(),
                request.getToDate(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getMinSeats()
        );

        cacheLock.readLock().lock();
        try {
            if (modificationCount.get() == lastCacheModificationCount && searchCache.containsKey(cacheKey)) {
                return searchCache.get(cacheKey);
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        Page<Ride> ridePage;
        if (request.isUseNative()) {
            ridePage = rideRepository.searchRidesNative(
                    request.getStartPoint(),
                    request.getEndPoint(),
                    request.getFromDate(),
                    request.getToDate(),
                    request.getMinPrice(),
                    request.getMaxPrice(),
                    request.getMinSeats(),
                    request.getPageable());
        } else {
            ridePage = rideRepository.searchRides(
                    request.getStartPoint(),
                    request.getEndPoint(),
                    request.getFromDate(),
                    request.getToDate(),
                    request.getMinPrice(),
                    request.getMaxPrice(),
                    request.getMinSeats(),
                    request.getPageable());
        }

        Page<RideResponseDto> resultPage = ridePage.map(rideMapper::toResponseDto);

        cacheLock.writeLock().lock();
        try {
            searchCache.put(cacheKey, resultPage);
            lastCacheModificationCount = modificationCount.get();
        } finally {
            cacheLock.writeLock().unlock();
        }

        return resultPage;
    }

    public void invalidateCache() {
        cacheLock.writeLock().lock();
        try {
            modificationCount.incrementAndGet();
            searchCache.clear();
            log.info("Cache invalidated. Modification count: {}", modificationCount.get());
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    public Map<String, Object> getCacheStats() {
        cacheLock.readLock().lock();
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("cacheSize", searchCache.size());
            stats.put("modificationCount", modificationCount.get());
            stats.put("cacheKeys", searchCache.keySet());
            return stats;
        } finally {
            cacheLock.readLock().unlock();
        }
    }
}