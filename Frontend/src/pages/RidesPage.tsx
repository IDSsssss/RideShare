import { Fragment, useCallback, useEffect, useMemo, useState } from "react";
import { bookingsApi, ridesApi, usersApi } from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";
import { getStoredUserId } from "../auth/session";
import type { Booking, BulkRideRequest, Ride, RideRequest, RideStatus, User } from "../types";
import { formatRideDeparture, rideShortLabel, userLabel } from "../utils/labels";
import { bookingStatusRu, rideStatusRu } from "../utils/statusLabels";

function toApiDateTime(localInput: string): string {
  const normalized = localInput.includes("T")
    ? localInput.replace("T", " ")
    : `${localInput} 00:00:00`;
  if (normalized.length === 16) return `${normalized}:00`;
  return normalized;
}

function defaultDeparture(): string {
  const d = new Date();
  d.setHours(d.getHours() + 2);
  d.setMinutes(0, 0, 0);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

const emptyRideReq = (departure = defaultDeparture()): RideRequest => ({
  departureTime: toApiDateTime(departure),
  availableSeats: 3,
  price: 500,
  route: { startPoint: "Москва", endPoint: "Тверь", distanceKm: 180, estimatedDurationMinutes: 120 },
});

export default function RidesPage() {
  const { role } = useAuth();
  const myUserId = Number.parseInt(getStoredUserId() ?? "", 10);

  const canManageRide = (r: Ride) =>
    role === "ADMIN" || (Number.isFinite(myUserId) && r.driver?.id === myUserId);
  const canCancelOwnRide = (r: Ride) =>
    Number.isFinite(myUserId) && r.driver?.id === myUserId;
  const [users, setUsers] = useState<User[]>([]);
  const [rides, setRides] = useState<Ride[]>([]);
  const [error, setError] = useState<string | null>(null);


  const [createDriverId, setCreateDriverId] = useState<number | "">("");
  const [createDeparture, setCreateDeparture] = useState(defaultDeparture());
  const [createForm, setCreateForm] = useState<RideRequest>(() => emptyRideReq());

  const [editing, setEditing] = useState<Ride | null>(null);
  const [editDeparture, setEditDeparture] = useState("");
  const [editForm, setEditForm] = useState<RideRequest | null>(null);

  const [expandedRideId, setExpandedRideId] = useState<number | null>(null);
  const [rideBookings, setRideBookings] = useState<Record<number, Booking[]>>({});
  const [bookingsLoadingId, setBookingsLoadingId] = useState<number | null>(null);

  const [showCreateForm, setShowCreateForm] = useState(false);

  const [listMode, setListMode] = useState<'all' | 'byDriver'>('all');
  const [filterDriverId, setFilterDriverId] = useState<number | ''>('');
  const [statusFilter, setStatusFilter] = useState<'' | RideStatus>('');
  const [textFilter, setTextFilter] = useState('');
  const [sortBy, setSortBy] = useState<'departure' | 'driver' | 'route' | 'seats' | 'price' | 'status'>('departure');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');

  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 10;

  const loadUsers = useCallback(async () => {
    if (role !== "ADMIN") return;
    try {
      const list = await usersApi.list();
      setUsers(list);
      setCreateDriverId((prev) => (prev === "" && list[0] ? list[0].id : prev));
    } catch {
      /* ignore */
    }
  }, [role]);

  const loadRides = useCallback(async () => {
    setError(null);
    try {
      const page = await ridesApi.searchAdvanced({
        page: 0,
        size: 10000,
        sort: "departureTime,asc",
      });
      setRides(page.content);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось загрузить поездки");
    }
  }, []);

  useEffect(() => {
    void loadUsers();
  }, [loadUsers]);

  useEffect(() => {
    void loadRides();
  }, [loadRides]);

  useEffect(() => {
    setCurrentPage(0);
  }, [statusFilter, listMode, filterDriverId, textFilter, sortBy, sortOrder]);

  const effectiveDriverId = useMemo(() => {
    if (role === "ADMIN") {
      return createDriverId === "" ? null : createDriverId;
    }
    return Number.isFinite(myUserId) && myUserId > 0 ? myUserId : null;
  }, [role, createDriverId, myUserId]);

  const createPayload = useMemo((): BulkRideRequest | null => {
    if (effectiveDriverId == null) return null;
    const ride: RideRequest = {
      ...createForm,
      departureTime: toApiDateTime(createDeparture),
      route: {
        ...createForm.route,
        distanceKm: Number(createForm.route.distanceKm),
        estimatedDurationMinutes: Number(createForm.route.estimatedDurationMinutes),
      },
    };
    return { driverId: effectiveDriverId, rides: [ride] };
  }, [effectiveDriverId, createForm, createDeparture]);

  async function createRide(e: React.FormEvent) {
    e.preventDefault();
    const body = createPayload;
    if (!body) {
      setError(
        role === "ADMIN"
          ? "Выберите водителя"
          : "Не удалось определить вашу учётную запись. Выйдите и войдите снова.",
      );
      return;
    }
    const r0 = body.rides[0];
    const dist = r0?.route?.distanceKm ?? Number.NaN;
    const mins = r0?.route?.estimatedDurationMinutes ?? Number.NaN;
    if (!Number.isFinite(dist) || dist <= 0 || !Number.isFinite(mins) || mins <= 0) {
      setError("Укажите расстояние (км) и время в пути (минуты) положительными числами.");
      return;
    }
    setError(null);
    try {
      await ridesApi.createBulk(body);
      setCreateForm(emptyRideReq(createDeparture));
      setShowCreateForm(false);
      await loadRides();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось создать поездку");
    }
  }

  function openEdit(r: Ride) {
    if (!canManageRide(r)) return;
    setEditing(r);
    const iso = r.departureTime;
    const m = iso.match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2})/);
    setEditDeparture(m ? `${m[1]}T${m[2]}` : defaultDeparture());
    setEditForm({
      departureTime: r.departureTime,
      availableSeats: r.availableSeats,
      price: r.price,
      route: {
        startPoint: r.route?.startPoint ?? "",
        endPoint: r.route?.endPoint ?? "",
        distanceKm: r.route?.distanceKm,
        estimatedDurationMinutes: r.route?.estimatedDurationMinutes,
        waypoints: r.route?.waypoints,
      },
    });
  }

  async function saveEdit(e: React.FormEvent) {
    e.preventDefault();
    if (!editing || !editForm) return;
    setError(null);
    try {
      const payload: RideRequest = {
        ...editForm,
        departureTime: toApiDateTime(editDeparture),
      };
      await ridesApi.update(editing.id, payload);
      setEditing(null);
      setEditForm(null);
      await loadRides();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось сохранить");
    }
  }

  async function removeRide(r: Ride) {
    if (!canManageRide(r)) return;
    if (!confirm(`Удалить поездку «${rideShortLabel(r)}»? Удаление возможно, если нет бронирований.`)) {
      return;
    }
    setError(null);
    try {
      await ridesApi.delete(r.id);
      await loadRides();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось удалить");
    }
  }

  async function cancelRideByDriver(r: Ride) {
    if (!canCancelOwnRide(r)) return;
    if (
      !confirm(
        "Отменить эту поездку? Активные бронирования будут отменены.",
      )
    ) {
      return;
    }
    setError(null);
    try {
      await ridesApi.cancel(r.id);
      await loadRides();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось отменить поездку");
    }
  }

  async function toggleRideBookings(rideId: number) {
    if (expandedRideId === rideId) {
      setExpandedRideId(null);
      return;
    }
    setExpandedRideId(rideId);
    if (rideBookings[rideId]) return;
    setBookingsLoadingId(rideId);
    setError(null);
    try {
      const list = await bookingsApi.byRide(rideId);
      setRideBookings((prev) => ({ ...prev, [rideId]: list }));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось загрузить бронирования");
    } finally {
      setBookingsLoadingId(null);
    }
  }

  const filtered = useMemo(() => {
    let list = rides;
    if (statusFilter) list = list.filter(r => r.status === statusFilter);
    if (listMode === 'byDriver' && filterDriverId !== '') list = list.filter(r => r.driver?.id === filterDriverId);
    if (textFilter.trim()) {
      const q = textFilter.toLowerCase();
      list = list.filter(r =>
        r.driver?.name?.toLowerCase().includes(q) ||
        r.route?.startPoint.toLowerCase().includes(q) ||
        r.route?.endPoint.toLowerCase().includes(q)
      );
    }
    const sorted = [...list].sort((a, b) => {
      let aVal: any, bVal: any;
      switch (sortBy) {
        case 'departure':
          aVal = new Date(a.departureTime).getTime();
          bVal = new Date(b.departureTime).getTime();
          break;
        case 'driver':
          aVal = a.driver?.name || '';
          bVal = b.driver?.name || '';
          break;
        case 'route':
          aVal = a.route ? `${a.route.startPoint} ${a.route.endPoint}` : '';
          bVal = b.route ? `${b.route.startPoint} ${b.route.endPoint}` : '';
          break;
        case 'seats':
          aVal = a.availableSeats;
          bVal = b.availableSeats;
          break;
        case 'price':
          aVal = a.price;
          bVal = b.price;
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
    const start = currentPage * pageSize;
    const paginated = sorted.slice(start, start + pageSize);
    return { list: paginated, total };
  }, [rides, statusFilter, listMode, filterDriverId, textFilter, sortBy, sortOrder, currentPage, pageSize]);

  return (
    <main className="page">
      <h2 className="page-title">Поездки</h2>
      <p className="page-lead">
        Расписание рейсов, водители, цены и статусы. Раскройте строку, чтобы увидеть список
        бронирований.
      </p>
      {error && <div className="alert alert-error">{error}</div>}


      <div className="card">
        <button type="button" className="btn btn-primary" onClick={() => setShowCreateForm(true)}>
          Создать поездку
        </button>
      </div>

      {showCreateForm && (
        <section className="card card-elevated create-ride-card">
          <header className="create-ride-head">
            <div>
              <h3 className="form-card-title">Новая поездка</h3>
              <p className="muted create-ride-lead">
                Укажите маршрут, расстояние и время в пути. При отсутствии совпадения в каталоге маршрут будет
                создан автоматически.
              </p>
            </div>
          </header>
          <form className="create-ride-form" onSubmit={createRide}>
            <div className="create-ride-section">
              <span className="create-ride-section-label">Участник и время</span>
              <div className="form-row">
                {role === "ADMIN" ? (
                  <label className="field grow">
                    Водитель
                    <select
                      value={createDriverId === "" ? "" : String(createDriverId)}
                      onChange={(e) =>
                        setCreateDriverId(e.target.value ? Number(e.target.value) : "")
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
                ) : (
                  <div className="field grow driver-self-card">
                    <span className="field-label-text">Водитель</span>
                    <div className="driver-self-pill">Вы — поездка от вашего имени</div>
                  </div>
                )}
                <label className="field">
                  Отправление
                  <input
                    type="datetime-local"
                    required
                    value={createDeparture}
                    onChange={(e) => {
                      setCreateDeparture(e.target.value);
                      setCreateForm((f) => ({ ...f, departureTime: toApiDateTime(e.target.value) }));
                    }}
                  />
                </label>
                <label className="field">
                  Мест
                  <input
                    type="number"
                    min={1}
                    max={8}
                    value={createForm.availableSeats}
                    onChange={(e) =>
                      setCreateForm({ ...createForm, availableSeats: Number(e.target.value) })
                    }
                  />
                </label>
                <label className="field">
                  Цена
                  <input
                    type="number"
                    min={0.01}
                    step={0.01}
                    max={10000}
                    value={createForm.price}
                    onChange={(e) =>
                      setCreateForm({ ...createForm, price: Number(e.target.value) })
                    }
                  />
                </label>
              </div>
            </div>

            <div className="create-ride-section">
              <span className="create-ride-section-label">Маршрут</span>
              <div className="form-row">
                <label className="field grow">
                  Откуда
                  <input
                    required
                    value={createForm.route.startPoint}
                    onChange={(e) =>
                      setCreateForm({
                        ...createForm,
                        route: { ...createForm.route, startPoint: e.target.value },
                      })
                    }
                  />
                </label>
                <label className="field grow">
                  Куда
                  <input
                    required
                    value={createForm.route.endPoint}
                    onChange={(e) =>
                      setCreateForm({
                        ...createForm,
                        route: { ...createForm.route, endPoint: e.target.value },
                      })
                    }
                  />
                </label>
                <label className="field">
                  Расстояние, км
                  <input
                    type="number"
                    required
                    min={0.1}
                    step={0.1}
                    value={createForm.route.distanceKm}
                    onChange={(e) =>
                      setCreateForm({
                        ...createForm,
                        route: { ...createForm.route, distanceKm: Number(e.target.value) },
                      })
                    }
                  />
                </label>
                <label className="field">
                  Время в пути, мин
                  <input
                    type="number"
                    required
                    min={1}
                    step={1}
                    value={createForm.route.estimatedDurationMinutes}
                    onChange={(e) =>
                      setCreateForm({
                        ...createForm,
                        route: { ...createForm.route, estimatedDurationMinutes: Number(e.target.value) },
                      })
                    }
                  />
                </label>
                <div className="field create-ride-submit-wrap">
                  <span className="field-label-text sr-only">Действие</span>
                  <button type="submit" className="btn btn-primary create-ride-submit">
                    Создать поездку
                  </button>
                  <button
                    type="button"
                    className="btn btn-ghost"
                    onClick={() => setShowCreateForm(false)}
                  >
                    Отмена
                  </button>
                </div>
              </div>
            </div>
          </form>
        </section>
      )}

      {editing && editForm && canManageRide(editing) && (
        <div className="card">
          <h3 className="form-card-title">Редактирование: {rideShortLabel(editing)}</h3>
          <form onSubmit={saveEdit}>
            <div className="form-row">
              <label className="field">
                Отправление
                <input
                  type="datetime-local"
                  required
                  value={editDeparture}
                  onChange={(e) => setEditDeparture(e.target.value)}
                />
              </label>
              <label className="field">
                Мест
                <input
                  type="number"
                  min={1}
                  max={8}
                  value={editForm.availableSeats}
                  onChange={(e) =>
                    setEditForm({ ...editForm, availableSeats: Number(e.target.value) })
                  }
                />
              </label>
              <label className="field">
                Цена
                <input
                  type="number"
                  min={0.01}
                  step={0.01}
                  value={editForm.price}
                  onChange={(e) => setEditForm({ ...editForm, price: Number(e.target.value) })}
                />
              </label>
            </div>
            <div className="form-row">
              <label className="field">
                Откуда
                <input
                  required
                  value={editForm.route.startPoint}
                  onChange={(e) =>
                    setEditForm({
                      ...editForm,
                      route: { ...editForm.route, startPoint: e.target.value },
                    })
                  }
                />
              </label>
              <label className="field">
                Куда
                <input
                  required
                  value={editForm.route.endPoint}
                  onChange={(e) =>
                    setEditForm({
                      ...editForm,
                      route: { ...editForm.route, endPoint: e.target.value },
                    })
                  }
                />
              </label>
              <label className="field">
                Расстояние, км
                <input
                  type="number"
                  required
                  min={0.1}
                  step={0.1}
                  value={editForm.route.distanceKm ?? ""}
                  onChange={(e) =>
                    setEditForm({
                      ...editForm,
                      route: { ...editForm.route, distanceKm: Number(e.target.value) },
                    })
                  }
                />
              </label>
              <label className="field">
                Время в пути, мин
                <input
                  type="number"
                  required
                  min={1}
                  step={1}
                  value={editForm.route.estimatedDurationMinutes ?? ""}
                  onChange={(e) =>
                    setEditForm({
                      ...editForm,
                      route: { ...editForm.route, estimatedDurationMinutes: Number(e.target.value) },
                    })
                  }
                />
              </label>
              <button type="submit" className="btn btn-primary">
                Сохранить
              </button>
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => {
                  setEditing(null);
                  setEditForm(null);
                }}
              >
                Отмена
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="card">
        <div className="form-row">
          <label className="field">
            Показать
            <select
              value={listMode}
              onChange={(e) => setListMode(e.target.value as 'all' | 'byDriver')}
            >
              <option value="all">Все поездки</option>
              <option value="byDriver">Только выбранный водитель</option>
            </select>
          </label>
          {listMode === "byDriver" && (
            <label className="field grow">
              Водитель
              <select
                value={filterDriverId === "" ? "" : String(filterDriverId)}
                onChange={(e) =>
                  setFilterDriverId(e.target.value ? Number(e.target.value) : "")
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
              onChange={(e) =>
                setStatusFilter(e.target.value as "" | RideStatus)
              }
            >
              <option value="">Все</option>
              <option value="SCHEDULED">Запланирована</option>
              <option value="IN_PROGRESS">В пути</option>
              <option value="COMPLETED">Завершена</option>
              <option value="CANCELLED">Отменена</option>
            </select>
          </label>
          <label className="field grow">
            Поиск по водителю или маршруту
            <input
              value={textFilter}
              onChange={(e) => setTextFilter(e.target.value)}
              placeholder="Имя водителя, город…"
            />
          </label>
        </div>
        <table className="data">
          <thead>
            <tr>
              <th className="col-expand" />
              <th>
                <button
                  type="button"
                  className="btn btn-ghost btn-xs"
                  onClick={() => {
                    if (sortBy === 'departure') setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
                    else { setSortBy('departure'); setSortOrder('asc'); }
                  }}
                >
                  Отправление {sortBy === 'departure' ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                </button>
              </th>
              <th>
                <button
                  type="button"
                  className="btn btn-ghost btn-xs"
                  onClick={() => {
                    if (sortBy === 'driver') setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
                    else { setSortBy('driver'); setSortOrder('asc'); }
                  }}
                >
                  Водитель {sortBy === 'driver' ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                </button>
              </th>
              <th>
                <button
                  type="button"
                  className="btn btn-ghost btn-xs"
                  onClick={() => {
                    if (sortBy === 'route') setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
                    else { setSortBy('route'); setSortOrder('asc'); }
                  }}
                >
                  Маршрут {sortBy === 'route' ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                </button>
              </th>
              <th>
                <button
                  type="button"
                  className="btn btn-ghost btn-xs"
                  onClick={() => {
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
                    if (sortBy === 'price') setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
                    else { setSortBy('price'); setSortOrder('desc'); }
                  }}
                >
                  Цена {sortBy === 'price' ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                </button>
              </th>
              <th>
                <button
                  type="button"
                  className="btn btn-ghost btn-xs"
                  onClick={() => {
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
            {filtered.list.map((r) => (
              <Fragment key={r.id}>
                <tr>
                  <td className="col-expand">
                    <button
                      type="button"
                      className="btn btn-ghost btn-expand"
                      title="Показать бронирования"
                      aria-expanded={expandedRideId === r.id}
                      onClick={() => void toggleRideBookings(r.id)}
                    >
                      {expandedRideId === r.id ? "▼" : "▶"}
                    </button>
                  </td>
                  <td>{formatRideDeparture(r.departureTime)}</td>
                  <td>{r.driver ? r.driver.name : "—"}</td>
                  <td>
                    {r.route
                      ? `${r.route.startPoint} → ${r.route.endPoint}`
                      : "—"}
                  </td>
                  <td>{r.availableSeats}</td>
                  <td>{r.price}</td>
                  <td>
                    <span className="tag">{rideStatusRu(r.status)}</span>
                  </td>
                  <td className="col-actions">
                    {canManageRide(r) && (
                      <>
                        <button type="button" className="btn btn-ghost btn-sm" onClick={() => openEdit(r)}>
                          Изменить
                        </button>{" "}
                        <button type="button" className="btn btn-danger btn-sm" onClick={() => void removeRide(r)}>
                          Удалить
                        </button>
                      </>
                    )}
                    {canCancelOwnRide(r) && r.status !== "CANCELLED" && r.status !== "COMPLETED" && (
                      <>
                        {canManageRide(r) ? " " : null}
                        <button
                          type="button"
                          className="btn btn-ghost btn-sm"
                          onClick={() => void cancelRideByDriver(r)}
                        >
                          Отменить поездку
                        </button>
                      </>
                    )}
                  </td>
                </tr>
                {expandedRideId === r.id && (
                  <tr className="nested-row">
                    <td colSpan={8}>
                      {bookingsLoadingId === r.id ? (
                        <p className="muted" style={{ margin: 0 }}>
                          Загрузка…
                        </p>
                      ) : (
                        <ul className="booking-nested-list">
                          {(rideBookings[r.id] ?? []).length === 0 ? (
                            <li className="muted">Нет бронирований</li>
                          ) : (
                            (rideBookings[r.id] ?? []).map((b) => (
                              <li key={b.id}>
                                {b.passenger?.name ?? "Пассажир"} — {b.seats}{" "}
                                {b.seats === 1 ? "место" : "мест"},{" "}
                                <span className="tag">{bookingStatusRu(b.status)}</span>
                              </li>
                            ))
                          )}
                        </ul>
                      )}
                    </td>
                  </tr>
                )}
              </Fragment>
            ))}
          </tbody>
        </table>
        <div className="pagination-controls">
          <button
            type="button"
            className="btn btn-ghost"
            onClick={() => setCurrentPage(Math.max(0, currentPage - 1))}
            disabled={currentPage === 0}
          >
            Предыдущая
          </button>
          <span className="pagination-info">
            Страница {currentPage + 1} из {Math.ceil(filtered.total / pageSize)}
          </span>
          <button
            type="button"
            className="btn btn-ghost"
            onClick={() => setCurrentPage(Math.min(Math.ceil(filtered.total / pageSize) - 1, currentPage + 1))}
            disabled={currentPage >= Math.ceil(filtered.total / pageSize) - 1}
          >
            Следующая
          </button>
        </div>
        <p className="muted pagination-hint">
          Найдено записей: {filtered.total}
        </p>
      </div>
    </main>
  );
}
