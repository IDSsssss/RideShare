package com.example.rideshare.service.impl;

import com.example.rideshare.exception.BusinessException;
import com.example.rideshare.exception.ConflictException;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.BookingMapper;
import com.example.rideshare.model.dto.BookingRequestDto;
import com.example.rideshare.model.dto.BookingResponseDto;
import com.example.rideshare.model.entity.Booking;
import com.example.rideshare.model.entity.Ride;
import com.example.rideshare.model.entity.User;
import com.example.rideshare.model.enums.BookingStatus;
import com.example.rideshare.model.enums.RideStatus;
import com.example.rideshare.repository.BookingRepository;
import com.example.rideshare.repository.RideRepository;
import com.example.rideshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Unit Tests")
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User testPassenger;
    private User testDriver;
    private Ride testRide;
    private Booking testBooking;
    private BookingRequestDto testRequest;
    private BookingResponseDto testResponse;

    @BeforeEach
    void setUp() {
        // Инициализация тестовых данных
        testPassenger = new User();
        testPassenger.setId(1L);
        testPassenger.setName("Test Passenger");

        testDriver = new User();
        testDriver.setId(10L);
        testDriver.setName("Test Driver");

        testRide = new Ride();
        testRide.setId(100L);
        testRide.setDriver(testDriver);
        testRide.setAvailableSeats(4);
        testRide.setPrice(1500.0);
        testRide.setStatus(RideStatus.SCHEDULED);

        testBooking = new Booking();
        testBooking.setId(1000L);
        testBooking.setRide(testRide);
        testBooking.setPassenger(testPassenger);
        testBooking.setSeats(2);
        testBooking.setStatus(BookingStatus.PENDING);

        testRequest = new BookingRequestDto();
        testRequest.setRideId(100L);
        testRequest.setPassengerId(1L);
        testRequest.setSeats(2);

        testResponse = new BookingResponseDto();
        testResponse.setId(1000L);
        testResponse.setSeats(2);
        testResponse.setStatus(BookingStatus.PENDING);
    }

    // ==================== CREATE BOOKING TESTS ====================

    @Nested
    @DisplayName("createBooking() tests")
    class CreateBookingTests {

        @Test
        @DisplayName("Should create booking successfully")
        void createBooking_Success_ShouldReturnBookingResponse() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.existsByPassengerIdAndRideIdAndStatusIn(
                    anyLong(), anyLong(), anyList())).thenReturn(false);
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(0);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testPassenger));
            when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
            when(bookingMapper.toResponseDto(any(Booking.class))).thenReturn(testResponse);

            // when
            BookingResponseDto result = bookingService.createBooking(testRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1000L);
            verify(bookingRepository, times(1)).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when ride not found")
        void createBooking_RideNotFound_ShouldThrowException() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> bookingService.createBooking(testRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Ride not found with id: 100");
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when ride status is not SCHEDULED")
        void createBooking_RideNotScheduled_ShouldThrowException() {
            // given
            testRide.setStatus(RideStatus.IN_PROGRESS);
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));

            // when & then
            assertThatThrownBy(() -> bookingService.createBooking(testRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cannot book seats in ride with status: IN_PROGRESS");
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when passenger already has active booking")
        void createBooking_AlreadyBooked_ShouldThrowException() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.existsByPassengerIdAndRideIdAndStatusIn(
                    anyLong(), anyLong(), anyList())).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> bookingService.createBooking(testRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Passenger already has an active booking for this ride");
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when not enough seats")
        void createBooking_NotEnoughSeats_ShouldThrowException() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.existsByPassengerIdAndRideIdAndStatusIn(
                    anyLong(), anyLong(), anyList())).thenReturn(false);
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(3);

            // when & then
            assertThatThrownBy(() -> bookingService.createBooking(testRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Not enough seats available");
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when passenger not found")
        void createBooking_PassengerNotFound_ShouldThrowException() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.existsByPassengerIdAndRideIdAndStatusIn(
                    anyLong(), anyLong(), anyList())).thenReturn(false);
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(0);
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> bookingService.createBooking(testRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Passenger not found with id: 1");
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw BusinessException when not enough seats")
        void createBooking_NotEnoughSeats_ShouldThrowBusinessException() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.existsByPassengerIdAndRideIdAndStatusIn(anyLong(), anyLong(), anyList()))
                    .thenReturn(false);
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(4); // уже 4 места занято
            testRequest.setSeats(2); // хотим ещё 2, но всего мест 4

            // when & then
            assertThatThrownBy(() -> bookingService.createBooking(testRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Not enough seats available");
        }

        @Test
        @DisplayName("Should handle null booked seats (when no bookings exist)")
        void createBooking_WhenNoBookingsExist_ShouldSetBookedSeatsToZero() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.existsByPassengerIdAndRideIdAndStatusIn(anyLong(), anyLong(), anyList()))
                    .thenReturn(false);
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(null); // ← null
            when(userRepository.findById(1L)).thenReturn(Optional.of(testPassenger));
            when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
            when(bookingMapper.toResponseDto(any(Booking.class))).thenReturn(testResponse);

            // when
            BookingResponseDto result = bookingService.createBooking(testRequest);

            // then
            assertThat(result).isNotNull();
            // Проверяем, что метод save был вызван (значит, bookedSeats был установлен в 0)
            verify(bookingRepository, times(1)).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should handle null booked seats correctly")
        void createBooking_NullBookedSeats_ShouldSetToZero() {
            // given
            // Рейд с 4 местами
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.existsByPassengerIdAndRideIdAndStatusIn(anyLong(), anyLong(), anyList()))
                    .thenReturn(false);
            // getTotalBookedSeatsForRide возвращает null (нет бронирований)
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testPassenger));

            // Создаём бронирование через ArgumentCaptor, чтобы проверить, что места доступны
            ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
            when(bookingRepository.save(bookingCaptor.capture())).thenReturn(testBooking);
            when(bookingMapper.toResponseDto(any(Booking.class))).thenReturn(testResponse);

            // when
            BookingResponseDto result = bookingService.createBooking(testRequest);

            // then
            assertThat(result).isNotNull();
            // Проверяем, что бронирование было создано (значит, проверка мест прошла успешно)
            verify(bookingRepository, times(1)).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should correctly calculate available seats when bookings exist")
        void createBooking_WithExistingBookings_ShouldCalculateCorrectly() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.existsByPassengerIdAndRideIdAndStatusIn(anyLong(), anyLong(), anyList()))
                    .thenReturn(false);
            // Уже забронировано 2 места (не null)
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(2);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testPassenger));
            when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
            when(bookingMapper.toResponseDto(any(Booking.class))).thenReturn(testResponse);

            // when
            BookingResponseDto result = bookingService.createBooking(testRequest);

            // then
            assertThat(result).isNotNull();
            verify(bookingRepository, times(1)).save(any(Booking.class));
        }
    }

    // ==================== CANCEL BOOKING TESTS ====================

    @Nested
    @DisplayName("cancelBooking() tests")
    class CancelBookingTests {

        @Test
        @DisplayName("Should cancel booking successfully")
        void cancelBooking_Success_ShouldReturnCancelledBooking() {
            // given
            when(bookingRepository.findById(1000L)).thenReturn(Optional.of(testBooking));
            when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
            when(bookingMapper.toResponseDto(any(Booking.class))).thenReturn(testResponse);

            // when
            BookingResponseDto result = bookingService.cancelBooking(1000L);

            // then
            assertThat(result).isNotNull();
            assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            verify(bookingRepository, times(1)).save(testBooking);
        }

        @Test
        @DisplayName("Should throw exception when booking not found")
        void cancelBooking_BookingNotFound_ShouldThrowException() {
            // given
            when(bookingRepository.findById(1000L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> bookingService.cancelBooking(1000L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Booking not found with id: 1000");
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when booking is already completed")
        void cancelBooking_AlreadyCompleted_ShouldThrowException() {
            // given
            testBooking.setStatus(BookingStatus.COMPLETED);
            when(bookingRepository.findById(1000L)).thenReturn(Optional.of(testBooking));

            // when & then
            assertThatThrownBy(() -> bookingService.cancelBooking(1000L))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Cannot cancel completed booking");
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when booking is already cancelled")
        void cancelBooking_AlreadyCancelled_ShouldThrowException() {
            // given
            testBooking.setStatus(BookingStatus.CANCELLED);
            when(bookingRepository.findById(1000L)).thenReturn(Optional.of(testBooking));

            // when & then
            assertThatThrownBy(() -> bookingService.cancelBooking(1000L))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Booking is already cancelled");
            verify(bookingRepository, never()).save(any(Booking.class));
        }
    }

    // ==================== CONFIRM BOOKING TESTS ====================

    @Nested
    @DisplayName("confirmBooking() tests")
    class ConfirmBookingTests {

        @Test
        @DisplayName("Should confirm booking successfully")
        void confirmBooking_Success_ShouldReturnConfirmedBooking() {
            // given
            when(bookingRepository.findById(1000L)).thenReturn(Optional.of(testBooking));
            when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
            when(bookingMapper.toResponseDto(any(Booking.class))).thenReturn(testResponse);

            // when
            BookingResponseDto result = bookingService.confirmBooking(1000L);

            // then
            assertThat(result).isNotNull();
            assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            verify(bookingRepository, times(1)).save(testBooking);
        }

        @Test
        @DisplayName("Should throw exception when booking is not pending")
        void confirmBooking_NotPending_ShouldThrowException() {
            // given
            testBooking.setStatus(BookingStatus.CONFIRMED);
            when(bookingRepository.findById(1000L)).thenReturn(Optional.of(testBooking));

            // when & then
            assertThatThrownBy(() -> bookingService.confirmBooking(1000L))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Only pending bookings can be confirmed");
            verify(bookingRepository, never()).save(any(Booking.class));
        }
    }

    // ==================== GET BOOKINGS BY USER TESTS ====================

    @Nested
    @DisplayName("getBookingsByUser() tests")
    class GetBookingsByUserTests {

        @Test
        @DisplayName("Should return list of bookings for user")
        void getBookingsByUser_Success_ShouldReturnBookingsList() {
            // given
            List<Booking> bookings = Arrays.asList(testBooking, testBooking);
            when(userRepository.existsById(1L)).thenReturn(true);
            when(bookingRepository.findByPassengerId(1L)).thenReturn(bookings);
            when(bookingMapper.toResponseDtoList(bookings)).thenReturn(
                    Arrays.asList(testResponse, testResponse));

            // when
            List<BookingResponseDto> result = bookingService.getBookingsByUser(1L);

            // then
            assertThat(result).hasSize(2);
            verify(bookingRepository, times(1)).findByPassengerId(1L);
        }

        @Test
        @DisplayName("Should return empty list when no bookings for user")
        void getBookingsByUser_NoBookings_ShouldReturnEmptyList() {
            // given
            when(userRepository.existsById(1L)).thenReturn(true);
            when(bookingRepository.findByPassengerId(1L)).thenReturn(List.of());
            when(bookingMapper.toResponseDtoList(List.of())).thenReturn(List.of());

            // when
            List<BookingResponseDto> result = bookingService.getBookingsByUser(1L);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void getBookingsByUser_UserNotFound_ShouldThrowException() {
            // given
            when(userRepository.existsById(999L)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> bookingService.getBookingsByUser(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with id: 999");
            verify(bookingRepository, never()).findByPassengerId(anyLong());
        }
    }

    // ==================== GET BOOKINGS BY RIDE TESTS ====================

    @Nested
    @DisplayName("getBookingsByRide() tests")
    class GetBookingsByRideTests {

        @Test
        @DisplayName("Should return list of bookings for ride")
        void getBookingsByRide_Success_ShouldReturnBookingsList() {
            // given
            List<Booking> bookings = Arrays.asList(testBooking, testBooking);
            when(rideRepository.existsById(100L)).thenReturn(true);
            when(bookingRepository.findByRideId(100L)).thenReturn(bookings);
            when(bookingMapper.toResponseDtoList(bookings)).thenReturn(
                    Arrays.asList(testResponse, testResponse));

            // when
            List<BookingResponseDto> result = bookingService.getBookingsByRide(100L);

            // then
            assertThat(result).hasSize(2);
            verify(bookingRepository, times(1)).findByRideId(100L);
        }

        @Test
        @DisplayName("Should return empty list when no bookings for ride")
        void getBookingsByRide_NoBookings_ShouldReturnEmptyList() {
            // given
            when(rideRepository.existsById(100L)).thenReturn(true);
            when(bookingRepository.findByRideId(100L)).thenReturn(List.of());
            when(bookingMapper.toResponseDtoList(List.of())).thenReturn(List.of());

            // when
            List<BookingResponseDto> result = bookingService.getBookingsByRide(100L);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when ride not found")
        void getBookingsByRide_RideNotFound_ShouldThrowException() {
            // given
            when(rideRepository.existsById(999L)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> bookingService.getBookingsByRide(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Ride not found with id: 999");
            verify(bookingRepository, never()).findByRideId(anyLong());
        }
    }
}