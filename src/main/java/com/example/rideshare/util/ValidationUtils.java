package com.example.rideshare.util;

import com.example.rideshare.exception.BusinessException;
import java.time.LocalDateTime;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new BusinessException(fieldName + " cannot be null");
        }
    }

    public static void validateFutureDateTime(LocalDateTime dateTime, String fieldName) {
        if (dateTime != null && dateTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException(fieldName + " must be in the future");
        }
    }

    public static void validatePositive(Number value, String fieldName) {
        if (value != null && value.doubleValue() <= 0) {
            throw new BusinessException(fieldName + " must be positive");
        }
    }

    public static void validateMin(Number value, Number min, String fieldName) {
        if (value != null && value.doubleValue() < min.doubleValue()) {
            throw new BusinessException(fieldName + " must be at least " + min);
        }
    }

    public static void validateMax(Number value, Number max, String fieldName) {
        if (value != null && value.doubleValue() > max.doubleValue()) {
            throw new BusinessException(fieldName + " must be at most " + max);
        }
    }
}