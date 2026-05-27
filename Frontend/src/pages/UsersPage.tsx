import { useCallback, useEffect, useMemo, useState } from "react";
import { usersApi } from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";
import type { User, UserRequest } from "../types";

const emptyUser: UserRequest = {
  name: "",
  email: "",
  phone: "",
  rating: 0,
  password: undefined,
};

export default function UsersPage() {
   const { role } = useAuth();
   const isAdmin = role === "ADMIN";
   const [users, setUsers] = useState<User[]>([]);
   const [filter, setFilter] = useState("");
   const [sortBy, setSortBy] = useState<'name' | 'email' | 'phone' | 'rating'>('name');
   const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');
   const [currentPage, setCurrentPage] = useState(0);
   const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<User | null>(null);
  const [form, setForm] = useState<UserRequest>(emptyUser);

  const load = useCallback(async () => {
    setError(null);
    try {
      setUsers(await usersApi.list());
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось загрузить список");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

   const filtered = useMemo(() => {
     const q = filter.trim().toLowerCase();
     let list = users;
     if (q) {
       list = list.filter(
         (u) =>
           u.name.toLowerCase().includes(q) ||
           u.email.toLowerCase().includes(q) ||
           u.phone.includes(q),
       );
     }
     const sorted = [...list].sort((a, b) => {
       let aVal: any, bVal: any;
       switch (sortBy) {
         case 'name':
           aVal = a.name;
           bVal = b.name;
           break;
         case 'email':
           aVal = a.email;
           bVal = b.email;
           break;
         case 'phone':
           aVal = a.phone;
           bVal = b.phone;
           break;
         case 'rating':
           aVal = a.rating || 0;
           bVal = b.rating || 0;
           break;
         default:
           aVal = a.id;
           bVal = b.id;
       }
       if (sortOrder === 'asc') return aVal > bVal ? 1 : aVal < bVal ? -1 : 0;
       return aVal < bVal ? 1 : aVal > bVal ? -1 : 0;
     });
     const pageSize = 5;
     const total = sorted.length;
     const paginated = sorted.slice(currentPage * pageSize, (currentPage + 1) * pageSize);
     return { list: paginated, total };
   }, [users, filter, sortBy, sortOrder, currentPage]);

  function openEdit(u: User) {
    setEditing(u);
    setForm({
      name: u.name,
      email: u.email,
      phone: u.phone,
      rating: u.rating ?? 0,
      password: undefined,
    });
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      const payload: UserRequest = { ...form };
      if (!payload.password?.trim()) {
        delete payload.password;
      }
      if (editing) {
        await usersApi.update(editing.id, payload);
      } else {
        await usersApi.create(payload);
      }
      setEditing(null);
      setForm(emptyUser);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось сохранить");
    }
  }

  async function remove(u: User) {
    if (!window.confirm(`Удалить участника «${u.name}»? Это действие необратимо.`)) return;
    setError(null);
    try {
      await usersApi.delete(u.id);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось удалить");
    }
  }

  return (
    <main className="page">
      <h2 className="page-title">Участники</h2>
      <p className="page-lead">
        Профили пользователей: контакты, рейтинг. Пароль нужен для входа участника по email (не
        показывается после сохранения).
      </p>
      {error && <div className="alert alert-error">{error}</div>}
      <div className="card">
         <div className="toolbar">
           <label className="field grow">
             Поиск по имени, email или телефону
             <input 
               value={filter} 
               onChange={(e) => {
                 setFilter(e.target.value);
                 setCurrentPage(0);
               }} 
               placeholder="Начните вводить…" 
             />
           </label>
         </div>
        {isAdmin && (
          <form onSubmit={submit} className="card form-card">
            <h3 className="form-card-title">{editing ? `Редактирование: ${editing.name}` : "Новый участник"}</h3>
            <div className="form-row">
              <label className="field">
                Имя
                <input
                  required
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                />
              </label>
              <label className="field">
                Email
                <input
                  required
                  type="email"
                  value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                />
              </label>
              <label className="field">
                Телефон
                <input
                  required
                  value={form.phone}
                  onChange={(e) => setForm({ ...form, phone: e.target.value })}
                />
              </label>
              <label className="field grow">
                {editing ? "Новый пароль (пусто — не менять)" : "Пароль для входа по email"}
                <input
                  type="password"
                  autoComplete="new-password"
                  value={form.password ?? ""}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      password: e.target.value || undefined,
                    })
                  }
                  placeholder={editing ? "не менять" : "минимум 8 символов"}
                />
              </label>
              <label className="field">
                Рейтинг
                <input
                  type="number"
                  min={0}
                  max={5}
                  step={0.1}
                  value={form.rating ?? 0}
                  onChange={(e) =>
                    setForm({ ...form, rating: Number.parseFloat(e.target.value) || 0 })
                  }
                />
              </label>
              <button type="submit" className="btn btn-primary">
                Сохранить
              </button>
              {editing && (
                <button type="button" className="btn btn-ghost" onClick={() => setEditing(null)}>
                  Отмена
                </button>
              )}
            </div>
          </form>
        )}
        <table className="data">
          <thead>
            <tr>
               <th>
                 <button
                   type="button"
                   className="btn btn-ghost btn-xs"
                   onClick={() => {
                     setCurrentPage(0);
                     if (sortBy === 'name') setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
                     else { setSortBy('name'); setSortOrder('asc'); }
                   }}
                 >
                   Участник {sortBy === 'name' ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                 </button>
               </th>
               <th>
                 <button
                   type="button"
                   className="btn btn-ghost btn-xs"
                   onClick={() => {
                     setCurrentPage(0);
                     if (sortBy === 'phone') setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
                     else { setSortBy('phone'); setSortOrder('asc'); }
                   }}
                 >
                   Телефон {sortBy === 'phone' ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                 </button>
               </th>
               <th>
                 <button
                   type="button"
                   className="btn btn-ghost btn-xs"
                   onClick={() => {
                     setCurrentPage(0);
                     if (sortBy === 'rating') setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
                     else { setSortBy('rating'); setSortOrder('desc'); }
                   }}
                 >
                   Рейтинг {sortBy === 'rating' ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                 </button>
               </th>
              {isAdmin && <th className="col-actions" />}
            </tr>
          </thead>
           <tbody>
             {filtered.list.map((u) => (
              <tr key={u.id}>
                <td>
                  <div className="cell-strong">{u.name}</div>
                  <div className="muted small">{u.email}</div>
                </td>
                <td>{u.phone}</td>
                <td>{u.rating}</td>
                {isAdmin && (
                   <td className="col-actions">
                     <button type="button" className="btn btn-ghost btn-sm" onClick={() => openEdit(u)}>
                       Изменить
                     </button>{" "}
                     <button type="button" className="btn btn-danger btn-sm" onClick={() => void remove(u)}>
                       Удалить
                     </button>
                   </td>
                 )}
               </tr>
             ))}
           </tbody>
         </table>
         <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1rem' }}>
           <p className="muted pagination-hint">
             Найдено записей: {filtered.total}
           </p>
           <div className="pagination-controls">
             <button
               type="button"
               className="btn btn-ghost btn-sm"
               onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
               disabled={currentPage === 0}
             >
               ← Предыдущая
             </button>
             <span className="pagination-info">
               Страница {currentPage + 1} из {Math.max(1, Math.ceil(filtered.total / 5))}
             </span>
             <button
               type="button"
               className="btn btn-ghost btn-sm"
               onClick={() =>
                 setCurrentPage((p) =>
                   Math.min(Math.ceil(filtered.total / 5) - 1, p + 1)
                 )
               }
               disabled={currentPage >= Math.ceil(filtered.total / 5) - 1}
             >
               Следующая →
             </button>
           </div>
         </div>
       </div>
     </main>
  );
}
