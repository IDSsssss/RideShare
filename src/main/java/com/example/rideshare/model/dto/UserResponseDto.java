package com.example.rideshare.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserResponseDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Double rating;
    private LocalDateTime createdAt;
}