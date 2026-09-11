import React, { useState, useRef, useEffect } from 'react';
import saigonbankLogo from '../assets/saigonbank.png';
import { authService } from '../services';
import type { JwtResponse } from '../types';
import './LoginPage.css';

interface LoginPageProps {
  onLoginSuccess: (user: JwtResponse) => void;
  showToast: (msg: string, type: 'success' | 'error' | 'warning' | 'info') => void;
}

interface OtpInputProps {
  value: string[];
  onChange: (val: string[]) => void;
}
const OtpInput: React.FC<OtpInputProps> = ({ value, onChange }) => {
  const refs = useRef<(HTMLInputElement | null)[]>([]);

  const handleChange = (idx: number, ch: string) => {
    const digit = ch.replace(/\D/g, '').slice(-1);
    const next = [...value];
    next[idx] = digit;
    onChange(next);
    if (digit && idx < 5) refs.current[idx + 1]?.focus();
  };

  const handleKeyDown = (idx: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !value[idx] && idx > 0) {
      refs.current[idx - 1]?.focus();
    }
    if (e.key === 'ArrowLeft' && idx > 0) refs.current[idx - 1]?.focus();
    if (e.key === 'ArrowRight' && idx < 5) refs.current[idx + 1]?.focus();
  };

  const handlePaste = (e: React.ClipboardEvent) => {
    const text = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (text.length === 6) {
      onChange(text.split(''));
      refs.current[5]?.focus();
    }
  };

  return (
    <div className="otp-inputs-grid">
      {value.map((digit, idx) => (
        <input
          key={idx}
          ref={(el) => { refs.current[idx] = el; }}
          type="text"
          inputMode="numeric"
          maxLength={1}
          value={digit}
          onChange={(e) => handleChange(idx, e.target.value)}
          onKeyDown={(e) => handleKeyDown(idx, e)}
          onPaste={handlePaste}
          className={`otp-digit-cell ${digit ? 'filled' : ''}`}
        />
      ))}
    </div>
  );
};

const useCountdown = (seconds: number, onExpire: () => void) => {
  const [remaining, setRemaining] = useState(seconds);
  useEffect(() => {
    setRemaining(seconds);
  }, [seconds]);
  useEffect(() => {
    if (remaining <= 0) { onExpire(); return; }
    const timer = setInterval(() => setRemaining((prev) => prev - 1), 1000);
    return () => clearInterval(timer);
  }, [remaining, onExpire]);
  const mm = String(Math.floor(remaining / 60)).padStart(2, '0');
  const ss = String(remaining % 60).padStart(2, '0');
  return { remaining, display: `${mm}:${ss}` };
};

const LeftPanel: React.FC = () => (
  <div className="login-left-panel">
    <div className="login-glow-gold" />
    <div className="login-glow-blue" />
    <div className="login-grid-overlay" />

    <div className="login-brand-content">
      <div className="login-system-tag">
        <div className="login-tag-dot" />
        <span className="login-tag-text">HỆ THỐNG QUẢN LÝ TÀI SẢN PIMS</span>
      </div>

      <h1 className="login-hero-title">
        Quản trị thông minh,<br />
        <span className="login-gradient-text">Vận hành tối ưu</span>
      </h1>

      <p className="login-hero-desc">
        Hệ thống Quản lý Ấn phẩm & Văn phòng phẩm (PIMS) hỗ trợ kiểm soát toàn diện quy trình nhập, xuất và kiểm kê tài sản.
      </p>

      <div className="login-stats-row">
        {[
          { label: 'Chi nhánh', value: '50+' },
          { label: 'Tài sản quản lý', value: '10K+' },
          { label: 'Giao dịch/ngày', value: '200+' },
        ].map((stat) => (
          <div key={stat.label}>
            <div className="login-stat-value">{stat.value}</div>
            <div className="login-stat-label">{stat.label}</div>
          </div>
        ))}
      </div>
    </div>
  </div>
);

export const LoginPage: React.FC<LoginPageProps> = ({ onLoginSuccess, showToast }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  const [step, setStep] = useState<1 | 2>(1);
  const [maskedEmail, setMaskedEmail] = useState('');
  const [otpDigits, setOtpDigits] = useState(['', '', '', '', '', '']);
  const [otpLoading, setOtpLoading] = useState(false);
  const [, setCountdownKey] = useState(0);
  const [otpExpired, setOtpExpired] = useState(false);

  const handleOtpExpire = () => setOtpExpired(true);
  const { remaining, display: countdownDisplay } = useCountdown(
    otpExpired ? 0 : 5 * 60,
    handleOtpExpire
  );

  const handleStep1 = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password) {
      showToast('Vui lòng nhập đầy đủ email và mật khẩu', 'warning');
      return;
    }
    try {
      setLoading(true);
      const res = await authService.initiateLogin(email.trim(), password);
      setMaskedEmail(res.maskedEmail);
      setOtpExpired(false);
      setOtpDigits(['', '', '', '', '', '']);
      setCountdownKey((k) => k + 1);
      setStep(2);
      showToast(`OTP đã gửi đến ${res.maskedEmail}`, 'info');
    } catch (err: any) {
      showToast(err.message || 'Đăng nhập thất bại. Kiểm tra lại thông tin.', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleStep2 = async (e: React.FormEvent) => {
    e.preventDefault();
    const otpCode = otpDigits.join('');
    if (otpCode.length < 6) {
      showToast('Vui lòng nhập đủ 6 chữ số OTP', 'warning');
      return;
    }
    if (otpExpired) {
      showToast('Mã OTP đã hết hạn. Vui lòng gửi lại.', 'warning');
      return;
    }
    try {
      setOtpLoading(true);
      const user = await authService.verifyOtp(email.trim(), otpCode);
      showToast(`Đăng nhập thành công! Xin chào ${user.fullName || user.username}`, 'success');
      onLoginSuccess(user);
    } catch (err: any) {
      showToast(err.message || 'Mã OTP không đúng hoặc đã hết hạn.', 'error');
      setOtpDigits(['', '', '', '', '', '']);
    } finally {
      setOtpLoading(false);
    }
  };

  const handleResend = async () => {
    try {
      setLoading(true);
      const res = await authService.initiateLogin(email.trim(), password);
      setMaskedEmail(res.maskedEmail);
      setOtpExpired(false);
      setOtpDigits(['', '', '', '', '', '']);
      setCountdownKey((k) => k + 1);
      showToast(`Đã gửi lại OTP đến ${res.maskedEmail}`, 'info');
    } catch (err: any) {
      showToast(err.message || 'Gửi lại OTP thất bại.', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <LeftPanel />

      {/* Right Panel */}
      <div className="login-right-panel">
        <div className="login-form-container animate-fade-in">
          {/* Logo */}
          <div className="login-logo-wrapper">
            <img src={saigonbankLogo} alt="Saigonbank Logo" className="login-logo-img" />
          </div>

          {/* ── STEP 1: Email + Password ── */}
          {step === 1 && (
            <>
              <div className="login-header-block">
                <h2 className="login-header-title">Đăng nhập hệ thống</h2>
                <p className="login-header-subtitle">Nhập email để nhận mã OTP xác thực</p>
              </div>

              <form onSubmit={handleStep1}>
                <div className="login-form-group">
                  <label htmlFor="login-email" className="login-label">
                    Email
                  </label>
                  <div className="login-input-wrapper">
                    <i className="bi bi-envelope-at login-input-icon" />
                    <input
                      id="login-email"
                      type="email"
                      placeholder="name@saigonbank.com.vn"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      autoComplete="email"
                      required
                      className="login-input"
                    />
                  </div>
                </div>

                <div className="login-form-group mb-password">
                  <label htmlFor="login-password" className="login-label">
                    Mật khẩu
                  </label>
                  <div className="login-input-wrapper">
                    <i className="bi bi-lock login-input-icon" />
                    <input
                      id="login-password"
                      type={showPassword ? 'text' : 'password'}
                      placeholder="Mật khẩu"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      autoComplete="current-password"
                      required
                      className="login-input login-input-password"
                      style={{ letterSpacing: showPassword || !password ? 'normal' : '0.15em' }}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="login-password-toggle"
                    >
                      <i className={`bi ${showPassword ? 'bi-eye-slash' : 'bi-eye'}`} />
                    </button>
                  </div>
                </div>

                <button
                  id="btn-login-submit"
                  type="submit"
                  disabled={loading}
                  className="login-btn-primary"
                >
                  {loading ? (
                    <>
                      <span className="spinner-border spinner-border-sm" role="status" />
                      <span>Đang gửi OTP...</span>
                    </>
                  ) : (
                    <>
                      <span>Tiếp tục</span>
                      <i className="bi bi-arrow-right" />
                    </>
                  )}
                </button>
              </form>
            </>
          )}

          {/* ── STEP 2: OTP Verification ── */}
          {step === 2 && (
            <>
              <div className="login-header-block">
                <div className="otp-icon-badge">
                  <i className="bi bi-shield-lock-fill" />
                </div>
                <h2 className="login-header-title">Xác thực OTP</h2>
                <p className="login-header-subtitle">
                  Mã xác thực 6 số đã gửi đến<br />
                  <strong style={{ color: '#0056A3' }}>{maskedEmail}</strong>
                </p>
              </div>

              <form onSubmit={handleStep2}>
                <div className="login-form-group">
                  <OtpInput value={otpDigits} onChange={setOtpDigits} />
                </div>

                {/* Countdown Timer */}
                <div className="otp-countdown-wrap">
                  {otpExpired ? (
                    <span className="otp-countdown-text expired">Mã OTP đã hết hạn</span>
                  ) : (
                    <span className={`otp-countdown-text ${remaining <= 60 ? 'urgent' : ''}`}>
                      Mã hết hạn sau <strong className="otp-countdown-value">{countdownDisplay}</strong>
                    </span>
                  )}
                </div>

                <button
                  id="btn-otp-submit"
                  type="submit"
                  disabled={otpLoading || otpExpired}
                  className="login-btn-primary"
                >
                  {otpLoading ? (
                    <>
                      <span className="spinner-border spinner-border-sm" role="status" />
                      <span>Đang xác thực...</span>
                    </>
                  ) : (
                    <>
                      <i className="bi bi-check-circle" />
                      <span>Xác nhận</span>
                    </>
                  )}
                </button>

                <div className="otp-actions-row">
                  <button
                    type="button"
                    onClick={() => setStep(1)}
                    className="otp-btn-back"
                  >
                    <i className="bi bi-arrow-left me-1" /> Quay lại
                  </button>
                  <button
                    type="button"
                    onClick={handleResend}
                    disabled={loading || (!otpExpired && remaining > 0 && remaining < 5 * 60 - 30)}
                    className="otp-btn-resend"
                  >
                    {loading ? (
                      <span className="spinner-border spinner-border-sm" role="status" />
                    ) : (
                      <>
                        <i className="bi bi-arrow-clockwise me-1" /> Gửi lại
                      </>
                    )}
                  </button>
                </div>
              </form>
            </>
          )}

          {/* Footer */}
          <p className="login-footer-copy">
            © 2026 Ngân hàng TMCP Sài Gòn Công Thương
          </p>
        </div>
      </div>
    </div>
  );
};
