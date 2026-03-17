package com.example.rideshare.model.enums;

import lombok.Getter;

@Getter
public enum RideStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
