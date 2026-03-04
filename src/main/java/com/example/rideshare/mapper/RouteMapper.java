package com.example.rideshare.mapper;

import com.example.rideshare.dto.RouteDto;
import com.example.rideshare.model.Route;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper(componentModel = "spring")
public interface RouteMapper {
    RouteMapper INSTANCE = Mappers.getMapper(RouteMapper.class);

    RouteDto toDto(Route route);

    Route toEntity(RouteDto routeDTO);

    List<RouteDto> toDtoList(List<Route> routes);
}