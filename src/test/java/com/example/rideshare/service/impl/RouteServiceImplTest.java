package com.example.rideshare.service.impl;

import com.example.rideshare.exception.BusinessException;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.RouteMapper;
import com.example.rideshare.model.dto.RouteRequestDto;
import com.example.rideshare.model.dto.RouteResponseDto;
import com.example.rideshare.model.entity.Route;
import com.example.rideshare.repository.RouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
@DisplayName("RouteService Unit Tests")
class RouteServiceImplTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private RouteMapper routeMapper;

    @InjectMocks
    private RouteServiceImpl routeService;

    private Route testRoute;
    private RouteRequestDto testRouteDto;
    private RouteResponseDto testResponseDto;

    @BeforeEach
    void setUp() {
        // Инициализация тестовых данных
        testRoute = new Route();
        testRoute.setId(10L);
        testRoute.setStartPoint("Москва");
        testRoute.setEndPoint("Санкт-Петербург");
        testRoute.setDistanceKm(700.5);
        testRoute.setEstimatedDurationMinutes(480);
        testRoute.setWaypoints("Тверь, Валдай");

        testRouteDto = new RouteRequestDto();
        testRouteDto.setStartPoint("Москва");
        testRouteDto.setEndPoint("Санкт-Петербург");
        testRouteDto.setDistanceKm(700.5);
        testRouteDto.setEstimatedDurationMinutes(480);
        testRouteDto.setWaypoints("Тверь, Валдай");

        testResponseDto = new RouteResponseDto();
        testResponseDto.setId(10L);
        testResponseDto.setStartPoint("Москва");
        testResponseDto.setEndPoint("Санкт-Петербург");
        testResponseDto.setDistanceKm(700.5);
        testResponseDto.setEstimatedDurationMinutes(480);
        testResponseDto.setWaypoints("Тверь, Валдай");
    }

    // ==================== GET ALL ROUTES TESTS ====================

    @Nested
    @DisplayName("getAllRoutes() tests")
    class GetAllRoutesTests {

        @Test
        @DisplayName("Should return list of all routes")
        void getAllRoutes_Success_ShouldReturnRoutesList() {
            // given
            List<Route> routes = Arrays.asList(testRoute, testRoute);
            List<RouteResponseDto> expectedResponse = Arrays.asList(testResponseDto, testResponseDto);
            when(routeRepository.findAll()).thenReturn(routes);
            when(routeMapper.toResponseDtoList(routes)).thenReturn(expectedResponse);

            // when
            List<RouteResponseDto> result = routeService.getAllRoutes();

            // then
            assertThat(result).hasSize(2);
            verify(routeRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no routes exist")
        void getAllRoutes_EmptyList_ShouldReturnEmptyList() {
            // given
            when(routeRepository.findAll()).thenReturn(List.of());
            when(routeMapper.toResponseDtoList(List.of())).thenReturn(List.of());

            // when
            List<RouteResponseDto> result = routeService.getAllRoutes();

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== GET ROUTE BY ID TESTS ====================

    @Nested
    @DisplayName("getRouteById() tests")
    class GetRouteByIdTests {

        @Test
        @DisplayName("Should return route when id exists")
        void getRouteById_Success_ShouldReturnRoute() {
            // given
            when(routeRepository.findById(10L)).thenReturn(Optional.of(testRoute));
            when(routeMapper.toResponseDto(testRoute)).thenReturn(testResponseDto);

            // when
            RouteResponseDto result = routeService.getRouteById(10L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getStartPoint()).isEqualTo("Москва");
            assertThat(result.getEndPoint()).isEqualTo("Санкт-Петербург");
        }

        @Test
        @DisplayName("Should throw exception when id is null")
        void getRouteById_NullId_ShouldThrowException() {
            // when & then
            assertThatThrownBy(() -> routeService.getRouteById(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Route ID cannot be null");
            verify(routeRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw exception when route not found")
        void getRouteById_NotFound_ShouldThrowException() {
            // given
            when(routeRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> routeService.getRouteById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Route not found with id: 999");
        }
    }

    // ==================== CREATE ROUTE TESTS ====================

    @Nested
    @DisplayName("createRoute() tests")
    class CreateRouteTests {

        @Test
        @DisplayName("Should create route successfully")
        void createRoute_Success_ShouldReturnCreatedRoute() {
            // given
            when(routeMapper.toEntity(testRouteDto)).thenReturn(testRoute);
            when(routeRepository.save(testRoute)).thenReturn(testRoute);
            when(routeMapper.toResponseDto(testRoute)).thenReturn(testResponseDto);

            // when
            RouteResponseDto result = routeService.createRoute(testRouteDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            verify(routeRepository, times(1)).save(testRoute);
        }

        @Test
        @DisplayName("Should create route with null waypoints")
        void createRoute_NullWaypoints_ShouldCreateSuccessfully() {
            // given
            testRouteDto.setWaypoints(null);
            testRoute.setWaypoints(null);
            testResponseDto.setWaypoints(null);

            when(routeMapper.toEntity(testRouteDto)).thenReturn(testRoute);
            when(routeRepository.save(testRoute)).thenReturn(testRoute);
            when(routeMapper.toResponseDto(testRoute)).thenReturn(testResponseDto);

            // when
            RouteResponseDto result = routeService.createRoute(testRouteDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getWaypoints()).isNull();
            verify(routeRepository, times(1)).save(testRoute);
        }
    }

    // ==================== UPDATE ROUTE TESTS ====================

    @Nested
    @DisplayName("updateRoute() tests")
    class UpdateRouteTests {

        @Test
        @DisplayName("Should update route successfully")
        void updateRoute_Success_ShouldReturnUpdatedRoute() {
            // given
            RouteRequestDto updateDto = new RouteRequestDto();
            updateDto.setStartPoint("Москва");
            updateDto.setEndPoint("Казань");
            updateDto.setDistanceKm(820.0);
            updateDto.setEstimatedDurationMinutes(540);
            updateDto.setWaypoints("Владимир, Нижний Новгород");

            Route updatedRoute = new Route();
            updatedRoute.setId(10L);
            updatedRoute.setStartPoint("Москва");
            updatedRoute.setEndPoint("Казань");
            updatedRoute.setDistanceKm(820.0);
            updatedRoute.setEstimatedDurationMinutes(540);
            updatedRoute.setWaypoints("Владимир, Нижний Новгород");

            RouteResponseDto updatedResponse = new RouteResponseDto();
            updatedResponse.setId(10L);
            updatedResponse.setStartPoint("Москва");
            updatedResponse.setEndPoint("Казань");
            updatedResponse.setDistanceKm(820.0);
            updatedResponse.setEstimatedDurationMinutes(540);
            updatedResponse.setWaypoints("Владимир, Нижний Новгород");

            when(routeRepository.findById(10L)).thenReturn(Optional.of(testRoute));
            when(routeRepository.save(any(Route.class))).thenReturn(updatedRoute);
            when(routeMapper.toResponseDto(any(Route.class))).thenReturn(updatedResponse);

            // when
            RouteResponseDto result = routeService.updateRoute(10L, updateDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEndPoint()).isEqualTo("Казань");
            assertThat(result.getDistanceKm()).isEqualTo(820.0);
            verify(routeRepository, times(1)).save(any(Route.class));
        }

        @Test
        @DisplayName("Should throw exception when route not found")
        void updateRoute_NotFound_ShouldThrowException() {
            // given
            when(routeRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> routeService.updateRoute(999L, testRouteDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Route not found with id: 999");
            verify(routeRepository, never()).save(any(Route.class));
        }
    }

    // ==================== DELETE ROUTE TESTS ====================

    @Nested
    @DisplayName("deleteRoute() tests")
    class DeleteRouteTests {

        @Test
        @DisplayName("Should delete route successfully")
        void deleteRoute_Success_ShouldDeleteRoute() {
            // given
            when(routeRepository.existsById(10L)).thenReturn(true);
            doNothing().when(routeRepository).deleteById(10L);

            // when
            routeService.deleteRoute(10L);

            // then
            verify(routeRepository, times(1)).deleteById(10L);
        }

        @Test
        @DisplayName("Should throw exception when route not found")
        void deleteRoute_NotFound_ShouldThrowException() {
            // given
            when(routeRepository.existsById(999L)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> routeService.deleteRoute(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Route not found with id: 999");
            verify(routeRepository, never()).deleteById(anyLong());
        }
    }

    // ==================== FIND ROUTES BY START AND END TESTS ====================

    @Nested
    @DisplayName("findRoutesByStartAndEnd() tests")
    class FindRoutesByStartAndEndTests {

        @Test
        @DisplayName("Should find routes by both start and end points")
        void findRoutesByStartAndEnd_WithBothParams_ShouldReturnFilteredRoutes() {
            // given
            List<Route> routes = Arrays.asList(testRoute);
            List<RouteResponseDto> expectedResponse = Arrays.asList(testResponseDto);
            when(routeRepository.findByStartPointAndEndPoint("Москва", "Санкт-Петербург"))
                    .thenReturn(routes);
            when(routeMapper.toResponseDtoList(routes)).thenReturn(expectedResponse);

            // when
            List<RouteResponseDto> result = routeService.findRoutesByStartAndEnd("Москва", "Санкт-Петербург");

            // then
            assertThat(result).hasSize(1);
            verify(routeRepository, times(1)).findByStartPointAndEndPoint("Москва", "Санкт-Петербург");
            verify(routeRepository, never()).findByStartPointContainingIgnoreCase(anyString());
            verify(routeRepository, never()).findByEndPointContainingIgnoreCase(anyString());
        }

        @Test
        @DisplayName("Should find routes by start point only")
        void findRoutesByStartAndEnd_WithStartOnly_ShouldReturnRoutesByStart() {
            // given
            List<Route> routes = Arrays.asList(testRoute);
            List<RouteResponseDto> expectedResponse = Arrays.asList(testResponseDto);
            when(routeRepository.findByStartPointContainingIgnoreCase("Москва"))
                    .thenReturn(routes);
            when(routeMapper.toResponseDtoList(routes)).thenReturn(expectedResponse);

            // when
            List<RouteResponseDto> result = routeService.findRoutesByStartAndEnd("Москва", null);

            // then
            assertThat(result).hasSize(1);
            verify(routeRepository, times(1)).findByStartPointContainingIgnoreCase("Москва");
        }

        @Test
        @DisplayName("Should find routes by end point only")
        void findRoutesByStartAndEnd_WithEndOnly_ShouldReturnRoutesByEnd() {
            // given
            List<Route> routes = Arrays.asList(testRoute);
            List<RouteResponseDto> expectedResponse = Arrays.asList(testResponseDto);
            when(routeRepository.findByEndPointContainingIgnoreCase("Санкт-Петербург"))
                    .thenReturn(routes);
            when(routeMapper.toResponseDtoList(routes)).thenReturn(expectedResponse);

            // when
            List<RouteResponseDto> result = routeService.findRoutesByStartAndEnd(null, "Санкт-Петербург");

            // then
            assertThat(result).hasSize(1);
            verify(routeRepository, times(1)).findByEndPointContainingIgnoreCase("Санкт-Петербург");
        }

        @Test
        @DisplayName("Should return empty list when no parameters provided")
        void findRoutesByStartAndEnd_NoParams_ShouldReturnEmptyList() {
            // when
            List<RouteResponseDto> result = routeService.findRoutesByStartAndEnd(null, null);

            // then
            assertThat(result).isEmpty();
            verify(routeRepository, never()).findByStartPointAndEndPoint(anyString(), anyString());
            verify(routeRepository, never()).findByStartPointContainingIgnoreCase(anyString());
            verify(routeRepository, never()).findByEndPointContainingIgnoreCase(anyString());
        }

        @Test
        @DisplayName("Should return empty list when no routes match")
        void findRoutesByStartAndEnd_NoMatches_ShouldReturnEmptyList() {
            // given
            when(routeRepository.findByStartPointAndEndPoint("Москва", "Сочи"))
                    .thenReturn(List.of());
            when(routeMapper.toResponseDtoList(List.of())).thenReturn(List.of());

            // when
            List<RouteResponseDto> result = routeService.findRoutesByStartAndEnd("Москва", "Сочи");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should be case insensitive when searching by start point")
        void findRoutesByStartAndEnd_CaseInsensitiveStart_ShouldReturnRoutes() {
            // given
            List<Route> routes = Arrays.asList(testRoute);
            List<RouteResponseDto> expectedResponse = Arrays.asList(testResponseDto);
            when(routeRepository.findByStartPointContainingIgnoreCase("москва"))
                    .thenReturn(routes);
            when(routeMapper.toResponseDtoList(routes)).thenReturn(expectedResponse);

            // when
            List<RouteResponseDto> result = routeService.findRoutesByStartAndEnd("москва", null);

            // then
            assertThat(result).hasSize(1);
            verify(routeRepository, times(1)).findByStartPointContainingIgnoreCase("москва");
        }
    }
}