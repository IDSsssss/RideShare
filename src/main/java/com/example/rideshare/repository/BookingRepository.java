package com.example.rideshare.repository;

import com.example.rideshare.model.entity.Booking;
import com.example.rideshare.model.entity.User;
import com.example.rideshare.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByPassengerId(Long passengerId);

    List<Booking> findByRideId(Long rideId);

    @Query("SELECT COALESCE(SUM(b.seats), 0) FROM Booking b WHERE b.ride.id = :rideId "
            + "AND b.status IN (com.example.rideshare.model.enums.BookingStatus.PENDING, "
            + "com.example.rideshare.model.enums.BookingStatus.CONFIRMED)")
    Integer getTotalBookedSeatsForRide(@Param("rideId") Long rideId);

    @Query("SELECT b.passenger FROM Booking b WHERE b.ride.id = :rideId")
    List<User> findPassengersByRideId(@Param("rideId") Long rideId);

    boolean existsByPassengerIdAndRideIdAndStatusIn(Long passengerId, Long rideId, List<BookingStatus> statuses);
}