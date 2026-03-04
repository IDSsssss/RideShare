package com.example.rideshare.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewResponseDto {
    private Long id;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private UserResponseDto reviewer;
    private RideResponseDto ride;
}