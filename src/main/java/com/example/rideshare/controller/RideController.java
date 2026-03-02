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

/**
 * REST controller for managing rides.
 *
 * <p>Provides endpoints for retrieving and creating ride entities.
 * All responses are returned in JSON format.</p>
 */
@RestController
@RequestMapping("/rides")
public class RideController {
  private final RideService service;
  private final RideMapper mapper;

  /**
   * Constructs a RideController with required dependencies.
   *
   * @param service the ride service layer
   * @param mapper  the mapper used to convert entities to DTOs
   */
  public RideController(RideService service, RideMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  /**
   * Retrieves a ride by its unique identifier.
   *
   * @param id the ride ID
   * @return the ride response DTO
   * @throws RuntimeException if ride is not found
   */
  @GetMapping("/{id}")
  public RideResponseDto getById(@PathVariable Long id) {
    Ride ride = service.getRideById(id).orElseThrow(() -> new RuntimeException("Ride not found"));
    return mapper.toDto(ride);
  }

  /**
   * Retrieves all rides or filters them by departure city.
   *
   * @param fromCity optional departure city for filtering
   * @return list of ride response DTOs
   */
  @GetMapping
  public List<RideResponseDto> getAll(@RequestParam(required = false) String fromCity) {

    List<Ride> rides = fromCity == null ? service.getAll() : service.getByFromCity(fromCity);

    return rides.stream().map(mapper::toDto).toList();
  }

  /**
   * Creates a new ride.
   *
   * @param ride the ride entity received from request body
   * @return created ride as response DTO
   */
  @PostMapping
  public RideResponseDto create(@RequestBody Ride ride) {
    return mapper.toDto(service.createRide(ride));
  }
}