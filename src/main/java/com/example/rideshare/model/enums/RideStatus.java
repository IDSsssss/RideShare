package com.example.rideshare.model.enums;

import lombok.Getter;

@Getter
public enum RideStatus {
    SCHEDULED,      // Запланирована
    IN_PROGRESS,    // В процессе
    COMPLETED,      // Завершена
    CANCELLED       // Отменена
}
