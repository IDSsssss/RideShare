import type { Ride, Route, User } from "../types";

export function userLabel(u: User): string {
  return `${u.name} · ${u.email}`;
}

export function routeLabel(r: Route): string {
  const extra =
    r.distanceKm != null && r.estimatedDurationMinutes != null
      ? ` · ${r.distanceKm} км, ${r.estimatedDurationMinutes} мин`
      : "";
  return `${r.startPoint} → ${r.endPoint}${extra}`;
}

export function formatRideDeparture(iso: string): string {
  const d = new Date(iso.replace(" ", "T"));
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString("ru-RU", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function rideLabel(r: Ride): string {
  const route = r.route ? `${r.route.startPoint} → ${r.route.endPoint}` : "маршрут не указан";
  return `${route}, ${formatRideDeparture(r.departureTime)}`;
}

export function rideShortLabel(r: Ride): string {
  const route = r.route ? `${r.route.startPoint} — ${r.route.endPoint}` : "поездка";
  return `${route}, ${formatRideDeparture(r.departureTime)}`;
}

export function formatBookingTime(iso: string): string {
  const d = new Date(iso.replace(" ", "T"));
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString("ru-RU", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
