package com.example.rideshare.service;

import com.example.rideshare.model.Ride;
import com.example.rideshare.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RideService {

    private final RideRepository repository;

    public RideService(RideRepository repository) {
        this.repository = repository;
    }

    public Ride createRide(Ride ride) {
        return repository.save(ride);
    }

    public Optional<Ride> getRideById(Long id) {
        return repository.findById(id);
    }

    public List<Ride> getAll() {
        return repository.findAll();
    }

    public List<Ride> getByFromCity(String fromCity) {
        return repository.findByFromCity(fromCity);
    }
}