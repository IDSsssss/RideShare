package com.example.rideshare.util;

public final class Constants {

    private Constants() {
    }

    public static final class RideStatus {
        public static final String SCHEDULED = "SCHEDULED";
        public static final String IN_PROGRESS = "IN_PROGRESS";
        public static final String COMPLETED = "COMPLETED";
        public static final String CANCELLED = "CANCELLED";

        private RideStatus() {
        }
    }

    public static final class BookingStatus {
        public static final String PENDING = "PENDING";
        public static final String CONFIRMED = "CONFIRMED";
        public static final String CANCELLED = "CANCELLED";
        public static final String COMPLETED = "COMPLETED";

        private BookingStatus() {
        }
    }

    public static final class Validation {
        public static final int MAX_SEATS = 8;
        public static final int MIN_SEATS = 1;
        public static final int MAX_RATING = 5;
        public static final int MIN_RATING = 1;

        private Validation() {
        }
    }
}