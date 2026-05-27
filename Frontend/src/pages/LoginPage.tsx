import { useState } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(username.trim(), password);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "";
      if (/forbidden|403/i.test(msg)) {
        setError("Доступ запрещён. Проверьте, что бэкенд запущен и CORS настроен для вашего адреса.");
      } else if (/unauthorized|401/i.test(msg) || !msg) {
        setError("Неверный логин или пароль.");
      } else {
        setError(msg || "Не удалось войти");
      }
    } finally {
      setLoading(false);
    }
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="login-screen">
      <div className="login-card card">
        <div className="login-brand">
          <span className="login-brand-title">Rideshare</span>
        </div>
        {error && <div className="alert alert-error">{error}</div>}
        <form onSubmit={(e) => void handleSubmit(e)} className="login-form">
          <label className="field">
            Email или логин администратора
            <input
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </label>
          <label className="field">
            Пароль
            <input
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </label>
          <button type="submit" className="btn btn-primary login-submit" disabled={loading}>
            {loading ? "Вход…" : "Войти"}
          </button>
        </form>
      </div>
    </div>
  );
}
