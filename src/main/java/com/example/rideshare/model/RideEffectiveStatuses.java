package com.example.rideshare.model;

import com.example.rideshare.model.entity.Ride;
import com.example.rideshare.model.entity.Route;
import com.example.rideshare.model.enums.RideStatus;

import java.time.LocalDateTime;

public final class RideEffectiveStatuses {

    private static final int DEFAULT_DURATION_MINUTES = 120;

    private RideEffectiveStatuses() {
    }

    /**
     * Вычисляет статус по текущему времени. Отменённые поездки не меняются.
     */
    public static RideStatus calculate(Ride ride, LocalDateTime now) {
        if (ride.getStatus() == RideStatus.CANCELLED) {
            return RideStatus.CANCELLED;
        }
        if (ride.getDepartureTime() == null) {
            return ride.getStatus();
        }
        LocalDateTime start = ride.getDepartureTime();
        int minutes = resolveDurationMinutes(ride.getRoute());
        LocalDateTime end = start.plusMinutes(minutes);
        if (!now.isBefore(end)) {
            return RideStatus.COMPLETED;
        }
        if (!now.isBefore(start)) {
            return RideStatus.IN_PROGRESS;
        }
        return RideStatus.SCHEDULED;
    }

    private static int resolveDurationMinutes(Route route) {
        if (route == null || route.getEstimatedDurationMinutes() == null
                || route.getEstimatedDurationMinutes() <= 0) {
            return DEFAULT_DURATION_MINUTES;
        }
        return route.getEstimatedDurationMinutes();
    }
}
