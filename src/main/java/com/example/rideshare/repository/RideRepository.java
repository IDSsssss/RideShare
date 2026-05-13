package com.example.rideshare.repository;

import com.example.rideshare.model.entity.Ride;
import com.example.rideshare.model.enums.RideStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    @EntityGraph(attributePaths = {"driver", "route", "bookings", "bookings.passenger"})
    @Query("SELECT r FROM Ride r")
    List<Ride> findAllWithDetailsViaEntityGraph();

    List<Ride> findByStatusNot(RideStatus status);

    @Query(value = "SELECT DISTINCT r.* FROM rides r "
            + "JOIN routes rt ON r.route_id = rt.id "
            + "WHERE (:startPattern IS NULL OR rt.start_point ILIKE CAST(:startPattern AS varchar)) "
            + "AND (:endPattern IS NULL OR rt.end_point ILIKE CAST(:endPattern AS varchar)) "
            + "AND (CAST(:fromDate AS TIMESTAMP) IS NULL OR r.departure_time >= CAST(:fromDate AS TIMESTAMP)) "
            + "AND (CAST(:toDate AS TIMESTAMP) IS NULL OR r.departure_time <= CAST(:toDate AS TIMESTAMP)) "
            + "AND (CAST(:minPrice AS NUMERIC) IS NULL OR r.price >= CAST(:minPrice AS NUMERIC)) "
            + "AND (CAST(:maxPrice AS NUMERIC) IS NULL OR r.price <= CAST(:maxPrice AS NUMERIC)) "
            + "AND (CAST(:minSeats AS INTEGER) IS NULL OR r.available_seats >= CAST(:minSeats AS INTEGER))",
            countQuery = "SELECT COUNT(DISTINCT r.id) FROM rides r "
                    + "JOIN routes rt ON r.route_id = rt.id "
                    + "WHERE (:startPattern IS NULL OR rt.start_point ILIKE CAST(:startPattern AS varchar)) "
                    + "AND (:endPattern IS NULL OR rt.end_point ILIKE CAST(:endPattern AS varchar)) "
                    + "AND (CAST(:fromDate AS TIMESTAMP) IS NULL OR r.departure_time >= CAST(:fromDate AS TIMESTAMP)) "
                    + "AND (CAST(:toDate AS TIMESTAMP) IS NULL OR r.departure_time <= CAST(:toDate AS TIMESTAMP)) "
                    + "AND (CAST(:minPrice AS NUMERIC) IS NULL OR r.price >= CAST(:minPrice AS NUMERIC)) "
                    + "AND (CAST(:maxPrice AS NUMERIC) IS NULL OR r.price <= CAST(:maxPrice AS NUMERIC)) "
                    + "AND (CAST(:minSeats AS INTEGER) IS NULL OR r.available_seats >= CAST(:minSeats AS INTEGER))",
            nativeQuery = true)
    List<Ride> searchRidesNative(
            @Param("startPattern") String startPattern,
            @Param("endPattern") String endPattern,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("minSeats") Integer minSeats);
}