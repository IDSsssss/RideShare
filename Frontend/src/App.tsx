import { Navigate, Route, Routes } from "react-router-dom";
import AdminShell from "./layout/AdminShell";
import RequireAuth from "./layout/RequireAuth";
import BookingsPage from "./pages/BookingsPage";
import LoginPage from "./pages/LoginPage";
import RidesPage from "./pages/RidesPage";
import RoutesPage from "./pages/RoutesPage";
import UsersPage from "./pages/UsersPage";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AdminShell />}>
          <Route index element={<BookingsPage />} />
          <Route path="users" element={<UsersPage />} />
          <Route path="routes" element={<RoutesPage />} />
          <Route path="rides" element={<RidesPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
