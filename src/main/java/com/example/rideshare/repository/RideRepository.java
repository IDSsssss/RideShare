package com.example.rideshare.repository;

import com.example.rideshare.model.Ride;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    // Исправленный метод с явным указанием типа
    @EntityGraph(attributePaths = {"driver", "route", "bookings"})
    @Query("SELECT r FROM Ride r WHERE r.departureTime > :currentTime")
    List<Ride> findUpcomingRidesWithDetails(@Param("currentTime") LocalDateTime currentTime);

    // Исправленный метод с JOIN FETCH
    @Query("SELECT DISTINCT r FROM Ride r "
            + "LEFT JOIN FETCH r.driver "
            + "LEFT JOIN FETCH r.route "
            + "LEFT JOIN FETCH r.bookings b "
            + "LEFT JOIN FETCH b.passenger "
            + "WHERE r.departureTime BETWEEN :start AND :end")
    List<Ride> findRidesInDateRangeWithAllDetails(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    // Добавляем метод с Optional для безопасной работы с null
    @Query("SELECT r FROM Ride r LEFT JOIN FETCH r.driver LEFT JOIN FETCH r.route WHERE r.id = :id")
    Optional<Ride> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT r FROM Ride r WHERE r.route.startPoint = :start AND r.route.endPoint = :end")
    List<Ride> findByRoute(@Param("start") String start, @Param("end") String end);

    List<Ride> findByDriverId(Long driverId);

    @Query("SELECT r FROM Ride r WHERE r.availableSeats >= :seats AND r.status = 'SCHEDULED'")
    List<Ride> findAvailableRides(@Param("seats") Integer seats);

    // Добавляем метод для поиска по статусу
    List<Ride> findByStatus(String status);
}