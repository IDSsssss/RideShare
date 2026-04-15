package com.example.rideshare.service;

import com.example.rideshare.model.dto.RideRequestDto;
import com.example.rideshare.model.dto.RideResponseDto;
import com.example.rideshare.model.dto.BulkRideRequestDto;

import java.util.List;

public interface RideService {
    List<RideResponseDto> getAllRides();

    RideResponseDto getRideById(Long id);

    RideResponseDto updateRide(Long id, RideRequestDto request);

    void deleteRide(Long id);

    RideResponseDto updateRideStatus(Long id, String status);

    List<RideResponseDto> createRidesBulk(BulkRideRequestDto request);
}