package com.example.rideshare.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Route information")
public class RouteRequestDto {

    @Schema(description = "Starting point", example = "Москва", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Start point is required")
    @Size(min = 2, max = 100, message = "Start point must be between 2 and 100 characters")
    private String startPoint;

    @Schema(description = "End point", example = "Санкт-Петербург", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "End point is required")
    @Size(min = 2, max = 100, message = "End point must be between 2 and 100 characters")
    private String endPoint;

    @Schema(description = "Distance in kilometers", example = "700.5")
    @Positive(message = "Distance must be positive")
    private Double distanceKm;

    @Schema(description = "Estimated duration in minutes", example = "480")
    @Positive(message = "Duration must be positive")
    private Integer estimatedDurationMinutes;

    @Schema(description = "Waypoints (optional)", example = "Тверь, Валдай")
    @Size(max = 1000, message = "Waypoints cannot exceed 1000 characters")
    private String waypoints;
}