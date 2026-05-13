package com.example.rideshare.service.impl;

import com.example.rideshare.exception.BusinessException;
import com.example.rideshare.exception.ConflictException;
import com.example.rideshare.exception.ForbiddenException;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.RideMapper;
import com.example.rideshare.mapper.RouteMapper;
import com.example.rideshare.model.RideEffectiveStatuses;
import com.example.rideshare.model.dto.BulkRideRequestDto;
import com.example.rideshare.model.dto.RideRequestDto;
import com.example.rideshare.model.dto.RideResponseDto;
import com.example.rideshare.model.dto.RideSearchCriteria;
import com.example.rideshare.model.dto.RideSearchRequest;
import com.example.rideshare.model.dto.RouteRequestDto;
import com.example.rideshare.model.entity.Booking;
import com.example.rideshare.model.entity.Ride;
import com.example.rideshare.model.entity.Route;
import com.example.rideshare.model.entity.User;
import com.example.rideshare.model.enums.BookingStatus;
import com.example.rideshare.model.enums.RideStatus;
import com.example.rideshare.repository.BookingRepository;
import com.example.rideshare.repository.RideRepository;
import com.example.rideshare.repository.RouteRepository;
import com.example.rideshare.repository.UserRepository;
import com.example.rideshare.security.CurrentUserAccessor;
import com.example.rideshare.service.RideService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
    private final CurrentUserAccessor currentUserAccessor;

    private final Map<RideSearchCriteria, Page<RideResponseDto>> searchCache = new HashMap<>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private final AtomicLong modificationCount = new AtomicLong(0);
    private long lastCacheModificationCount = -1;

    private static final String RIDE_NOT_FOUND = "Ride not found with id: ";
    private static final String RIDE_ID_NULL = "Ride ID cannot be null";
    private static final String DEPARTURE_IN_PAST = "Время отправления должно быть в будущем";
    private static final String CANNOT_DELETE = "Cannot delete ride with existing bookings";

    @Override
    @Transactional(readOnly = true)
    public List<RideResponseDto> getAllRides() {
        return rideMapper.toResponseDtoList(rideRepository.findAllWithDetailsViaEntityGraph());
    }

    @Override
    @Transactional
    public RideResponseDto getRideById(Long id) {
        if (id == null) {
            throw new BusinessException(RIDE_ID_NULL);
        }

        Ride ride = rideRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException(RIDE_NOT_FOUND + id));

        syncRideStatusOnDb(ride);
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

        Long driverUserId = existingRide.getDriver() != null ? existingRide.getDriver().getId() : null;
        currentUserAccessor.requireAdminOrDriver(driverUserId);

        if (!request.getDepartureTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(DEPARTURE_IN_PAST);
        }
        existingRide.setDepartureTime(request.getDepartureTime());
        existingRide.setAvailableSeats(request.getAvailableSeats());
        existingRide.setPrice(request.getPrice());

        if (existingRide.getRoute() != null && request.getRoute() != null) {
            Route updatedRoute = routeMapper.toEntity(request.getRoute());
            updatedRoute.setId(existingRide.getRoute().getId());
            updatedRoute.setCreatedByUserId(existingRide.getRoute().getCreatedByUserId());
            existingRide.setRoute(updatedRoute);
        }

        Ride updatedRide = rideRepository.save(existingRide);
        syncRideStatusOnDb(updatedRide);
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

        Long driverUserId = ride.getDriver() != null ? ride.getDriver().getId() : null;
        currentUserAccessor.requireAdminOrDriver(driverUserId);

        Integer bookedSeats = bookingRepository.getTotalBookedSeatsForRide(id);
        if (bookedSeats != null && bookedSeats > 0) {
            throw new BusinessException(CANNOT_DELETE);
        }

        rideRepository.delete(ride);

        this.invalidateCache();
    }

    @Override
    @Transactional
    public RideResponseDto cancelRide(Long id) {
        if (id == null) {
            throw new BusinessException(RIDE_ID_NULL);
        }

        Ride ride = rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RIDE_NOT_FOUND + id));

        Long driverUserId = ride.getDriver() != null ? ride.getDriver().getId() : null;
        currentUserAccessor.requireRideDriver(driverUserId);

        if (ride.getStatus() == RideStatus.CANCELLED) {
            throw new ConflictException("Поездка уже отменена");
        }

        for (Booking b : bookingRepository.findByRideId(id)) {
            if (b.getStatus() == BookingStatus.PENDING || b.getStatus() == BookingStatus.CONFIRMED) {
                b.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(b);
            }
        }

        ride.setStatus(RideStatus.CANCELLED);
        Ride saved = rideRepository.save(ride);
        invalidateCache();
        return rideMapper.toResponseDto(saved);
    }

    @Override
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
            if (modificationCount.get() == lastCacheModificationCount) {
                Page<RideResponseDto> cachedPage = searchCache.get(cacheKey);
                if (cachedPage != null) {
                    cachedData = cachedPage.getContent();
                }
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
            String startPattern = likePatternOrNull(request.getStartPoint());
            String endPattern = likePatternOrNull(request.getEndPoint());
            // Нативный SQL с CAST — иначе PostgreSQL даёт «could not determine data type of parameter»
            // на выражениях вида (:fromDate IS NULL OR ... >= :fromDate) из JPQL.
            List<Ride> allRides = rideRepository.searchRidesNative(
                    startPattern,
                    endPattern,
                    request.getFromDate(),
                    request.getToDate(),
                    request.getMinPrice(),
                    request.getMaxPrice(),
                    request.getMinSeats());

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
    @Transactional
    public List<RideResponseDto> createRidesBulk(BulkRideRequestDto request) {

        Long driverId = request.driverId();
        if (!currentUserAccessor.isAdmin()) {
            Long uid = currentUserAccessor.currentUserIdOrNull();
            if (uid == null) {
                throw new ForbiddenException("Нужна авторизация участника.");
            }
            driverId = uid;
        }

        Long finalDriverId = driverId;
        User driver = userRepository.findById(finalDriverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + finalDriverId));

        Map<String, Route> routeBatchCache = new HashMap<>();

        List<RideResponseDto> results = new ArrayList<>();

        try {
            results = IntStream.range(0, request.rides().size())
                    .mapToObj(i -> {
                        int index = i + 1;
                        RideRequestDto rideDto = request.rides().get(i);

                        if (rideDto.getPrice() > 10000) {
                            throw new BusinessException("ERROR Price must be lower than 10000");
                        }

                        Optional.of(rideDto)
                                .filter(r -> r.getDepartureTime().isAfter(LocalDateTime.now()))
                                .orElseThrow(() -> new BusinessException(
                                        "Departure time must be in the future for ride #" + index));

                        Route route = resolveRouteForRide(rideDto.getRoute(), finalDriverId, routeBatchCache);

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
                    .toList();

        } catch (Exception e) {
            boolean hadTransaction = TransactionSynchronizationManager.isActualTransactionActive();
            throw new BusinessException("Bulk ride creation failed after " + results.size()
                    + " rides. Transaction active: " + hadTransaction + ". Error: " + e.getMessage());
        }

        Optional.of(driver).ifPresent(userRepository::save);

        invalidateCache();

        return results;
    }

    /**
     * Подбирает существующий маршрут с теми же пунктами, длиной и временем в пути, иначе создаёт новый.
     */
    private Route resolveRouteForRide(RouteRequestDto dto, Long creatorUserId, Map<String, Route> batchCache) {
        if (dto == null) {
            throw new BusinessException("Маршрут поездки обязателен.");
        }
        String start = normalizeRoutePoint(dto.getStartPoint());
        String end = normalizeRoutePoint(dto.getEndPoint());
        Double dist = dto.getDistanceKm();
        Integer mins = dto.getEstimatedDurationMinutes();
        if (dist == null || mins == null) {
            throw new BusinessException("Укажите расстояние (км) и время в пути (мин) для маршрута.");
        }
        String cacheKey = start + "|" + end + "|" + dist + "|" + mins;
        Route cached = batchCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Route route = routeRepository
                .findFirstByStartPointAndEndPointAndDistanceKmAndEstimatedDurationMinutes(start, end, dist, mins)
                .orElseGet(() -> {
                    Route nr = routeMapper.toEntity(dto);
                    nr.setStartPoint(start);
                    nr.setEndPoint(end);
                    nr.setDistanceKm(dist);
                    nr.setEstimatedDurationMinutes(mins);
                    nr.setCreatedByUserId(creatorUserId);
                    return routeRepository.save(nr);
                });
        batchCache.put(cacheKey, route);
        return route;
    }

    private static String normalizeRoutePoint(String raw) {
        return raw == null ? "" : raw.trim();
    }

    /**
     * Периодически синхронизирует статусы в БД (кроме отменённых).
     */
    @Scheduled(fixedDelayString = "${app.rides.status-refresh-ms:60000}")
    @Transactional
    public void scheduledRefreshRideStatuses() {
        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;
        for (Ride ride : rideRepository.findByStatusNot(RideStatus.CANCELLED)) {
            RideStatus target = RideEffectiveStatuses.calculate(ride, now);
            if (target != ride.getStatus()) {
                ride.setStatus(target);
                rideRepository.save(ride);
                changed = true;
            }
        }
        if (changed) {
            invalidateCache();
        }
    }

    private void syncRideStatusOnDb(Ride ride) {
        if (ride.getStatus() == RideStatus.CANCELLED) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        RideStatus target = RideEffectiveStatuses.calculate(ride, now);
        if (target != ride.getStatus()) {
            ride.setStatus(target);
            rideRepository.save(ride);
            invalidateCache();
        }
    }

    /**
     * Готовый шаблон для LIKE/ILIKE (нижний регистр для JPQL LOWER(column) LIKE pattern).
     * {@code null} — не фильтровать по этому полю.
     */
    private static String likePatternOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return "%" + trimmed.toLowerCase(Locale.ROOT) + "%";
    }

    @Override
    public void invalidateCache() {
        cacheLock.writeLock().lock();
        try {
            modificationCount.incrementAndGet();
            searchCache.clear();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    @Override
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
