import type { BookingStatus } from "../types";

const BOOKING: Record<BookingStatus, string> = {
  PENDING: "Ожидает",
  CONFIRMED: "Подтверждено",
  CANCELLED: "Отменено",
  COMPLETED: "Завершено",
};

export function bookingStatusRu(s: BookingStatus): string {
  return BOOKING[s] ?? s;
}

const RIDE: Record<string, string> = {
  SCHEDULED: "Запланирована",
  IN_PROGRESS: "В пути",
  COMPLETED: "Завершена",
  CANCELLED: "Отменена",
};

export function rideStatusRu(s: string): string {
  return RIDE[s] ?? s;
}
