package com.example.rideshare.mapper;

import com.example.rideshare.model.dto.RouteRequestDto;
import com.example.rideshare.model.dto.RouteResponseDto;
import com.example.rideshare.model.entity.Route;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RouteMapper {

    public RouteResponseDto toResponseDto(Route route) {
        if (route == null) {
            return null;
        }

        RouteResponseDto dto = new RouteResponseDto();
        dto.setId(route.getId());
        dto.setStartPoint(route.getStartPoint());
        dto.setEndPoint(route.getEndPoint());
        dto.setDistanceKm(route.getDistanceKm());
        dto.setEstimatedDurationMinutes(route.getEstimatedDurationMinutes());
        dto.setWaypoints(route.getWaypoints());
        return dto;
    }

    public Route toEntity(RouteRequestDto dto) {
        if (dto == null) {
            return null;
        }

        Route route = new Route();
        route.setStartPoint(dto.getStartPoint());
        route.setEndPoint(dto.getEndPoint());
        route.setDistanceKm(dto.getDistanceKm());
        route.setEstimatedDurationMinutes(dto.getEstimatedDurationMinutes());
        route.setWaypoints(dto.getWaypoints());
        return route;
    }

    public List<RouteResponseDto> toResponseDtoList(List<Route> routes) {
        if (routes == null) {
            return List.of();
        }
        return routes.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }
}