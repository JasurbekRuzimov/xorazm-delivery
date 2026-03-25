import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { orderApi } from '../api/client';
import { useTracking } from '../hooks/useTracking';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl:       'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl:     'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

const STEPS = ['SEARCHING','ASSIGNED','PICKED_UP','ON_THE_WAY','DELIVERED'];
const STATUS = {
  PENDING:    { label: 'Kutilmoqda',           color: 'var(--text2)',  bg: 'var(--bg3)'      },
  SEARCHING:  { label: 'Kuryer qidirilmoqda',  color: 'var(--gold)',   bg: 'var(--gold-dim)' },
  ASSIGNED:   { label: 'Kuryer tayinlandi',    color: 'var(--blue)',   bg: 'var(--blue-dim)' },
  PICKED_UP:  { label: 'Olib ketildi',         color: 'var(--blue)',   bg: 'var(--blue-dim)' },
  ON_THE_WAY: { label: "Yo'lda",               color: 'var(--blue)',   bg: 'var(--blue-dim)' },
  DELIVERED:  { label: '✓ Yetkazildi',         color: 'var(--green)',  bg: 'var(--green-dim)' },
  CANCELLED:  { label: 'Bekor qilindi',        color: 'var(--red)',    bg: 'var(--red-dim)'  },
  FAILED:     { label: 'Muvaffaqiyatsiz',      color: 'var(--red)',    bg: 'var(--red-dim)'  },
};

export default function TrackingPage() {
  const { id }                          = useParams();
  const [order, setOrder]               = useState(null);
  const [loading, setLoading]           = useState(true);
  const { location, connected }         = useTracking(id);

  useEffect(() => {
    orderApi.getById(id)
      .then(({ data }) => setOrder(data.data))
      .catch((err) => { if (err?.response?.status !== 401) setLoading(false); })
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return (
    <div style={s.center}>
      <div style={s.spinner} />
    </div>
  );
  if (!order) return (
    <div style={s.center}><span style={{ color: 'var(--text3)' }}>Buyurtma topilmadi</span></div>
  );

  const st      = STATUS[order.status] || STATUS.PENDING;
  const stepIdx = STEPS.indexOf(order.status);
  const courierPos  = location?.available ? [location.lat, location.lng] : null;
  const pickupPos   = [order.pickupLat,   order.pickupLng];
  const deliveryPos = [order.deliveryLat, order.deliveryLng];
  const mapCenter   = courierPos || pickupPos;

  return (
    <div style={s.page}>
      {/* Header */}
      <div style={s.header}>
        <div>
          <div style={s.orderId}>#{order.id?.slice(0, 8).toUpperCase()}</div>
          <span style={{ ...s.statusBadge, color: st.color, background: st.bg }}>{st.label}</span>
        </div>
        <div style={s.priceWrap}>
          <div style={s.price}>{order.totalFee?.toLocaleString()}</div>
          <div style={s.priceSub}>so'm</div>
        </div>
      </div>

      {/* Progress */}
      <div style={s.progressWrap}>
        {STEPS.map((step, i) => (
          <React.Fragment key={step}>
            <div style={s.stepWrap}>
              <div style={{
                ...s.stepDot,
                background: i < stepIdx ? 'var(--green)' : i === stepIdx ? 'var(--gold)' : 'var(--bg4)',
                boxShadow: i === stepIdx ? '0 0 10px var(--gold)' : i < stepIdx ? '0 0 8px var(--green)' : 'none',
              }}>
                {i < stepIdx && <span style={{ fontSize: 9, color: '#0c0f0e', fontWeight: 800 }}>✓</span>}
              </div>
            </div>
            {i < STEPS.length - 1 && (
              <div style={{
                ...s.stepLine,
                background: i < stepIdx ? 'var(--green)' : 'var(--border2)',
              }} />
            )}
          </React.Fragment>
        ))}
      </div>

      {/* Map */}
      <div style={s.mapWrap}>
        <MapContainer center={mapCenter} zoom={13} style={{ height: '100%', width: '100%' }}>
          <TileLayer
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            attribution='&copy; OpenStreetMap'
          />
          <Marker position={pickupPos}><Popup>📦 Pickup: {order.pickupAddress}</Popup></Marker>
          <Marker position={deliveryPos}><Popup>🏠 Yetkazish: {order.deliveryAddress}</Popup></Marker>
          {courierPos && <Marker position={courierPos}><Popup>🚴 Kuryer {connected ? '(live)' : ''}</Popup></Marker>}
        </MapContainer>
      </div>

      {/* Details */}
      <div style={s.detailCard}>
        {[
          { l: 'Pickup',     v: order.pickupAddress },
          { l: 'Yetkazish',  v: order.deliveryAddress },
          { l: 'Masofa',     v: order.distanceKm ? `${order.distanceKm} km` : '—' },
          { l: 'WebSocket',  v: connected ? '● Ulangan' : '○ Ulanmagan', highlight: connected },
        ].map(({ l, v, highlight }) => (
          <div key={l} style={s.detailRow}>
            <span style={s.detailLabel}>{l}</span>
            <span style={{ ...s.detailVal, ...(highlight ? { color: 'var(--green)' } : {}) }}>{v}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

const s = {
  page:   { maxWidth: 680, margin: '0 auto', padding: '24px 16px' },
  center: { display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '40vh', gap: 12 },
  spinner: {
    width: 28, height: 28, borderRadius: '50%',
    border: '2px solid var(--border2)',
    borderTopColor: 'var(--green)',
    animation: 'spin 0.8s linear infinite',
  },
  header: {
    display: 'flex', justifyContent: 'space-between',
    alignItems: 'flex-start', marginBottom: 20,
  },
  orderId: {
    fontFamily: "'DM Mono', monospace",
    fontSize: 18, fontWeight: 600,
    color: 'var(--text)', letterSpacing: '0.05em',
  },
  statusBadge: {
    display: 'inline-block',
    padding: '4px 12px', borderRadius: 100,
    fontSize: 11, fontWeight: 700,
    textTransform: 'uppercase', letterSpacing: '0.05em',
    marginTop: 6,
  },
  priceWrap: { textAlign: 'right' },
  price: {
    fontFamily: "'DM Mono', monospace",
    fontSize: 26, fontWeight: 700,
    color: 'var(--green)', letterSpacing: '-0.02em',
  },
  priceSub: { fontSize: 12, color: 'var(--text3)', marginTop: 2 },
  progressWrap: {
    display: 'flex', alignItems: 'center',
    marginBottom: 20,
  },
  stepWrap: { display: 'flex', alignItems: 'center', flexShrink: 0 },
  stepDot: {
    width: 26, height: 26, borderRadius: '50%',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    transition: 'all 0.3s',
  },
  stepLine: { flex: 1, height: 2, borderRadius: 2, transition: 'background 0.3s' },
  mapWrap: {
    height: 280,
    borderRadius: 'var(--radius-lg)',
    overflow: 'hidden',
    marginBottom: 16,
    border: '1px solid var(--border)',
  },
  detailCard: {
    background: 'var(--bg2)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-lg)',
    padding: '4px 16px',
  },
  detailRow: {
    display: 'flex', justifyContent: 'space-between',
    padding: '11px 0',
    borderBottom: '1px solid var(--border)',
    fontSize: 13,
  },
  detailLabel: { color: 'var(--text3)' },
  detailVal: { color: 'var(--text2)', fontWeight: 500, textAlign: 'right', maxWidth: '60%' },
};
