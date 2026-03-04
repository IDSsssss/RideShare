package com.example.rideshare.model.dto;

import lombok.Data;

@Data
public class RouteResponseDto {
    private Long id;
    private String startPoint;
    private String endPoint;
    private Double distanceKm;
    private Integer estimatedDurationMinutes;
    private String waypoints;
}