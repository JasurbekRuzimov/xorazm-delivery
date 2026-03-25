import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export default function Navbar() {
  const { user, logout, role } = useAuth();
  const { pathname } = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);

  const links = role === 'ADMIN'
    ? [{ to: '/admin',   label: 'Dashboard' }]
    : role === 'COURIER'
    ? [{ to: '/courier', label: 'Panel' }, { to: '/orders', label: 'Buyurtmalar' }]
    : [
        { to: '/',           label: 'Bosh sahifa' },
        { to: '/orders/new', label: 'Buyurtma' },
        { to: '/orders',     label: 'Tarix' },
      ];

  return (
    <nav style={s.nav}>
      <Link to="/" style={s.brand}>
        Xorazm<span style={s.accent}>Delivery</span>
      </Link>

      <div style={s.links}>
        {links.map(({ to, label }) => (
          <Link key={to} to={to} style={{
            ...s.link,
            ...(pathname === to ? s.active : {}),
          }}>
            {label}
          </Link>
        ))}
      </div>

      <div style={s.right}>
        {user ? (
          <>
            <div style={s.userPill}>
              <div style={s.dot} />
              <span style={s.userPhone}>{user.phone || role}</span>
            </div>
            <button style={s.logoutBtn} onClick={logout}>Chiqish</button>
          </>
        ) : (
          <Link to="/login" style={s.loginBtn}>Kirish →</Link>
        )}
      </div>
    </nav>
  );
}

const s = {
  nav: {
    background: 'var(--bg2)',
    borderBottom: '1px solid var(--border)',
    display: 'flex',
    alignItems: 'center',
    padding: '0 24px',
    height: 58,
    gap: 16,
    position: 'sticky',
    top: 0,
    zIndex: 100,
    backdropFilter: 'blur(12px)',
  },
  brand: {
    fontWeight: 800,
    fontSize: 18,
    color: 'var(--green)',
    letterSpacing: '-0.02em',
    flexShrink: 0,
  },
  accent: { color: 'var(--gold)' },
  links: { display: 'flex', gap: 2, flex: 1 },
  link: {
    padding: '6px 12px',
    borderRadius: 8,
    fontSize: 14,
    fontWeight: 500,
    color: 'var(--text2)',
    transition: 'all 0.15s',
  },
  active: {
    background: 'var(--green-dim)',
    color: 'var(--green)',
  },
  right: { display: 'flex', alignItems: 'center', gap: 10 },
  dot: {
    width: 7, height: 7, borderRadius: '50%',
    background: 'var(--green)',
    boxShadow: '0 0 6px var(--green)',
    flexShrink: 0,
  },
  userPill: {
    display: 'flex',
    alignItems: 'center',
    gap: 7,
    background: 'var(--bg3)',
    border: '1px solid var(--border2)',
    borderRadius: 100,
    padding: '5px 12px',
  },
  userPhone: { fontSize: 12, color: 'var(--text2)', fontWeight: 500 },
  logoutBtn: {
    padding: '6px 14px',
    background: 'transparent',
    border: '1px solid var(--border2)',
    borderRadius: 8,
    fontSize: 13,
    cursor: 'pointer',
    color: 'var(--text3)',
    transition: 'all 0.15s',
  },
  loginBtn: {
    padding: '7px 16px',
    background: 'var(--green)',
    color: '#0c0f0e',
    borderRadius: 8,
    fontSize: 13,
    fontWeight: 700,
    transition: 'all 0.15s',
  },
};
