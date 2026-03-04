package com.example.rideshare.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_time", nullable = false)
    private LocalDateTime bookingTime;

    @Column(nullable = false)
    private Integer seats;

    @Column(nullable = false)
    private String status; // PENDING, CONFIRMED, CANCELLED, COMPLETED

    private Double totalPrice;

    // ManyToOne: Много бронирований у одного пассажира
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    // ManyToOne: Много бронирований у одной поездки
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @PrePersist
    protected void onCreate() {
        bookingTime = LocalDateTime.now();
    }
}