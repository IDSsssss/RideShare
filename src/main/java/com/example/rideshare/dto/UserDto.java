package com.example.rideshare.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Double rating;
    private LocalDateTime createdAt;
}