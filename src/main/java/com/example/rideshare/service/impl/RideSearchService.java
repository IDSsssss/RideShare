package com.example.rideshare.service.impl;

import com.example.rideshare.model.dto.RideSearchCriteria;
import com.example.rideshare.model.dto.RideSearchRequest;
import com.example.rideshare.model.dto.RideResponseDto;
import com.example.rideshare.model.entity.Ride;
import com.example.rideshare.repository.RideRepository;
import com.example.rideshare.mapper.RideMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideSearchService {

    private final RideRepository rideRepository;
    private final RideMapper rideMapper;

    private final Map<RideSearchCriteria, Page<RideResponseDto>> searchCache = new HashMap<>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private final AtomicLong modificationCount = new AtomicLong(0);
    private long lastCacheModificationCount = -1;

    @Transactional(readOnly = true)
    public Page<RideResponseDto> searchRides(RideSearchRequest request) {
        log.info("Searching rides with request: {}", request);

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
                log.info("Cache hit for key: {}", cacheKey);
                return searchCache.get(cacheKey);
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        log.info("Cache miss for key: {}", cacheKey);

        Page<Ride> ridePage;
        if (request.isUseNative()) {
            ridePage = rideRepository.searchRidesNative(request, request.getPageable());
        } else {
            ridePage = rideRepository.searchRidesWithFilters(request, request.getPageable());
        }

        Page<RideResponseDto> resultPage = ridePage.map(rideMapper::toResponseDto);

        cacheLock.writeLock().lock();
        try {
            searchCache.put(cacheKey, resultPage);
            lastCacheModificationCount = modificationCount.get();
            log.info("Cached result for key: {}, cache size: {}", cacheKey, searchCache.size());
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