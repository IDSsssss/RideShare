package com.example.rideshare.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingDto {
    private Long id;
    private LocalDateTime bookingTime;
    private Integer seats;
    private String status;
    private Double totalPrice;
    private UserDto passenger;
    private RideDto ride;
}