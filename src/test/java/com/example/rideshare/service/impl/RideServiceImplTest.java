package com.example.rideshare.service.impl;

import com.example.rideshare.exception.BusinessException;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.RideMapper;
import com.example.rideshare.mapper.RouteMapper;
import com.example.rideshare.model.dto.BulkRideRequestDto;
import com.example.rideshare.model.dto.RideRequestDto;
import com.example.rideshare.model.dto.RideResponseDto;
import com.example.rideshare.model.dto.RouteRequestDto;
import com.example.rideshare.model.entity.Ride;
import com.example.rideshare.model.entity.Route;
import com.example.rideshare.model.entity.User;
import com.example.rideshare.model.enums.RideStatus;
import com.example.rideshare.repository.BookingRepository;
import com.example.rideshare.repository.RideRepository;
import com.example.rideshare.repository.RouteRepository;
import com.example.rideshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RideService Unit Tests")
class RideServiceImplTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private RideMapper rideMapper;

    @Mock
    private RouteMapper routeMapper;

    @InjectMocks
    private RideServiceImpl rideService;

    private User testDriver;
    private Route testRoute;
    private Ride testRide;
    private RideRequestDto testRideDto;
    private RideResponseDto testResponseDto;
    private BulkRideRequestDto testBulkRequest;

    @BeforeEach
    void setUp() {
        // Инициализация тестовых данных
        testDriver = new User();
        testDriver.setId(1L);
        testDriver.setName("Test Driver");
        testDriver.setRidesAsDriver(new ArrayList<>());

        testRoute = new Route();
        testRoute.setId(10L);
        testRoute.setStartPoint("Москва");
        testRoute.setEndPoint("Санкт-Петербург");
        testRoute.setDistanceKm(700.0);
        testRoute.setEstimatedDurationMinutes(480);

        testRide = new Ride();
        testRide.setId(100L);
        testRide.setDriver(testDriver);
        testRide.setRoute(testRoute);
        testRide.setDepartureTime(LocalDateTime.now().plusDays(7));
        testRide.setAvailableSeats(4);
        testRide.setPrice(1500.0);
        testRide.setStatus(RideStatus.SCHEDULED);
        testRide.setBookings(new ArrayList<>());

        RouteRequestDto routeDto = new RouteRequestDto();
        routeDto.setStartPoint("Москва");
        routeDto.setEndPoint("Санкт-Петербург");
        routeDto.setDistanceKm(700.0);
        routeDto.setEstimatedDurationMinutes(480);

        testRideDto = new RideRequestDto();
        testRideDto.setDepartureTime(LocalDateTime.now().plusDays(7));
        testRideDto.setAvailableSeats(4);
        testRideDto.setPrice(1500.0);
        testRideDto.setRoute(routeDto);

        testResponseDto = new RideResponseDto();
        testResponseDto.setId(100L);
        testResponseDto.setPrice(1500.0);
        testResponseDto.setStatus("SCHEDULED");

        testBulkRequest = new BulkRideRequestDto(
                1L,  // driverId
                List.of(testRideDto, testRideDto)  // rides
        );
    }

    // ==================== GET ALL RIDES TESTS ====================

    @Nested
    @DisplayName("getAllRides() tests")
    class GetAllRidesTests {

        @Test
        @DisplayName("Should return list of all rides")
        void getAllRides_Success_ShouldReturnRidesList() {
            // given
            List<Ride> rides = Arrays.asList(testRide, testRide);
            List<RideResponseDto> expectedResponse = Arrays.asList(testResponseDto, testResponseDto);
            when(rideRepository.findAllWithDetailsViaEntityGraph()).thenReturn(rides);
            when(rideMapper.toResponseDtoList(rides)).thenReturn(expectedResponse);

            // when
            List<RideResponseDto> result = rideService.getAllRides();

            // then
            assertThat(result).hasSize(2);
            verify(rideRepository, times(1)).findAllWithDetailsViaEntityGraph();
        }
    }

    // ==================== GET RIDE BY ID TESTS ====================

    @Nested
    @DisplayName("getRideById() tests")
    class GetRideByIdTests {

        @Test
        @DisplayName("Should return ride when id exists")
        void getRideById_Success_ShouldReturnRide() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(rideMapper.toResponseDto(testRide)).thenReturn(testResponseDto);

            // when
            RideResponseDto result = rideService.getRideById(100L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Should throw exception when id is null")
        void getRideById_NullId_ShouldThrowException() {
            // when & then
            assertThatThrownBy(() -> rideService.getRideById(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Ride ID cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when ride not found")
        void getRideById_NotFound_ShouldThrowException() {
            // given
            when(rideRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> rideService.getRideById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Ride not found with id: 999");
        }
    }

    // ==================== UPDATE RIDE TESTS ====================

    @Nested
    @DisplayName("updateRide() tests")
    class UpdateRideTests {

        @Test
        @DisplayName("Should update ride successfully")
        void updateRide_Success_ShouldReturnUpdatedRide() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(routeMapper.toEntity(any(RouteRequestDto.class))).thenReturn(testRoute);  // ← добавить
            when(rideRepository.save(any(Ride.class))).thenReturn(testRide);
            when(rideMapper.toResponseDto(testRide)).thenReturn(testResponseDto);

            // when
            RideResponseDto result = rideService.updateRide(100L, testRideDto);

            // then
            assertThat(result).isNotNull();
            verify(rideRepository, times(1)).save(testRide);
        }

        @Test
        @DisplayName("Should throw exception when id is null")
        void updateRide_NullId_ShouldThrowException() {
            // when & then
            assertThatThrownBy(() -> rideService.updateRide(null, testRideDto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Ride ID cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when ride not found")
        void updateRide_NotFound_ShouldThrowException() {
            // given
            when(rideRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> rideService.updateRide(999L, testRideDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Ride not found with id: 999");
        }

        @Test
        @DisplayName("Should throw BusinessException when departure time is in past")
        void updateRide_DepartureTimeInPast_ShouldThrowException() {
            // given
            testRideDto.setDepartureTime(LocalDateTime.now().minusDays(1));
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));

            // when & then
            assertThatThrownBy(() -> rideService.updateRide(100L, testRideDto))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ==================== DELETE RIDE TESTS ====================

    @Nested
    @DisplayName("deleteRide() tests")
    class DeleteRideTests {

        @Test
        @DisplayName("Should delete ride successfully")
        void deleteRide_Success_ShouldDeleteRide() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(0);
            doNothing().when(rideRepository).delete(testRide);

            // when
            rideService.deleteRide(100L);

            // then
            verify(rideRepository, times(1)).delete(testRide);
        }

        @Test
        @DisplayName("Should throw exception when ride has existing bookings")
        void deleteRide_WithBookings_ShouldThrowException() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(2);

            // when & then
            assertThatThrownBy(() -> rideService.deleteRide(100L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cannot delete ride with existing bookings");
            verify(rideRepository, never()).delete(any(Ride.class));
        }
    }

    // ==================== UPDATE RIDE STATUS TESTS ====================

    @Nested
    @DisplayName("updateRideStatus() tests")
    class UpdateRideStatusTests {

        @Test
        @DisplayName("Should update status to IN_PROGRESS successfully")
        void updateRideStatus_ToInProgress_ShouldReturnUpdatedRide() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(rideRepository.save(any(Ride.class))).thenReturn(testRide);
            when(rideMapper.toResponseDto(testRide)).thenReturn(testResponseDto);

            // when
            RideResponseDto result = rideService.updateRideStatus(100L, "IN_PROGRESS");

            // then
            assertThat(result).isNotNull();
            assertThat(testRide.getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Should update status to COMPLETED successfully")
        void updateRideStatus_ToCompleted_ShouldReturnUpdatedRide() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(rideRepository.save(any(Ride.class))).thenReturn(testRide);
            when(rideMapper.toResponseDto(testRide)).thenReturn(testResponseDto);

            // when
            RideResponseDto result = rideService.updateRideStatus(100L, "COMPLETED");

            // then
            assertThat(result).isNotNull();
            assertThat(testRide.getStatus()).isEqualTo(RideStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should throw exception when status is invalid")
        void updateRideStatus_InvalidStatus_ShouldThrowException() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));

            // when & then
            assertThatThrownBy(() -> rideService.updateRideStatus(100L, "INVALID"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid status: INVALID");
        }

        @Test
        @DisplayName("Should throw exception when status is null")
        void updateRideStatus_NullStatus_ShouldThrowException() {
            // when & then
            assertThatThrownBy(() -> rideService.updateRideStatus(100L, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Status cannot be null or empty");
        }
    }

    // ==================== BULK CREATE RIDES TESTS ====================

    @Nested
    @DisplayName("createRidesBulk() tests")
    class CreateRidesBulkTests {

//        @Test
//        @DisplayName("Should create multiple rides successfully")
//        void createRidesBulk_Success_ShouldReturnListOfRides() {
//
//            when(userRepository.findById(1L)).thenReturn(Optional.of(testDriver));
//            when(routeRepository.findAll()).thenReturn(List.of(testRoute));
//            when(routeMapper.toEntity(any(RouteRequestDto.class))).thenReturn(testRoute);
//            when(rideMapper.toEntity(any(RideRequestDto.class))).thenReturn(testRide);
//            when(rideRepository.save(any(Ride.class))).thenReturn(testRide);
//            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);
//
//            List<RideResponseDto> result = rideService.createRidesBulk(testBulkRequest);
//
//            assertThat(result).hasSize(2);
//            verify(rideRepository, times(2)).save(any(Ride.class));
//        }

        @Test
        @DisplayName("Should throw exception when driver not found")
        void createRidesBulk_DriverNotFound_ShouldThrowException() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> rideService.createRidesBulk(testBulkRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Driver not found");
        }

        @Test
        @DisplayName("Should create new route when not exists")
        void createRidesBulk_NewRoute_ShouldCreateRoute() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testDriver));
            when(routeRepository.findAll()).thenReturn(List.of()); // нет существующих маршрутов
            when(routeMapper.toEntity(any(RouteRequestDto.class))).thenReturn(testRoute);
            when(rideMapper.toEntity(any(RideRequestDto.class))).thenReturn(testRide);
            when(rideRepository.save(any(Ride.class))).thenReturn(testRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);
            when(routeRepository.save(any(Route.class))).thenReturn(testRoute);

            // when
            List<RideResponseDto> result = rideService.createRidesBulk(testBulkRequest);

            // then
            assertThat(result).hasSize(2);
            verify(routeRepository, atLeastOnce()).save(any(Route.class));
        }

        @Test
        @DisplayName("Should throw BusinessException when price > 10000")
        void createRidesBulk_PriceTooHigh_ShouldThrowException() {
            // given
            testRideDto.setPrice(15000.0);
            testBulkRequest = new BulkRideRequestDto(1L, List.of(testRideDto));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testDriver));

            // when & then
            assertThatThrownBy(() -> rideService.createRidesBulk(testBulkRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("DEMO ERROR");
        }
    }

    // ==================== CACHE TESTS ====================

    @Nested
    @DisplayName("Cache operations tests")
    class CacheTests {

        @Test
        @DisplayName("invalidateCache() should clear cache and increment modification count")
        void invalidateCache_ShouldClearCache() {
            // given
            Map<String, Object> statsBefore = rideService.getCacheStats();
            long countBefore = (Long) statsBefore.get("modificationCount");

            // when
            rideService.invalidateCache();

            // then
            Map<String, Object> statsAfter = rideService.getCacheStats();
            long countAfter = (Long) statsAfter.get("modificationCount");
            assertThat(countAfter).isGreaterThan(countBefore);
        }
    }
}