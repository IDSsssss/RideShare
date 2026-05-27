import { useCallback, useEffect, useMemo, useState } from "react";
import { routesApi } from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";
import { getStoredUserId } from "../auth/session";
import type { Route, RouteRequest } from "../types";
import { routeLabel } from "../utils/labels";

const emptyRoute: RouteRequest = {
  startPoint: "",
  endPoint: "",
  distanceKm: undefined,
  estimatedDurationMinutes: undefined,
  waypoints: "",
};

export default function RoutesPage() {
   const { role } = useAuth();
   const myUserId = Number.parseInt(getStoredUserId() ?? "", 10);

   const [routes, setRoutes] = useState<Route[]>([]);
   const [clientFilter, setClientFilter] = useState("");
   const [startFilter, setStartFilter] = useState("");
   const [endFilter, setEndFilter] = useState("");
   const [sortBy, setSortBy] = useState<'startPoint' | 'endPoint' | 'distanceKm' | 'estimatedDurationMinutes'>('startPoint');
   const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');
   const [currentPage, setCurrentPage] = useState(0);
   const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<Route | null>(null);
  const [form, setForm] = useState<RouteRequest>(emptyRoute);

  const loadAll = useCallback(async () => {
    setError(null);
    try {
      setRoutes(await routesApi.list());
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось загрузить маршруты");
    }
  }, []);

  useEffect(() => {
    void loadAll();
  }, [loadAll]);

   const displayedRoutes = useMemo(() => {
     const q = clientFilter.trim().toLowerCase();
     let list = routes;
     if (q) {
       list = list.filter(
         (r) =>
           r.startPoint.toLowerCase().includes(q) ||
           r.endPoint.toLowerCase().includes(q) ||
           (r.waypoints?.toLowerCase().includes(q) ?? false),
       );
     }
     const sorted = [...list].sort((a, b) => {
       let aVal: any, bVal: any;
       switch (sortBy) {
         case 'startPoint':
           aVal = a.startPoint;
           bVal = b.startPoint;
           break;
         case 'endPoint':
           aVal = a.endPoint;
           bVal = b.endPoint;
           break;
         case 'distanceKm':
           aVal = a.distanceKm || 0;
           bVal = b.distanceKm || 0;
           break;
         case 'estimatedDurationMinutes':
           aVal = a.estimatedDurationMinutes || 0;
           bVal = b.estimatedDurationMinutes || 0;
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
   }, [routes, clientFilter, sortBy, sortOrder, currentPage]);

  const canEditRoute = (r: Route) =>
    role === "ADMIN" ||
    (Number.isFinite(myUserId) &&
      r.createdByUserId != null &&
      r.createdByUserId === myUserId);

  async function applyServerFilter() {
    setError(null);
    try {
      const list = await routesApi.search(
        startFilter.trim() || undefined,
        endFilter.trim() || undefined,
      );
      setRoutes(list);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Поиск не выполнен");
    }
  }

  function openEdit(r: Route) {
    if (!canEditRoute(r)) return;
    setEditing(r);
    setForm({
      startPoint: r.startPoint,
      endPoint: r.endPoint,
      distanceKm: r.distanceKm,
      estimatedDurationMinutes: r.estimatedDurationMinutes,
      waypoints: r.waypoints ?? "",
    });
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!editing) return;
    setError(null);
    try {
      const payload: RouteRequest = {
        ...form,
        waypoints: form.waypoints || undefined,
        distanceKm: form.distanceKm || undefined,
        estimatedDurationMinutes: form.estimatedDurationMinutes || undefined,
      };
      await routesApi.update(editing.id, payload);
      setEditing(null);
      setForm(emptyRoute);
      await loadAll();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось сохранить");
    }
  }

  async function remove(r: Route) {
    if (!canEditRoute(r)) return;
    if (!confirm(`Удалить маршрут «${r.startPoint} → ${r.endPoint}»?`)) return;
    setError(null);
    try {
      await routesApi.delete(r.id);
      await loadAll();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось удалить");
    }
  }

  return (
    <main className="page">
      <h2 className="page-title">Маршруты</h2>
      <p className="page-lead">
        Справочник направлений. Новые маршруты создаются автоматически при оформлении поездки.
      </p>
      {error && <div className="alert alert-error">{error}</div>}
      <div className="card">
        <div className="toolbar">
          <label className="field">
            Откуда
            <input value={startFilter} onChange={(e) => setStartFilter(e.target.value)} placeholder="Город или точка" />
          </label>
          <label className="field">
            Куда
            <input value={endFilter} onChange={(e) => setEndFilter(e.target.value)} placeholder="Город или точка" />
          </label>
          <button type="button" className="btn btn-primary" onClick={() => void applyServerFilter()}>
            Найти
          </button>
          <button type="button" className="btn btn-ghost" onClick={() => void loadAll()}>
            Показать все
          </button>
        </div>
         <label className="field block-field">
           Быстрый поиск по текущей таблице
           <input
             value={clientFilter}
             onChange={(e) => {
               setClientFilter(e.target.value);
               setCurrentPage(0);
             }}
             placeholder="Фильтр без запроса к серверу…"
           />
         </label>
        {editing && canEditRoute(editing) && (
          <form onSubmit={submit} className="card form-card">
            <h3 className="form-card-title">Редактирование: {routeLabel(editing)}</h3>
            <div className="form-row">
              <label className="field">
                Откуда
                <input
                  required
                  value={form.startPoint}
                  onChange={(e) => setForm({ ...form, startPoint: e.target.value })}
                />
              </label>
              <label className="field">
                Куда
                <input
                  required
                  value={form.endPoint}
                  onChange={(e) => setForm({ ...form, endPoint: e.target.value })}
                />
              </label>
              <label className="field">
                Расстояние, км
                <input
                  type="number"
                  min={0.01}
                  step={0.1}
                  value={form.distanceKm ?? ""}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      distanceKm: e.target.value ? Number(e.target.value) : undefined,
                    })
                  }
                />
              </label>
              <label className="field">
                Время в пути, мин
                <input
                  type="number"
                  min={1}
                  value={form.estimatedDurationMinutes ?? ""}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      estimatedDurationMinutes: e.target.value
                        ? Number(e.target.value)
                        : undefined,
                    })
                  }
                />
              </label>
              <label className="field grow">
                Промежуточные точки
                <input
                  value={form.waypoints ?? ""}
                  onChange={(e) => setForm({ ...form, waypoints: e.target.value })}
                />
              </label>
              <button type="submit" className="btn btn-primary">
                Сохранить
              </button>
              <button type="button" className="btn btn-ghost" onClick={() => setEditing(null)}>
                Закрыть
              </button>
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
                     if (sortBy === 'startPoint') setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
                     else { setSortBy('startPoint'); setSortOrder('asc'); }
                   }}
                 >
                   Направление {sortBy === 'startPoint' ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                 </button>
               </th>
               <th>
                 <button
                   type="button"
                   className="btn btn-ghost btn-xs"
                   onClick={() => {
                     setCurrentPage(0);
                     if (sortBy === 'distanceKm') setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
                     else { setSortBy('distanceKm'); setSortOrder('asc'); }
                   }}
                 >
                   км {sortBy === 'distanceKm' ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                 </button>
               </th>
               <th>
                 <button
                   type="button"
                   className="btn btn-ghost btn-xs"
                   onClick={() => {
                     setCurrentPage(0);
                     if (sortBy === 'estimatedDurationMinutes') setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
                     else { setSortBy('estimatedDurationMinutes'); setSortOrder('asc'); }
                   }}
                 >
                   мин {sortBy === 'estimatedDurationMinutes' ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                 </button>
               </th>
               <th className="col-actions" />
             </tr>
           </thead>
           <tbody>
             {displayedRoutes.list.map((r) => (
               <tr key={r.id}>
                 <td>
                   <div className="cell-strong">
                     {r.startPoint} → {r.endPoint}
                   </div>
                   {r.waypoints ? <div className="muted small">Через: {r.waypoints}</div> : null}
                 </td>
                 <td>{r.distanceKm ?? "—"}</td>
                 <td>{r.estimatedDurationMinutes ?? "—"}</td>
                 <td className="col-actions">
                   {canEditRoute(r) && (
                     <>
                       <button type="button" className="btn btn-ghost btn-sm" onClick={() => openEdit(r)}>
                         Изменить
                       </button>{" "}
                       <button type="button" className="btn btn-danger btn-sm" onClick={() => void remove(r)}>
                         Удалить
                       </button>
                     </>
                   )}
                 </td>
               </tr>
             ))}
           </tbody>
         </table>
         <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1rem' }}>
           <p className="muted pagination-hint">
             Найдено записей: {displayedRoutes.total}
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
               Страница {currentPage + 1} из {Math.max(1, Math.ceil(displayedRoutes.total / 5))}
             </span>
             <button
               type="button"
               className="btn btn-ghost btn-sm"
               onClick={() =>
                 setCurrentPage((p) =>
                   Math.min(Math.ceil(displayedRoutes.total / 5) - 1, p + 1)
                 )
               }
               disabled={currentPage >= Math.ceil(displayedRoutes.total / 5) - 1}
             >
               Следующая →
             </button>
           </div>
         </div>
       </div>
     </main>
  );
}
