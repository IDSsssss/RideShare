package com.example.rideshare.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "routes")
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_point", nullable = false)
    private String startPoint;

    @Column(name = "end_point", nullable = false)
    private String endPoint;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(length = 1000)
    private String waypoints;

    // OneToOne: Маршрут принадлежит одной поездке
    @OneToOne(mappedBy = "route")
    private Ride ride;
}