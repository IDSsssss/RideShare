package com.example.rideshare.repository;

import com.example.rideshare.model.Ride;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryRideRepository implements RideRepository {

    private final Map<Long, Ride> storage = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();

    @PostConstruct
    public void init() {
        save(new Ride(null, "Mikhail", "Minsk", "Gomel", LocalDateTime.now().plusDays(1), 3));
        save(new Ride(null, "Georgiy", "Minsk", "Brest", LocalDateTime.now().plusDays(2), 2));
        save(new Ride(null, "Oleg", "Mogilev", "Grodno", LocalDateTime.now().plusDays(3), 4));
        save(new Ride(null, "Andrey", "Gomel", "Brest", LocalDateTime.now().plusDays(5), 1));
    }

    @Override
    public Ride save(Ride ride) {
        Long id = idGenerator.incrementAndGet();
        ride.setId(id);
        storage.put(id, ride);
        return ride;
    }

    @Override
    public Optional<Ride> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Ride> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<Ride> findByFromCity(String fromCity) {
        return storage.values().stream().filter(ride -> ride.getFromCity().equalsIgnoreCase(fromCity)).toList();
    }
}