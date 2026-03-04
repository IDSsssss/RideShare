package com.example.rideshare.mapper;

import com.example.rideshare.dto.RouteDto;
import com.example.rideshare.model.Route;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RouteMapper {

    public RouteDto toDto(Route route) {
        if (route == null) {
            return null;
        }

        RouteDto dto = new RouteDto();
        dto.setId(route.getId());
        dto.setStartPoint(route.getStartPoint());
        dto.setEndPoint(route.getEndPoint());
        dto.setDistanceKm(route.getDistanceKm());
        dto.setEstimatedDurationMinutes(route.getEstimatedDurationMinutes());
        dto.setWaypoints(route.getWaypoints());
        return dto;
    }

    public Route toEntity(RouteDto dto) {
        if (dto == null) {
            return null;
        }

        Route route = new Route();
        route.setId(dto.getId());
        route.setStartPoint(dto.getStartPoint());
        route.setEndPoint(dto.getEndPoint());
        route.setDistanceKm(dto.getDistanceKm());
        route.setEstimatedDurationMinutes(dto.getEstimatedDurationMinutes());
        route.setWaypoints(dto.getWaypoints());
        return route;
    }

    public List<RouteDto> toDtoList(List<Route> routes) {
        if (routes == null) {
            return List.of();
        }
        return routes.stream().map(this::toDto).collect(Collectors.toList());
    }
}