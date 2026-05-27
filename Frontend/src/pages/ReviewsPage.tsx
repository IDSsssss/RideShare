import { useCallback, useEffect, useMemo, useState } from "react";
import { reviewsApi, ridesApi, usersApi } from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";
import { getStoredUserId } from "../auth/session";
import type { Review, ReviewRequest, Ride, User } from "../types";
import { rideShortLabel, userLabel } from "../utils/labels";

type ReviewsSource = "ride" | "user";

function formatCreated(iso?: string): string {
  if (!iso) return "—";
  const d = new Date(iso.replace(" ", "T"));
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString("ru-RU", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function StarsDisplay({ rating }: { rating: number }) {
  return (
    <span className="stars-display" aria-label={`Оценка ${rating} из 5`}>
      {[1, 2, 3, 4, 5].map((n) => (
        <span key={n} className={n <= rating ? "star on" : "star"}>
          ★
        </span>
      ))}
    </span>
  );
}

export default function ReviewsPage() {
   const { role, displayName } = useAuth();
   const myUserId = Number.parseInt(getStoredUserId() ?? "", 10);
   const isAdmin = role === "ADMIN";

   const [rides, setRides] = useState<Ride[]>([]);
   const [users, setUsers] = useState<User[]>([]);
   const [source, setSource] = useState<ReviewsSource>("ride");
   const [rideId, setRideId] = useState<number | "">("");
   const [userId, setUserId] = useState<number | "">("");
   const [reviews, setReviews] = useState<Review[]>([]);
   const [minRating, setMinRating] = useState("");
   const [currentPage, setCurrentPage] = useState(0);
   const [error, setError] = useState<string | null>(null);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");
  const [adminReviewerId, setAdminReviewerId] = useState<number | "">("");

  const selfLabel = useMemo(() => {
    if (!Number.isFinite(myUserId)) return displayName ?? "Участник";
    const u = users.find((x) => x.id === myUserId);
    return u ? userLabel(u) : displayName ?? `Учётная запись #${myUserId}`;
  }, [users, myUserId, displayName]);

  const loadRefs = useCallback(async () => {
    try {
      const [r, u] = await Promise.all([ridesApi.list(), usersApi.list()]);
      setRides(r);
      setUsers(u);
      const firstRide = r[0]?.id;
      setRideId(firstRide ?? "");
      if (isAdmin && u[0]) {
        setAdminReviewerId(u[0].id);
      }
      if (!isAdmin && Number.isFinite(myUserId)) {
        setUserId(myUserId);
      } else {
        setUserId(u[0]?.id ?? "");
      }
    } catch {
      /* ignore */
    }
  }, [isAdmin, myUserId]);

  const loadReviews = useCallback(async () => {
    setError(null);
    try {
      if (source === "ride") {
        if (rideId === "") {
          setReviews([]);
          return;
        }
        setReviews(await reviewsApi.byRide(rideId));
      } else {
        if (userId === "") {
          setReviews([]);
          return;
        }
        setReviews(await reviewsApi.byUser(userId));
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось загрузить отзывы");
    }
  }, [source, rideId, userId]);

  useEffect(() => {
    void loadRefs();
  }, [loadRefs]);

  useEffect(() => {
    void loadReviews();
  }, [loadReviews]);

   const visibleReviews = useMemo(() => {
     const min = minRating.trim() === "" ? null : Number(minRating);
     let filtered = reviews;
     if (min != null && !Number.isNaN(min)) {
       filtered = filtered.filter((rev) => rev.rating >= min);
     }
     const pageSize = 10;
     const total = filtered.length;
     const paginated = filtered.slice(currentPage * pageSize, (currentPage + 1) * pageSize);
     return { list: paginated, total };
   }, [reviews, minRating, currentPage]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    const rid = rideId === "" ? 0 : rideId;
    if (!rid) {
      setError("Выберите поездку");
      return;
    }
    if (isAdmin && adminReviewerId === "") {
      setError("Выберите автора отзыва");
      return;
    }
    const body: ReviewRequest = {
      rating,
      rideId: rid,
      comment: comment.trim() || undefined,
    };
    if (isAdmin && adminReviewerId !== "") {
      body.reviewerId = adminReviewerId as number;
    }
    try {
      await reviewsApi.create(body);
      setComment("");
      setRating(5);
      await loadReviews();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось отправить отзыв");
    }
  }

  async function remove(rev: Review) {
    if (!confirm("Удалить этот отзыв?")) return;
    setError(null);
    try {
      await reviewsApi.delete(rev.id);
      await loadReviews();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось удалить");
    }
  }

  return (
    <main className="page reviews-page">
      <header className="section-hero section-hero--reviews">
        <h2 className="page-title">Отзывы</h2>
        <p className="page-lead">
          Оценки завершённых поездок.
        </p>
      </header>
      {error && <div className="alert alert-error">{error}</div>}

       <section className="card card-elevated reviews-browse">
         <div className="card-head">
           <h3 className="form-card-title">Просмотр</h3>
           <span className="card-head-badge">{visibleReviews.total}</span>
         </div>
         <div className="form-row">
           <label className="field">
             Показать
             <select
               value={source}
               onChange={(e) => {
                 setSource(e.target.value as ReviewsSource);
                 setCurrentPage(0);
               }}
             >
               <option value="ride">По поездке</option>
               <option value="user">По автору</option>
             </select>
           </label>
           {source === "ride" ? (
             <label className="field grow">
               Поездка
               <select
                 value={rideId === "" ? "" : String(rideId)}
                 onChange={(e) => {
                   setRideId(e.target.value ? Number(e.target.value) : "");
                   setCurrentPage(0);
                 }}
               >
                 <option value="">Выберите…</option>
                 {rides.map((r) => (
                   <option key={r.id} value={r.id}>
                     {rideShortLabel(r)}
                   </option>
                 ))}
               </select>
             </label>
           ) : (
             <label className="field grow">
               Автор
               <select
                 value={userId === "" ? "" : String(userId)}
                 onChange={(e) => {
                   setUserId(e.target.value ? Number(e.target.value) : "");
                   setCurrentPage(0);
                 }}
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
             Мин. оценка
             <input
               type="number"
               min={1}
               max={5}
               value={minRating}
               onChange={(e) => {
                 setMinRating(e.target.value);
                 setCurrentPage(0);
               }}
               placeholder="1–5"
             />
           </label>
         </div>
       </section>

      <section className="card card-elevated reviews-compose">
        <div className="card-head">
          <h3 className="form-card-title">Новый отзыв</h3>
        </div>
        <form className="reviews-form" onSubmit={submit}>
          <div className="reviews-form-grid">
            <label className="field grow">
              Поездка
              <select
                value={rideId === "" ? "" : String(rideId)}
                onChange={(e) => {
                  const v = e.target.value ? Number(e.target.value) : "";
                  setRideId(v);
                }}
              >
                <option value="">Выберите…</option>
                {rides.map((r) => (
                  <option key={r.id} value={r.id}>
                    {rideShortLabel(r)}
                  </option>
                ))}
              </select>
            </label>

            {isAdmin ? (
              <label className="field grow">
                Автор отзыва
                <select
                  value={adminReviewerId === "" ? "" : String(adminReviewerId)}
                  onChange={(e) =>
                    setAdminReviewerId(e.target.value ? Number(e.target.value) : "")
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
              <div className="field grow reviewer-self-note">
                <span className="field-label-text">Автор</span>
                <div className="reviewer-pill">{selfLabel}</div>
              </div>
            )}

            <div className="field">
              <span className="field-label-text">Оценка</span>
              <div className="rating-stars-input" role="radiogroup" aria-label="Оценка от 1 до 5">
                {[1, 2, 3, 4, 5].map((n) => (
                  <button
                    key={n}
                    type="button"
                    className={`star-btn ${n <= rating ? "on" : ""}`}
                    onClick={() => setRating(n)}
                    aria-pressed={n <= rating}
                    aria-label={`${n} из 5`}
                  >
                    ★
                  </button>
                ))}
              </div>
            </div>

            <label className="field grow reviews-comment-field">
              Комментарий
              <textarea
                rows={3}
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="Как прошла поездка?"
                maxLength={1000}
              />
            </label>

            <div className="reviews-form-actions">
              <button type="submit" className="btn btn-primary">
                Отправить отзыв
              </button>
            </div>
          </div>
        </form>
      </section>

       <section className="reviews-feed-section">
         <h3 className="form-card-title feed-title">Список</h3>
         {visibleReviews.total === 0 ? (
           <p className="muted reviews-empty">Нет отзывов по выбранным условиям.</p>
         ) : (
           <>
             <ul className="review-feed">
               {visibleReviews.list.map((rev) => (
                 <li key={rev.id} className="review-feed-item">
                   <div className="review-feed-top">
                     <StarsDisplay rating={rev.rating} />
                     <time className="review-feed-date">{formatCreated(rev.createdAt)}</time>
                   </div>
                   <p className="review-feed-comment">{rev.comment?.trim() ? rev.comment : "Без текста"}</p>
                   <div className="review-feed-meta">
                     <span className="review-meta-chip">
                       {rev.reviewer ? rev.reviewer.name : "—"}
                     </span>
                     <span className="review-meta-chip muted-chip">
                       {rev.ride ? rideShortLabel(rev.ride) : "—"}
                     </span>
                   </div>
                   <div className="review-feed-actions">
                     <button
                       type="button"
                       className="btn btn-danger btn-sm"
                       onClick={() => void remove(rev)}
                     >
                       Удалить
                     </button>
                   </div>
                 </li>
               ))}
             </ul>
             <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1rem' }}>
               <p className="muted pagination-hint">
                 Найдено записей: {visibleReviews.total}
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
                   Страница {currentPage + 1} из {Math.max(1, Math.ceil(visibleReviews.total / 10))}
                 </span>
                 <button
                   type="button"
                   className="btn btn-ghost btn-sm"
                   onClick={() =>
                     setCurrentPage((p) =>
                       Math.min(Math.ceil(visibleReviews.total / 10) - 1, p + 1)
                     )
                   }
                   disabled={currentPage >= Math.ceil(visibleReviews.total / 10) - 1}
                 >
                   Следующая →
                 </button>
               </div>
             </div>
           </>
         )}
      </section>
    </main>
  );
}
