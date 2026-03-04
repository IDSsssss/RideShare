package com.example.rideshare.mapper;

import com.example.rideshare.model.dto.BookingRequestDto;
import com.example.rideshare.model.dto.BookingResponseDto;
import com.example.rideshare.model.entity.Booking;
import com.example.rideshare.model.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookingMapper {

    private final UserMapper userMapper;
    private final RideMapper rideMapper;

    public BookingResponseDto toResponseDto(Booking booking) {
        if (booking == null) {
            return null;
        }

        BookingResponseDto dto = new BookingResponseDto();
        dto.setId(booking.getId());
        dto.setBookingTime(booking.getBookingTime());
        dto.setSeats(booking.getSeats());
        dto.setStatus(booking.getStatus());
        dto.setTotalPrice(booking.getTotalPrice());

        if (booking.getPassenger() != null) {
            dto.setPassenger(userMapper.toResponseDto(booking.getPassenger()));
        }

        if (booking.getRide() != null) {
            dto.setRide(rideMapper.toResponseDto(booking.getRide()));
        }

        return dto;
    }

    public Booking toEntity(BookingRequestDto dto) {
        if (dto == null) {
            return null;
        }

        Booking booking = new Booking();
        booking.setSeats(dto.getSeats());
        booking.setStatus(BookingStatus.PENDING);
        return booking;
    }

    public List<BookingResponseDto> toResponseDtoList(List<Booking> bookings) {
        if (bookings == null) {
            return List.of();
        }

        return bookings.stream().map(this::toResponseDto).collect(Collectors.toList());
    }
}