const TOKEN_KEY = "rideshare_token";
const DISPLAY_KEY = "rideshare_display";
const ROLE_KEY = "rideshare_role";
const USER_ID_KEY = "rideshare_user_id";

export function getToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function getStoredDisplayName(): string | null {
  const v = sessionStorage.getItem(DISPLAY_KEY);
  if (v) return v;
  return sessionStorage.getItem("rideshare_user");
}

export function getStoredRole(): string | null {
  return sessionStorage.getItem(ROLE_KEY);
}

export function getStoredUserId(): string | null {
  return sessionStorage.getItem(USER_ID_KEY);
}

export function setSession(
  token: string,
  displayName: string,
  role: string,
  userId: number | null,
): void {
  sessionStorage.setItem(TOKEN_KEY, token);
  sessionStorage.setItem(DISPLAY_KEY, displayName);
  sessionStorage.setItem(ROLE_KEY, role);
  if (userId != null) {
    sessionStorage.setItem(USER_ID_KEY, String(userId));
  } else {
    sessionStorage.removeItem(USER_ID_KEY);
  }
}

export function clearSession(): void {
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(DISPLAY_KEY);
  sessionStorage.removeItem(ROLE_KEY);
  sessionStorage.removeItem(USER_ID_KEY);
  sessionStorage.removeItem("rideshare_user");
}
