package com.example.rideshare.repository;

import com.example.rideshare.model.dto.RideSearchRequest;
import com.example.rideshare.model.entity.Ride;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    @EntityGraph(attributePaths = {"driver", "route", "bookings", "bookings.passenger"})
    @Query("SELECT r FROM Ride r")
    List<Ride> findAllWithDetailsViaEntityGraph();

    @Query("SELECT DISTINCT r FROM Ride r "
            + "JOIN r.route rt "
            + "WHERE (cast(:startPoint as string) IS NULL OR rt.startPoint LIKE %:startPoint%) "
            + "AND (cast(:endPoint as string) IS NULL OR rt.endPoint LIKE %:endPoint%) "
            + "AND (cast(:fromDate as date) IS NULL OR r.departureTime >= :fromDate) "
            + "AND (cast(:toDate as date) IS NULL OR r.departureTime <= :toDate) "
            + "AND (cast(:minPrice as double) IS NULL OR r.price >= :minPrice) "
            + "AND (cast(:maxPrice as double) IS NULL OR r.price <= :maxPrice) "
            + "AND (cast(:minSeats as integer) IS NULL OR r.availableSeats >= :minSeats)")
    Page<Ride> searchRidesWithFilters(@Param("request") RideSearchRequest request, Pageable pageable);

    @Query(value = "SELECT DISTINCT r.* FROM rides r "
            + "JOIN routes rt ON r.route_id = rt.id "
            + "WHERE (:startPoint IS NULL OR rt.start_point ILIKE CONCAT('%', :startPoint, '%')) "
            + "AND (:endPoint IS NULL OR rt.end_point ILIKE CONCAT('%', :endPoint, '%')) "
            + "AND (cast(:fromDate as timestamp) IS NULL OR r.departure_time >= cast(:fromDate as timestamp)) "
            + "AND (cast(:toDate as timestamp) IS NULL OR r.departure_time <= cast(:toDate as timestamp)) "
            + "AND (cast(:minPrice as numeric) IS NULL OR r.price >= :minPrice) "
            + "AND (cast(:maxPrice as numeric) IS NULL OR r.price <= :maxPrice) "
            + "AND (cast(:minSeats as integer) IS NULL OR r.available_seats >= :minSeats)",
            countQuery = "SELECT COUNT(DISTINCT r.id) FROM rides r "
                    + "JOIN routes rt ON r.route_id = rt.id "
                    + "WHERE (:startPoint IS NULL OR rt.start_point ILIKE CONCAT('%', :startPoint, '%')) "
                    + "AND (:endPoint IS NULL OR rt.end_point ILIKE CONCAT('%', :endPoint, '%')) "
                    + "AND (cast(:fromDate as timestamp) IS NULL OR r.departure_time >= cast(:fromDate as timestamp)) "
                    + "AND (cast(:toDate as timestamp) IS NULL OR r.departure_time <= cast(:toDate as timestamp)) "
                    + "AND (cast(:minPrice as numeric) IS NULL OR r.price >= :minPrice) "
                    + "AND (cast(:maxPrice as numeric) IS NULL OR r.price <= :maxPrice) "
                    + "AND (cast(:minSeats as integer) IS NULL OR r.available_seats >= :minSeats)",
            nativeQuery = true)
    Page<Ride> searchRidesNative(@Param("request") RideSearchRequest request, Pageable pageable);
}