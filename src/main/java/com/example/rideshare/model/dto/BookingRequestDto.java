package com.example.rideshare.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

@Data
@Schema(description = "Booking creation request")
public class BookingRequestDto {

    @Schema(description = "Ride ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Ride ID is required")
    @Positive(message = "Ride ID must be positive")
    private Long rideId;

    @Schema(description = "Passenger ID", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Passenger ID is required")
    @Positive(message = "Passenger ID must be positive")
    private Long passengerId;

    @Schema(description = "Number of seats to book", example = "2", minimum = "1", maximum = "8",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Number of seats is required")
    @Min(value = 1, message = "At least 1 seat must be booked")
    @Max(value = 8, message = "Maximum 8 seats can be booked")
    private Integer seats;
}