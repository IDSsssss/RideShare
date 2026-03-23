package com.example.rideshare.repository;

import com.example.rideshare.model.entity.Ride;
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

    @EntityGraph(attributePaths = {"driver", "route", "bookings", "bookings.passenger"})
    @Query("SELECT DISTINCT r FROM Ride r "
            + "JOIN r.route rt "
            + "WHERE (cast(:startPoint as string) IS NULL OR rt.startPoint LIKE %:startPoint%) "
            + "AND (cast(:endPoint as string) IS NULL OR rt.endPoint LIKE %:endPoint%) "
            + "AND (cast(:fromDate as date) IS NULL OR r.departureTime >= :fromDate) "
            + "AND (cast(:toDate as date) IS NULL OR r.departureTime <= :toDate) "
            + "AND (cast(:minPrice as double) IS NULL OR r.price >= :minPrice) "
            + "AND (cast(:maxPrice as double) IS NULL OR r.price <= :maxPrice) "
            + "AND (cast(:minSeats as integer) IS NULL OR r.availableSeats >= :minSeats)")
    List<Ride> searchRides(
            @Param("startPoint") String startPoint,
            @Param("endPoint") String endPoint,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("minSeats") Integer minSeats);

    @Query(value = "SELECT DISTINCT r.* FROM rides r "
            + "JOIN routes rt ON r.route_id = rt.id "
            + "WHERE (CAST(:startPoint AS VARCHAR) IS NULL OR rt.start_point ILIKE CONCAT('%', "
            + "CAST(:startPoint AS VARCHAR), '%')) "
            + "AND (CAST(:endPoint AS VARCHAR) IS "
            + "NULL OR rt.end_point ILIKE CONCAT('%', CAST(:endPoint AS VARCHAR), '%')) "
            + "AND (CAST(:fromDate AS TIMESTAMP) IS NULL OR r.departure_time >= CAST(:fromDate AS TIMESTAMP)) "
            + "AND (CAST(:toDate AS TIMESTAMP) IS NULL OR r.departure_time <= CAST(:toDate AS TIMESTAMP)) "
            + "AND (CAST(:minPrice AS NUMERIC) IS NULL OR r.price >= CAST(:minPrice AS NUMERIC)) "
            + "AND (CAST(:maxPrice AS NUMERIC) IS NULL OR r.price <= CAST(:maxPrice AS NUMERIC)) "
            + "AND (CAST(:minSeats AS INTEGER) IS NULL OR r.available_seats >= CAST(:minSeats AS INTEGER))",
            countQuery = "SELECT COUNT(DISTINCT r.id) FROM rides r "
                    + "JOIN routes rt ON r.route_id = rt.id "
                    + "WHERE (CAST(:startPoint AS VARCHAR) IS NULL "
                    + "OR rt.start_point ILIKE CONCAT('%', CAST(:startPoint AS VARCHAR), '%')) "
                    + "AND (CAST(:endPoint AS VARCHAR) IS NULL "
                    + "OR rt.end_point ILIKE CONCAT('%', CAST(:endPoint AS VARCHAR), '%')) "
                    + "AND (CAST(:fromDate AS TIMESTAMP) IS NULL OR r.departure_time >= CAST(:fromDate AS TIMESTAMP)) "
                    + "AND (CAST(:toDate AS TIMESTAMP) IS NULL OR r.departure_time <= CAST(:toDate AS TIMESTAMP)) "
                    + "AND (CAST(:minPrice AS NUMERIC) IS NULL OR r.price >= CAST(:minPrice AS NUMERIC)) "
                    + "AND (CAST(:maxPrice AS NUMERIC) IS NULL OR r.price <= CAST(:maxPrice AS NUMERIC)) "
                    + "AND (CAST(:minSeats AS INTEGER) IS NULL OR r.available_seats >= CAST(:minSeats AS INTEGER))",
            nativeQuery = true)
    List<Ride> searchRidesNative(
            @Param("startPoint") String startPoint,
            @Param("endPoint") String endPoint,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("minSeats") Integer minSeats);
}