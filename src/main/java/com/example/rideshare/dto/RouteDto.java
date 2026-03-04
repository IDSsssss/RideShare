package com.example.rideshare.dto;

import lombok.Data;

@Data
public class RouteDto {
    private Long id;
    private String startPoint;
    private String endPoint;
    private Double distanceKm;
    private Integer estimatedDurationMinutes;
    private String waypoints;
}