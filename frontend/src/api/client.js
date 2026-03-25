import axios from 'axios';

const BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/v1';

const api = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// ── Request interceptor: har so'rovga JWT qo'shish ────────────
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  },
  (error) => Promise.reject(error)
);

// ── Response interceptor: 401 bo'lsa token yangilash ─────────
let isRefreshing = false;
let failedQueue  = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach((p) => (error ? p.reject(error) : p.resolve(token)));
  failedQueue = [];
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // 401 va retry qilinmagan bo'lsa
    if (error.response?.status === 401 && !originalRequest._retry) {

      // /auth/ endpointlari uchun qayta urinmaymiz
      if (originalRequest.url?.includes('/auth/')) {
        return Promise.reject(error);
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return api(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) {
        localStorage.clear();
        processQueue(error, null);
        isRefreshing = false;
        // Xato ko'rsatmasdan login sahifasiga o'tkazamiz
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
        // Promise ni hech qachon resolve/reject qilmaymiz — xato overlay ko'rinmaydi
        return new Promise(() => {});
      }

      try {
        const { data } = await axios.post(`${BASE_URL}/auth/refresh`, { refreshToken });
        const newToken = data.data.accessToken;
        localStorage.setItem('accessToken', newToken);
        processQueue(null, newToken);
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        localStorage.clear();
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
        return new Promise(() => {});
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default api;

// ── Auth API ──────────────────────────────────────────────────
export const authApi = {
  sendOtp:   (phone)          => api.post('/auth/send-otp', { phone }),
  verifyOtp: (phone, otp)     => api.post('/auth/verify-otp', { phone, otp }),
  refresh:   (refreshToken)   => api.post('/auth/refresh', { refreshToken }),
  logout:    ()               => api.post('/auth/logout'),
};

// ── Orders API ────────────────────────────────────────────────
export const orderApi = {
  create:       (data)                => api.post('/orders', data),
  getById:      (id)                  => api.get(`/orders/${id}`),
  getMyOrders:  (page = 0, size = 20) => api.get('/orders/my', { params: { page, size } }),
  updateStatus: (id, status)          => api.patch(`/orders/${id}/status`, { status }),
  cancel:       (id, reason)          => api.delete(`/orders/${id}/cancel`, { data: { reason } }),
  accept:       (id)                  => api.post(`/orders/${id}/accept`),
  reject:       (id)                  => api.post(`/orders/${id}/reject`),
};

// ── Price API ─────────────────────────────────────────────────
export const priceApi = {
  calculate: (data) => api.post('/price/calculate', data),
};

// ── Tracking API ──────────────────────────────────────────────
export const trackingApi = {
  getLocation:    (orderId) => api.get(`/tracking/${orderId}`),
  updateLocation: (lat, lng) => api.post('/tracking/location', { lat, lng }),
};

// ── Courier API ───────────────────────────────────────────────
export const courierApi = {
  register:      (data)    => api.post('/couriers/register', data),
  getProfile:    ()        => api.get('/couriers/me'),
  setStatus:     (online)  => api.patch('/couriers/me/status', null, { params: { online } }),
  getStats:      ()        => api.get('/couriers/me/stats'),
  requestPayout: ()        => api.post('/couriers/me/payout'),
};

// ── Payment API ───────────────────────────────────────────────
export const paymentApi = {
  initiate: (orderId, provider) => api.post('/payments/initiate', { orderId, provider }),
};

// ── User API ──────────────────────────────────────────────────
export const userApi = {
  getMe:    ()     => api.get('/users/me'),
  updateMe: (data) => api.patch('/users/me', data),
};

// ── Admin API ─────────────────────────────────────────────────
export const adminApi = {
  getDashboard:  ()               => api.get('/admin/dashboard'),
  getUsers:      (page = 0)       => api.get('/admin/users', { params: { page } }),
  setUserActive: (id, active)     => api.patch(`/admin/users/${id}/active`, null, { params: { active } }),
  verifyCourier: (id, verified)   => api.patch(`/admin/couriers/${id}/verify`, null, { params: { verified } }),
  broadcast:     (message, role)  => api.post('/admin/broadcast', { message, role }),
};
