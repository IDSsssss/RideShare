package com.example.rideshare.model.dto;

import com.example.rideshare.model.enums.BookingStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingResponseDto {
    private Long id;
    private LocalDateTime bookingTime;
    private Integer seats;
    private BookingStatus status;
    private UserResponseDto passenger;
    private RideResponseDto ride;

    @JsonProperty("totalPrice")
    public Double getTotalPrice() {
        if (ride != null && ride.getPrice() != null && seats != null) {
            return ride.getPrice() * seats;
        }
        return null;
    }
}