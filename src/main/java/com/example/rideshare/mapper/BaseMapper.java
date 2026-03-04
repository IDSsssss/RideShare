package com.example.rideshare.mapper;

import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseMapper<E, D> {

    public abstract D toDto(E entity);

    public abstract E toEntity(D dto);

    public List<D> toDtoList(List<E> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<E> toEntityList(List<D> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}