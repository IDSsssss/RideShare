package com.example.rideshare.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RideDto {
    private Long id;
    private LocalDateTime departureTime;
    private Integer availableSeats;
    private Double price;
    private String status;
    private UserDto driver;
    private RouteDto route;
    private List<BookingDto> bookings;
}