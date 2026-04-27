package com.example.rideshare.model.dto;

public record AsyncTaskResponse(
        String taskId,
        String status,
        String message
) {
}