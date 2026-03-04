package com.example.rideshare.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 1000)
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ManyToOne: Много отзывов у одного пользователя (который оставляет отзыв)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    // ManyToOne: Много отзывов у одной поездки
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}