import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

const nav = [
  { to: "/", label: "Бронирования", end: true },
  { to: "/users", label: "Участники" },
  { to: "/routes", label: "Маршруты" },
  { to: "/rides", label: "Поездки" },
];

export default function AdminShell() {
  const { displayName, role, logout } = useAuth();

  return (
    <div className="app-shell">
      <aside className="nav-panel">
        <div className="brand-block">
          <span className="brand-name">Rideshare</span>
        </div>
        <nav className="nav-links" aria-label="Разделы">
          {nav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => (isActive ? "active" : "")}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="admin-column">
        <header className="admin-header">
          <div className="admin-header-spacer" />
          <div className="admin-user">
            <span className="admin-role" data-role={role ?? ""}>
              {role === "ADMIN" ? "Администратор" : role === "USER" ? "Участник" : ""}
            </span>
            <span className="admin-user-name">{displayName ?? "—"}</span>
            <button type="button" className="btn btn-ghost btn-sm" onClick={logout}>
              Выйти
            </button>
          </div>
        </header>
        <div className="admin-content">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
