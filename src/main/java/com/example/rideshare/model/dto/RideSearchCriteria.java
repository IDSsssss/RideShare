package com.example.rideshare.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Objects;

@Data
@AllArgsConstructor
public class RideSearchCriteria {
    private String startPoint;
    private String endPoint;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private Double minPrice;
    private Double maxPrice;
    private Integer minSeats;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RideSearchCriteria that = (RideSearchCriteria) o;
        return Objects.equals(startPoint, that.startPoint)
                && Objects.equals(endPoint, that.endPoint)
                && Objects.equals(fromDate, that.fromDate)
                && Objects.equals(toDate, that.toDate)
                && Objects.equals(minPrice, that.minPrice)
                && Objects.equals(maxPrice, that.maxPrice)
                && Objects.equals(minSeats, that.minSeats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startPoint, endPoint, fromDate, toDate, minPrice, maxPrice, minSeats);
    }
}