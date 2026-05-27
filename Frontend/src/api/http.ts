import { clearSession, getToken } from "../auth/session";
import type { ApiError } from "../types";

const BASE_URL = import.meta.env.VITE_API_URL || '';

function authHeaders(path: string): Record<string, string> {
    if (path.startsWith("/api/auth")) {
        return {};
    }
    const token = getToken();
    if (!token) {
        return {};
    }
    return { Authorization: `Bearer ${token}` };
}

async function parseError(res: Response): Promise<Error> {
    try {
        const body = (await res.json()) as ApiError;
        const parts = [body.message];
        if (body.validationErrors) {
            parts.push(
                ...Object.entries(body.validationErrors).map(([k, v]) => `${k}: ${v}`),
            );
        }
        return new Error(parts.filter(Boolean).join(" — "));
    } catch {
        return new Error(res.statusText || `HTTP ${res.status}`);
    }
}

function onUnauthorized(path: string): void {
    if (path.startsWith("/api/auth")) {
        return;
    }
    if (getToken()) {
        clearSession();
        if (window.location.pathname !== "/login") {
            window.location.assign("/login");
        }
    }
}

export async function apiGet<T>(path: string): Promise<T> {
    const url = `${BASE_URL}${path}`;
    const res = await fetch(url, {
        credentials: "include",
        headers: { ...authHeaders(path) },
    });
    if (res.status === 401) {
        onUnauthorized(path);
    }
    if (!res.ok) throw await parseError(res);
    if (res.status === 204) return undefined as T;
    return res.json() as Promise<T>;
}

export async function apiPost<T, B>(path: string, body: B): Promise<T> {
    const url = `${BASE_URL}${path}`;
    const res = await fetch(url, {
        method: "POST",
        credentials: "include",
        headers: {
            "Content-Type": "application/json",
            ...authHeaders(path),
        },
        body: JSON.stringify(body),
    });
    if (res.status === 401) {
        onUnauthorized(path);
    }
    if (!res.ok) throw await parseError(res);
    return res.json() as Promise<T>;
}

export async function apiPut<T, B>(path: string, body: B): Promise<T> {
    const url = `${BASE_URL}${path}`;
    const res = await fetch(url, {
        method: "PUT",
        credentials: "include",
        headers: {
            "Content-Type": "application/json",
            ...authHeaders(path),
        },
        body: JSON.stringify(body),
    });
    if (res.status === 401) {
        onUnauthorized(path);
    }
    if (!res.ok) throw await parseError(res);
    return res.json() as Promise<T>;
}

export async function apiPatch<T, B>(path: string, body: B): Promise<T> {
    const url = `${BASE_URL}${path}`;
    const res = await fetch(url, {
        method: "PATCH",
        credentials: "include",
        headers: {
            "Content-Type": "application/json",
            ...authHeaders(path),
        },
        body: JSON.stringify(body),
    });
    if (res.status === 401) {
        onUnauthorized(path);
    }
    if (!res.ok) throw await parseError(res);
    return res.json() as Promise<T>;
}

export async function apiDelete(path: string): Promise<void> {
    const url = `${BASE_URL}${path}`;
    const res = await fetch(url, {
        method: "DELETE",
        credentials: "include",
        headers: { ...authHeaders(path) },
    });
    if (res.status === 401) {
        onUnauthorized(path);
    }
    if (!res.ok) throw await parseError(res);
}