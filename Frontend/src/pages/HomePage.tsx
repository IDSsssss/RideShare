import { NavLink } from "react-router-dom";

const tiles = [
  { to: "/users", title: "Участники", desc: "Профили, контакты и рейтинги пользователей." },
  { to: "/routes", title: "Маршруты", desc: "Направления, расстояние и время в пути." },
  { to: "/rides", title: "Поездки", desc: "Рейсы, водители, цены и статусы." },
  { to: "/bookings", title: "Бронирования", desc: "Заявки на места и их обработка." },
  { to: "/reviews", title: "Отзывы", desc: "Оценки и комментарии к поездкам." },
];

export default function HomePage() {
  return (
    <main className="page home-dashboard">
      <h2 className="page-title">Обзор</h2>
      <div className="dash-grid">
        {tiles.map((t) => (
          <NavLink key={t.to} to={t.to} className="dash-card card">
            <h3>{t.title}</h3>
            <p className="muted dash-desc">{t.desc}</p>
            <span className="dash-link">Открыть →</span>
          </NavLink>
        ))}
      </div>
    </main>
  );
}
