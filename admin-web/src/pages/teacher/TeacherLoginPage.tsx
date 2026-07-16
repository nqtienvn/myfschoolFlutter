import { useState, type FormEvent } from 'react';
import { loginTeacher } from '../../api/teacher';

export default function TeacherLoginPage({ onLoggedIn }: { onLoggedIn: () => void }) {
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault(); setError(''); setLoading(true);
    try { await loginTeacher(phone.trim(), password); onLoggedIn(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Đăng nhập thất bại.'); }
    finally { setLoading(false); }
  }

  return <main className="teacher-login-page">
    <section className="teacher-login-card">
      <div className="teacher-login-brand"><span>F</span><div><strong>FPT Schools</strong><small>Cổng nghiệp vụ Giáo viên</small></div></div>
      <div className="teacher-login-copy"><p className="teacher-eyebrow">Teacher Web Portal</p><h1>Chào mừng Thầy/Cô</h1><p>Đăng nhập bằng tài khoản giáo viên đang dùng trên ứng dụng Mobile.</p></div>
      {error && <div className="notice error" role="alert">{error}</div>}
      <form onSubmit={submit} className="teacher-login-form">
        <label>Số điện thoại<input autoFocus autoComplete="username" required value={phone} onChange={event => setPhone(event.target.value)} placeholder="Nhập số điện thoại" /></label>
        <label>Mật khẩu<input type="password" autoComplete="current-password" required value={password} onChange={event => setPassword(event.target.value)} placeholder="Nhập mật khẩu" /></label>
        <button disabled={loading}>{loading ? 'Đang đăng nhập…' : 'Đăng nhập cổng Giáo viên'}</button>
      </form>
      <a href="/">Quay lại trang Quản trị</a>
    </section>
  </main>;
}
