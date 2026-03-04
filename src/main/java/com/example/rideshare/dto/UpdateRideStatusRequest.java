package com.example.rideshare.dto;

import lombok.Data;

@Data
public class UpdateRideStatusRequest {
    private String status; // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
}