package com.example.rideshare.mapper;

import com.example.rideshare.dto.RideDto;
import com.example.rideshare.model.Ride;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper(componentModel = "spring", uses = {UserMapper.class, RouteMapper.class, BookingMapper.class})
public interface RideMapper {
    RideMapper INSTANCE = Mappers.getMapper(RideMapper.class);

    @Mapping(target = "bookings", ignore = true) // Избегаем циклических ссылок
    RideDto toDto(Ride ride);

    @Mapping(target = "bookings", ignore = true)
    Ride toEntity(RideDto rideDTO);

    List<RideDto> toDtoList(List<Ride> rides);

    // Метод для полного маппинга с bookings
    @Mapping(target = "bookings", expression = "java(ride.getBookings() != null ? bookingMapper.toDtoList(ride.getBookings()) : null)")
    RideDto toDtoWithBookings(Ride ride, @org.mapstruct.Context BookingMapper bookingMapper);
}