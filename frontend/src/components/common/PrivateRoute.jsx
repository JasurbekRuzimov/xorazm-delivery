import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export default function PrivateRoute({ children, roles }) {
  const { user, loading, role } = useAuth();

  if (loading) return <div style={{ textAlign:'center', padding:60, color:'#6b8080' }}>Yuklanmoqda...</div>;

  // user null bo'lsa ham token borligini tekshiramiz
  const token = localStorage.getItem('accessToken');
  if (!user && !token) return <Navigate to="/login" replace />;

  // Rol tekshiruvi — user.role yoki localStorage.role
  const currentRole = user?.role || role;
  if (roles && !roles.includes(currentRole)) return <Navigate to="/" replace />;

  return children;
}
