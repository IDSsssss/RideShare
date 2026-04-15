package com.example.rideshare.service.impl;

import com.example.rideshare.exception.BusinessException;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.RideMapper;
import com.example.rideshare.mapper.RouteMapper;
import com.example.rideshare.model.dto.*;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.example.rideshare.model.dto.RideSearchRequest;

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

        private Ride existingRide;
        private RideRequestDto updateRequest;
        private RouteRequestDto routeDto;

        @BeforeEach
        void setUp() {
            existingRide = new Ride();
            existingRide.setId(100L);
            existingRide.setDepartureTime(LocalDateTime.now().plusDays(7));
            existingRide.setAvailableSeats(4);
            existingRide.setPrice(1500.0);

            Route existingRoute = new Route();
            existingRoute.setId(10L);
            existingRoute.setStartPoint("Москва");
            existingRoute.setEndPoint("СПб");
            existingRide.setRoute(existingRoute);

            routeDto = new RouteRequestDto();
            routeDto.setStartPoint("Москва");
            routeDto.setEndPoint("Казань");
            routeDto.setDistanceKm(820.0);
            routeDto.setEstimatedDurationMinutes(540);

            updateRequest = new RideRequestDto();
            updateRequest.setDepartureTime(LocalDateTime.now().plusDays(14));
            updateRequest.setAvailableSeats(3);
            updateRequest.setPrice(2000.0);
            updateRequest.setRoute(routeDto);
        }

        @Test
        @DisplayName("Should update route when both existing route and request route are not null")
        void updateRide_BothRoutesExist_ShouldUpdateRoute() {
            // given
            Route updatedRoute = new Route();
            updatedRoute.setId(10L);
            updatedRoute.setStartPoint("Москва");
            updatedRoute.setEndPoint("Казань");

            when(rideRepository.findById(100L)).thenReturn(Optional.of(existingRide));
            when(routeMapper.toEntity(any(RouteRequestDto.class))).thenReturn(updatedRoute);
            when(rideRepository.save(any(Ride.class))).thenReturn(existingRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // when
            RideResponseDto result = rideService.updateRide(100L, updateRequest);

            // then
            assertThat(result).isNotNull();
            verify(routeMapper, times(1)).toEntity(any(RouteRequestDto.class));
            verify(rideRepository, times(1)).save(existingRide);
        }

        @Test
        @DisplayName("Should NOT update route when existing route is null")
        void updateRide_ExistingRouteIsNull_ShouldNotUpdateRoute() {
            // given
            existingRide.setRoute(null);

            when(rideRepository.findById(100L)).thenReturn(Optional.of(existingRide));
            when(rideRepository.save(any(Ride.class))).thenReturn(existingRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // when
            RideResponseDto result = rideService.updateRide(100L, updateRequest);

            // then
            assertThat(result).isNotNull();
            verify(routeMapper, never()).toEntity(any(RouteRequestDto.class));
            verify(rideRepository, times(1)).save(existingRide);
        }

        @Test
        @DisplayName("Should NOT update route when request route is null")
        void updateRide_RequestRouteIsNull_ShouldNotUpdateRoute() {
            // given
            updateRequest.setRoute(null);

            when(rideRepository.findById(100L)).thenReturn(Optional.of(existingRide));
            when(rideRepository.save(any(Ride.class))).thenReturn(existingRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // when
            RideResponseDto result = rideService.updateRide(100L, updateRequest);

            // then
            assertThat(result).isNotNull();
            verify(routeMapper, never()).toEntity(any(RouteRequestDto.class));
            verify(rideRepository, times(1)).save(existingRide);
        }

        @Test
        @DisplayName("Should NOT update route when both existing and request routes are null")
        void updateRide_BothRoutesNull_ShouldNotUpdateRoute() {
            // given
            existingRide.setRoute(null);
            updateRequest.setRoute(null);

            when(rideRepository.findById(100L)).thenReturn(Optional.of(existingRide));
            when(rideRepository.save(any(Ride.class))).thenReturn(existingRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // when
            RideResponseDto result = rideService.updateRide(100L, updateRequest);

            // then
            assertThat(result).isNotNull();
            verify(routeMapper, never()).toEntity(any(RouteRequestDto.class));
            verify(rideRepository, times(1)).save(existingRide);
        }

        @Test
        @DisplayName("Should throw exception when ride not found")
        void updateRide_RideNotFound_ShouldThrowException() {
            // given
            when(rideRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> rideService.updateRide(999L, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Ride not found with id: 999");
        }

        @Test
        @DisplayName("Should throw exception when id is null")
        void updateRide_NullId_ShouldThrowException() {
            // when & then
            assertThatThrownBy(() -> rideService.updateRide(null, updateRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Ride ID cannot be null");
        }

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
        @DisplayName("Should throw exception when id is null")
        void deleteRide_NullId_ShouldThrowException() {
            // when & then
            assertThatThrownBy(() -> rideService.deleteRide(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Ride ID cannot be null");
            verify(rideRepository, never()).delete(any(Ride.class));
        }

        @Test
        @DisplayName("Should throw exception when ride not found")
        void deleteRide_NotFound_ShouldThrowException() {
            // given
            when(rideRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> rideService.deleteRide(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Ride not found with id: 999");
            verify(rideRepository, never()).delete(any(Ride.class));
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

        @Test
        @DisplayName("Should handle null bookedSeats correctly")
        void deleteRide_WhenBookedSeatsNull_ShouldDeleteSuccessfully() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(null); // ← null
            doNothing().when(rideRepository).delete(testRide);

            // when
            rideService.deleteRide(100L);

            // then
            verify(rideRepository, times(1)).delete(testRide);
        }

        @Test
        @DisplayName("Should invalidate cache after deletion")
        void deleteRide_ShouldInvalidateCache() throws Exception {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(0);
            doNothing().when(rideRepository).delete(testRide);

            // Создаём spy для проверки вызова метода
            RideServiceImpl spyService = spy(rideService);

            // when
            spyService.deleteRide(100L);

            // then
            verify(spyService, times(1)).invalidateCache();
        }
    }

    // ==================== UPDATE RIDE STATUS TESTS ====================

    @Nested
    @DisplayName("updateRideStatus() tests")
    class UpdateRideStatusTests {

        private Ride existingRide;

        @BeforeEach
        void setUp() {
            existingRide = new Ride();
            existingRide.setId(100L);
            existingRide.setStatus(RideStatus.SCHEDULED);
        }

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
        @DisplayName("Should update status to IN_PROGRESS successfully")
        void updateRideStatus_ToInProgress_ShouldSucceed() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(existingRide));
            when(rideRepository.save(any(Ride.class))).thenReturn(existingRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // when
            RideResponseDto result = rideService.updateRideStatus(100L, "IN_PROGRESS");

            // then
            assertThat(result).isNotNull();
            assertThat(existingRide.getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
            verify(rideRepository, times(1)).save(existingRide);
        }

        @Test
        @DisplayName("Should update status to COMPLETED successfully")
        void updateRideStatus_ToCompleted_ShouldSucceed() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(existingRide));
            when(rideRepository.save(any(Ride.class))).thenReturn(existingRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // when
            RideResponseDto result = rideService.updateRideStatus(100L, "COMPLETED");

            // then
            assertThat(result).isNotNull();
            assertThat(existingRide.getStatus()).isEqualTo(RideStatus.COMPLETED);
            verify(rideRepository, times(1)).save(existingRide);
        }

        @Test
        @DisplayName("Should update status to CANCELLED when no bookings")
        void updateRideStatus_ToCancelled_NoBookings_ShouldSucceed() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(existingRide));
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(0);
            when(rideRepository.save(any(Ride.class))).thenReturn(existingRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // when
            RideResponseDto result = rideService.updateRideStatus(100L, "CANCELLED");

            // then
            assertThat(result).isNotNull();
            assertThat(existingRide.getStatus()).isEqualTo(RideStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should throw exception when id is null")
        void updateRideStatus_NullId_ShouldThrowException() {
            // when & then
            assertThatThrownBy(() -> rideService.updateRideStatus(null, "IN_PROGRESS"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Ride ID cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when ride not found")
        void updateRideStatus_RideNotFound_ShouldThrowException() {
            // given
            when(rideRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> rideService.updateRideStatus(999L, "IN_PROGRESS"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Ride not found with id: 999");
        }

        @Test
        @DisplayName("Should throw exception when status is empty")
        void updateRideStatus_EmptyStatus_ShouldThrowException() {

            assertThatThrownBy(() -> rideService.updateRideStatus(100L, ""))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Status cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception when status is blank")
        void updateRideStatus_BlankStatus_ShouldThrowException() {

            assertThatThrownBy(() -> rideService.updateRideStatus(100L, "   "))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Status cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception when cancelling ride with existing bookings")
        void updateRideStatus_CancelWithBookings_ShouldThrowException() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(existingRide));
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(2);

            // when & then
            assertThatThrownBy(() -> rideService.updateRideStatus(100L, "CANCELLED"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cannot cancel ride with existing bookings");
            verify(rideRepository, never()).save(any(Ride.class));
        }

        @Test
        @DisplayName("Should handle null bookedSeats when cancelling (no bookings)")
        void updateRideStatus_CancelWithNullBookedSeats_ShouldSucceed() {
            // given
            when(rideRepository.findById(100L)).thenReturn(Optional.of(existingRide));
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(null);
            when(rideRepository.save(any(Ride.class))).thenReturn(existingRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // when
            RideResponseDto result = rideService.updateRideStatus(100L, "CANCELLED");

            // then
            assertThat(result).isNotNull();
            assertThat(existingRide.getStatus()).isEqualTo(RideStatus.CANCELLED);
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

        @Test
        @DisplayName("Should create multiple rides successfully when driver has existing list")
        void createRidesBulk_Success_WhenDriverHasExistingList() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testDriver));
            when(routeRepository.findAll()).thenReturn(List.of(testRoute));
            // ✅ НУЖНЫ ЭТИ МОКИ
            when(rideMapper.toEntity(any(RideRequestDto.class))).thenReturn(testRide);
            when(rideRepository.save(any(Ride.class))).thenReturn(testRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // when
            List<RideResponseDto> result = rideService.createRidesBulk(testBulkRequest);

            // then
            assertThat(result).hasSize(2);
            assertThat(testDriver.getRidesAsDriver()).hasSize(2);
            verify(userRepository, times(1)).save(testDriver);
        }

        @Test
        @DisplayName("Should handle duplicate routes in repository")
        void createRidesBulk_WhenDuplicateRoutesExist_ShouldUseExistingRoute() {
            // given
            // Создаём два одинаковых маршрута
            Route route1 = new Route();
            route1.setId(10L);
            route1.setStartPoint("Москва");
            route1.setEndPoint("СПб");

            Route route2 = new Route();
            route2.setId(20L);
            route2.setStartPoint("Москва");
            route2.setEndPoint("СПб");

            List<Route> duplicateRoutes = Arrays.asList(route1, route2);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testDriver));
            when(routeRepository.findAll()).thenReturn(duplicateRoutes); // два одинаковых маршрута
            when(rideMapper.toEntity(any(RideRequestDto.class))).thenReturn(testRide);
            when(rideRepository.save(any(Ride.class))).thenReturn(testRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // when
            List<RideResponseDto> result = rideService.createRidesBulk(testBulkRequest);

            // then
            assertThat(result).hasSize(2);
            // Проверяем, что использовался первый маршрут (existing), а не второй
            verify(rideRepository, times(2)).save(any(Ride.class));
        }

        @Test
        @DisplayName("Should create new list when driver's ridesAsDriver is null")
        void createRidesBulk_WhenDriverRidesAsDriverIsNull_ShouldCreateNewList() {
            // given
            User driverWithNullList = new User();
            driverWithNullList.setId(1L);
            driverWithNullList.setName("Test Driver");
            driverWithNullList.setRidesAsDriver(null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(driverWithNullList));
            when(routeRepository.findAll()).thenReturn(List.of(testRoute));
            // ✅ НУЖНЫ ЭТИ МОКИ
            when(rideMapper.toEntity(any(RideRequestDto.class))).thenReturn(testRide);
            when(rideRepository.save(any(Ride.class))).thenReturn(testRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // when
            List<RideResponseDto> result = rideService.createRidesBulk(testBulkRequest);

            // then
            assertThat(result).hasSize(2);
            assertThat(driverWithNullList.getRidesAsDriver()).isNotNull();
            assertThat(driverWithNullList.getRidesAsDriver()).hasSize(2);
            verify(userRepository, times(1)).save(driverWithNullList);
        }

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
        @DisplayName("Should throw BusinessException when price > 10000 (demo error)")
        void createRidesBulk_PriceTooHigh_ShouldThrowException() {
            // given
            RideRequestDto highPriceRide = new RideRequestDto();
            highPriceRide.setPrice(15000.0);
            highPriceRide.setDepartureTime(LocalDateTime.now().plusDays(7));
            highPriceRide.setAvailableSeats(4);

            RouteRequestDto routeDto = new RouteRequestDto();
            routeDto.setStartPoint("Москва");
            routeDto.setEndPoint("СПб");
            highPriceRide.setRoute(routeDto);

            // Используем конструктор record
            BulkRideRequestDto bulkRequest = new BulkRideRequestDto(
                    1L,                    // driverId
                    List.of(highPriceRide) // rides
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testDriver));

            // when & then
            assertThatThrownBy(() -> rideService.createRidesBulk(bulkRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("DEMO ERROR");
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
    }

    @Nested
    @DisplayName("searchRides() tests")
    class SearchRidesTests {

        private RideSearchRequest searchRequest;
        private Pageable pageable;
        private List<Ride> mockRides;
        private List<RideResponseDto> mockResponses;

        @BeforeEach
        void setUp() {
            pageable = PageRequest.of(0, 10);

            mockRides = Arrays.asList(testRide, testRide);
            mockResponses = Arrays.asList(testResponseDto, testResponseDto);

            searchRequest = new RideSearchRequest();
            searchRequest.setStartPoint("Москва");
            searchRequest.setEndPoint("СПб");
            searchRequest.setFromDate(LocalDateTime.now());
            searchRequest.setToDate(LocalDateTime.now().plusDays(30));
            searchRequest.setMinPrice(1000.0);
            searchRequest.setMaxPrice(3000.0);
            searchRequest.setMinSeats(2);
            searchRequest.setPageable(pageable);
            searchRequest.setUseNative(false);
        }

        @Test
        @DisplayName("Should search with JPQL when cache miss and useNative=false")
        void searchRides_CacheMiss_JPQL_ShouldSearchFromDatabase() {
            // given
            when(rideRepository.searchRides(
                    eq("Москва"), eq("СПб"), any(), any(), eq(1000.0), eq(3000.0), eq(2)))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // when
            Page<RideResponseDto> result = rideService.searchRides(searchRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            verify(rideRepository, times(1)).searchRides(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should search with Native query when cache miss and useNative=true")
        void searchRides_CacheMiss_Native_ShouldSearchFromDatabase() {
            // given
            searchRequest.setUseNative(true);
            when(rideRepository.searchRidesNative(
                    eq("Москва"), eq("СПб"), any(), any(), eq(1000.0), eq(3000.0), eq(2)))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // when
            Page<RideResponseDto> result = rideService.searchRides(searchRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            verify(rideRepository, times(1)).searchRidesNative(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should return empty page when no results")
        void searchRides_NoResults_ShouldReturnEmptyPage() {
            // given
            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of());

            Page<RideResponseDto> result = rideService.searchRides(searchRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Should apply pagination correctly")
        void searchRides_Pagination_ShouldReturnCorrectPage() {
            // given
            Pageable pageable2 = PageRequest.of(0, 1);
            searchRequest.setPageable(pageable2);

            List<Ride> singleRide = List.of(testRide);
            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(singleRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // when
            Page<RideResponseDto> result = rideService.searchRides(searchRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getPageable().getPageSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle empty cache after invalidation")
        void searchRides_CacheInvalidated_ShouldSearchAgain() {
            // given
            // Заполняем кэш
            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            rideService.searchRides(searchRequest);

            // Инвалидируем кэш
            rideService.invalidateCache();

            // Сбрасываем счётчики вызовов
            reset(rideRepository, rideMapper);

            // Настраиваем моки для второго поиска
            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // when
            Page<RideResponseDto> result = rideService.searchRides(searchRequest);

            // then
            assertThat(result).isNotNull();
            // Репозиторий должен быть вызван снова (кэш очищен)
            verify(rideRepository, times(1)).searchRides(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should handle null filters correctly")
        void searchRides_NullFilters_ShouldSearchWithoutFilters() {
            // given
            RideSearchRequest requestWithNulls = new RideSearchRequest();
            requestWithNulls.setPageable(pageable);
            requestWithNulls.setUseNative(false);

            when(rideRepository.searchRides(null, null, null, null, null, null, null))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // when
            Page<RideResponseDto> result = rideService.searchRides(requestWithNulls);

            // then
            assertThat(result).isNotNull();
            verify(rideRepository, times(1)).searchRides(null, null, null, null, null, null, null);
        }

        @Test
        @DisplayName("Should return cached data when cache hit occurs")
        void searchRides_CacheHit_ShouldReturnCachedData() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            searchRequest.setPageable(pageable);

            // Первый вызов — заполняет кэш
            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            Page<RideResponseDto> firstResult = rideService.searchRides(searchRequest);

            // Сбрасываем моки — второй вызов не должен обращаться к репозиторию
            reset(rideRepository, rideMapper);

            // when — второй вызов (должен взять из кэша)
            Page<RideResponseDto> secondResult = rideService.searchRides(searchRequest);

            // then
            assertThat(secondResult).isNotNull();
            assertThat(secondResult.getContent()).hasSize(2);
            // Репозиторий НЕ вызывается при cache hit
            verify(rideRepository, never()).searchRides(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should NOT use cache when modification count changed")
        void searchRides_CacheMiss_WhenModificationCountChanged() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            searchRequest.setPageable(pageable);

            // Первый вызов — заполняет кэш
            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            rideService.searchRides(searchRequest);

            // Инвалидируем кэш — меняет modificationCount
            rideService.invalidateCache();

            // Сбрасываем моки и настраиваем для второго вызова
            reset(rideRepository, rideMapper);
            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // when
            Page<RideResponseDto> secondResult = rideService.searchRides(searchRequest);

            // then
            assertThat(secondResult).isNotNull();
            // Репозиторий ДОЛЖЕН быть вызван снова (cache miss)
            verify(rideRepository, times(1)).searchRides(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("searchRides() cache tests")
    class SearchRidesCacheTests {

        private RideSearchRequest searchRequest;
        private Pageable pageable;
        private List<Ride> mockRides;
        private List<RideResponseDto> mockResponses;

        @BeforeEach
        void setUp() {
            pageable = PageRequest.of(0, 10);

            mockRides = Arrays.asList(testRide, testRide);
            mockResponses = Arrays.asList(testResponseDto, testResponseDto);

            searchRequest = new RideSearchRequest();
            searchRequest.setStartPoint("Москва");
            searchRequest.setEndPoint("СПб");
            searchRequest.setPageable(pageable);
            searchRequest.setUseNative(false);
        }

        @Test
        @DisplayName("Should return cached data when cache hit")
        void searchRides_CacheHit_WhenModificationCountUnchangedAndKeyExists() {
            // given - первый вызов заполняет кэш
            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // Первый вызов — заполняем кэш
            rideService.searchRides(searchRequest);

            // Сбрасываем моки для второго вызова
            reset(rideRepository, rideMapper);

            // Второй вызов — должен взять из кэша
            Page<RideResponseDto> result = rideService.searchRides(searchRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            // Репозиторий не должен вызываться при cache hit
            verify(rideRepository, never()).searchRides(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should NOT use cache when modification count changed")
        void searchRides_CacheMiss_WhenModificationCountChanged() {
            // given - первый вызов заполняет кэш
            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // Первый вызов — заполняем кэш
            rideService.searchRides(searchRequest);

            // Инвалидируем кэш (изменяет modificationCount)
            rideService.invalidateCache();

            // Сбрасываем моки и настраиваем для второго вызова
            reset(rideRepository, rideMapper);
            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // when
            Page<RideResponseDto> result = rideService.searchRides(searchRequest);

            // then
            assertThat(result).isNotNull();
            // Репозиторий должен быть вызван снова (cache miss)
            verify(rideRepository, times(1)).searchRides(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should NOT use cache when key does not exist")
        void searchRides_CacheMiss_WhenKeyNotExists() {
            // given
            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // when (первый вызов, кэш пуст)
            Page<RideResponseDto> result = rideService.searchRides(searchRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            verify(rideRepository, times(1)).searchRides(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should use cache for different page of same filters")
        void searchRides_CacheHit_DifferentPage() throws Exception {
            // given - первый запрос page 0
            Pageable pageable0 = PageRequest.of(0, 2);
            searchRequest.setPageable(pageable0);

            List<Ride> manyRides = Arrays.asList(testRide, testRide, testRide, testRide);
            List<RideResponseDto> manyResponses = Arrays.asList(
                    testResponseDto, testResponseDto, testResponseDto, testResponseDto);

            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(manyRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // Первый вызов — заполняем кэш (сохраняются ВСЕ 4 элемента)
            Page<RideResponseDto> firstPage = rideService.searchRides(searchRequest);

            // Сбрасываем моки
            reset(rideRepository, rideMapper);

            // Второй вызов — page 1
            Pageable pageable1 = PageRequest.of(1, 2);
            searchRequest.setPageable(pageable1);

            // when
            Page<RideResponseDto> secondPage = rideService.searchRides(searchRequest);

            // then
            assertThat(secondPage).isNotNull();
            // Репозиторий НЕ вызывается — данные берутся из кэша
            verify(rideRepository, never()).searchRides(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("invalidateCache() should clear cache and increment modification count")
        void invalidateCache_ShouldClearCache() {
            // given
            // Заполняем кэш
            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of(testRide));
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            rideService.searchRides(searchRequest);

            Map<String, Object> statsBefore = rideService.getCacheStats();

            // when
            rideService.invalidateCache();

            // then
            Map<String, Object> statsAfter = rideService.getCacheStats();
            assertThat((Integer) statsAfter.get("cacheSize")).isZero();
            assertThat((Long) statsAfter.get("modificationCount"))
                    .isGreaterThan((Long) statsBefore.get("modificationCount"));
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

    @Nested
    @DisplayName("searchRides() pagination tests")
    class SearchRidesPaginationTests {

        private RideSearchRequest searchRequest;
        private List<Ride> mockRides;
        private List<RideResponseDto> mockResponses;

        @BeforeEach
        void setUp() {
            searchRequest = new RideSearchRequest();
            searchRequest.setStartPoint("Москва");
            searchRequest.setEndPoint("СПб");
            searchRequest.setUseNative(false);

            // Создаём 5 тестовых поездок
            mockRides = Arrays.asList(testRide, testRide, testRide, testRide, testRide);
            mockResponses = Arrays.asList(
                    testResponseDto, testResponseDto, testResponseDto, testResponseDto, testResponseDto);
        }

        @Test
        @DisplayName("Should return paged content when start < cachedData.size()")
        void searchRides_WhenStartLessThanSize_ShouldReturnSubList() {
            // given
            Pageable pageable = PageRequest.of(0, 3);
            searchRequest.setPageable(pageable);

            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // Первый вызов заполняет кэш (сохраняются ВСЕ 5 элементов)
            rideService.searchRides(searchRequest);

            // Сбрасываем моки
            reset(rideRepository, rideMapper);

            // when - второй вызов, page 0, size 3
            Page<RideResponseDto> result = rideService.searchRides(searchRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3); // start=0 < 5 → берём подсписок
            verify(rideRepository, never()).searchRides(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should return empty list when start >= cachedData.size()")
        void searchRides_WhenStartGreaterThanOrEqualSize_ShouldReturnEmptyList() {
            // given
            Pageable pageable = PageRequest.of(10, 3); // page 10, start = 10*3 = 30
            searchRequest.setPageable(pageable);

            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // Первый вызов заполняет кэш (сохраняются ВСЕ 5 элементов)
            rideService.searchRides(searchRequest);

            // Сбрасываем моки
            reset(rideRepository, rideMapper);

            // when - второй вызов, page 10 (start=30 >= 5)
            Page<RideResponseDto> result = rideService.searchRides(searchRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty(); // start >= size → пустой список
            verify(rideRepository, never()).searchRides(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should return last page correctly when end exceeds size")
        void searchRides_WhenEndExceedsSize_ShouldReturnRemainingElements() {
            // given
            Pageable pageable = PageRequest.of(1, 3); // page 1, start = 3, end = 6
            searchRequest.setPageable(pageable);

            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // Первый вызов заполняет кэш (сохраняются ВСЕ 5 элементов)
            rideService.searchRides(searchRequest);

            // Сбрасываем моки
            reset(rideRepository, rideMapper);

            // when
            Page<RideResponseDto> result = rideService.searchRides(searchRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2); // end=6, но size=5 → берём элементы с 3 по 4
            verify(rideRepository, never()).searchRides(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should work correctly with page size 1")
        void searchRides_WithPageSizeOne_ShouldReturnSingleElement() {
            // given
            Pageable pageable = PageRequest.of(0, 1);
            searchRequest.setPageable(pageable);

            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // Первый вызов заполняет кэш
            rideService.searchRides(searchRequest);

            // Сбрасываем моки
            reset(rideRepository, rideMapper);

            // when
            Page<RideResponseDto> result = rideService.searchRides(searchRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(rideRepository, never()).searchRides(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should work correctly with empty cached data")
        void searchRides_WithEmptyCachedData_ShouldReturnEmptyPage() {
            // given
            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of()); // пустой результат

            Pageable pageable = PageRequest.of(0, 10);
            searchRequest.setPageable(pageable);

            // when
            Page<RideResponseDto> result = rideService.searchRides(searchRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }
}