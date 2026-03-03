package com.example.rideshare.mapper;

import com.example.rideshare.dto.RideResponseDto;
import com.example.rideshare.model.Ride;
import org.springframework.stereotype.Component;

@Component
public class RideMapper {

    public RideResponseDto toDto(final Ride ride) {
        RideResponseDto dto = new RideResponseDto();
        dto.setId(ride.getId());
        dto.setDriverName(ride.getDriverName());
        dto.setFromCity(ride.getFromCity());
        dto.setToCity(ride.getToCity());
        dto.setDepartureTime(ride.getDepartureTime());
        dto.setAvailableSeats(ride.getAvailableSeats());
        return dto;
    }
}