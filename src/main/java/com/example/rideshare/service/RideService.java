package com.example.rideshare.service;

import com.example.rideshare.model.Ride;
import com.example.rideshare.repository.RideRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Service layer for managing rides.
 *
 * <p>Contains business logic related to Ride entities and
 * communicates with the repository layer.</p>
 */
@Service
public class RideService {

  private final RideRepository repository;

  /**
   * Constructs a RideService with the required repository dependency.
   *
   * @param repository the ride repository
   */
  public RideService(RideRepository repository) {
    this.repository = repository;
  }

  /**
   * Creates and saves a new ride.
   *
   * @param ride the ride entity to create
   * @return the saved ride entity
   */
  public Ride createRide(Ride ride) {
    return repository.save(ride);
  }

  /**
   * Retrieves a ride by its ID.
   *
   * @param id the ride ID
   * @return an Optional containing the ride if found
   */
  public Optional<Ride> getRideById(Long id) {
    return repository.findById(id);
  }

  /**
   * Retrieves all rides.
   *
   * @return list of all rides
   */
  public List<Ride> getAll() {
    return repository.findAll();
  }

  /**
   * Retrieves rides filtered by departure city.
   *
   * @param fromCity the departure city
   * @return list of matching rides
   */
  public List<Ride> getByFromCity(String fromCity) {
    return repository.findByFromCity(fromCity);
  }
}