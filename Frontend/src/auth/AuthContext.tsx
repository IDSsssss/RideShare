import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { useNavigate } from "react-router-dom";
import { authApi } from "../api/endpoints";
import {
  clearSession,
  getStoredDisplayName,
  getStoredRole,
  getToken,
  setSession,
} from "./session";

type AuthContextValue = {
  token: string | null;
  displayName: string | null;
  role: string | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const [token, setToken] = useState<string | null>(() => getToken());
  const [displayName, setDisplayName] = useState<string | null>(() => getStoredDisplayName());
  const [role, setRole] = useState<string | null>(() => getStoredRole());

  const login = useCallback(async (username: string, password: string) => {
    const res = await authApi.login({ username: username.trim(), password });
    setSession(res.accessToken, res.displayName, res.role, res.userId ?? null);
    setToken(res.accessToken);
    setDisplayName(res.displayName);
    setRole(res.role);
    navigate("/", { replace: true });
  }, [navigate]);

  const logout = useCallback(() => {
    clearSession();
    setToken(null);
    setDisplayName(null);
    setRole(null);
    navigate("/login", { replace: true });
  }, [navigate]);

  const value = useMemo(
    () => ({
      token,
      displayName,
      role,
      isAuthenticated: Boolean(token),
      login,
      logout,
    }),
    [token, displayName, role, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return ctx;
}
