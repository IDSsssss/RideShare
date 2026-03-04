package com.example.rideshare.service;

import com.example.rideshare.model.dto.RouteRequestDto;
import com.example.rideshare.model.dto.RouteResponseDto;
import java.util.List;

public interface RouteService {
    List<RouteResponseDto> getAllRoutes();

    RouteResponseDto getRouteById(Long id);

    RouteResponseDto createRoute(RouteRequestDto routeDto);

    RouteResponseDto updateRoute(Long id, RouteRequestDto routeDto);

    void deleteRoute(Long id);

    List<RouteResponseDto> findRoutesByStartAndEnd(String startPoint, String endPoint);
}