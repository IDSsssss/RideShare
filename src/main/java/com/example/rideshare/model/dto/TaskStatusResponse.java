package com.example.rideshare.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Async task status response")
public record TaskStatusResponse(
        @Schema(description = "Task ID")
        String taskId,

        @Schema(description = "Current status (PENDING/PROCESSING/COMPLETED/FAILED)")
        String status,

        @Schema(description = "Number of processed bookings")
        Integer processedCount,

        @Schema(description = "Total number of bookings to process")
        Integer totalCount,

        @Schema(description = "Progress percentage")
        Double progressPercent,

        @Schema(description = "Task start time")
        LocalDateTime startTime,

        @Schema(description = "Task end time (if completed)")
        LocalDateTime endTime,

        @Schema(description = "Error message (if failed)")
        String errorMessage,

        @Schema(description = "List of individual booking errors")
        List<String> errors
) {}