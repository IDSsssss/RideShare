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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.anyList;


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
    private Ride testRide;
    private Booking testBooking;
    private BookingRequestDto testRequest;
    private BookingResponseDto testResponse;

    @BeforeEach
    void setUp() {
        testPassenger = new User();
        testPassenger.setId(1L);
        testPassenger.setName("Test Passenger");

        User testDriver = new User();
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

    @Nested
    @DisplayName("createBooking() tests")
    class CreateBookingTests {

        @Test
        @DisplayName("Should create booking successfully")
        void createBooking_Success_ShouldReturnBookingResponse() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.existsByPassengerIdAndRideIdAndStatusIn(
                    anyLong(), anyLong(), anyList())).thenReturn(false);
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(0);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testPassenger));
            when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
            when(bookingMapper.toResponseDto(any(Booking.class))).thenReturn(testResponse);

            BookingResponseDto result = bookingService.createBooking(testRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1000L);
            verify(bookingRepository, times(1)).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when ride not found")
        void createBooking_RideNotFound_ShouldThrowException() {
            when(rideRepository.findById(100L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.createBooking(testRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Ride not found with id: 100");
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when ride status is not SCHEDULED")
        void createBooking_RideNotScheduled_ShouldThrowException() {
            testRide.setStatus(RideStatus.IN_PROGRESS);
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));

            assertThatThrownBy(() -> bookingService.createBooking(testRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cannot book seats in ride with status: IN_PROGRESS");
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when passenger already has active booking")
        void createBooking_AlreadyBooked_ShouldThrowException() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.existsByPassengerIdAndRideIdAndStatusIn(
                    anyLong(), anyLong(), anyList())).thenReturn(true);

            assertThatThrownBy(() -> bookingService.createBooking(testRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Passenger already has an active booking for this ride");
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when not enough seats")
        void createBooking_NotEnoughSeats_ShouldThrowException() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.existsByPassengerIdAndRideIdAndStatusIn(
                    anyLong(), anyLong(), anyList())).thenReturn(false);
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(3);

            assertThatThrownBy(() -> bookingService.createBooking(testRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Not enough seats available");
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when passenger not found")
        void createBooking_PassengerNotFound_ShouldThrowException() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.existsByPassengerIdAndRideIdAndStatusIn(
                    anyLong(), anyLong(), anyList())).thenReturn(false);
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(0);
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.createBooking(testRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Passenger not found with id: 1");
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw BusinessException when not enough seats")
        void createBooking_NotEnoughSeats_ShouldThrowBusinessException() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.existsByPassengerIdAndRideIdAndStatusIn(anyLong(), anyLong(), anyList()))
                    .thenReturn(false);
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(4); // уже 4 места занято

            assertThatThrownBy(() -> bookingService.createBooking(testRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Not enough seats available");
        }

        @Test
        @DisplayName("Should handle null booked seats (when no bookings exist)")
        void createBooking_WhenNoBookingsExist_ShouldSetBookedSeatsToZero() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.existsByPassengerIdAndRideIdAndStatusIn(anyLong(), anyLong(), anyList()))
                    .thenReturn(false);
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(null); // ← null
            when(userRepository.findById(1L)).thenReturn(Optional.of(testPassenger));
            when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
            when(bookingMapper.toResponseDto(any(Booking.class))).thenReturn(testResponse);

            BookingResponseDto result = bookingService.createBooking(testRequest);

            assertThat(result).isNotNull();
            verify(bookingRepository, times(1)).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should handle null booked seats correctly")
        void createBooking_NullBookedSeats_ShouldSetToZero() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.existsByPassengerIdAndRideIdAndStatusIn(anyLong(), anyLong(), anyList()))
                    .thenReturn(false);
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testPassenger));

            ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
            when(bookingRepository.save(bookingCaptor.capture())).thenReturn(testBooking);
            when(bookingMapper.toResponseDto(any(Booking.class))).thenReturn(testResponse);

            BookingResponseDto result = bookingService.createBooking(testRequest);

            assertThat(result).isNotNull();
            verify(bookingRepository, times(1)).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should correctly calculate available seats when bookings exist")
        void createBooking_WithExistingBookings_ShouldCalculateCorrectly() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.existsByPassengerIdAndRideIdAndStatusIn(anyLong(), anyLong(), anyList()))
                    .thenReturn(false);
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(2);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testPassenger));
            when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
            when(bookingMapper.toResponseDto(any(Booking.class))).thenReturn(testResponse);

            BookingResponseDto result = bookingService.createBooking(testRequest);

            assertThat(result).isNotNull();
            verify(bookingRepository, times(1)).save(any(Booking.class));
        }
    }

    @Nested
    @DisplayName("cancelBooking() tests")
    class CancelBookingTests {

        @Test
        @DisplayName("Should cancel booking successfully")
        void cancelBooking_Success_ShouldReturnCancelledBooking() {
            when(bookingRepository.findById(1000L)).thenReturn(Optional.of(testBooking));
            when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
            when(bookingMapper.toResponseDto(any(Booking.class))).thenReturn(testResponse);

            BookingResponseDto result = bookingService.cancelBooking(1000L);

            assertThat(result).isNotNull();
            assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            verify(bookingRepository, times(1)).save(testBooking);
        }

        @Test
        @DisplayName("Should throw exception when booking not found")
        void cancelBooking_BookingNotFound_ShouldThrowException() {
            when(bookingRepository.findById(1000L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.cancelBooking(1000L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Booking not found with id: 1000");
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when booking is already completed")
        void cancelBooking_AlreadyCompleted_ShouldThrowException() {
            testBooking.setStatus(BookingStatus.COMPLETED);
            when(bookingRepository.findById(1000L)).thenReturn(Optional.of(testBooking));

            assertThatThrownBy(() -> bookingService.cancelBooking(1000L))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Cannot cancel completed booking");
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when booking is already cancelled")
        void cancelBooking_AlreadyCancelled_ShouldThrowException() {
            testBooking.setStatus(BookingStatus.CANCELLED);
            when(bookingRepository.findById(1000L)).thenReturn(Optional.of(testBooking));

            assertThatThrownBy(() -> bookingService.cancelBooking(1000L))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Booking is already cancelled");
            verify(bookingRepository, never()).save(any(Booking.class));
        }
    }

    @Nested
    @DisplayName("confirmBooking() tests")
    class ConfirmBookingTests {

        @Test
        @DisplayName("Should confirm booking successfully")
        void confirmBooking_Success_ShouldReturnConfirmedBooking() {
            when(bookingRepository.findById(1000L)).thenReturn(Optional.of(testBooking));
            when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
            when(bookingMapper.toResponseDto(any(Booking.class))).thenReturn(testResponse);

            BookingResponseDto result = bookingService.confirmBooking(1000L);

            assertThat(result).isNotNull();
            assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            verify(bookingRepository, times(1)).save(testBooking);
        }

        @Test
        @DisplayName("Should throw exception when booking is not pending")
        void confirmBooking_NotPending_ShouldThrowException() {
            testBooking.setStatus(BookingStatus.CONFIRMED);
            when(bookingRepository.findById(1000L)).thenReturn(Optional.of(testBooking));

            assertThatThrownBy(() -> bookingService.confirmBooking(1000L))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Only pending bookings can be confirmed");
            verify(bookingRepository, never()).save(any(Booking.class));
        }
    }

    @Nested
    @DisplayName("getBookingsByUser() tests")
    class GetBookingsByUserTests {

        @Test
        @DisplayName("Should return list of bookings for user")
        void getBookingsByUser_Success_ShouldReturnBookingsList() {
            List<Booking> bookings = Arrays.asList(testBooking, testBooking);
            when(userRepository.existsById(1L)).thenReturn(true);
            when(bookingRepository.findByPassengerId(1L)).thenReturn(bookings);
            when(bookingMapper.toResponseDtoList(bookings)).thenReturn(
                    Arrays.asList(testResponse, testResponse));

            List<BookingResponseDto> result = bookingService.getBookingsByUser(1L);

            assertThat(result).hasSize(2);
            verify(bookingRepository, times(1)).findByPassengerId(1L);
        }

        @Test
        @DisplayName("Should return empty list when no bookings for user")
        void getBookingsByUser_NoBookings_ShouldReturnEmptyList() {
            when(userRepository.existsById(1L)).thenReturn(true);
            when(bookingRepository.findByPassengerId(1L)).thenReturn(List.of());
            when(bookingMapper.toResponseDtoList(List.of())).thenReturn(List.of());

            List<BookingResponseDto> result = bookingService.getBookingsByUser(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void getBookingsByUser_UserNotFound_ShouldThrowException() {
            when(userRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> bookingService.getBookingsByUser(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with id: 999");
            verify(bookingRepository, never()).findByPassengerId(anyLong());
        }
    }

    @Nested
    @DisplayName("getBookingsByRide() tests")
    class GetBookingsByRideTests {

        @Test
        @DisplayName("Should return list of bookings for ride")
        void getBookingsByRide_Success_ShouldReturnBookingsList() {
            List<Booking> bookings = Arrays.asList(testBooking, testBooking);
            when(rideRepository.existsById(100L)).thenReturn(true);
            when(bookingRepository.findByRideId(100L)).thenReturn(bookings);
            when(bookingMapper.toResponseDtoList(bookings)).thenReturn(
                    Arrays.asList(testResponse, testResponse));

            List<BookingResponseDto> result = bookingService.getBookingsByRide(100L);

            assertThat(result).hasSize(2);
            verify(bookingRepository, times(1)).findByRideId(100L);
        }

        @Test
        @DisplayName("Should return empty list when no bookings for ride")
        void getBookingsByRide_NoBookings_ShouldReturnEmptyList() {
            when(rideRepository.existsById(100L)).thenReturn(true);
            when(bookingRepository.findByRideId(100L)).thenReturn(List.of());
            when(bookingMapper.toResponseDtoList(List.of())).thenReturn(List.of());

            List<BookingResponseDto> result = bookingService.getBookingsByRide(100L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when ride not found")
        void getBookingsByRide_RideNotFound_ShouldThrowException() {
            when(rideRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> bookingService.getBookingsByRide(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Ride not found with id: 999");
            verify(bookingRepository, never()).findByRideId(anyLong());
        }
    }

    @Nested
    @DisplayName("getAllBookings() tests")
    class GetAllBookingsTests {

        @Test
        @DisplayName("Should return all bookings successfully")
        void getAllBookings_Success_ShouldReturnAllBookings() {
            List<Booking> bookings = Arrays.asList(testBooking, testBooking, testBooking);
            List<BookingResponseDto> expectedResponses = Arrays.asList(testResponse, testResponse, testResponse);

            when(bookingRepository.findAll()).thenReturn(bookings);
            when(bookingMapper.toResponseDtoList(bookings)).thenReturn(expectedResponses);

            List<BookingResponseDto> result = bookingService.getAllBookings();

            assertThat(result)
                    .isNotNull()
                    .hasSize(3)
                    .isEqualTo(expectedResponses);

            verify(bookingRepository, times(1)).findAll();
            verify(bookingMapper, times(1)).toResponseDtoList(bookings);
        }

        @Test
        @DisplayName("Should return single booking when only one exists")
        void getAllBookings_SingleBooking_ShouldReturnOneBooking() {
            List<Booking> singleBooking = List.of(testBooking);
            List<BookingResponseDto> singleResponse = List.of(testResponse);

            when(bookingRepository.findAll()).thenReturn(singleBooking);
            when(bookingMapper.toResponseDtoList(singleBooking)).thenReturn(singleResponse);

            List<BookingResponseDto> result = bookingService.getAllBookings();

            assertThat(result)
                    .isNotNull()
                    .hasSize(1);

            assertThat(result.get(0).getId()).isEqualTo(1000L);

            verify(bookingRepository, times(1)).findAll();
            verify(bookingMapper, times(1)).toResponseDtoList(singleBooking);
        }

        @Test
        @DisplayName("Should handle bookings with different statuses")
        void getAllBookings_DifferentStatuses_ShouldReturnAll() {
            Booking confirmedBooking = new Booking();
            confirmedBooking.setId(2000L);
            confirmedBooking.setStatus(BookingStatus.CONFIRMED);

            Booking cancelledBooking = new Booking();
            cancelledBooking.setId(3000L);
            cancelledBooking.setStatus(BookingStatus.CANCELLED);

            Booking completedBooking = new Booking();
            completedBooking.setId(4000L);
            completedBooking.setStatus(BookingStatus.COMPLETED);

            List<Booking> bookings = Arrays.asList(
                    testBooking, confirmedBooking, cancelledBooking, completedBooking);

            when(bookingRepository.findAll()).thenReturn(bookings);
            when(bookingMapper.toResponseDtoList(bookings)).thenReturn(
                    Arrays.asList(testResponse, testResponse, testResponse, testResponse));

            List<BookingResponseDto> result = bookingService.getAllBookings();

            assertThat(result)
                    .isNotNull()
                    .hasSize(4);

            verify(bookingRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return bookings sorted by ID (default order from DB)")
        void getAllBookings_ShouldMaintainOrderFromDatabase() {
            Booking booking1 = new Booking();
            booking1.setId(1L);

            Booking booking2 = new Booking();
            booking2.setId(2L);

            Booking booking3 = new Booking();
            booking3.setId(3L);

            List<Booking> bookings = Arrays.asList(booking1, booking2, booking3);

            when(bookingRepository.findAll()).thenReturn(bookings);
            when(bookingMapper.toResponseDtoList(bookings)).thenReturn(
                    Arrays.asList(new BookingResponseDto(), new BookingResponseDto(), new BookingResponseDto()));

            List<BookingResponseDto> result = bookingService.getAllBookings();

            assertThat(result)
                    .isNotNull()
                    .hasSize(3);

            verify(bookingRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should handle null response from mapper gracefully")
        void getAllBookings_NullMapperResponse_ShouldHandleGracefully() {
            List<Booking> bookings = Arrays.asList(testBooking, testBooking);

            when(bookingRepository.findAll()).thenReturn(bookings);
            when(bookingMapper.toResponseDtoList(bookings)).thenReturn(null);

            List<BookingResponseDto> result = bookingService.getAllBookings();

            assertThat(result).isNull();

            verify(bookingRepository, times(1)).findAll();
            verify(bookingMapper, times(1)).toResponseDtoList(bookings);
        }

        @Test
        @DisplayName("Should call repository.findAll exactly once")
        void getAllBookings_ShouldCallFindAllExactlyOnce() {
            List<Booking> bookings = Arrays.asList(testBooking, testBooking);

            when(bookingRepository.findAll()).thenReturn(bookings);
            when(bookingMapper.toResponseDtoList(bookings)).thenReturn(
                    Arrays.asList(testResponse, testResponse));

            bookingService.getAllBookings();

            verify(bookingRepository, times(1)).findAll();
            verify(bookingMapper, times(1)).toResponseDtoList(bookings);
        }

        @Test
        @DisplayName("Should handle large number of bookings efficiently")
        void getAllBookings_LargeDataSet_ShouldHandleEfficiently() {
            List<Booking> largeBookings = new java.util.ArrayList<>();
            for (int i = 0; i < 100; i++) {
                Booking booking = new Booking();
                booking.setId((long) i);
                largeBookings.add(booking);
            }

            when(bookingRepository.findAll()).thenReturn(largeBookings);
            when(bookingMapper.toResponseDtoList(anyList())).thenReturn(new java.util.ArrayList<>());

            List<BookingResponseDto> result = bookingService.getAllBookings();

            assertThat(result).isNotNull();

            verify(bookingRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should preserve booking data integrity")
        void getAllBookings_ShouldPreserveDataIntegrity() {
            Booking originalBooking = new Booking();
            originalBooking.setId(1000L);
            originalBooking.setSeats(3);
            originalBooking.setStatus(BookingStatus.CONFIRMED);

            BookingResponseDto expectedResponse = new BookingResponseDto();
            expectedResponse.setId(1000L);
            expectedResponse.setSeats(3);
            expectedResponse.setStatus(BookingStatus.CONFIRMED);

            when(bookingRepository.findAll()).thenReturn(List.of(originalBooking));
            when(bookingMapper.toResponseDtoList(List.of(originalBooking))).thenReturn(List.of(expectedResponse));

            List<BookingResponseDto> result = bookingService.getAllBookings();

            assertThat(result)
                    .hasSize(1);

            BookingResponseDto actual = result.get(0);
            assertThat(actual.getId()).isEqualTo(1000L);
            assertThat(actual.getSeats()).isEqualTo(3);
            assertThat(actual.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        }
    }
}