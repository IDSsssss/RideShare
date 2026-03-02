package com.example.rideshare.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RideResponseDto {

    private Long id;
    private String driverName;
    private String fromCity;
    private String toCity;
    private LocalDateTime departureTime;
    private int availableSeats;
}