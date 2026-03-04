package com.example.rideshare.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Entity
@Table(name = "rides")
@ToString(exclude = {"driver", "bookings", "route", "passengers"})
public class Ride {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private String status; // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED

    // ManyToOne: Много поездок может быть у одного водителя
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    // OneToMany: Одна поездка может иметь много бронирований
    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();

    // OneToOne: У поездки один маршрут
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", unique = true)
    private Route route;

    // ManyToMany: Пассажиры через бронирования
    @ManyToMany
    @JoinTable(
            name = "ride_passengers",
            joinColumns = @JoinColumn(name = "ride_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> passengers = new HashSet<>();
}