import React, { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { orderApi, priceApi } from '../api/client';

export default function CreateOrderPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [price,   setPrice]   = useState(null);
  const [form, setForm] = useState({
    pickupAddress:   '',
    pickupLat:       41.5500,
    pickupLng:       60.6200,
    deliveryAddress: '',
    deliveryLat:     41.3775,
    deliveryLng:     60.3619,
    weightKg:        1,
    fragile:         false,
    description:     '',
  });

  const set = (key) => (e) =>
    setForm((f) => ({ ...f, [key]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }));

  const calcPrice = useCallback(async () => {
    try {
      const { data } = await priceApi.calculate({
        pickupLat:   Number(form.pickupLat),
        pickupLng:   Number(form.pickupLng),
        deliveryLat: Number(form.deliveryLat),
        deliveryLng: Number(form.deliveryLng),
        weightKg:    Number(form.weightKg),
      });
      setPrice(data.data);
    } catch {
      toast.error('Narxni hisoblashda xato');
    }
  }, [form]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const { data } = await orderApi.create({
        ...form,
        pickupLat:   Number(form.pickupLat),
        pickupLng:   Number(form.pickupLng),
        deliveryLat: Number(form.deliveryLat),
        deliveryLng: Number(form.deliveryLng),
        weightKg:    Number(form.weightKg),
      });
      toast.success('Buyurtma yaratildi!');
      navigate(`/orders/${data.data.id}`);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Xato yuz berdi');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={s.page}>
      <div style={s.pageTop}>
        <h2 style={s.pageTitle}>Yangi buyurtma</h2>
        <p style={s.pageSub}>Manzillarni kiriting va buyurtma bering</p>
      </div>

      <form onSubmit={handleSubmit}>
        {/* Addresses */}
        <div style={s.section}>
          <div style={s.sectionLabel}>📍 Manzillar</div>
          <div style={s.field}>
            <label style={s.label}>Olib ketish manzili *</label>
            <input value={form.pickupAddress} onChange={set('pickupAddress')}
              placeholder="Urgench, Al-Xorazmiy 12" required />
          </div>
          <div style={s.twoCol}>
            <div style={s.field}>
              <label style={s.label}>Lat</label>
              <input type="number" step="0.0001" value={form.pickupLat} onChange={set('pickupLat')} />
            </div>
            <div style={s.field}>
              <label style={s.label}>Lng</label>
              <input type="number" step="0.0001" value={form.pickupLng} onChange={set('pickupLng')} />
            </div>
          </div>
          <div style={{ ...s.field, marginTop: 8 }}>
            <div style={s.divider}><div style={s.divLine} /><span style={s.divText}>→</span><div style={s.divLine} /></div>
          </div>
          <div style={s.field}>
            <label style={s.label}>Yetkazish manzili *</label>
            <input value={form.deliveryAddress} onChange={set('deliveryAddress')}
              placeholder="Xiva, Polvon Qori 5" required />
          </div>
          <div style={s.twoCol}>
            <div style={s.field}>
              <label style={s.label}>Lat</label>
              <input type="number" step="0.0001" value={form.deliveryLat} onChange={set('deliveryLat')} />
            </div>
            <div style={s.field}>
              <label style={s.label}>Lng</label>
              <input type="number" step="0.0001" value={form.deliveryLng} onChange={set('deliveryLng')} />
            </div>
          </div>
        </div>

        {/* Package */}
        <div style={s.section}>
          <div style={s.sectionLabel}>📦 Yuk ma'lumotlari</div>
          <div style={s.twoCol}>
            <div style={s.field}>
              <label style={s.label}>Og'irlik (kg)</label>
              <input type="number" min="0.1" step="0.5" value={form.weightKg} onChange={set('weightKg')} required />
            </div>
            <div style={s.field}>
              <label style={s.label}>Tavsif</label>
              <input value={form.description} onChange={set('description')} placeholder="Meva, quti..." />
            </div>
          </div>
          <label style={s.checkRow}>
            <div style={{ ...s.checkbox, ...(form.fragile ? s.checkboxOn : {}) }}>
              {form.fragile && <span style={s.checkMark}>✓</span>}
            </div>
            <input type="checkbox" checked={form.fragile} onChange={set('fragile')} style={{ display: 'none' }} />
            <span style={s.checkLabel}>Mo'rt buyum — ehtiyot bilan yetkazish</span>
          </label>
        </div>

        {/* Price box */}
        {price && (
          <div style={s.priceBox}>
            <div style={s.priceRow}>
              <span style={s.pLabel}>Masofa</span>
              <span style={s.pVal}>{price.distanceKm} km</span>
            </div>
            <div style={s.priceRow}>
              <span style={s.pLabel}>Og'irlik</span>
              <span style={s.pVal}>{form.weightKg} kg</span>
            </div>
            {price.isNightRate && (
              <div style={s.priceRow}>
                <span style={s.pLabel}>Tunda</span>
                <span style={{ ...s.pVal, color: 'var(--gold)' }}>+30%</span>
              </div>
            )}
            {price.isBulkDiscount && (
              <div style={s.priceRow}>
                <span style={s.pLabel}>Chegirma</span>
                <span style={{ ...s.pVal, color: 'var(--green)' }}>-15%</span>
              </div>
            )}
            <div style={{ ...s.priceRow, ...s.priceTotal }}>
              <span>Jami narx</span>
              <span style={s.totalVal}>{price.totalFee?.toLocaleString()} so'm</span>
            </div>
          </div>
        )}

        {/* Buttons */}
        <div style={s.btnRow}>
          <button type="button" style={s.btnSecondary} onClick={calcPrice}>
            Narx hisoblash
          </button>
          <button type="submit" style={{ ...s.btnPrimary, opacity: loading ? 0.7 : 1 }} disabled={loading}>
            {loading ? 'Yuborilmoqda...' : '✓ Buyurtma berish'}
          </button>
        </div>
      </form>
    </div>
  );
}

const s = {
  page: { maxWidth: 720, margin: '0 auto', padding: '28px 16px' },
  pageTop: { marginBottom: 24 },
  pageTitle: { fontSize: 22, fontWeight: 800, color: 'var(--text)', letterSpacing: '-0.02em' },
  pageSub: { fontSize: 13, color: 'var(--text3)', marginTop: 3 },
  section: {
    background: 'var(--bg2)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-lg)',
    padding: '20px',
    marginBottom: 12,
  },
  sectionLabel: {
    fontSize: 11, fontWeight: 700,
    textTransform: 'uppercase', letterSpacing: '0.09em',
    color: 'var(--green)', marginBottom: 16,
  },
  field: { marginBottom: 12 },
  label: {
    display: 'block',
    fontSize: 11, fontWeight: 700,
    color: 'var(--text3)',
    textTransform: 'uppercase', letterSpacing: '0.07em',
    marginBottom: 6,
  },
  twoCol: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 },
  divider: { display: 'flex', alignItems: 'center', gap: 10, margin: '4px 0 12px' },
  divLine: { flex: 1, height: 1, background: 'var(--border)' },
  divText: { color: 'var(--text3)', fontSize: 14 },
  checkRow: {
    display: 'flex', alignItems: 'center', gap: 10,
    cursor: 'pointer', marginTop: 4,
  },
  checkbox: {
    width: 18, height: 18,
    border: '1.5px solid var(--border2)',
    borderRadius: 5,
    background: 'var(--bg3)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    flexShrink: 0, transition: 'all 0.15s',
  },
  checkboxOn: { background: 'var(--green)', borderColor: 'var(--green)' },
  checkMark: { fontSize: 11, color: '#0c0f0e', fontWeight: 700 },
  checkLabel: { fontSize: 14, color: 'var(--text2)' },
  priceBox: {
    background: 'var(--bg2)',
    border: '1px solid rgba(62,207,142,0.2)',
    borderRadius: 'var(--radius-lg)',
    padding: '16px 20px',
    marginBottom: 12,
  },
  priceRow: {
    display: 'flex', justifyContent: 'space-between',
    padding: '7px 0',
    borderBottom: '1px solid var(--border)',
    fontSize: 14,
  },
  priceTotal: {
    borderBottom: 'none',
    paddingTop: 12, marginTop: 4,
    fontSize: 15, fontWeight: 700,
    color: 'var(--text)',
  },
  pLabel: { color: 'var(--text3)' },
  pVal: { fontFamily: "'DM Mono', monospace", color: 'var(--text2)' },
  totalVal: {
    fontFamily: "'DM Mono', monospace",
    color: 'var(--green)', fontSize: 18, fontWeight: 700,
  },
  btnRow: { display: 'flex', gap: 10 },
  btnPrimary: {
    flex: 2,
    padding: '13px 0',
    background: 'var(--green)',
    color: '#0c0f0e',
    border: 'none',
    borderRadius: 'var(--radius)',
    fontSize: 15, fontWeight: 700,
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
  btnSecondary: {
    flex: 1,
    padding: '13px 0',
    background: 'transparent',
    color: 'var(--green)',
    border: '1px solid rgba(62,207,142,0.3)',
    borderRadius: 'var(--radius)',
    fontSize: 14, fontWeight: 600,
    cursor: 'pointer',
  },
};
