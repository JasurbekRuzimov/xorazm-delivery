import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { orderApi } from '../api/client';

const STATUS = {
  DELIVERED:  { label: 'Yetkazildi',           color: 'var(--green)',  bg: 'var(--green-dim)' },
  ON_THE_WAY: { label: "Yo'lda",               color: 'var(--blue)',   bg: 'var(--blue-dim)'  },
  ASSIGNED:   { label: 'Tayinlandi',            color: 'var(--blue)',   bg: 'var(--blue-dim)'  },
  SEARCHING:  { label: 'Kuryer qidirilmoqda',   color: 'var(--gold)',   bg: 'var(--gold-dim)'  },
  PENDING:    { label: 'Kutilmoqda',            color: 'var(--text2)',  bg: 'var(--bg3)'       },
  CANCELLED:  { label: 'Bekor qilindi',         color: 'var(--red)',    bg: 'var(--red-dim)'   },
  FAILED:     { label: 'Muvaffaqiyatsiz',       color: 'var(--red)',    bg: 'var(--red-dim)'   },
};

export default function MyOrdersPage() {
  const [orders,  setOrders]  = useState([]);
  const [loading, setLoading] = useState(true);
  const [page,    setPage]    = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [error,   setError]   = useState(null);

  useEffect(() => {
    setError(null);
    orderApi.getMyOrders(page)
      .then(({ data }) => {
        const p = data.data;
        setOrders((prev) => page === 0 ? p.content : [...prev, ...p.content]);
        setHasMore(!p.last);
      })
      .catch((err) => {
        if (err?.response?.status !== 401) setError('Buyurtmalarni yuklashda xatolik');
      })
      .finally(() => setLoading(false));
  }, [page]);

  if (loading && page === 0) return (
    <div style={s.loadWrap}>
      <div style={s.loadDot} />
      <span style={{ color: 'var(--text3)', fontSize: 14 }}>Yuklanmoqda...</span>
    </div>
  );

  if (error) return (
    <div style={s.loadWrap}>
      <span style={{ color: 'var(--red)', fontSize: 14 }}>{error}</span>
    </div>
  );

  return (
    <div style={s.page}>
      {/* Header */}
      <div style={s.pageTop}>
        <div>
          <h2 style={s.pageTitle}>Buyurtmalarim</h2>
          <p style={s.pageSub}>Barcha buyurtmalaringiz tarixi</p>
        </div>
        <Link to="/orders/new" style={s.newBtn}>+ Yangi</Link>
      </div>

      {orders.length === 0 ? (
        <div style={s.empty}>
          <div style={s.emptyIcon}>📦</div>
          <p style={s.emptyText}>Hali buyurtma yo'q</p>
          <Link to="/orders/new" style={s.emptyLink}>Birinchi buyurtma berish →</Link>
        </div>
      ) : (
        <div style={s.list}>
          {orders.map((o) => {
            const st = STATUS[o.status] || STATUS.PENDING;
            return (
              <Link key={o.id} to={`/orders/${o.id}`} style={s.card}>
                <div style={s.cardLeft}>
                  <div style={s.idBox}>
                    <span style={s.idText}>#{o.id?.slice(0, 6).toUpperCase()}</span>
                  </div>
                </div>
                <div style={s.cardMid}>
                  <div style={s.addr}>{o.deliveryAddress?.slice(0, 48) || '—'}</div>
                  <div style={s.meta}>
                    {new Date(o.createdAt).toLocaleDateString('uz-UZ')}
                    {o.distanceKm && <span> · {o.distanceKm} km</span>}
                  </div>
                </div>
                <div style={s.cardRight}>
                  <div style={s.price}>{o.totalFee?.toLocaleString()} so'm</div>
                  <span style={{ ...s.badge, color: st.color, background: st.bg }}>
                    {st.label}
                  </span>
                </div>
              </Link>
            );
          })}
        </div>
      )}

      {hasMore && (
        <button style={s.moreBtn} onClick={() => setPage((p) => p + 1)}>
          Ko'proq yuklash
        </button>
      )}
    </div>
  );
}

const s = {
  page: { maxWidth: 720, margin: '0 auto', padding: '28px 16px' },
  loadWrap: { display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10, padding: '60px 0' },
  loadDot: {
    width: 8, height: 8, borderRadius: '50%',
    background: 'var(--green)',
    boxShadow: '0 0 8px var(--green)',
    animation: 'pulse 1.5s infinite',
  },
  pageTop: {
    display: 'flex', justifyContent: 'space-between',
    alignItems: 'flex-start', marginBottom: 24,
  },
  pageTitle: { fontSize: 22, fontWeight: 800, color: 'var(--text)', letterSpacing: '-0.02em' },
  pageSub: { fontSize: 13, color: 'var(--text3)', marginTop: 3 },
  newBtn: {
    padding: '8px 16px',
    background: 'var(--green)',
    color: '#0c0f0e',
    borderRadius: 8,
    fontSize: 13, fontWeight: 700,
    flexShrink: 0,
  },
  list: { display: 'flex', flexDirection: 'column', gap: 8 },
  card: {
    display: 'flex', alignItems: 'center', gap: 14,
    background: 'var(--bg2)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-lg)',
    padding: '14px 16px',
    transition: 'border-color 0.2s, background 0.2s',
    cursor: 'pointer',
  },
  cardLeft: { flexShrink: 0 },
  idBox: {
    width: 48, height: 48,
    background: 'var(--bg3)',
    border: '1px solid var(--border2)',
    borderRadius: 10,
    display: 'flex', alignItems: 'center', justifyContent: 'center',
  },
  idText: {
    fontFamily: "'DM Mono', monospace",
    fontSize: 9, fontWeight: 500,
    color: 'var(--text2)',
    letterSpacing: '0.05em',
    lineHeight: 1.4, textAlign: 'center',
  },
  cardMid: { flex: 1, minWidth: 0 },
  addr: {
    fontSize: 14, fontWeight: 600,
    color: 'var(--text)',
    whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
  },
  meta: { fontSize: 12, color: 'var(--text3)', marginTop: 3 },
  cardRight: { textAlign: 'right', flexShrink: 0 },
  price: {
    fontFamily: "'DM Mono', monospace",
    fontSize: 15, fontWeight: 600,
    color: 'var(--text)',
  },
  badge: {
    display: 'inline-block',
    padding: '3px 9px',
    borderRadius: 100,
    fontSize: 10, fontWeight: 700,
    letterSpacing: '0.04em',
    textTransform: 'uppercase',
    marginTop: 5,
  },
  empty: {
    textAlign: 'center', padding: '60px 0',
    display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12,
  },
  emptyIcon: { fontSize: 40 },
  emptyText: { color: 'var(--text3)', fontSize: 15 },
  emptyLink: { color: 'var(--green)', fontWeight: 600, fontSize: 14 },
  moreBtn: {
    width: '100%', marginTop: 12,
    padding: '12px 0',
    background: 'transparent',
    border: '1px solid var(--border2)',
    borderRadius: 'var(--radius)',
    fontSize: 14, color: 'var(--text2)',
    cursor: 'pointer', fontWeight: 500,
  },
};
