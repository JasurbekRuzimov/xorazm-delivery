import React, { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { courierApi, orderApi } from '../api/client';
import { useCourierLocation } from '../hooks/useTracking';

export default function CourierDashboard() {
  const [stats,  setStats]  = useState(null);
  const [online, setOnline] = useState(false);
  const [orders, setOrders] = useState([]);
  const { active, start, stop } = useCourierLocation();

  useEffect(() => {
    courierApi.getStats().then(({ data }) => {
      setStats(data.data);
      setOnline(data.data.isOnline);
    });
    orderApi.getMyOrders().then(({ data }) => setOrders(data.data?.content || []));
  }, []);

  const toggleOnline = async () => {
    try {
      await courierApi.setStatus(!online);
      setOnline(!online);
      if (!online) start(); else stop();
      toast.success(online ? 'Offline bo\'ldingiz' : 'Online bo\'ldingiz');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Xato');
    }
  };

  const payout = async () => {
    try {
      await courierApi.requestPayout();
      toast.success("To'lov so'rovi yuborildi!");
    } catch (err) {
      toast.error(err.response?.data?.message || 'Xato');
    }
  };

  return (
    <div style={s.page}>
      {/* Top bar */}
      <div style={s.topBar}>
        <div>
          <h2 style={s.title}>Kuryer paneli</h2>
          {active && <div style={s.gpsPill}>● GPS faol</div>}
        </div>
        <button
          style={{ ...s.onlineBtn, ...(online ? s.onlineBtnOn : s.onlineBtnOff) }}
          onClick={toggleOnline}
        >
          <span style={{ ...s.onlineDot, background: online ? '#0c0f0e' : 'var(--text3)' }} />
          {online ? 'ONLINE' : 'OFFLINE'}
        </button>
      </div>

      {/* Stats */}
      {stats && (
        <div style={s.statsGrid}>
          <StatCard label="Bugungi buyurtma" value={stats.todayOrders ?? 0} icon="📦" />
          <StatCard label="Bugungi daromad"  value={`${(stats.todayEarnings || 0).toLocaleString()}`} sub="so'm" icon="💰" gold />
          <StatCard label="Reyting"          value={`${stats.rating ?? '—'}`} sub="/ 5.0" icon="⭐" />
          <StatCard label="Balans"           value={`${(stats.balance || 0).toLocaleString()}`} sub="so'm" icon="💳" green />
        </div>
      )}

      {/* Orders list */}
      <div style={s.card}>
        <div style={s.cardTitle}>So'nggi buyurtmalar</div>
        {orders.length === 0 ? (
          <p style={{ color: 'var(--text3)', fontSize: 13, padding: '8px 0' }}>Hali buyurtma yo'q</p>
        ) : (
          orders.slice(0, 10).map((o) => (
            <div key={o.id} style={s.orderRow}>
              <div>
                <div style={s.orderId}>#{o.id?.slice(0, 8).toUpperCase()}</div>
                <div style={s.orderAddr}>{o.deliveryAddress?.slice(0, 40)}</div>
              </div>
              <div style={s.orderFee}>{o.totalFee?.toLocaleString()} so'm</div>
            </div>
          ))
        )}
      </div>

      {/* Payout */}
      <button style={s.payoutBtn} onClick={payout}>
        Pul chiqarish so'rovi → (min 50,000 so'm)
      </button>
    </div>
  );
}

function StatCard({ label, value, sub, icon, gold, green }) {
  const color = gold ? 'var(--gold)' : green ? 'var(--green)' : 'var(--text)';
  return (
    <div style={sc.card}>
      <div style={sc.icon}>{icon}</div>
      <div style={{ ...sc.value, color }}>
        {value}
        {sub && <span style={sc.sub}> {sub}</span>}
      </div>
      <div style={sc.label}>{label}</div>
    </div>
  );
}

const sc = {
  card: {
    background: 'var(--bg2)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-lg)',
    padding: '16px',
  },
  icon: { fontSize: 18, marginBottom: 10 },
  value: { fontSize: 22, fontWeight: 800, letterSpacing: '-0.02em', fontFamily: "'DM Mono', monospace" },
  sub: { fontSize: 12, fontWeight: 400, color: 'var(--text3)' },
  label: { fontSize: 11, color: 'var(--text3)', marginTop: 4, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em' },
};

const s = {
  page: { maxWidth: 680, margin: '0 auto', padding: '28px 16px' },
  topBar: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 20 },
  title: { fontSize: 22, fontWeight: 800, color: 'var(--text)', letterSpacing: '-0.02em' },
  gpsPill: {
    display: 'inline-block',
    fontSize: 11, fontWeight: 600,
    color: 'var(--green)',
    marginTop: 6,
  },
  onlineBtn: {
    display: 'flex', alignItems: 'center', gap: 7,
    padding: '9px 18px',
    border: 'none', borderRadius: 100,
    fontSize: 12, fontWeight: 800,
    cursor: 'pointer', letterSpacing: '0.06em',
    transition: 'all 0.2s',
  },
  onlineBtnOn:  { background: 'var(--green)', color: '#0c0f0e' },
  onlineBtnOff: { background: 'var(--bg3)', color: 'var(--text3)', border: '1px solid var(--border2)' },
  onlineDot: { width: 7, height: 7, borderRadius: '50%' },
  statsGrid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 16 },
  card: {
    background: 'var(--bg2)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-lg)',
    padding: '16px 18px',
    marginBottom: 12,
  },
  cardTitle: { fontSize: 13, fontWeight: 700, color: 'var(--text)', marginBottom: 14, letterSpacing: '-0.01em' },
  orderRow: {
    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
    padding: '10px 0',
    borderBottom: '1px solid var(--border)',
  },
  orderId: { fontFamily: "'DM Mono', monospace", fontSize: 12, fontWeight: 500, color: 'var(--text2)' },
  orderAddr: { fontSize: 12, color: 'var(--text3)', marginTop: 2 },
  orderFee: { fontFamily: "'DM Mono', monospace", fontSize: 13, fontWeight: 600, color: 'var(--green)' },
  payoutBtn: {
    width: '100%', padding: '13px 0',
    background: 'transparent',
    color: 'var(--text2)',
    border: '1px solid var(--border2)',
    borderRadius: 'var(--radius)',
    fontSize: 14, fontWeight: 600,
    cursor: 'pointer',
  },
};
