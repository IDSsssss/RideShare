package com.example.rideshare.service.impl;

import com.example.rideshare.exception.BusinessException;
import com.example.rideshare.exception.ForbiddenException;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.RouteMapper;
import com.example.rideshare.model.dto.RouteRequestDto;
import com.example.rideshare.model.dto.RouteResponseDto;
import com.example.rideshare.model.entity.Route;
import com.example.rideshare.repository.RouteRepository;
import com.example.rideshare.security.CurrentUserAccessor;
import com.example.rideshare.service.RouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final RouteMapper routeMapper;
    private final CurrentUserAccessor currentUserAccessor;

    private static final String ROUTE_NOT_FOUND = "Route not found with id: ";
    private static final String ROUTE_ID_NULL = "Route ID cannot be null";

    @Override
    @Transactional(readOnly = true)
    public List<RouteResponseDto> getAllRoutes() {
        return routeMapper.toResponseDtoList(routeRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponseDto getRouteById(Long id) {
        if (id == null) {
            throw new BusinessException(ROUTE_ID_NULL);
        }

        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ROUTE_NOT_FOUND + id));

        return routeMapper.toResponseDto(route);
    }

    @Override
    @Transactional
    public RouteResponseDto createRoute(RouteRequestDto routeDto) {
        Route route = routeMapper.toEntity(routeDto);
        route.setCreatedByUserId(currentUserAccessor.currentUserIdOrNull());
        Route savedRoute = routeRepository.save(route);

        return routeMapper.toResponseDto(savedRoute);
    }

    @Override
    @Transactional
    public RouteResponseDto updateRoute(Long id, RouteRequestDto routeDto) {
        Route existingRoute = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ROUTE_NOT_FOUND + id));

        if (!currentUserAccessor.isAdmin()) {
            if (existingRoute.getCreatedByUserId() == null) {
                throw new ForbiddenException("Редактирование этого маршрута доступно только администратору.");
            }
            currentUserAccessor.requireAdminOrRouteCreator(existingRoute.getCreatedByUserId());
        }

        existingRoute.setStartPoint(routeDto.getStartPoint());
        existingRoute.setEndPoint(routeDto.getEndPoint());
        existingRoute.setDistanceKm(routeDto.getDistanceKm());
        existingRoute.setEstimatedDurationMinutes(routeDto.getEstimatedDurationMinutes());
        existingRoute.setWaypoints(routeDto.getWaypoints());

        Route updatedRoute = routeRepository.save(existingRoute);

        return routeMapper.toResponseDto(updatedRoute);
    }

    @Override
    @Transactional
    public void deleteRoute(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ROUTE_NOT_FOUND + id));

        if (!currentUserAccessor.isAdmin()) {
            if (route.getCreatedByUserId() == null) {
                throw new ForbiddenException("Удаление этого маршрута доступно только администратору.");
            }
            currentUserAccessor.requireAdminOrRouteCreator(route.getCreatedByUserId());
        }

        routeRepository.delete(route);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteResponseDto> findRoutesByStartAndEnd(String startPoint, String endPoint) {
        if (startPoint != null && endPoint != null) {
            return routeMapper.toResponseDtoList(routeRepository.findByStartPointAndEndPoint(startPoint, endPoint));
        } else if (startPoint != null) {
            return routeMapper.toResponseDtoList(routeRepository.findByStartPointContainingIgnoreCase(startPoint));
        } else if (endPoint != null) {
            return routeMapper.toResponseDtoList(routeRepository.findByEndPointContainingIgnoreCase(endPoint));
        }

        return List.of();
    }
}
