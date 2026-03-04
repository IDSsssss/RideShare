package com.example.rideshare.mapper;

import com.example.rideshare.dto.RideDto;
import com.example.rideshare.model.Ride;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RideMapper {

    private final UserMapper userMapper;
    private final RouteMapper routeMapper;
    private final BookingMapper bookingMapper;

    public RideDto toDto(Ride ride) {
        if (ride == null) {
            return null;
        }

        RideDto dto = new RideDto();
        dto.setId(ride.getId());
        dto.setDepartureTime(ride.getDepartureTime());
        dto.setAvailableSeats(ride.getAvailableSeats());
        dto.setPrice(ride.getPrice());
        dto.setStatus(ride.getStatus());

        if (ride.getDriver() != null) {
            dto.setDriver(userMapper.toDto(ride.getDriver()));
        }
        if (ride.getRoute() != null) {
            dto.setRoute(routeMapper.toDto(ride.getRoute()));
        }

        return dto;
    }

    public List<RideDto> toDtoList(List<Ride> rides) {
        if (rides == null) {
            return List.of();
        }
        return rides.stream().map(this::toDto).collect(Collectors.toList());
    }

    public RideDto toDtoWithBookings(Ride ride) {
        RideDto dto = toDto(ride);
        if (ride != null && ride.getBookings() != null && !ride.getBookings().isEmpty()) {
            dto.setBookings(bookingMapper.toDtoList(ride.getBookings()));
        }
        return dto;
    }
}