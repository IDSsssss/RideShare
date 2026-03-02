package com.example.rideshare.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a ride with driver, route, departure time, and available seats.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ride {
  private Long id;
  private String driverName;
  private String fromCity;
  private String toCity;
  private LocalDateTime departureTime;
  private int availableSeats;
}