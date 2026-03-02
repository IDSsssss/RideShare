package com.example.rideshare.dto;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * Data Transfer Object representing a ride response.
 */
@Data
public class RideResponseDto {
  private Long id;
  private String driverName;
  private String fromCity;
  private String toCity;
  private LocalDateTime departureTime;
  private int availableSeats;
}