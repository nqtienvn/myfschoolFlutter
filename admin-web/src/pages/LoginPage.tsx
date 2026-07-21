import { useState } from 'react';
import { login } from '../api/auth';

interface Props {
  onLogin: () => void;
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" width="18" height="18">
      <polyline points="20 6 9 17 4 12" />
    </svg>
  );
}
function ShieldIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" width="18" height="18">
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
    </svg>
  );
}
function UsersIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" width="18" height="18">
      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
      <path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </svg>
  );
}
function BarChartIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" width="18" height="18">
      <line x1="18" y1="20" x2="18" y2="10" />
      <line x1="12" y1="20" x2="12" y2="4" />
      <line x1="6" y1="20" x2="6" y2="14" />
    </svg>
  );
}

const FEATURES = [
  { icon: UsersIcon,    text: 'Quản lý giáo viên, học sinh và phụ huynh toàn diện' },
  { icon: CheckIcon,    text: 'Điểm danh tự động theo thời khóa biểu thực tế' },
  { icon: BarChartIcon, text: 'Báo cáo điểm số & hạnh kiểm theo học kỳ' },
  { icon: ShieldIcon,   text: 'Bảo mật dữ liệu và phân quyền người dùng' },
];

export default function LoginPage({ onLogin }: Props) {
  const [phone, setPhone]       = useState('');
  const [password, setPassword] = useState('');
  const [error, setError]       = useState('');
  const [loading, setLoading]   = useState(false);
  const [showPass, setShowPass] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!phone.trim() || !password.trim()) {
      setError('Vui lòng nhập đầy đủ số điện thoại và mật khẩu.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const user = await login(phone, password);
      if (user.role !== 'ADMIN') {
        setError('Tài khoản của bạn không có quyền truy cập vào cổng quản trị.');
        return;
      }
      onLogin();
    } catch (err: any) {
      setError(err.message || 'Đăng nhập thất bại. Kiểm tra lại thông tin đăng nhập.');
    } finally {
      setLoading(false);
    }
  }

  return (
    /* SEO: semantic landmark */
    <div className="login-page">

      {/* ── Left: Branding panel ─────────────────────────────────── */}
      <section className="login-brand-panel" aria-label="Giới thiệu hệ thống MyFschool">
        {/* Logo */}
        <div className="login-brand-logo">
          <div className="login-brand-logo-mark" aria-hidden="true">MF</div>
          <div className="login-brand-logo-name">
            <strong>MyFschool</strong>
            <span>Cổng Quản Trị Nhà Trường</span>
          </div>
        </div>

        {/* Hero content */}
        <div className="login-brand-content">
          <h2>Hệ thống sổ liên lạc điện tử FPT Schools</h2>
          <p>
            Quản lý toàn diện hoạt động học đường — từ cấu hình năm học, lớp học,
            đến điểm số, điểm danh và giao tiếp với phụ huynh — trên một nền tảng
            hiện đại, an toàn và dễ sử dụng.
          </p>

          {/* Feature list */}
          <ul className="login-features" role="list" aria-label="Tính năng hệ thống">
            {FEATURES.map((f, i) => {
              const Icon = f.icon;
              return (
                <li key={i} className="login-feature" role="listitem">
                  <span className="login-feature-icon" aria-hidden="true"><Icon /></span>
                  {f.text}
                </li>
              );
            })}
          </ul>
        </div>

        {/* Footer */}
        <p className="login-brand-footer">
          © {new Date().getFullYear()} MyFschool · FPT Schools · Dành cho Quản trị viên
        </p>
      </section>

      {/* ── Right: Login form ────────────────────────────────────── */}
      <section className="login-form-panel" aria-label="Đăng nhập quản trị">
        <div className="login-form-container">
          <header className="login-form-header">
            {/* SEO: H1 on login page */}
            <h1>Đăng nhập</h1>
            <p>Nhập thông tin tài khoản quản trị để tiếp tục</p>
          </header>

          {/* Error notice */}
          {error && (
            <div className="login-error" role="alert" aria-live="polite">
              <span aria-hidden="true">⚠</span>
              {error}
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} noValidate aria-label="Form đăng nhập">
            {/* Phone */}
            <div className="login-field">
              <label htmlFor="login-phone">Số điện thoại</label>
              <div className="login-field-wrap">
                <input
                  id="login-phone"
                  type="tel"
                  inputMode="tel"
                  autoComplete="username"
                  placeholder="Ví dụ: 0868589707"
                  value={phone}
                  onChange={e => setPhone(e.target.value)}
                  required
                  aria-required="true"
                  aria-describedby={error ? 'login-error' : undefined}
                  autoFocus
                />
              </div>
            </div>

            {/* Password */}
            <div className="login-field">
              <label htmlFor="login-password">Mật khẩu</label>
              <div className="login-field-wrap" style={{ position: 'relative' }}>
                <input
                  id="login-password"
                  type={showPass ? 'text' : 'password'}
                  autoComplete="current-password"
                  placeholder="••••••••"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  required
                  aria-required="true"
                  style={{ paddingRight: 44 }}
                />
                <button
                  type="button"
                  onClick={() => setShowPass(v => !v)}
                  aria-label={showPass ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  style={{
                    position: 'absolute', right: 12, top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'transparent', border: 'none',
                    color: 'var(--text-disabled)', cursor: 'pointer',
                    minHeight: 'auto', padding: 0, fontSize: 14,
                  }}
                >
                  {showPass ? '🙈' : '👁'}
                </button>
              </div>
            </div>

            {/* Submit */}
            <button
              type="submit"
              className="login-submit"
              disabled={loading}
              aria-busy={loading}
            >
              {loading ? 'Đang đăng nhập…' : 'Đăng nhập'}
            </button>
          </form>

          {/* Footer note */}
          <p className="login-footer">
            <a href="/forgot-password?portal=admin">Quên mật khẩu?</a>
          </p>
        </div>
      </section>
    </div>
  );
}
