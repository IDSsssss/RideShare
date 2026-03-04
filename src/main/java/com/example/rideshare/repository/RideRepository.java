package com.example.rideshare.repository;

import com.example.rideshare.model.Ride;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    // Решение проблемы N+1 через @EntityGraph
    @EntityGraph(attributePaths = {"driver", "route", "bookings"})
    @Query("SELECT r FROM Ride r WHERE r.departureTime > :currentTime")
    List<Ride> findUpcomingRidesWithDetails(@Param("currentTime") LocalDateTime currentTime);

    // Решение проблемы N+1 через JOIN FETCH
    @Query("SELECT DISTINCT r FROM Ride r " + "LEFT JOIN FETCH r.driver " + "LEFT JOIN FETCH r.route "
            + "LEFT JOIN FETCH r.bookings b " + "LEFT JOIN FETCH b.passenger "
            + "WHERE r.departureTime BETWEEN :start AND :end")
    List<Ride> findRidesInDateRangeWithAllDetails(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    @Query("SELECT r FROM Ride r WHERE r.route.startPoint = :start AND r.route.endPoint = :end")
    List<Ride> findByRoute(@Param("start") String start, @Param("end") String end);

    List<Ride> findByDriverId(Long driverId);

    @Query("SELECT r FROM Ride r WHERE r.availableSeats >= :seats AND r.status = 'SCHEDULED'")
    List<Ride> findAvailableRides(@Param("seats") Integer seats);
}