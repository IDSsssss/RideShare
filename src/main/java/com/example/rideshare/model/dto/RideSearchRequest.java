package com.example.rideshare.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

@Data
@Schema(description = "Ride search request with filters")
public class RideSearchRequest {

    @Schema(description = "Starting point filter", example = "Москва")
    @Size(max = 100, message = "Start point cannot exceed 100 characters")
    private String startPoint;

    @Schema(description = "End point filter", example = "Санкт-Петербург")
    @Size(max = 100, message = "End point cannot exceed 100 characters")
    private String endPoint;

    @Schema(description = "Filter rides from this date", example = "2026-04-01T00:00:00")
    private LocalDateTime fromDate;

    @Schema(description = "Filter rides to this date", example = "2026-04-30T23:59:59")
    private LocalDateTime toDate;

    @Schema(description = "Minimum price filter", example = "1000")
    @Positive(message = "Min price must be positive")
    private Double minPrice;

    @Schema(description = "Maximum price filter", example = "3000")
    @Positive(message = "Max price must be positive")
    private Double maxPrice;

    @Schema(description = "Minimum seats filter", example = "2")
    @Min(value = 1, message = "Min seats must be at least 1")
    @Max(value = 8, message = "Min seats cannot exceed 8")
    private Integer minSeats;

    private Pageable pageable;
    private boolean useNative;
}