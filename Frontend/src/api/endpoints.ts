import { apiDelete, apiGet, apiPost, apiPut } from "./http";
import type {
  Booking,
  BookingRequest,
  BulkRideRequest,
  LoginResponse,
  Page,
  Review,
  ReviewRequest,
  Ride,
  RideRequest,
  Route,
  RouteRequest,
  User,
  UserRequest,
} from "../types";

export const authApi = {
  login: (body: { username: string; password: string }) =>
    apiPost<LoginResponse, { username: string; password: string }>("/api/auth/login", body),
};

export const usersApi = {
  list: () => apiGet<User[]>("/users"),
  get: (id: number) => apiGet<User>(`/users/${id}`),
  create: (body: UserRequest) => apiPost<User, UserRequest>("/users", body),
  update: (id: number, body: UserRequest) =>
    apiPut<User, UserRequest>(`/users/${id}`, body),
  delete: (id: number) => apiDelete(`/users/${id}`),
};

export const routesApi = {
  list: () => apiGet<Route[]>("/routes"),
  get: (id: number) => apiGet<Route>(`/routes/${id}`),
  search: (start?: string, end?: string) => {
    const q = new URLSearchParams();
    if (start) q.set("start", start);
    if (end) q.set("end", end);
    const s = q.toString();
    return apiGet<Route[]>(`/routes/search${s ? `?${s}` : ""}`);
  },
  update: (id: number, body: RouteRequest) =>
    apiPut<Route, RouteRequest>(`/routes/${id}`, body),
  delete: (id: number) => apiDelete(`/routes/${id}`),
};

export const ridesApi = {
  list: () => apiGet<Ride[]>("/rides"),
  get: (id: number) => apiGet<Ride>(`/rides/${id}`),
  createBulk: (body: BulkRideRequest) =>
    apiPost<Ride[], BulkRideRequest>("/rides", body),
  update: (id: number, body: RideRequest) =>
    apiPut<Ride, RideRequest>(`/rides/${id}`, body),
  delete: (id: number) => apiDelete(`/rides/${id}`),
  cancel: (id: number) => apiPost<Ride, Record<string, never>>(`/rides/${id}/cancel`, {}),
  searchAdvanced: (params: {
    startPoint?: string;
    endPoint?: string;
    fromDate?: string;
    toDate?: string;
    minPrice?: number;
    maxPrice?: number;
    minSeats?: number;
    page?: number;
    size?: number;
    sort?: string;
  }) => {
    const q = new URLSearchParams();
    if (params.startPoint) q.set("startPoint", params.startPoint);
    if (params.endPoint) q.set("endPoint", params.endPoint);
    if (params.fromDate) q.set("fromDate", params.fromDate);
    if (params.toDate) q.set("toDate", params.toDate);
    if (params.minPrice != null) q.set("minPrice", String(params.minPrice));
    if (params.maxPrice != null) q.set("maxPrice", String(params.maxPrice));
    if (params.minSeats != null) q.set("minSeats", String(params.minSeats));
    if (params.page != null) q.set("page", String(params.page));
    if (params.size != null) q.set("size", String(params.size));
    if (params.sort) q.set("sort", params.sort);
    return apiGet<Page<Ride>>(`/rides/advanced?${q.toString()}`);
  },
};

export const bookingsApi = {
  list: () => apiGet<Booking[]>("/bookings"),
  byUser: (userId: number) => apiGet<Booking[]>(`/bookings/user/${userId}`),
  byRide: (rideId: number) => apiGet<Booking[]>(`/bookings/ride/${rideId}`),
  create: (body: BookingRequest) =>
    apiPost<Booking, BookingRequest>("/bookings", body),
  cancel: (id: number) =>
    apiPut<Booking, Record<string, never>>(`/bookings/${id}/cancel`, {}),
  confirm: (id: number) =>
    apiPut<Booking, Record<string, never>>(`/bookings/${id}/confirm`, {}),
};

export const reviewsApi = {
  byRide: (rideId: number) => apiGet<Review[]>(`/reviews/ride/${rideId}`),
  byUser: (userId: number) => apiGet<Review[]>(`/reviews/user/${userId}`),
  driverRating: (driverId: number) =>
    apiGet<number>(`/reviews/driver/${driverId}/rating`),
  create: (body: ReviewRequest) =>
    apiPost<Review, ReviewRequest>("/reviews", body),
  delete: (id: number) => apiDelete(`/reviews/${id}`),
};
