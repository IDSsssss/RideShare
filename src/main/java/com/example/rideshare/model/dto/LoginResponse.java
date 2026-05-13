package com.example.rideshare.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String role;
    private String displayName;
    private Long userId;
}
