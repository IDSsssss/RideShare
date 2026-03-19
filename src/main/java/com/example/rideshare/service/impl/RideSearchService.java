package com.example.rideshare.service.impl;

import com.example.rideshare.model.dto.RideSearchCriteria;
import com.example.rideshare.model.dto.RideResponseDto;
import com.example.rideshare.model.entity.Ride;
import com.example.rideshare.repository.RideRepository;
import com.example.rideshare.mapper.RideMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideSearchService {

    private final RideRepository rideRepository;
    private final RideMapper rideMapper;

    // 4. In-memory индекс на основе HashMap (не ConcurrentHashMap)
    private final Map<RideSearchCriteria, Page<RideResponseDto>> searchCache = new HashMap<>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    // Счетчик изменений для инвалидации кэша
    private volatile long modificationCount = 0;
    private long lastCacheModificationCount = -1;

    /**
     * 1-2. Поиск поездок с фильтрацией (JPQL + Native + Пагинация)
     */
    @Transactional(readOnly = true)
    public Page<RideResponseDto> searchRides(
            String startPoint,
            String endPoint,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Double minPrice,
            Double maxPrice,
            Integer minSeats,
            Pageable pageable,
            boolean useNative) {

        log.info("Searching rides with filters: start={}, end={}, from={}, to={}, "
                        + "minPrice={}, maxPrice={}, minSeats={}, page={}, size={}, native={}",
                startPoint, endPoint, fromDate, toDate, minPrice, maxPrice,
                minSeats, pageable.getPageNumber(), pageable.getPageSize(), useNative);

        // Создаем ключ для кэша
        RideSearchCriteria cacheKey = new RideSearchCriteria(
                startPoint, endPoint, fromDate, toDate, minPrice, maxPrice, minSeats
        );

        // 4. Проверяем кэш с использованием Read Lock
        cacheLock.readLock().lock();
        try {
            // Проверяем, актуален ли кэш
            if (modificationCount == lastCacheModificationCount && searchCache.containsKey(cacheKey)) {
                log.info("Cache hit for key: {}", cacheKey);
                return searchCache.get(cacheKey);
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        log.info("Cache miss for key: {}", cacheKey);

        // Выполняем запрос к БД
        Page<Ride> ridePage;
        if (useNative) {
            ridePage = rideRepository.searchRidesNative(
                    startPoint, endPoint, fromDate, toDate,
                    minPrice, maxPrice, minSeats, pageable);
        } else {
            ridePage = rideRepository.searchRidesWithFilters(
                    startPoint, endPoint, fromDate, toDate,
                    minPrice, maxPrice, minSeats, pageable);
        }

        Page<RideResponseDto> resultPage = ridePage.map(rideMapper::toResponseDto);

        // 4. Сохраняем в кэш с Write Lock
        cacheLock.writeLock().lock();
        try {
            searchCache.put(cacheKey, resultPage);
            lastCacheModificationCount = modificationCount;
            log.info("Cached result for key: {}, cache size: {}", cacheKey, searchCache.size());
        } finally {
            cacheLock.writeLock().unlock();
        }

        return resultPage;
    }

    /**
     * 5. Инвалидация кэша при изменении данных
     * Вызывать после создания/обновления/удаления поездок
     */
    public void invalidateCache() {
        cacheLock.writeLock().lock();
        try {
            modificationCount++;
            searchCache.clear();
            log.info("Cache invalidated. Modification count: {}", modificationCount);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Получить статистику кэша
     */
    public Map<String, Object> getCacheStats() {
        cacheLock.readLock().lock();
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("cacheSize", searchCache.size());
            stats.put("modificationCount", modificationCount);
            stats.put("cacheKeys", searchCache.keySet());
            return stats;
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    @PostConstruct
    public void init() {
        log.info("RideSearchService initialized with HashMap cache (non-concurrent)");
    }
}