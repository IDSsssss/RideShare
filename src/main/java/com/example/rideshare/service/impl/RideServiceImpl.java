package com.example.rideshare.service.impl;

import com.example.rideshare.model.dto.BulkRideRequestDto;
import com.example.rideshare.model.dto.RideResponseDto;
import com.example.rideshare.model.dto.RideRequestDto;
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
import com.example.rideshare.repository.RouteRepository;
import com.example.rideshare.repository.UserRepository;
import com.example.rideshare.service.RideService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final RouteRepository routeRepository;
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
    private static final String TIME_IN_FUTURE = "Departure time must be in the future";
    private static final String CANNOT_DELETE = "Cannot delete ride with existing bookings";
    private static final String CANNOT_CANCEL = "Cannot cancel ride with existing bookings";
    private static final String INVALID_STATUS = "Invalid status: ";
    private static final String STATUS_IS_NULL = "Status cannot be null or empty";

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
            throw new BusinessException(CANNOT_DELETE);
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
            throw new BusinessException(STATUS_IS_NULL);
        }

        Ride ride = rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RIDE_NOT_FOUND + id));

        if (!VALID_STATUSES.contains(status)) {
            throw new BusinessException(INVALID_STATUS + status);
        }

        if (STATUS_CANCELLED.equals(status)) {
            Integer bookedSeats = bookingRepository.getTotalBookedSeatsForRide(id);
            if (bookedSeats != null && bookedSeats > 0) {
                throw new BusinessException(CANNOT_CANCEL);
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

        List<RideResponseDto> cachedData = null;

        cacheLock.readLock().lock();
        try {
            if (modificationCount.get() == lastCacheModificationCount && searchCache.containsKey(cacheKey)) {
                Page<RideResponseDto> cachedPage = searchCache.get(cacheKey);
                cachedData = cachedPage.getContent();
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        Page<RideResponseDto> resultPage;

        if (cachedData != null) {
            int start = (int) request.getPageable().getOffset();
            int end = Math.min(start + request.getPageable().getPageSize(), cachedData.size());

            List<RideResponseDto> pagedContent = start < cachedData.size()
                    ? cachedData.subList(start, end)
                    : List.of();

            resultPage = new PageImpl<>(pagedContent, request.getPageable(), cachedData.size());
        } else {
            List<Ride> allRides;
            if (request.isUseNative()) {
                allRides = rideRepository.searchRidesNative(
                        request.getStartPoint(),
                        request.getEndPoint(),
                        request.getFromDate(),
                        request.getToDate(),
                        request.getMinPrice(),
                        request.getMaxPrice(),
                        request.getMinSeats());
            } else {
                allRides = rideRepository.searchRides(
                        request.getStartPoint(),
                        request.getEndPoint(),
                        request.getFromDate(),
                        request.getToDate(),
                        request.getMinPrice(),
                        request.getMaxPrice(),
                        request.getMinSeats());
            }

            List<RideResponseDto> allData = allRides.stream()
                    .map(rideMapper::toResponseDto)
                    .toList();

            cacheLock.writeLock().lock();
            try {
                Page<RideResponseDto> fullPage = new PageImpl<>(allData, Pageable.unpaged(), allData.size());
                searchCache.put(cacheKey, fullPage);
                lastCacheModificationCount = modificationCount.get();
            } finally {
                cacheLock.writeLock().unlock();
            }

            int start = (int) request.getPageable().getOffset();
            int end = Math.min(start + request.getPageable().getPageSize(), allData.size());
            List<RideResponseDto> pagedContent = start < allData.size()
                    ? allData.subList(start, end)
                    : List.of();

            resultPage = new PageImpl<>(pagedContent, request.getPageable(), allData.size());
        }

        return resultPage;
    }

    @Override
    public List<RideResponseDto> createRidesBulk(BulkRideRequestDto request) {

        Long driverId = request.driverId();

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + driverId));

        Set<String> routeKeys = request.rides().stream()
                .map(ride -> ride.getRoute().getStartPoint() + "|" + ride.getRoute().getEndPoint())
                .collect(Collectors.toSet());

        Map<String, Route> routesByKey = routeRepository.findAll().stream()
                .filter(r -> routeKeys.contains(r.getStartPoint() + "|" + r.getEndPoint()))
                .collect(Collectors.toMap(
                        r -> r.getStartPoint() + "|" + r.getEndPoint(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        List<RideResponseDto> results = new ArrayList<>();

        try {
            results = IntStream.range(0, request.rides().size())
                    .mapToObj(i -> {
                        int index = i + 1;
                        RideRequestDto rideDto = request.rides().get(i);

                        if (rideDto.getPrice() > 10000) {
                            throw new BusinessException("DEMO ERROR: Failed to process 3rd ride");
                        }

                        Optional.of(rideDto)
                                .filter(r -> r.getDepartureTime().isAfter(LocalDateTime.now()))
                                .orElseThrow(() -> new BusinessException(
                                        "Departure time must be in the future for ride #" + index));

                        String routeKey = rideDto.getRoute().getStartPoint() + "|" + rideDto.getRoute().getEndPoint();
                        Route route = Optional.ofNullable(routesByKey.get(routeKey))
                                .orElseGet(() -> {
                                    Route newRoute = routeMapper.toEntity(rideDto.getRoute());
                                    Route savedRoute = routeRepository.save(newRoute);
                                    routesByKey.put(routeKey, savedRoute);
                                    return savedRoute;
                                });

                        Ride ride = rideMapper.toEntity(rideDto);
                        ride.setDriver(driver);
                        ride.setRoute(route);
                        ride.setStatus(RideStatus.SCHEDULED);
                        ride.setBookings(new ArrayList<>());

                        Ride savedRide = rideRepository.save(ride);

                        Optional.ofNullable(driver.getRidesAsDriver())
                                .ifPresentOrElse(
                                        list -> list.add(savedRide),
                                        () -> {
                                            List<Ride> newList = new ArrayList<>();
                                            newList.add(savedRide);
                                            driver.setRidesAsDriver(newList);
                                        }
                                );

                        return rideMapper.toResponseDto(savedRide);
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            boolean hadTransaction = TransactionSynchronizationManager.isActualTransactionActive();
            throw new RuntimeException("Bulk ride creation failed after " + results.size()
                    + " rides. Transaction active: " + hadTransaction + ". Error: " + e.getMessage());
        }

        Optional.of(driver).ifPresent(userRepository::save);

        invalidateCache();

        return results;
    }

    public void invalidateCache() {
        cacheLock.writeLock().lock();
        try {
            modificationCount.incrementAndGet();
            searchCache.clear();
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