package com.example.rideshare.controller;

import com.example.rideshare.dto.RideResponseDto;
import com.example.rideshare.mapper.RideMapper;
import com.example.rideshare.model.Ride;
import com.example.rideshare.service.RideService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rides")
public class RideController {
    private final RideService service;
    private final RideMapper mapper;

    public RideController(RideService service, RideMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/{id}")
    public RideResponseDto getById(@PathVariable Long id) {
        Ride ride = service.getRideById(id).orElseThrow(() -> new RuntimeException("Ride not found"));
        return mapper.toDto(ride);
    }

    @GetMapping
    public List<RideResponseDto> getAll(@RequestParam(required = false) String fromCity) {
        List<Ride> rides = fromCity == null ? service.getAll() : service.getByFromCity(fromCity);
        return rides.stream().map(mapper::toDto).toList();
    }

    @PostMapping
    public RideResponseDto create(@RequestBody Ride ride) {
        return mapper.toDto(service.createRide(ride));
    }
}