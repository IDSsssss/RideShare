package com.example.rideshare.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ride {

    private Long id;
    private String driverName;
    private String fromCity;
    private String toCity;
    private LocalDateTime departureTime;
    private int availableSeats;
}