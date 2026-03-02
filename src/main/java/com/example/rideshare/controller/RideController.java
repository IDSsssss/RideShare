package com.example.rideshare.controller;

import com.example.rideshare.dto.RideResponseDto;
import com.example.rideshare.mapper.RideMapper;
import com.example.rideshare.model.Ride;
import com.example.rideshare.service.RideService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rides")
public class RideController {

    private final RideService service;
    private final RideMapper mapper;

    public RideController(RideService service, RideMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    // GET с PathVariable
    @GetMapping("/{id}")
    public RideResponseDto getById(@PathVariable Long id) {
        Ride ride = service.getRideById(id)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        return mapper.toDto(ride);
    }

    // GET с RequestParam
    @GetMapping
    public List<RideResponseDto> getAll(
            @RequestParam(required = false) String fromCity) {

        List<Ride> rides = fromCity == null
                ? service.getAll()
                : service.getByFromCity(fromCity);

        return rides.stream()
                .map(mapper::toDto)
                .toList();
    }

    @PostMapping
    public RideResponseDto create(@RequestBody Ride ride) {
        return mapper.toDto(service.createRide(ride));
    }
}