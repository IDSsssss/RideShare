package com.example.rideshare.mapper;

import com.example.rideshare.dto.BookingDto;
import com.example.rideshare.model.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper(componentModel = "spring", uses = {UserMapper.class, RideMapper.class})
public interface BookingMapper {
    BookingMapper INSTANCE = Mappers.getMapper(BookingMapper.class);

    @Mapping(target = "ride", ignore = true) // Избегаем циклических ссылок
    BookingDto toDto(Booking booking);

    Booking toEntity(BookingDto bookingDTO);

    List<BookingDto> toDtoList(List<Booking> bookings);
}