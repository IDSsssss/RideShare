package com.example.rideshare.repository;

import com.example.rideshare.model.Ride;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class RideRepository {

    private final Map<Long, Ride> storage = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();

    // Метод выполнится автоматически при старте приложения
    @PostConstruct
    public void init() {
        save(new Ride(null, "Alex", "Kyiv", "Lviv",
                LocalDateTime.now().plusDays(1), 3));

        save(new Ride(null, "Maria", "Kyiv", "Odessa",
                LocalDateTime.now().plusDays(2), 2));

        save(new Ride(null, "John", "Lviv", "Warsaw",
                LocalDateTime.now().plusDays(3), 4));

        save(new Ride(null, "Olena", "Odessa", "Kyiv",
                LocalDateTime.now().plusDays(1), 1));
    }

    public Ride save(Ride ride) {
        Long id = idGenerator.incrementAndGet();
        ride.setId(id);
        storage.put(id, ride);
        return ride;
    }

    public Optional<Ride> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<Ride> findAll() {
        return new ArrayList<>(storage.values());
    }

    public List<Ride> findByFromCity(String fromCity) {
        return storage.values()
                .stream()
                .filter(ride -> ride.getFromCity()
                        .equalsIgnoreCase(fromCity))
                .toList();
    }
}