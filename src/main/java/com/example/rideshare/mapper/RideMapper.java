package com.example.rideshare.mapper;

import com.example.rideshare.model.RideEffectiveStatuses;
import com.example.rideshare.model.dto.RideRequestDto;
import com.example.rideshare.model.dto.RideResponseDto;
import com.example.rideshare.model.entity.Ride;
import com.example.rideshare.model.enums.RideStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RideMapper {

    private final UserMapper userMapper;
    private final RouteMapper routeMapper;

    public RideResponseDto toResponseDto(Ride ride) {
        if (ride == null) {
            return null;
        }

        RideResponseDto dto = new RideResponseDto();
        dto.setId(ride.getId());
        dto.setDepartureTime(ride.getDepartureTime());
        dto.setAvailableSeats(ride.getAvailableSeats());
        dto.setPrice(ride.getPrice());
        dto.setStatus(String.valueOf(RideEffectiveStatuses.calculate(ride, LocalDateTime.now())));

        if (ride.getDriver() != null) {
            dto.setDriver(userMapper.toResponseDto(ride.getDriver()));
        }

        if (ride.getRoute() != null) {
            dto.setRoute(routeMapper.toResponseDto(ride.getRoute()));
        }

        return dto;
    }

    public Ride toEntity(RideRequestDto dto) {
        if (dto == null) {
            return null;
        }

        Ride ride = new Ride();
        ride.setDepartureTime(dto.getDepartureTime());
        ride.setAvailableSeats(dto.getAvailableSeats());
        ride.setPrice(dto.getPrice());
        ride.setStatus(RideStatus.SCHEDULED);

        return ride;
    }

    public List<RideResponseDto> toResponseDtoList(List<Ride> rides) {
        if (rides == null) {
            return List.of();
        }

        return rides.stream().map(this::toResponseDto).toList();
    }
}