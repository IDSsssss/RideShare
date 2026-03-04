package com.example.rideshare.service.impl;

import com.example.rideshare.model.dto.RouteRequestDto;
import com.example.rideshare.model.dto.RouteResponseDto;
import com.example.rideshare.model.entity.Route;
import com.example.rideshare.exception.BusinessException;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.RouteMapper;
import com.example.rideshare.repository.RouteRepository;
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

    @Override
    @Transactional(readOnly = true)
    public List<RouteResponseDto> getAllRoutes() {
        log.debug("Fetching all routes");
        return routeMapper.toResponseDtoList(routeRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponseDto getRouteById(Long id) {
        log.debug("Fetching route by id: {}", id);

        if (id == null) {
            throw new BusinessException("Route ID cannot be null");
        }

        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + id));

        return routeMapper.toResponseDto(route);
    }

    @Override
    @Transactional
    public RouteResponseDto createRoute(RouteRequestDto routeDto) {
        log.debug("Creating new route from {} to {}", routeDto.getStartPoint(), routeDto.getEndPoint());

        Route route = routeMapper.toEntity(routeDto);
        Route savedRoute = routeRepository.save(route);
        log.info("Route created successfully with id: {}", savedRoute.getId());

        return routeMapper.toResponseDto(savedRoute);
    }

    @Override
    @Transactional
    public RouteResponseDto updateRoute(Long id, RouteRequestDto routeDto) {
        log.debug("Updating route with id: {}", id);

        Route existingRoute = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + id));

        existingRoute.setStartPoint(routeDto.getStartPoint());
        existingRoute.setEndPoint(routeDto.getEndPoint());
        existingRoute.setDistanceKm(routeDto.getDistanceKm());
        existingRoute.setEstimatedDurationMinutes(routeDto.getEstimatedDurationMinutes());
        existingRoute.setWaypoints(routeDto.getWaypoints());

        Route updatedRoute = routeRepository.save(existingRoute);
        log.info("Route updated successfully with id: {}", updatedRoute.getId());

        return routeMapper.toResponseDto(updatedRoute);
    }

    @Override
    @Transactional
    public void deleteRoute(Long id) {
        log.debug("Deleting route with id: {}", id);

        if (!routeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Route not found with id: " + id);
        }

        routeRepository.deleteById(id);
        log.info("Route deleted successfully with id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteResponseDto> findRoutesByStartAndEnd(String startPoint, String endPoint) {
        log.debug("Finding routes from {} to {}", startPoint, endPoint);

        if (startPoint != null && endPoint != null) {
            return routeMapper.toResponseDtoList(
                    routeRepository.findByStartPointAndEndPoint(startPoint, endPoint));
        } else if (startPoint != null) {
            return routeMapper.toResponseDtoList(
                    routeRepository.findByStartPointContainingIgnoreCase(startPoint));
        } else if (endPoint != null) {
            return routeMapper.toResponseDtoList(
                    routeRepository.findByEndPointContainingIgnoreCase(endPoint));
        }

        return List.of();
    }
}