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
@Table(name = "users")
@ToString(exclude = {"ridesAsDriver", "bookings", "reviews"})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    private Double rating = 0.0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // OneToMany: Водитель может создавать много поездок
    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Ride> ridesAsDriver = new ArrayList<>();

    // OneToMany: Пользователь может делать много бронирований
    @OneToMany(mappedBy = "passenger", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();

    // OneToMany: Пользователь может оставлять много отзывов
    @OneToMany(mappedBy = "reviewer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}