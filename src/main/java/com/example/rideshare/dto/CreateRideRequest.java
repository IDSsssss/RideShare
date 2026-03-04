package com.example.rideshare.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Future;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateRideRequest {
    @NotNull(message = "Driver ID is required")
    private Long driverId;

    @NotNull(message = "Departure time is required")
    @Future(message = "Departure time must be in the future")
    private LocalDateTime departureTime;

    @NotNull(message = "Available seats is required")
    @Min(value = 1, message = "At least 1 seat must be available")
    @Max(value = 8, message = "Maximum 8 seats allowed")
    private Integer availableSeats;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private Double price;

    @NotNull(message = "Route information is required")
    private RouteDto route;
}