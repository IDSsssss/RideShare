package com.example.rideshare.repository;

import com.example.rideshare.model.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    /**
     * Точное совпадение маршрута (водитель задаёт длину и время в пути).
     */
    Optional<Route> findFirstByStartPointAndEndPointAndDistanceKmAndEstimatedDurationMinutes(
            String startPoint,
            String endPoint,
            Double distanceKm,
            Integer estimatedDurationMinutes);

    List<Route> findByStartPointAndEndPoint(String startPoint, String endPoint);

    List<Route> findByStartPointContainingIgnoreCase(String startPoint);

    List<Route> findByEndPointContainingIgnoreCase(String endPoint);
}