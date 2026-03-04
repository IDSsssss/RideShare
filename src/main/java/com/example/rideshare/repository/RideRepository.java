package com.example.rideshare.repository;

import com.example.rideshare.model.entity.Ride;
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

    @EntityGraph(attributePaths = {"driver", "route"})
    List<Ride> findByDriverId(Long driverId);

    @EntityGraph(attributePaths = {"driver", "route", "bookings"})
    @Query("SELECT r FROM Ride r WHERE r.departureTime > :currentTime")
    List<Ride> findUpcomingRidesWithDetails(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT DISTINCT r FROM Ride r "
            + "LEFT JOIN FETCH r.driver "
            + "LEFT JOIN FETCH r.route "
            + "LEFT JOIN FETCH r.bookings b "
            + "LEFT JOIN FETCH b.passenger "
            + "WHERE r.departureTime BETWEEN :start AND :end")
    List<Ride> findRidesInDateRangeWithAllDetails(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    @Query("SELECT r FROM Ride r LEFT JOIN FETCH r.driver LEFT JOIN FETCH r.route WHERE r.id = :id")
    Optional<Ride> findByIdWithDetails(@Param("id") Long id);

    List<Ride> findByStatus(String status);
}