import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi, userApi } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser]       = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    const role  = localStorage.getItem('role');
    if (!token) {
      setLoading(false);
      return;
    }
    userApi.getMe()
      .then(({ data }) => setUser(data.data))
      .catch(() => {
        // getMe muvaffaqiyatsiz — lekin token bor, role dan foydalanamiz
        // Token yaroqsizligini interceptor hal qiladi (refresh yoki logout)
        if (role) {
          setUser({ role });
        } else {
          localStorage.clear();
          setUser(null);
        }
      })
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback((authData) => {
    localStorage.setItem('accessToken',  authData.accessToken);
    localStorage.setItem('refreshToken', authData.refreshToken);
    localStorage.setItem('role',         authData.role);
    userApi.getMe()
      .then(({ data }) => setUser(data.data))
      .catch(() => {
        // getMe ishlamasa ham — role dan foydalanamiz
        setUser({ role: authData.role });
      });
  }, []);

  const logout = useCallback(async () => {
    try { await authApi.logout(); } catch {}
    localStorage.clear();
    setUser(null);
  }, []);

  const role = localStorage.getItem('role');

  return (
    <AuthContext.Provider value={{
      user, loading, login, logout, role,
      isAdmin:    role === 'ADMIN',
      isCourier:  role === 'COURIER',
      isCustomer: role === 'CUSTOMER' || role === 'FARMER',
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
