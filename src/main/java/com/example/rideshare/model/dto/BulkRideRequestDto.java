package com.example.rideshare.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record BulkRideRequestDto(

        @Schema(description = "Driver ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Driver ID is required")
        @Positive(message = "Driver ID must be positive")
        Long driverId,

        @NotEmpty(message = "Rides list must not be empty")
        @Valid
        List<RideRequestDto> rides
) {

}