export interface User {
  id: number;
  name: string;
  email: string;
  phone: string;
  rating: number;
  createdAt?: string;
}

export interface Route {
  id: number;
  startPoint: string;
  endPoint: string;
  distanceKm?: number;
  estimatedDurationMinutes?: number;
  waypoints?: string;
  createdByUserId?: number | null;
}

export type RideStatus = "SCHEDULED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export interface Ride {
  id: number;
  departureTime: string;
  availableSeats: number;
  price: number;
  status: string;
  driver?: User;
  route?: Route;
  bookings?: Booking[];
}

export type BookingStatus = "PENDING" | "CONFIRMED" | "CANCELLED" | "COMPLETED";

export interface Booking {
  id: number;
  bookingTime: string;
  seats: number;
  status: BookingStatus;
  passenger?: User;
  ride?: Ride;
  totalPrice?: number;
}

export interface Review {
  id: number;
  rating: number;
  comment?: string;
  createdAt?: string;
  reviewer?: User;
  ride?: Ride;
}

export interface UserRequest {
  name: string;
  email: string;
  phone: string;
  rating?: number;
  password?: string;
}

export interface RouteRequest {
  startPoint: string;
  endPoint: string;
  /** Длина пути, км (обязательно при создании поездки; на бэкенде — @NotNull в составе поездки) */
  distanceKm?: number;
  /** Время в пути, минуты */
  estimatedDurationMinutes?: number;
  waypoints?: string;
}

export interface RideRequest {
  departureTime: string;
  availableSeats: number;
  price: number;
  route: RouteRequest;
}

export interface BulkRideRequest {
  driverId: number;
  rides: RideRequest[];
}

export interface BookingRequest {
  rideId: number;
  passengerId: number;
  seats: number;
}

export interface ReviewRequest {
  rating: number;
  comment?: string;
  /** Только для ADMIN; для остальных автор берётся из сессии на сервере */
  reviewerId?: number;
  rideId: number;
}

export interface LoginResponse {
  accessToken: string;
  role: string;
  displayName: string;
  userId?: number | null;
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
  path?: string;
  validationErrors?: Record<string, string>;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
