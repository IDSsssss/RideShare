package com.example.rideshare.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "Ride creation/update request")
public class RideRequestDto {

    @Schema(description = "Driver ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Driver ID is required")
    @Positive(message = "Driver ID must be positive")
    private Long driverId;

    @Schema(description = "Departure date and time", example = "2026-04-10 09:00:00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Departure time is required")
    @Future(message = "Departure time must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime departureTime;

    @Schema(description = "Number of available seats", example = "4", minimum = "1", maximum = "8",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Available seats is required")
    @Min(value = 1, message = "At least 1 seat must be available")
    @Max(value = 8, message = "Maximum 8 seats allowed")
    private Integer availableSeats;

    @Schema(description = "Price per seat", example = "1500.0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    private Double price;

    @Valid
    @NotNull(message = "Route information is required")
    private RouteRequestDto route;
}