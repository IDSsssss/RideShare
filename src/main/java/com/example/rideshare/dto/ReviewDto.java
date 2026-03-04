package com.example.rideshare.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewDto {
    private Long id;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private UserDto reviewer;
    private RideDto ride;
}