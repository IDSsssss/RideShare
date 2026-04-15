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
        @DisplayName("Should return cached data when cache hit")
        void searchRides_CacheHit_ShouldReturnCachedData() {
            // given
            List<RideResponseDto> cachedContent = mockResponses;

            // Сначала заполняем кэш
            when(rideRepository.searchRides(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(mockRides);
            when(rideMapper.toResponseDto(any(Ride.class))).thenReturn(testResponseDto);

            // Первый вызов — заполняет кэш
            rideService.searchRides(searchRequest);

            // Сбрасываем моки для второго вызова
            reset(rideRepository, rideMapper);

            // Второй вызов — должен взять из кэша (репозиторий не вызывается)
            Page<RideResponseDto> result = rideService.searchRides(searchRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            verify(rideRepository, never()).searchRides(any(), any(), any(), any(), any(), any(), any());
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