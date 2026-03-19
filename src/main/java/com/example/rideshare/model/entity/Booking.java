package com.example.rideshare.model.entity;

import com.example.rideshare.model.enums.BookingStatus;
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

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "booking_status")
    private BookingStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @PrePersist
    protected void onCreate() {
        bookingTime = LocalDateTime.now();
    }
}