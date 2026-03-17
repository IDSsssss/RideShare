package com.example.rideshare.service;

import com.example.rideshare.model.dto.RideRequestDto;
import com.example.rideshare.model.dto.RideResponseDto;
import java.time.LocalDateTime;
import java.util.List;

public interface RideService {
    List<RideResponseDto> getAllRides();

    RideResponseDto getRideById(Long id);

    RideResponseDto createRide(RideRequestDto request);

    RideResponseDto updateRide(Long id, RideRequestDto request);

    void deleteRide(Long id);

    RideResponseDto updateRideStatus(Long id, String status);
}