package com.example.rideshare.controller;

import com.example.rideshare.dto.CreateRideRequest;
import com.example.rideshare.dto.RideDto;
import com.example.rideshare.dto.UpdateRideStatusRequest;
import com.example.rideshare.service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/rides")
@RequiredArgsConstructor
public class RideController {
    private final RideService rideService;

    @GetMapping
    public ResponseEntity<List<RideDto>> getAllRides() {
        return ResponseEntity.ok(rideService.getAllRides());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RideDto> getRideById(@PathVariable Long id) {
        return ResponseEntity.ok(rideService.getRideById(id));
    }

    // GET endpoint с @RequestParam для фильтрации
    @GetMapping("/search")
    public ResponseEntity<List<RideDto>> searchRides(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        if (from != null && to != null) {
            return ResponseEntity.ok(rideService.getRidesInDateRange(from, to));
        }
        return ResponseEntity.ok(rideService.getUpcomingRidesWithDetails());
    }

    @PostMapping
    public ResponseEntity<RideDto> createRide(@Valid @RequestBody CreateRideRequest request) {
        RideDto createdRide = rideService.createRide(request);
        return new ResponseEntity<>(createdRide, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RideDto> updateRide(@PathVariable Long id, @Valid @RequestBody CreateRideRequest request) {
        RideDto updatedRide = rideService.updateRide(id, request);
        return ResponseEntity.ok(updatedRide);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRide(@PathVariable Long id) {
        rideService.deleteRide(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<RideDto>> getUpcomingRides() {
        // Демонстрация решения N+1 проблемы
        return ResponseEntity.ok(rideService.getUpcomingRidesWithDetails());
    }

    @PostMapping("/{rideId}/bookings")
    public ResponseEntity<RideDto> addBookingsToRide(
            @PathVariable Long rideId,
            @RequestBody List<Long> passengerIds) {
        // Демонстрация сохранения нескольких связанных сущностей
        RideDto updatedRide = rideService.createRideWithBookings(rideId, passengerIds);
        return ResponseEntity.ok(updatedRide);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RideDto> updateRideStatus(
            @PathVariable Long id,
            @RequestBody UpdateRideStatusRequest request) {
        RideDto updatedRide = rideService.updateRideStatus(id, request.getStatus());
        return ResponseEntity.ok(updatedRide);
    }
}