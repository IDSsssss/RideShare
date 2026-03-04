package com.example.rideshare.model.dto;

import com.example.rideshare.model.enums.BookingStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingResponseDto {
    private Long id;
    private LocalDateTime bookingTime;
    private Integer seats;
    private BookingStatus status;
    private Double totalPrice;
    private UserResponseDto passenger;
    private RideResponseDto ride;
}