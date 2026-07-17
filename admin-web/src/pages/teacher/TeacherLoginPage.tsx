import { useState, type FormEvent } from 'react';
import { loginTeacher } from '../../api/teacher';

function GradeIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M4 5h16v14H4z"/><path d="M8 9h8M8 13h5"/></svg>;
}

function ReviewIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M5 4h14v16H5z"/><path d="M8 8h8M8 12h8M8 16h5"/></svg>;
}

function ChatIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4z"/><path d="M8 9h8M8 13h5"/></svg>;
}

const FEATURES = [
  { text: 'Nhập điểm và nhận xét đúng lớp được phân công', Icon: GradeIcon },
  { text: 'Quản lý công tác lớp chủ nhiệm theo học kỳ', Icon: ReviewIcon },
  { text: 'Trao đổi tập trung với phụ huynh và học sinh', Icon: ChatIcon },
];

export default function TeacherLoginPage({ onLoggedIn }: { onLoggedIn: () => void }) {
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!phone.trim() || !password.trim()) {
      setError('Vui lòng nhập đầy đủ số điện thoại và mật khẩu.');
      return;
    }

    setError('');
    setLoading(true);
    try {
      await loginTeacher(phone.trim(), password);
      onLoggedIn();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Đăng nhập thất bại.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-page teacher-login-page">
      <section className="login-brand-panel" aria-label="Giới thiệu cổng nghiệp vụ giáo viên">
        <div className="login-brand-logo">
          <div className="login-brand-logo-mark" aria-hidden="true">MF</div>
          <div className="login-brand-logo-name">
            <strong>MyFschool</strong>
            <span>Cổng Nghiệp Vụ Giáo Viên</span>
          </div>
        </div>

        <div className="login-brand-content">
          <h2>Một không gian thống nhất cho công việc giảng dạy</h2>
          <p>
            Truy cập điểm số, nhận xét, hồ sơ lớp và trao đổi học đường trên cùng
            hệ thống dữ liệu với ứng dụng MyFschool Mobile.
          </p>
          <ul className="login-features" role="list" aria-label="Tính năng dành cho giáo viên">
            {FEATURES.map(({ text, Icon }) => (
              <li key={text} className="login-feature" role="listitem">
                <span className="login-feature-icon" aria-hidden="true"><Icon /></span>
                {text}
              </li>
            ))}
          </ul>
        </div>

        <p className="login-brand-footer">
          © {new Date().getFullYear()} MyFschool · FPT Schools · Dành cho Giáo viên
        </p>
      </section>

      <section className="login-form-panel" aria-label="Đăng nhập giáo viên">
        <div className="login-form-container">
          <header className="login-form-header">
            <h1>Đăng nhập</h1>
            <p>Sử dụng tài khoản giáo viên đang dùng trên ứng dụng Mobile</p>
          </header>

          {error && (
            <div className="login-error" id="teacher-login-error" role="alert" aria-live="polite">
              <span aria-hidden="true">⚠</span>
              {error}
            </div>
          )}

          <form onSubmit={submit} noValidate aria-label="Form đăng nhập giáo viên">
            <div className="login-field">
              <label htmlFor="teacher-login-phone">Số điện thoại</label>
              <div className="login-field-wrap">
                <input
                  id="teacher-login-phone"
                  type="tel"
                  inputMode="tel"
                  autoFocus
                  autoComplete="username"
                  required
                  aria-required="true"
                  aria-describedby={error ? 'teacher-login-error' : undefined}
                  value={phone}
                  onChange={event => setPhone(event.target.value)}
                  placeholder="Nhập số điện thoại"
                />
              </div>
            </div>

            <div className="login-field">
              <label htmlFor="teacher-login-password">Mật khẩu</label>
              <div className="login-field-wrap login-password-wrap">
                <input
                  id="teacher-login-password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  required
                  aria-required="true"
                  value={password}
                  onChange={event => setPassword(event.target.value)}
                  placeholder="••••••••"
                />
                <button
                  type="button"
                  className="login-password-toggle"
                  onClick={() => setShowPassword(value => !value)}
                  aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                >
                  {showPassword ? 'Ẩn' : 'Hiện'}
                </button>
              </div>
            </div>

            <button type="submit" className="login-submit" disabled={loading} aria-busy={loading}>
              {loading ? 'Đang đăng nhập…' : 'Đăng nhập'}
            </button>
          </form>

          <p className="login-footer">
            Nếu quên mật khẩu, liên hệ quản trị hệ thống để được hỗ trợ.
            <a href="/">Quay lại cổng Quản trị</a>
          </p>
        </div>
      </section>
    </div>
  );
}
