import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import Navbar from './components/common/Navbar';
import PrivateRoute from './components/common/PrivateRoute';

import LoginPage        from './pages/LoginPage';
import CreateOrderPage  from './pages/CreateOrderPage';
import MyOrdersPage     from './pages/MyOrdersPage';
import TrackingPage     from './pages/TrackingPage';
import CourierDashboard from './pages/CourierDashboard';
import AdminDashboard   from './pages/AdminDashboard';

function HomePage() {
  return (
    <div style={{ background: 'var(--bg)', minHeight: 'calc(100vh - 58px)', overflow: 'hidden' }}>
      {/* Hero */}
      <div style={h.hero}>
        <div style={h.glow} />
        <div style={h.grid} />
        <div style={h.content}>
          <div style={h.tag}>
            <span style={h.tagDot} />
            Xorazm viloyati bo'ylab
          </div>
          <h1 style={h.heading}>
            Tez va ishonchli<br />
            <span style={{ color: 'var(--green)' }}>yetkazib berish</span>
          </h1>
          <p style={h.sub}>
            Buyurtma bering, kuryeringizni real vaqtda kuzating.
          </p>
          <div style={h.btnRow}>
            <a href="/orders/new" style={h.btnPrimary}>Buyurtma berish →</a>
            <a href="/orders" style={h.btnGhost}>Tarixni ko'rish</a>
          </div>
        </div>
      </div>

      {/* Stats */}
      <div style={h.statsRow}>
        {[
          { n: '2.4k+', l: 'Yetkazilgan' },
          { n: '98%',   l: 'Muvaffaqiyat' },
          { n: '45min', l: "O'rtacha vaqt" },
        ].map(({ n, l }) => (
          <div key={l} style={h.statBox}>
            <div style={h.statNum}>{n}</div>
            <div style={h.statLabel}>{l}</div>
          </div>
        ))}
      </div>

      {/* Features */}
      <div style={h.features}>
        <div style={h.secTag}>Nima uchun biz?</div>
        <div style={h.featureGrid}>
          {[
            { icon: '📍', t: 'Real-time tracking',  d: "Kuryeringizni xaritada jonli kuzating" },
            { icon: '⚡', t: 'Tez yetkazish',        d: "O'rtacha 45 daqiqada eshigingizga" },
            { icon: '🔒', t: 'Xavfsiz to\'lov',      d: "Naqd yoki Click orqali to'lang" },
            { icon: '📦', t: 'Har qanday yuk',       d: "Kichik paketdan yirik yukkacha" },
          ].map(({ icon, t, d }) => (
            <div key={t} style={h.fCard}>
              <div style={h.fIcon}>{icon}</div>
              <div style={h.fTitle}>{t}</div>
              <div style={h.fDesc}>{d}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

const h = {
  hero: {
    padding: '72px 24px 56px',
    position: 'relative',
    overflow: 'hidden',
    maxWidth: 800,
    margin: '0 auto',
  },
  glow: {
    position: 'absolute', top: -80, right: -80,
    width: 500, height: 500, borderRadius: '50%',
    background: 'radial-gradient(circle, rgba(62,207,142,0.08) 0%, transparent 70%)',
    pointerEvents: 'none',
  },
  grid: {
    position: 'absolute', inset: 0,
    backgroundImage: 'radial-gradient(rgba(255,255,255,0.03) 1px, transparent 1px)',
    backgroundSize: '32px 32px',
    pointerEvents: 'none',
  },
  content: { position: 'relative', zIndex: 1, maxWidth: 540 },
  tag: {
    display: 'inline-flex', alignItems: 'center', gap: 8,
    background: 'var(--green-dim)',
    border: '1px solid rgba(62,207,142,0.2)',
    borderRadius: 100, padding: '5px 14px',
    fontSize: 11, fontWeight: 600, color: 'var(--green)',
    letterSpacing: '0.07em', textTransform: 'uppercase', marginBottom: 20,
  },
  tagDot: {
    width: 6, height: 6, borderRadius: '50%',
    background: 'var(--green)',
    display: 'inline-block',
    boxShadow: '0 0 8px var(--green)',
  },
  heading: {
    fontSize: 'clamp(34px,5vw,56px)',
    fontWeight: 800,
    color: 'var(--text)',
    lineHeight: 1.1,
    letterSpacing: '-0.03em',
    marginBottom: 16,
  },
  sub: { fontSize: 16, color: 'var(--text2)', lineHeight: 1.6, marginBottom: 32, maxWidth: 400 },
  btnRow: { display: 'flex', gap: 12, flexWrap: 'wrap' },
  btnPrimary: {
    display: 'inline-block',
    background: 'var(--green)',
    color: '#0c0f0e',
    padding: '13px 28px', borderRadius: 10,
    fontSize: 15, fontWeight: 700,
    transition: 'all 0.2s',
  },
  btnGhost: {
    display: 'inline-block',
    background: 'transparent',
    color: 'var(--text2)',
    border: '1px solid var(--border2)',
    padding: '13px 28px', borderRadius: 10,
    fontSize: 15, fontWeight: 500,
  },
  statsRow: {
    display: 'grid',
    gridTemplateColumns: 'repeat(3,1fr)',
    borderTop: '1px solid var(--border)',
    borderBottom: '1px solid var(--border)',
    background: 'var(--bg2)',
  },
  statBox: {
    padding: '28px 24px', textAlign: 'center',
    borderRight: '1px solid var(--border)',
  },
  statNum: {
    fontSize: 28, fontWeight: 800,
    color: 'var(--green)',
    letterSpacing: '-0.03em',
    fontFamily: "'DM Mono', monospace",
  },
  statLabel: {
    fontSize: 11, color: 'var(--text3)',
    fontWeight: 600, textTransform: 'uppercase',
    letterSpacing: '0.07em', marginTop: 4,
  },
  features: { padding: '48px 24px', maxWidth: 800, margin: '0 auto' },
  secTag: {
    fontSize: 11, fontWeight: 700, letterSpacing: '0.1em',
    textTransform: 'uppercase', color: 'var(--green)',
    marginBottom: 24,
  },
  featureGrid: { display: 'grid', gridTemplateColumns: 'repeat(2,1fr)', gap: 14 },
  fCard: {
    background: 'var(--bg2)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-lg)',
    padding: '20px',
    transition: 'border-color 0.2s',
  },
  fIcon: {
    width: 40, height: 40, borderRadius: 10,
    background: 'var(--green-dim)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    fontSize: 18, marginBottom: 12,
  },
  fTitle: { fontSize: 14, fontWeight: 700, color: 'var(--text)', marginBottom: 4 },
  fDesc: { fontSize: 13, color: 'var(--text2)', lineHeight: 1.5 },
};

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 3000,
            style: {
              background: 'var(--bg3)',
              color: 'var(--text)',
              border: '1px solid var(--border2)',
              borderRadius: '10px',
              fontSize: '14px',
            },
          }}
        />
        <Navbar />
        <main style={{ minHeight: 'calc(100vh - 58px)', background: 'var(--bg)' }}>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/" element={<HomePage />} />
            <Route path="/orders/new" element={
              <PrivateRoute roles={['CUSTOMER','FARMER']}><CreateOrderPage /></PrivateRoute>
            } />
            <Route path="/orders" element={
              <PrivateRoute><MyOrdersPage /></PrivateRoute>
            } />
            <Route path="/orders/:id" element={
              <PrivateRoute><TrackingPage /></PrivateRoute>
            } />
            <Route path="/courier" element={
              <PrivateRoute roles={['COURIER']}><CourierDashboard /></PrivateRoute>
            } />
            <Route path="/admin" element={
              <PrivateRoute roles={['ADMIN']}><AdminDashboard /></PrivateRoute>
            } />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
      </AuthProvider>
    </BrowserRouter>
  );
}
