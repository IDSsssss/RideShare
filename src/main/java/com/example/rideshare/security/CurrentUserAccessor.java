package com.example.rideshare.security;

import com.example.rideshare.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserAccessor {

    private AuthPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        if (auth.getPrincipal() instanceof AuthPrincipal p) {
            return p;
        }
        return null;
    }

    public boolean isAdmin() {
        AuthPrincipal p = principal();
        return p != null && "ADMIN".equals(p.getRole());
    }

    public Long currentUserIdOrNull() {
        AuthPrincipal p = principal();
        return p == null ? null : p.getUserId();
    }

    public void requireAdminOrDriver(Long driverUserId) {
        if (isAdmin()) {
            return;
        }
        Long uid = currentUserIdOrNull();
        if (uid == null || driverUserId == null || !uid.equals(driverUserId)) {
            throw new ForbiddenException("Это действие доступно только водителю поездки или администратору.");
        }
    }

    public void requireAdminOrRouteCreator(Long createdByUserId) {
        if (isAdmin()) {
            return;
        }
        Long uid = currentUserIdOrNull();
        if (uid == null || createdByUserId == null || !uid.equals(createdByUserId)) {
            throw new ForbiddenException("Это действие доступно только автору маршрута или администратору.");
        }
    }

    /** Отмена бронирования: пассажир заявки, водитель поездки или администратор. */
    public void requireAdminOrPassengerOrDriver(Long passengerUserId, Long driverUserId) {
        if (isAdmin()) {
            return;
        }
        Long uid = currentUserIdOrNull();
        if (uid == null) {
            throw new ForbiddenException("Нужна авторизация.");
        }
        if (passengerUserId != null && uid.equals(passengerUserId)) {
            return;
        }
        if (driverUserId != null && uid.equals(driverUserId)) {
            return;
        }
        throw new ForbiddenException(
                "Отменить бронирование может пассажир, водитель этой поездки или администратор.");
    }

    /** Только назначенный водитель (без исключения для администратора). */
    public void requireRideDriver(Long driverUserId) {
        Long uid = currentUserIdOrNull();
        if (uid == null || driverUserId == null || !uid.equals(driverUserId)) {
            throw new ForbiddenException("Отменить поездку может только её водитель.");
        }
    }
}
