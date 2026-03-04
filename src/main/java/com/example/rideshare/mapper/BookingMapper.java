package com.example.rideshare.mapper;

import com.example.rideshare.dto.BookingDto;
import com.example.rideshare.model.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookingMapper {

    private final UserMapper userMapper;

    public BookingDto toDto(Booking booking) {
        if (booking == null) {
            return null;
        }

        BookingDto dto = new BookingDto();
        dto.setId(booking.getId());
        dto.setBookingTime(booking.getBookingTime());
        dto.setSeats(booking.getSeats());
        dto.setStatus(booking.getStatus());
        dto.setTotalPrice(booking.getTotalPrice());

        if (booking.getPassenger() != null) {
            dto.setPassenger(userMapper.toDto(booking.getPassenger()));
        }

        return dto;
    }

    public List<BookingDto> toDtoList(List<Booking> bookings) {
        if (bookings == null) {
            return List.of();
        }
        return bookings.stream().map(this::toDto).collect(Collectors.toList());
    }
}