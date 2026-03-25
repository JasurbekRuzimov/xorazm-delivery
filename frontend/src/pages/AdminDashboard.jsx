import React, { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { adminApi } from '../api/client';

export default function AdminDashboard() {
  const [dash,  setDash]  = useState(null);
  const [users, setUsers] = useState([]);
  const [msg,   setMsg]   = useState('');

  useEffect(() => {
    adminApi.getDashboard().then(({ data }) => setDash(data.data));
    adminApi.getUsers().then(({ data }) => setUsers(data.data?.content || []));
  }, []);

  const toggleActive = async (user) => {
    try {
      await adminApi.setUserActive(user.id, !user.active);
      setUsers((u) => u.map((x) => x.id === user.id ? { ...x, active: !x.active } : x));
      toast.success(user.active ? 'Bloklandi' : 'Faollashtirildi');
    } catch { toast.error('Xato'); }
  };

  const broadcast = async () => {
    if (!msg.trim()) return;
    try {
      await adminApi.broadcast(msg, null);
      toast.success('SMS yuborilmoqda...');
      setMsg('');
    } catch { toast.error('Xato'); }
  };

  const STATS = dash ? [
    { label: 'Foydalanuvchilar', value: dash.totalUsers,    icon: '👥' },
    { label: 'Premium',          value: dash.premiumSubs,   icon: '⭐', gold: true },
    { label: 'Biznes',           value: dash.businessSubs,  icon: '🏢', gold: true },
    { label: 'Faol kuryerlar',   value: dash.activeCouriers,icon: '🚴', green: true },
    { label: 'Aktiv buyurtmalar',value: dash.activeOrders,  icon: '📦', green: true },
    { label: 'Kutilayotgan',     value: dash.pendingOrders, icon: '⏳' },
  ] : [];

  return (
    <div style={s.page}>
      <div style={s.pageTop}>
        <h2 style={s.title}>Admin panel</h2>
        <div style={s.badge}>ADMIN</div>
      </div>

      {dash && (
        <div style={s.statsGrid}>
          {STATS.map(({ label, value, icon, gold, green }) => (
            <div key={label} style={s.statCard}>
              <div style={s.statIcon}>{icon}</div>
              <div style={{
                ...s.statVal,
                color: gold ? 'var(--gold)' : green ? 'var(--green)' : 'var(--text)',
              }}>{value ?? '—'}</div>
              <div style={s.statLabel}>{label}</div>
            </div>
          ))}
        </div>
      )}

      {/* Broadcast */}
      <div style={s.card}>
        <div style={s.cardTitle}>📢 Ommaviy SMS</div>
        <div style={s.msgRow}>
          <input
            style={s.msgInput}
            placeholder="Barcha foydalanuvchilarga xabar..."
            value={msg}
            onChange={(e) => setMsg(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && broadcast()}
          />
          <button style={s.sendBtn} onClick={broadcast}>Yuborish</button>
        </div>
      </div>

      {/* Users table */}
      <div style={s.card}>
        <div style={s.cardTitle}>👥 Foydalanuvchilar ({users.length})</div>
        <div style={s.tableWrap}>
          <table style={s.table}>
            <thead>
              <tr>
                {['Ism', 'Telefon', 'Rol', 'Holat', 'Amal'].map((h) => (
                  <th key={h} style={s.th}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id} style={s.tr}>
                  <td style={s.td}>{u.fullName || '—'}</td>
                  <td style={{ ...s.td, fontFamily: "'DM Mono', monospace", fontSize: 12 }}>{u.phone}</td>
                  <td style={s.td}>
                    <span style={{
                      ...s.roleBadge,
                      background: u.role === 'ADMIN' ? 'var(--gold-dim)' :
                                  u.role === 'COURIER' ? 'var(--blue-dim)' : 'var(--green-dim)',
                      color: u.role === 'ADMIN' ? 'var(--gold)' :
                             u.role === 'COURIER' ? 'var(--blue)' : 'var(--green)',
                    }}>
                      {u.role}
                    </span>
                  </td>
                  <td style={s.td}>
                    <span style={{ color: u.active ? 'var(--green)' : 'var(--red)', fontWeight: 600, fontSize: 12 }}>
                      {u.active ? '● Faol' : '○ Bloklangan'}
                    </span>
                  </td>
                  <td style={s.td}>
                    <button style={{ ...s.actionBtn, ...(u.active ? s.blockBtn : s.unblockBtn) }}
                      onClick={() => toggleActive(u)}>
                      {u.active ? 'Bloklash' : 'Ochish'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

const s = {
  page: { maxWidth: 960, margin: '0 auto', padding: '28px 16px' },
  pageTop: { display: 'flex', alignItems: 'center', gap: 12, marginBottom: 24 },
  title: { fontSize: 22, fontWeight: 800, color: 'var(--text)', letterSpacing: '-0.02em' },
  badge: {
    padding: '3px 10px',
    background: 'var(--gold-dim)',
    color: 'var(--gold)',
    borderRadius: 6,
    fontSize: 10, fontWeight: 800,
    letterSpacing: '0.1em',
  },
  statsGrid: { display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 10, marginBottom: 16 },
  statCard: {
    background: 'var(--bg2)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-lg)',
    padding: '16px', textAlign: 'center',
  },
  statIcon: { fontSize: 20, marginBottom: 8 },
  statVal: { fontSize: 26, fontWeight: 800, letterSpacing: '-0.03em', fontFamily: "'DM Mono', monospace" },
  statLabel: { fontSize: 11, color: 'var(--text3)', marginTop: 4, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em' },
  card: {
    background: 'var(--bg2)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-lg)',
    padding: '18px 20px',
    marginBottom: 14,
  },
  cardTitle: { fontSize: 13, fontWeight: 700, color: 'var(--text)', marginBottom: 14 },
  msgRow: { display: 'flex', gap: 10 },
  msgInput: { flex: 1 },
  sendBtn: {
    padding: '11px 20px',
    background: 'var(--green)',
    color: '#0c0f0e',
    border: 'none',
    borderRadius: 'var(--radius)',
    fontSize: 13, fontWeight: 700,
    cursor: 'pointer', flexShrink: 0,
  },
  tableWrap: { overflowX: 'auto' },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: 13 },
  th: {
    textAlign: 'left',
    padding: '8px 10px',
    borderBottom: '1px solid var(--border)',
    color: 'var(--text3)',
    fontWeight: 700,
    fontSize: 11,
    textTransform: 'uppercase',
    letterSpacing: '0.06em',
  },
  tr: { transition: 'background 0.15s' },
  td: { padding: '11px 10px', borderBottom: '1px solid var(--border)', color: 'var(--text2)' },
  roleBadge: {
    display: 'inline-block',
    padding: '3px 8px',
    borderRadius: 6,
    fontSize: 10, fontWeight: 800,
    letterSpacing: '0.06em',
  },
  actionBtn: {
    padding: '5px 12px',
    border: '1px solid',
    borderRadius: 7,
    fontSize: 12, cursor: 'pointer',
    fontFamily: 'inherit', fontWeight: 600,
    background: 'transparent',
  },
  blockBtn:   { borderColor: 'rgba(229,83,75,0.3)',  color: 'var(--red)' },
  unblockBtn: { borderColor: 'rgba(62,207,142,0.3)', color: 'var(--green)' },
};
