package com.example.rideshare.service;

import com.example.rideshare.model.dto.BulkRideRequestDto;
import com.example.rideshare.model.dto.RideRequestDto;
import com.example.rideshare.model.dto.RideResponseDto;
import com.example.rideshare.model.dto.RideSearchRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface RideService {
    List<RideResponseDto> getAllRides();

    RideResponseDto getRideById(Long id);

    RideResponseDto updateRide(Long id, RideRequestDto request);

    void deleteRide(Long id);

    RideResponseDto cancelRide(Long id);

    List<RideResponseDto> createRidesBulk(BulkRideRequestDto request);

    Page<RideResponseDto> searchRides(RideSearchRequest request);

    Map<String, Object> getCacheStats();

    void invalidateCache();
}
