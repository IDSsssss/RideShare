package com.example.rideshare.controller;

import com.example.rideshare.model.dto.RideRequestDto;
import com.example.rideshare.model.dto.RideResponseDto;
import com.example.rideshare.service.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rides")
@RequiredArgsConstructor
public class RideController extends BaseController {

    private final RideService rideService;

    @GetMapping
    public ResponseEntity<List<RideResponseDto>> getAllRides() {
        return ok(rideService.getAllRides());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RideResponseDto> getRideById(@PathVariable Long id) {
        return ok(rideService.getRideById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<RideResponseDto>> searchRides(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        if (from != null && to != null) {
            return ok(rideService.getRidesInDateRange(from, to));
        }
        return ok(rideService.getUpcomingRidesWithDetails());
    }

    @PostMapping
    public ResponseEntity<RideResponseDto> createRide(@Valid @RequestBody RideRequestDto request) {
        return created(rideService.createRide(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RideResponseDto> updateRide(@PathVariable Long id,
                                                      @Valid @RequestBody RideRequestDto request) {
        return ok(rideService.updateRide(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRide(@PathVariable Long id) {
        rideService.deleteRide(id);
        return noContent();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RideResponseDto> updateRideStatus(@PathVariable Long id,
                                                            @RequestBody Map<String, String> request) {
        return ok(rideService.updateRideStatus(id, request.get("status")));
    }
}