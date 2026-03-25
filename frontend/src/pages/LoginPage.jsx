import React, { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { authApi } from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const [step, setStep]       = useState('phone');
  const [phone, setPhone]     = useState('');
  const [otp, setOtp]         = useState(['', '', '', '', '', '']);
  const [loading, setLoading] = useState(false);
  const { login }             = useAuth();
  const navigate              = useNavigate();
  const otpRefs               = useRef([]);

  const handleSendOtp = async (e) => {
    e.preventDefault();
    const formatted = phone.startsWith('+') ? phone : '+998' + phone;
    setLoading(true);
    try {
      await authApi.sendOtp(formatted);
      setPhone(formatted);
      setStep('otp');
      toast.success('SMS yuborildi!');
      setTimeout(() => otpRefs.current[0]?.focus(), 100);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Xato yuz berdi');
    } finally {
      setLoading(false);
    }
  };

  const handleOtpChange = (i, val) => {
    const v = val.replace(/\D/g, '').slice(0, 1);
    const next = [...otp];
    next[i] = v;
    setOtp(next);
    if (v && i < 5) otpRefs.current[i + 1]?.focus();
  };

  const handleOtpKey = (i, e) => {
    if (e.key === 'Backspace' && !otp[i] && i > 0) otpRefs.current[i - 1]?.focus();
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    const code = otp.join('');
    setLoading(true);
    try {
      const { data } = await authApi.verifyOtp(phone, code);
      login(data.data);
      toast.success(data.message);
      const role = data.data.role;
      if (role === 'ADMIN') navigate('/admin');
      else if (role === 'COURIER') navigate('/courier');
      else navigate('/');
    } catch (err) {
      toast.error(err.response?.data?.message || "OTP noto'g'ri");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={s.page}>
      <div style={s.card}>
        {/* Logo */}
        <div style={s.logoWrap}>
          <div style={s.logoIcon}>🚚</div>
          <div style={s.logoText}>
            Xorazm<span style={{ color: 'var(--gold)' }}>Delivery</span>
          </div>
          <p style={s.logoSub}>Xorazm bo'ylab tez yetkazib berish</p>
        </div>

        {/* Steps indicator */}
        <div style={s.steps}>
          <div style={{ ...s.stepDot, ...(step === 'phone' ? s.stepActive : s.stepDone) }} />
          <div style={s.stepLine} />
          <div style={{ ...s.stepDot, ...(step === 'otp' ? s.stepActive : {}) }} />
        </div>

        {step === 'phone' ? (
          <form onSubmit={handleSendOtp}>
            <div style={s.fieldGroup}>
              <label style={s.label}>Telefon raqam</label>
              <div style={s.phoneRow}>
                <div style={s.prefix}>+998</div>
                <input
                  style={s.phoneInput}
                  type="tel"
                  placeholder="90 123 45 67"
                  value={phone.replace('+998', '')}
                  onChange={(e) => setPhone(e.target.value.replace(/\D/g, '').slice(0, 9))}
                  maxLength={9}
                  required
                  autoFocus
                />
              </div>
            </div>
            <button style={{ ...s.btn, opacity: loading ? 0.7 : 1 }} type="submit" disabled={loading}>
              {loading ? 'Yuborilmoqda...' : 'SMS kod olish →'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleVerifyOtp}>
            <p style={s.sentMsg}>{phone} ga SMS yuborildi</p>
            <div style={s.fieldGroup}>
              <label style={s.label}>6 xonali kod</label>
              <div style={s.otpRow}>
                {otp.map((val, i) => (
                  <input
                    key={i}
                    ref={(el) => (otpRefs.current[i] = el)}
                    style={s.otpBox}
                    type="text"
                    inputMode="numeric"
                    maxLength={1}
                    value={val}
                    onChange={(e) => handleOtpChange(i, e.target.value)}
                    onKeyDown={(e) => handleOtpKey(i, e)}
                  />
                ))}
              </div>
            </div>
            <button
              style={{ ...s.btn, opacity: (loading || otp.join('').length < 6) ? 0.6 : 1 }}
              type="submit"
              disabled={loading || otp.join('').length < 6}
            >
              {loading ? 'Tekshirilmoqda...' : 'Kirish →'}
            </button>
            <button
              style={s.backBtn}
              type="button"
              onClick={() => { setStep('phone'); setOtp(['','','','','','']); }}
            >
              ← Raqamni o'zgartirish
            </button>
          </form>
        )}
      </div>
    </div>
  );
}

const s = {
  page: {
    minHeight: 'calc(100vh - 58px)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    background: 'var(--bg)',
    padding: '32px 16px',
  },
  card: {
    background: 'var(--bg2)',
    border: '1px solid var(--border2)',
    borderRadius: 20,
    padding: '40px 36px',
    width: '100%',
    maxWidth: 380,
  },
  logoWrap: { textAlign: 'center', marginBottom: 28 },
  logoIcon: {
    width: 54, height: 54,
    borderRadius: 16,
    background: 'var(--green-dim)',
    border: '1px solid rgba(62,207,142,0.2)',
    display: 'flex',
    alignItems: 'center', justifyContent: 'center',
    fontSize: 24, margin: '0 auto 12px',
  },
  logoText: { fontSize: 22, fontWeight: 800, color: 'var(--text)', letterSpacing: '-0.02em' },
  logoSub: { fontSize: 13, color: 'var(--text3)', marginTop: 4 },
  steps: { display: 'flex', alignItems: 'center', marginBottom: 28 },
  stepDot: {
    width: 8, height: 8, borderRadius: '50%',
    background: 'var(--bg4)', flexShrink: 0,
    transition: 'all 0.3s',
  },
  stepActive: { background: 'var(--green)', width: 24, borderRadius: 4, boxShadow: '0 0 8px var(--green)' },
  stepDone: { background: 'var(--green)' },
  stepLine: { flex: 1, height: 1, background: 'var(--border2)', margin: '0 8px' },
  fieldGroup: { marginBottom: 20 },
  label: {
    display: 'block',
    fontSize: 11, fontWeight: 700,
    color: 'var(--text3)',
    letterSpacing: '0.08em',
    textTransform: 'uppercase',
    marginBottom: 8,
  },
  phoneRow: { display: 'flex', gap: 8 },
  prefix: {
    padding: '11px 14px',
    background: 'var(--bg3)',
    border: '1px solid var(--border2)',
    borderRadius: 'var(--radius)',
    fontSize: 14, fontWeight: 600,
    color: 'var(--green)',
    flexShrink: 0,
    fontFamily: "'DM Mono', monospace",
  },
  phoneInput: { flex: 1 },
  otpRow: { display: 'flex', gap: 8, justifyContent: 'space-between' },
  otpBox: {
    width: 48, height: 56,
    textAlign: 'center',
    fontSize: 22, fontWeight: 700,
    fontFamily: "'DM Mono', monospace",
    background: 'var(--bg3)',
    border: '1px solid var(--border2)',
    borderRadius: 'var(--radius)',
    color: 'var(--text)',
    padding: 0,
  },
  btn: {
    width: '100%',
    padding: '14px',
    background: 'var(--green)',
    color: '#0c0f0e',
    border: 'none',
    borderRadius: 'var(--radius)',
    fontSize: 15, fontWeight: 700,
    cursor: 'pointer',
    transition: 'all 0.2s',
    letterSpacing: '0.01em',
  },
  backBtn: {
    width: '100%',
    marginTop: 12,
    padding: '8px',
    background: 'transparent',
    border: 'none',
    color: 'var(--text3)',
    fontSize: 13,
    cursor: 'pointer',
    textAlign: 'center',
  },
  sentMsg: {
    fontSize: 13,
    color: 'var(--green)',
    fontWeight: 500,
    marginBottom: 16,
    padding: '10px 14px',
    background: 'var(--green-dim)',
    borderRadius: 8,
  },
};
