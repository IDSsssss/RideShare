package com.example.rideshare.repository;

import com.example.rideshare.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByPassengerId(Long passengerId);

    List<Booking> findByRideId(Long rideId);

    @Query("SELECT b FROM Booking b WHERE b.passenger.id = :userId AND b.status = 'CONFIRMED'")
    List<Booking> findConfirmedBookingsByUser(@Param("userId") Long userId);

    @Query("SELECT SUM(b.seats) FROM Booking b WHERE b.ride.id = :rideId AND b.status IN ('CONFIRMED', 'PENDING')")
    Integer getTotalBookedSeatsForRide(@Param("rideId") Long rideId);
}