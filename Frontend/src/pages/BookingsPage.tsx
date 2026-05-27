import { useCallback, useEffect, useMemo, useState } from "react";
import { bookingsApi, ridesApi, usersApi } from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";
import { getStoredUserId } from "../auth/session";
import type { Booking, BookingRequest, BookingStatus, Ride, User } from "../types";
import { formatBookingTime, rideShortLabel, userLabel } from "../utils/labels";
import { bookingStatusRu } from "../utils/statusLabels";

type ListMode = "all" | "byUser";

function canConfirmBooking(
  b: Booking,
  role: string | null,
  myUserId: number,
): boolean {
  if (b.status !== "PENDING") return false;
  const driverId = b.ride?.driver?.id;
  if (role === "ADMIN") return true;
  return Number.isFinite(myUserId) && driverId === myUserId;
}

/** В интерфейсе отмена только для ожидающих заявок (у подтверждённых кнопок нет). */
function canCancelBooking(
  b: Booking,
  role: string | null,
  myUserId: number,
): boolean {
  if (b.status !== "PENDING") return false;
  const passengerId = b.passenger?.id;
  const driverId = b.ride?.driver?.id;
  if (role === "ADMIN") return true;
  if (!Number.isFinite(myUserId)) return false;
  return passengerId === myUserId || driverId === myUserId;
}

export default function BookingsPage() {
   const { role } = useAuth();
   const myUserId = Number.parseInt(getStoredUserId() ?? "", 10);
   const [bookings, setBookings] = useState<Booking[]>([]);
   const [rides, setRides] = useState<Ride[]>([]);
   const [users, setUsers] = useState<User[]>([]);
   const [listMode, setListMode] = useState<ListMode>("all");
   const [filterUserId, setFilterUserId] = useState<number | "">("");
   const [statusFilter, setStatusFilter] = useState<"" | BookingStatus>("");
   const [textFilter, setTextFilter] = useState("");
   const [sortBy, setSortBy] = useState<'created' | 'passenger' | 'ride' | 'seats' | 'status'>('created');
   const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
   const [currentPage, setCurrentPage] = useState(0);
   const [error, setError] = useState<string | null>(null);

  const [form, setForm] = useState<BookingRequest>({
    rideId: 0,
    passengerId: 0,
    seats: 1,
  });

  const loadRefs = useCallback(async () => {
    setError(null);
    try {
      const [r, u] = await Promise.all([ridesApi.list(), usersApi.list()]);
      setRides(r);
      setUsers(u);
      const scheduled = r.filter((ride) => ride.status === "SCHEDULED");
      setForm((f) => ({
        ...f,
        rideId: scheduled[0]?.id ?? 0,
        passengerId: u[0]?.id ?? 0,
      }));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось загрузить данные");
    }
  }, []);

  const bookableRides = useMemo(
    () => rides.filter((ride) => ride.status === "SCHEDULED"),
    [rides],
  );

  const loadBookings = useCallback(async () => {
    setError(null);
    try {
      if (listMode === "byUser") {
        if (filterUserId === "") {
          setBookings([]);
          return;
        }
        setBookings(await bookingsApi.byUser(filterUserId));
      } else {
        setBookings(await bookingsApi.list());
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось загрузить бронирования");
    }
  }, [listMode, filterUserId]);

  useEffect(() => {
    void loadRefs();
  }, [loadRefs]);

  useEffect(() => {
    void loadBookings();
  }, [loadBookings]);

   const pageSize = 10;

   const filtered = useMemo(() => {
     const q = textFilter.trim().toLowerCase();
     let list = bookings;
     if (statusFilter) list = list.filter((b) => b.status === statusFilter);
     if (!q) {
       const sorted = [...list].sort((a, b) => {
         let aVal: any, bVal: any;
         switch (sortBy) {
           case 'created':
             aVal = new Date(a.bookingTime).getTime();
             bVal = new Date(b.bookingTime).getTime();
             break;
           case 'passenger':
             aVal = a.passenger?.name || '';
             bVal = b.passenger?.name || '';
             break;
           case 'ride':
             aVal = a.ride ? rideShortLabel(a.ride) : '';
             bVal = b.ride ? rideShortLabel(b.ride) : '';
             break;
           case 'seats':
             aVal = a.seats;
             bVal = b.seats;
             break;
           case 'status':
             aVal = a.status;
             bVal = b.status;
             break;
           default:
             aVal = a.id;
             bVal = b.id;
         }
         if (sortOrder === 'asc') return aVal > bVal ? 1 : aVal < bVal ? -1 : 0;
         return aVal < bVal ? 1 : aVal > bVal ? -1 : 0;
       });
       const total = sorted.length;
       const paginated = sorted.slice(currentPage * pageSize, (currentPage + 1) * pageSize);
       return { list: paginated, total };
     }
     const filteredList = list.filter((b) => {
       const p = b.passenger?.name?.toLowerCase() ?? "";
       const route = b.ride?.route;
       const routeStr = route
         ? `${route.startPoint} ${route.endPoint}`.toLowerCase()
         : "";
       const rideLine = b.ride ? rideShortLabel(b.ride).toLowerCase() : "";
       return p.includes(q) || routeStr.includes(q) || rideLine.includes(q);
     });
     const sorted = [...filteredList].sort((a, b) => {
       let aVal: any, bVal: any;
       switch (sortBy) {
         case 'created':
           aVal = new Date(a.bookingTime).getTime();
           bVal = new Date(b.bookingTime).getTime();
           break;
         case 'passenger':
           aVal = a.passenger?.name || '';
           bVal = b.passenger?.name || '';
           break;
         case 'ride':
           aVal = a.ride ? rideShortLabel(a.ride) : '';
           bVal = b.ride ? rideShortLabel(b.ride) : '';
           break;
         case 'seats':
           aVal = a.seats;
           bVal = b.seats;
           break;
         case 'status':
           aVal = a.status;
           bVal = b.status;
           break;
         default:
           aVal = a.id;
           bVal = b.id;
       }
       if (sortOrder === 'asc') return aVal > bVal ? 1 : aVal < bVal ? -1 : 0;
       return aVal < bVal ? 1 : aVal > bVal ? -1 : 0;
     });
     const total = sorted.length;
     const paginated = sorted.slice(currentPage * pageSize, (currentPage + 1) * pageSize);
     return { list: paginated, total };
   }, [bookings, statusFilter, textFilter, sortBy, sortOrder, currentPage, pageSize]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!form.passengerId || !form.rideId) {
      setError("Выберите поездку и пассажира");
      return;
    }
    if (!bookableRides.some((r) => r.id === form.rideId)) {
      setError("Бронирование возможно только для поездок в статусе «Запланирована»");
      return;
    }
    try {
      await bookingsApi.create(form);
      await loadBookings();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось создать бронирование");
    }
  }

  async function cancelBooking(b: Booking) {
    if (!window.confirm("Отменить это бронирование?")) return;
    setError(null);
    try {
      await bookingsApi.cancel(b.id);
      await loadBookings();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Ошибка отмены");
    }
  }

  async function confirmBooking(b: Booking) {
    setError(null);
    try {
      await bookingsApi.confirm(b.id);
      await loadBookings();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Ошибка подтверждения");
    }
  }

  return (
    <main className="page">
      <h2 className="page-title">Бронирования</h2>
      <p className="page-lead">Места в поездках: создание, подтверждение и отмена заявок.</p>
      {error && <div className="alert alert-error">{error}</div>}

      <div className="card">
        <h3 className="form-card-title">Новое бронирование</h3>
        <form onSubmit={submit}>
          <div className="form-row">
            <label className="field grow">
              Поездка
              <select
                value={form.rideId || ""}
                onChange={(e) =>
                  setForm({ ...form, rideId: Number(e.target.value) })
                }
              >
                {bookableRides.length === 0 ? (
                  <option value="">Нет запланированных поездок</option>
                ) : (
                  bookableRides.map((r) => (
                    <option key={r.id} value={r.id}>
                      {rideShortLabel(r)}
                    </option>
                  ))
                )}
              </select>
            </label>
            <label className="field grow">
              Пассажир
              <select
                value={form.passengerId || ""}
                onChange={(e) =>
                  setForm({ ...form, passengerId: Number(e.target.value) })
                }
              >
                {users.map((u) => (
                  <option key={u.id} value={u.id}>
                    {userLabel(u)}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              Мест
              <input
                type="number"
                min={1}
                max={8}
                value={form.seats}
                onChange={(e) =>
                  setForm({ ...form, seats: Number(e.target.value) })
                }
              />
            </label>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={bookableRides.length === 0 || users.length === 0}
            >
              Забронировать
            </button>
          </div>
        </form>
      </div>

      <div className="card">
        <h3 className="form-card-title">Список и фильтры</h3>
        <div className="form-row">
          <label className="field">
            Показать
            <select
              value={listMode}
              onChange={(e) => setListMode(e.target.value as ListMode)}
            >
              <option value="all">Все бронирования</option>
              <option value="byUser">Только выбранный пассажир</option>
            </select>
          </label>
          {listMode === "byUser" && (
            <label className="field grow">
              Пассажир
              <select
                value={filterUserId === "" ? "" : String(filterUserId)}
                onChange={(e) =>
                  setFilterUserId(e.target.value ? Number(e.target.value) : "")
                }
              >
                <option value="">Выберите…</option>
                {users.map((u) => (
                  <option key={u.id} value={u.id}>
                    {userLabel(u)}
                  </option>
                ))}
              </select>
            </label>
          )}
           <label className="field">
             Статус
             <select
               value={statusFilter}
               onChange={(e) => {
                 setStatusFilter(e.target.value as "" | BookingStatus);
                 setCurrentPage(0);
               }}
             >
               <option value="">Все</option>
               <option value="PENDING">Ожидает</option>
               <option value="CONFIRMED">Подтверждено</option>
               <option value="CANCELLED">Отменено</option>
               <option value="COMPLETED">Завершено</option>
             </select>
           </label>
           <label className="field grow">
             Поиск по имени или маршруту
             <input
               value={textFilter}
               onChange={(e) => {
                 setTextFilter(e.target.value);
                 setCurrentPage(0);
               }}
               placeholder="Имя пассажира, город…"
             />
           </label>
           <button type="button" className="btn btn-ghost" onClick={() => void loadBookings()}>
             Обновить
           </button>
         </div>
         <table className="data">
           <thead>
             <tr>
               <th>
                 <button
                   type="button"
                   className="btn btn-ghost btn-xs"
                   onClick={() => {
                     setCurrentPage(0);
                     if (sortBy === 'created') setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
                     else { setSortBy('created'); setSortOrder('desc'); }
                   }}
                 >
                   Создано {sortBy === 'created' ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                 </button>
               </th>
               <th>
                 <button
                   type="button"
                   className="btn btn-ghost btn-xs"
                   onClick={() => {
                     setCurrentPage(0);
                     if (sortBy === 'passenger') setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
                     else { setSortBy('passenger'); setSortOrder('asc'); }
                   }}
                 >
                   Пассажир {sortBy === 'passenger' ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                 </button>
               </th>
               <th>
                 <button
                   type="button"
                   className="btn btn-ghost btn-xs"
                   onClick={() => {
                     setCurrentPage(0);
                     if (sortBy === 'ride') setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
                     else { setSortBy('ride'); setSortOrder('asc'); }
                   }}
                 >
                   Поездка {sortBy === 'ride' ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                 </button>
               </th>
               <th>
                 <button
                   type="button"
                   className="btn btn-ghost btn-xs"
                   onClick={() => {
                     setCurrentPage(0);
                     if (sortBy === 'seats') setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
                     else { setSortBy('seats'); setSortOrder('desc'); }
                   }}
                 >
                   Мест {sortBy === 'seats' ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                 </button>
               </th>
               <th>
                 <button
                   type="button"
                   className="btn btn-ghost btn-xs"
                   onClick={() => {
                     setCurrentPage(0);
                     if (sortBy === 'status') setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
                     else { setSortBy('status'); setSortOrder('asc'); }
                   }}
                 >
                   Статус {sortBy === 'status' ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                 </button>
               </th>
              <th className="col-actions" />
            </tr>
           </thead>
           <tbody>
             {filtered.list.map((b) => (
              <tr key={b.id}>
                <td>{formatBookingTime(b.bookingTime)}</td>
                <td>{b.passenger ? b.passenger.name : "—"}</td>
                <td>{b.ride ? rideShortLabel(b.ride) : "—"}</td>
                <td>{b.seats}</td>
                <td>
                  <span className="tag">{bookingStatusRu(b.status)}</span>
                </td>
                <td className="col-actions">
                  {(() => {
                    const showConfirm = canConfirmBooking(b, role, myUserId);
                    const showCancel = canCancelBooking(b, role, myUserId);
                    if (!showConfirm && !showCancel) {
                      return <span className="muted">—</span>;
                    }
                    return (
                      <>
                        {showConfirm ? (
                          <button
                            type="button"
                            className="btn btn-ghost btn-sm"
                            onClick={() => void confirmBooking(b)}
                          >
                            Подтвердить
                          </button>
                        ) : null}
                        {showConfirm && showCancel ? " " : null}
                        {showCancel ? (
                          <button
                            type="button"
                            className="btn btn-danger btn-sm"
                            onClick={() => void cancelBooking(b)}
                          >
                            Отменить
                          </button>
                        ) : null}
                      </>
                    );
                  })()}
                </td>
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
               Страница {currentPage + 1} из {Math.max(1, Math.ceil(filtered.total / pageSize))}
             </span>
             <button
               type="button"
               className="btn btn-ghost btn-sm"
               onClick={() =>
                 setCurrentPage((p) =>
                   Math.min(Math.ceil(filtered.total / pageSize) - 1, p + 1)
                 )
               }
               disabled={currentPage >= Math.ceil(filtered.total / pageSize) - 1}
             >
               Следующая →
             </button>
           </div>
         </div>
       </div>
    </main>
  );
}
