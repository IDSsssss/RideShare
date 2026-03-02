package com.example.rideshare.mapper;

import com.example.rideshare.dto.RideResponseDto;
import com.example.rideshare.model.Ride;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting Ride entities to RideResponseDto objects.
 */
@Component
public class RideMapper {

  /**
   * Converts a Ride entity to a RideResponseDto.
   *
   * @param ride the Ride entity to convert.
   * @return the corresponding RideResponseDto
   */
  public RideResponseDto toDto(Ride ride) {
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