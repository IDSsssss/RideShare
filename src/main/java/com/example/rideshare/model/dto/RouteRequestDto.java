package com.example.rideshare.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class RouteRequestDto {
    @NotBlank(message = "Start point is required")
    private String startPoint;

    @NotBlank(message = "End point is required")
    private String endPoint;

    @Positive(message = "Distance must be positive")
    private Double distanceKm;

    @Positive(message = "Duration must be positive")
    private Integer estimatedDurationMinutes;

    private String waypoints;
}