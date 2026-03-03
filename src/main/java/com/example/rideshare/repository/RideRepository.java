package com.example.rideshare.repository;

import com.example.rideshare.model.Ride;
import java.util.List;
import java.util.Optional;

public interface RideRepository {

    Ride save(Ride ride);

    Optional<Ride> findById(Long id);

    List<Ride> findAll();

    List<Ride> findByFromCity(String fromCity);
}