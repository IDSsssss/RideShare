package com.example.rideshare.controller;

import com.example.rideshare.dto.CreateRideRequest;
import com.example.rideshare.dto.RideDto;
import com.example.rideshare.dto.UpdateRideStatusRequest;
import com.example.rideshare.service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/rides")
@RequiredArgsConstructor
public class RideController extends BaseController {
    private final RideService rideService;

    @GetMapping
    public ResponseEntity<List<RideDto>> getAllRides() {
        return ok(rideService.getAllRides());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RideDto> getRideById(@PathVariable Long id) {
        return ok(rideService.getRideById(id));
    }

    // GET endpoint с @RequestParam для фильтрации
    @GetMapping("/search")
    public ResponseEntity<List<RideDto>> searchRides(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        if (from != null && to != null) {
            return ResponseEntity.ok(rideService.getRidesInDateRange(from, to));
        }

        return ok(rideService.getUpcomingRidesWithDetails());
    }

    @PostMapping
    public ResponseEntity<RideDto> createRide(@Valid @RequestBody CreateRideRequest request) {

        return created(rideService.createRide(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RideDto> updateRide(@PathVariable Long id, @Valid @RequestBody CreateRideRequest request) {
        RideDto updatedRide = rideService.updateRide(id, request);

        return ok(updatedRide);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRide(@PathVariable Long id) {
        rideService.deleteRide(id);

        return noContent();
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<RideDto>> getUpcomingRides() {
        // Демонстрация решения N+1 проблемы
        return ok(rideService.getUpcomingRidesWithDetails());
    }

    @PostMapping("/{rideId}/bookings")
    public ResponseEntity<RideDto> addBookingsToRide(@PathVariable Long rideId, @RequestBody List<Long> passengerIds) {
        // Демонстрация сохранения нескольких связанных сущностей
        RideDto updatedRide = rideService.createRideWithBookings(rideId, passengerIds);

        return ok(updatedRide);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RideDto> updateRideStatus(@PathVariable Long id,
                                                    @RequestBody UpdateRideStatusRequest request) {
        RideDto updatedRide = rideService.updateRideStatus(id, request.getStatus());

        return ok(updatedRide);
    }
}