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
    private RideSearchRequest searchRequest;
    private List<Ride> mockRides;

    @BeforeEach
    void setUp() {
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

        testBulkRequest = new BulkRideRequestDto(1L, List.of(testRideDto, testRideDto));

        mockRides = Arrays.asList(testRide, testRide);
        searchRequest = new RideSearchRequest();
        searchRequest.setStartPoint("Москва");
        searchRequest.setEndPoint("СПб");
        searchRequest.setPageable(PageRequest.of(0, 10));
        searchRequest.setUseNative(false);
    }

    // ==================== GET ALL RIDES TESTS ====================

    @Nested
    @DisplayName("getAllRides() tests")
    class GetAllRidesTests {
        @Test
        @DisplayName("Should return list of all rides")
        void getAllRides_Success_ShouldReturnRidesList() {
            List<Ride> rides = Arrays.asList(testRide, testRide);
            when(rideRepository.findAllWithDetailsViaEntityGraph()).thenReturn(rides);
            when(rideMapper.toResponseDtoList(rides)).thenReturn(Arrays.asList(testResponseDto, testResponseDto));

            List<RideResponseDto> result = rideService.getAllRides();

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
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(rideMapper.toResponseDto(testRide)).thenReturn(testResponseDto);

            RideResponseDto result = rideService.getRideById(100L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Should throw exception when id is null")
        void getRideById_NullId_ShouldThrowException() {
            assertThatThrownBy(() -> rideService.getRideById(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Ride ID cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when ride not found")
        void getRideById_NotFound_ShouldThrowException() {
            when(rideRepository.findById(999L)).thenReturn(Optional.empty());

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
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(routeMapper.toEntity(any(RouteRequestDto.class))).thenReturn(testRoute);
            when(rideRepository.save(any(Ride.class))).thenReturn(testRide);
            when(rideMapper.toResponseDto(testRide)).thenReturn(testResponseDto);

            RideResponseDto result = rideService.updateRide(100L, testRideDto);

            assertThat(result).isNotNull();
            verify(rideRepository, times(1)).save(testRide);
        }

        @Test
        @DisplayName("Should throw exception when id is null")
        void updateRide_NullId_ShouldThrowException() {
            assertThatThrownBy(() -> rideService.updateRide(null, testRideDto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Ride ID cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when ride not found")
        void updateRide_RideNotFound_ShouldThrowException() {
            when(rideRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> rideService.updateRide(999L, testRideDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Ride not found with id: 999");
        }

        @Test
        @DisplayName("Should throw exception when departure time is in past")
        void updateRide_DepartureTimeInPast_ShouldThrowException() {
            RideRequestDto pastTimeRequest = new RideRequestDto();
            pastTimeRequest.setDepartureTime(LocalDateTime.now().minusDays(1));
            pastTimeRequest.setAvailableSeats(4);
            pastTimeRequest.setPrice(1500.0);
            pastTimeRequest.setRoute(new RouteRequestDto());

            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));

            assertThatThrownBy(() -> rideService.updateRide(100L, pastTimeRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Departure time must be in the future");
        }

        @Test
        @DisplayName("Should update route when both routes exist")
        void updateRide_BothRoutesExist_ShouldUpdateRoute() {
            Ride existingRide = new Ride();
            existingRide.setId(100L);
            existingRide.setDepartureTime(LocalDateTime.now().plusDays(7));
            existingRide.setAvailableSeats(4);
            existingRide.setPrice(1500.0);

            Route existingRoute = new Route();
            existingRoute.setId(10L);
            existingRoute.setStartPoint("Москва");
            existingRoute.setEndPoint("СПб");
            existingRide.setRoute(existingRoute);

            RouteRequestDto routeDto = new RouteRequestDto();
            routeDto.setStartPoint("Москва");
            routeDto.setEndPoint("Казань");

            RideRequestDto updateRequest = new RideRequestDto();
            updateRequest.setRoute(routeDto);
            updateRequest.setDepartureTime(LocalDateTime.now().plusDays(14));
            updateRequest.setAvailableSeats(3);
            updateRequest.setPrice(2000.0);

            Route updatedRoute = new Route();
            updatedRoute.setId(10L);
            updatedRoute.setStartPoint("Москва");
            updatedRoute.setEndPoint("Казань");

            when(rideRepository.findById(100L)).thenReturn(Optional.of(existingRide));
            when(routeMapper.toEntity(any(RouteRequestDto.class))).thenReturn(updatedRoute);
            when(rideRepository.save(any(Ride.class))).thenReturn(existingRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            rideService.updateRide(100L, updateRequest);

            verify(routeMapper, times(1)).toEntity(any(RouteRequestDto.class));
        }

        @Test
        @DisplayName("Should NOT update route when existing route is null")
        void updateRide_ExistingRouteIsNull_ShouldNotUpdateRoute() {
            Ride existingRide = new Ride();
            existingRide.setId(100L);
            existingRide.setRoute(null);
            existingRide.setDepartureTime(LocalDateTime.now().plusDays(7));
            existingRide.setAvailableSeats(4);
            existingRide.setPrice(1500.0);

            RouteRequestDto routeDto = new RouteRequestDto();
            routeDto.setStartPoint("Москва");
            routeDto.setEndPoint("Казань");

            RideRequestDto updateRequest = new RideRequestDto();
            updateRequest.setRoute(routeDto);
            updateRequest.setDepartureTime(LocalDateTime.now().plusDays(14));
            updateRequest.setAvailableSeats(3);
            updateRequest.setPrice(2000.0);

            when(rideRepository.findById(100L)).thenReturn(Optional.of(existingRide));
            when(rideRepository.save(any(Ride.class))).thenReturn(existingRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            rideService.updateRide(100L, updateRequest);

            verify(routeMapper, never()).toEntity(any(RouteRequestDto.class));
        }

        @Test
        @DisplayName("Should NOT update route when request route is null")
        void updateRide_RequestRouteIsNull_ShouldNotUpdateRoute() {
            Ride existingRide = new Ride();
            existingRide.setId(100L);
            Route existingRoute = new Route();
            existingRoute.setId(10L);
            existingRoute.setStartPoint("Москва");
            existingRoute.setEndPoint("СПб");
            existingRide.setRoute(existingRoute);
            existingRide.setDepartureTime(LocalDateTime.now().plusDays(7));
            existingRide.setAvailableSeats(4);
            existingRide.setPrice(1500.0);

            RideRequestDto updateRequest = new RideRequestDto();
            updateRequest.setRoute(null);
            updateRequest.setDepartureTime(LocalDateTime.now().plusDays(14));
            updateRequest.setAvailableSeats(3);
            updateRequest.setPrice(2000.0);

            when(rideRepository.findById(100L)).thenReturn(Optional.of(existingRide));
            when(rideRepository.save(any(Ride.class))).thenReturn(existingRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            rideService.updateRide(100L, updateRequest);

            verify(routeMapper, never()).toEntity(any(RouteRequestDto.class));
        }

        @Test
        @DisplayName("Should NOT update route when both routes are null")
        void updateRide_BothRoutesNull_ShouldNotUpdateRoute() {
            Ride existingRide = new Ride();
            existingRide.setId(100L);
            existingRide.setRoute(null);
            existingRide.setDepartureTime(LocalDateTime.now().plusDays(7));
            existingRide.setAvailableSeats(4);
            existingRide.setPrice(1500.0);

            RideRequestDto updateRequest = new RideRequestDto();
            updateRequest.setRoute(null);
            updateRequest.setDepartureTime(LocalDateTime.now().plusDays(14));
            updateRequest.setAvailableSeats(3);
            updateRequest.setPrice(2000.0);

            when(rideRepository.findById(100L)).thenReturn(Optional.of(existingRide));
            when(rideRepository.save(any(Ride.class))).thenReturn(existingRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            rideService.updateRide(100L, updateRequest);

            verify(routeMapper, never()).toEntity(any(RouteRequestDto.class));
        }

        @Test
        @DisplayName("Should invalidate cache after update")
        void updateRide_ShouldInvalidateCache() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(routeMapper.toEntity(any(RouteRequestDto.class))).thenReturn(testRoute);
            when(rideRepository.save(any(Ride.class))).thenReturn(testRide);
            when(rideMapper.toResponseDto(testRide)).thenReturn(testResponseDto);

            RideServiceImpl spyService = spy(rideService);

            spyService.updateRide(100L, testRideDto);

            verify(spyService, times(1)).invalidateCache();
        }
    }

    // ==================== DELETE RIDE TESTS ====================

    @Nested
    @DisplayName("deleteRide() tests")
    class DeleteRideTests {

        @Test
        @DisplayName("Should throw exception when id is null")
        void deleteRide_NullId_ShouldThrowException() {
            assertThatThrownBy(() -> rideService.deleteRide(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Ride ID cannot be null");
            verify(rideRepository, never()).delete(any(Ride.class));
        }

        @Test
        @DisplayName("Should throw exception when ride not found")
        void deleteRide_RideNotFound_ShouldThrowException() {
            when(rideRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> rideService.deleteRide(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Ride not found with id: 999");
            verify(rideRepository, never()).delete(any(Ride.class));
        }

        @Test
        @DisplayName("Should throw exception when ride has existing bookings")
        void deleteRide_WithBookings_ShouldThrowException() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(2);

            assertThatThrownBy(() -> rideService.deleteRide(100L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cannot delete ride with existing bookings");
            verify(rideRepository, never()).delete(any(Ride.class));
        }

        @Test
        @DisplayName("Should delete ride successfully when no bookings")
        void deleteRide_Success_ShouldDeleteRide() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(0);
            doNothing().when(rideRepository).delete(testRide);

            rideService.deleteRide(100L);

            verify(rideRepository, times(1)).delete(testRide);
        }

        @Test
        @DisplayName("Should delete ride successfully when bookedSeats is null")
        void deleteRide_WhenBookedSeatsNull_ShouldDeleteSuccessfully() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(null);
            doNothing().when(rideRepository).delete(testRide);

            rideService.deleteRide(100L);

            verify(rideRepository, times(1)).delete(testRide);
        }

        @Test
        @DisplayName("Should invalidate cache after deletion")
        void deleteRide_ShouldInvalidateCache() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(0);
            doNothing().when(rideRepository).delete(testRide);

            RideServiceImpl spyService = spy(rideService);

            spyService.deleteRide(100L);

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
        void updateRideStatus_ToInProgress_ShouldSucceed() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(rideRepository.save(any(Ride.class))).thenReturn(testRide);
            when(rideMapper.toResponseDto(testRide)).thenReturn(testResponseDto);

            RideResponseDto result = rideService.updateRideStatus(100L, "IN_PROGRESS");

            assertThat(result).isNotNull();
            assertThat(testRide.getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
            verify(rideRepository, times(1)).save(testRide);
        }

        @Test
        @DisplayName("Should update status to COMPLETED successfully")
        void updateRideStatus_ToCompleted_ShouldSucceed() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(rideRepository.save(any(Ride.class))).thenReturn(testRide);
            when(rideMapper.toResponseDto(testRide)).thenReturn(testResponseDto);

            RideResponseDto result = rideService.updateRideStatus(100L, "COMPLETED");

            assertThat(result).isNotNull();
            assertThat(testRide.getStatus()).isEqualTo(RideStatus.COMPLETED);
            verify(rideRepository, times(1)).save(testRide);
        }

        @Test
        @DisplayName("Should update status to CANCELLED when no bookings")
        void updateRideStatus_ToCancelled_NoBookings_ShouldSucceed() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(0);
            when(rideRepository.save(any(Ride.class))).thenReturn(testRide);
            when(rideMapper.toResponseDto(testRide)).thenReturn(testResponseDto);

            RideResponseDto result = rideService.updateRideStatus(100L, "CANCELLED");

            assertThat(result).isNotNull();
            assertThat(testRide.getStatus()).isEqualTo(RideStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should throw exception when id is null")
        void updateRideStatus_NullId_ShouldThrowException() {
            assertThatThrownBy(() -> rideService.updateRideStatus(null, "IN_PROGRESS"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Ride ID cannot be null");
            verify(rideRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw exception when status is null")
        void updateRideStatus_NullStatus_ShouldThrowException() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));

            assertThatThrownBy(() -> rideService.updateRideStatus(100L, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Status cannot be null or empty");
            verify(rideRepository, never()).save(any(Ride.class));
        }

        @Test
        @DisplayName("Should throw exception when status is empty")
        void updateRideStatus_EmptyStatus_ShouldThrowException() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));

            assertThatThrownBy(() -> rideService.updateRideStatus(100L, ""))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Status cannot be null or empty");
            verify(rideRepository, never()).save(any(Ride.class));
        }

        @Test
        @DisplayName("Should throw exception when status is blank")
        void updateRideStatus_BlankStatus_ShouldThrowException() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));

            assertThatThrownBy(() -> rideService.updateRideStatus(100L, "   "))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Status cannot be null or empty");
            verify(rideRepository, never()).save(any(Ride.class));
        }

        @Test
        @DisplayName("Should throw exception when ride not found")
        void updateRideStatus_RideNotFound_ShouldThrowException() {
            when(rideRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> rideService.updateRideStatus(999L, "IN_PROGRESS"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Ride not found with id: 999");
            verify(rideRepository, never()).save(any(Ride.class));
        }

        @Test
        @DisplayName("Should throw exception when status is invalid")
        void updateRideStatus_InvalidStatus_ShouldThrowException() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));

            assertThatThrownBy(() -> rideService.updateRideStatus(100L, "INVALID_STATUS"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid status: INVALID_STATUS");
            verify(rideRepository, never()).save(any(Ride.class));
        }

        @Test
        @DisplayName("Should throw exception when cancelling ride with existing bookings")
        void updateRideStatus_CancelWithBookings_ShouldThrowException() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(2);

            assertThatThrownBy(() -> rideService.updateRideStatus(100L, "CANCELLED"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cannot cancel ride with existing bookings");
            verify(rideRepository, never()).save(any(Ride.class));
        }

        @Test
        @DisplayName("Should handle null bookedSeats when cancelling")
        void updateRideStatus_CancelWithNullBookedSeats_ShouldSucceed() {
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(bookingRepository.getTotalBookedSeatsForRide(100L)).thenReturn(null);
            when(rideRepository.save(any(Ride.class))).thenReturn(testRide);
            when(rideMapper.toResponseDto(testRide)).thenReturn(testResponseDto);

            RideResponseDto result = rideService.updateRideStatus(100L, "CANCELLED");

            assertThat(result).isNotNull();
            assertThat(testRide.getStatus()).isEqualTo(RideStatus.CANCELLED);
            verify(rideRepository, times(1)).save(testRide);
        }
    }
    // ==================== BULK CREATE RIDES TESTS ====================

    @Nested
    @DisplayName("createRidesBulk() tests")
    class CreateRidesBulkTests {
        @Test
        @DisplayName("Should create multiple rides successfully")
        void createRidesBulk_Success_ShouldReturnListOfRides() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testDriver));
            when(routeRepository.findAll()).thenReturn(List.of(testRoute));
            when(rideMapper.toEntity(any(RideRequestDto.class))).thenReturn(testRide);
            when(rideRepository.save(any(Ride.class))).thenReturn(testRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            List<RideResponseDto> result = rideService.createRidesBulk(testBulkRequest);

            assertThat(result).hasSize(2);
            verify(userRepository, times(1)).save(testDriver);
        }

        @Test
        @DisplayName("Should create new list when driver's ridesAsDriver is null")
        void createRidesBulk_WhenDriverRidesAsDriverIsNull_ShouldCreateNewList() {
            User driverWithNullList = new User();
            driverWithNullList.setId(1L);
            driverWithNullList.setRidesAsDriver(null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(driverWithNullList));
            when(routeRepository.findAll()).thenReturn(List.of(testRoute));
            when(rideMapper.toEntity(any(RideRequestDto.class))).thenReturn(testRide);
            when(rideRepository.save(any(Ride.class))).thenReturn(testRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            List<RideResponseDto> result = rideService.createRidesBulk(testBulkRequest);

            assertThat(result).hasSize(2);
            assertThat(driverWithNullList.getRidesAsDriver()).isNotNull();
            verify(userRepository, times(1)).save(driverWithNullList);
        }

        @Test
        @DisplayName("Should handle duplicate routes")
        void createRidesBulk_WhenDuplicateRoutesExist_ShouldKeepExistingRoute() {
            Route route1 = new Route();
            route1.setId(10L);
            route1.setStartPoint("Москва");
            route1.setEndPoint("СПб");

            Route route2 = new Route();
            route2.setId(20L);
            route2.setStartPoint("Москва");
            route2.setEndPoint("СПб");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testDriver));
            when(routeRepository.findAll()).thenReturn(Arrays.asList(route1, route2));
            when(rideMapper.toEntity(any(RideRequestDto.class))).thenReturn(testRide);
            when(rideRepository.save(any(Ride.class))).thenReturn(testRide);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            List<RideResponseDto> result = rideService.createRidesBulk(testBulkRequest);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should throw exception when driver not found")
        void createRidesBulk_DriverNotFound_ShouldThrowException() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> rideService.createRidesBulk(testBulkRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== SEARCH RIDES TESTS ====================

    @Nested
    @DisplayName("searchRides() tests")
    class SearchRidesTests {
        @Test
        @DisplayName("Should search with JPQL")
        void searchRides_JPQL_ShouldSearchFromDatabase() {
            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            Page<RideResponseDto> result = rideService.searchRides(searchRequest);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            verify(rideRepository, times(1)).searchRides(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should search with Native query")
        void searchRides_Native_ShouldSearchFromDatabase() {
            searchRequest.setUseNative(true);
            when(rideRepository.searchRidesNative(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            Page<RideResponseDto> result = rideService.searchRides(searchRequest);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            verify(rideRepository, times(1)).searchRidesNative(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should return cached data on second call")
        void searchRides_SecondCall_ShouldUseCache() {
            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // Первый вызов
            rideService.searchRides(searchRequest);
            verify(rideRepository, times(1)).searchRides(any(), any(), any(), any(), any(), any(), any());

            // Второй вызов - из кэша
            Page<RideResponseDto> secondResult = rideService.searchRides(searchRequest);

            assertThat(secondResult.getContent()).hasSize(2);
            verify(rideRepository, times(1)).searchRides(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should return empty page when no results")
        void searchRides_NoResults_ShouldReturnEmptyPage() {
            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of());

            Page<RideResponseDto> result = rideService.searchRides(searchRequest);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Should return paged content from cache")
        void searchRides_FromCache_ShouldReturnPagedContent() {
            Pageable pageable = PageRequest.of(0, 2);
            searchRequest.setPageable(pageable);

            List<Ride> manyRides = Arrays.asList(testRide, testRide, testRide, testRide);

            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(manyRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // Первый вызов - заполняем кэш
            rideService.searchRides(searchRequest);

            // Второй вызов - из кэша с другой страницей
            pageable = PageRequest.of(1, 2);
            searchRequest.setPageable(pageable);
            Page<RideResponseDto> result = rideService.searchRides(searchRequest);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            verify(rideRepository, times(1)).searchRides(any(), any(), any(), any(), any(), any(), any());
        }
    }
}