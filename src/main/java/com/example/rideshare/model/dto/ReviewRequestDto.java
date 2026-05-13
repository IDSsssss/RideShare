package com.example.rideshare.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

@Data
@Schema(description = "Review creation request")
public class ReviewRequestDto {

    @Schema(description = "Rating (1-5)", example = "5", minimum = "1", maximum = "5",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Schema(description = "Review comment", example = "Great ride, driver was very polite!")
    @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
    private String comment;

    @Schema(description = "Автор отзыва (только для администратора; иначе берётся из сессии)",
            example = "3", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Positive(message = "Reviewer ID must be positive")
    private Long reviewerId;

    @Schema(description = "Ride ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Ride ID is required")
    @Positive(message = "Ride ID must be positive")
    private Long rideId;
}