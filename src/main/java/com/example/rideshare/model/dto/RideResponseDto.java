package com.example.rideshare.model.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RideResponseDto {
    private Long id;
    private LocalDateTime departureTime;
    private Integer availableSeats;
    private Double price;
    private String status;
    private UserResponseDto driver;
    private RouteResponseDto route;
    private List<BookingResponseDto> bookings;
}