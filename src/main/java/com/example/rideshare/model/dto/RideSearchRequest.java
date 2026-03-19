package com.example.rideshare.model.dto;

import lombok.Data;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

@Data
public class RideSearchRequest {
    private String startPoint;
    private String endPoint;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private Double minPrice;
    private Double maxPrice;
    private Integer minSeats;
    private Pageable pageable;
    private boolean useNative;
}