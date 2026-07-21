import { useEffect, useState, type FormEvent, type ReactNode } from 'react';
import {
  confirmPasswordReset,
  requestPasswordReset,
  validatePasswordReset,
  type PasswordResetValidation,
} from '../api/auth';

function backUrl() {
  const portal = new URLSearchParams(window.location.search).get('portal');
  return portal === 'teacher' ? '/teacher/login' : '/';
}

export function ForgotPasswordPage() {
  const [phone, setPhone] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!/^0\d{9}$/.test(phone.trim())) {
      setError('Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      await requestPasswordReset(phone.trim());
      setSent(true);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không thể gửi yêu cầu. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  }

  return <RecoveryShell title="Quên mật khẩu" description="Khôi phục tài khoản Phụ huynh, Học sinh hoặc Giáo viên qua email đã xác minh.">
    {sent ? <div className="recovery-state success" role="status">
      <span aria-hidden="true">✓</span>
      <h2>Đã tiếp nhận yêu cầu</h2>
      <p>Nếu tài khoản đủ điều kiện, hướng dẫn đặt lại mật khẩu sẽ được gửi qua email. Vui lòng kiểm tra cả thư mục spam.</p>
      <a className="login-submit recovery-link-button" href={backUrl()}>Quay lại đăng nhập</a>
    </div> : <form onSubmit={submit} noValidate>
      {error && <div className="login-error" role="alert">{error}</div>}
      <div className="login-field">
        <label htmlFor="recovery-phone">Số điện thoại đăng nhập</label>
        <div className="login-field-wrap">
          <input id="recovery-phone" type="tel" inputMode="numeric" autoComplete="username"
            value={phone} onChange={event => setPhone(event.target.value.replace(/\D/g, '').slice(0, 10))}
            placeholder="09xxxxxxxx" autoFocus required />
        </div>
      </div>
      <button type="submit" className="login-submit" disabled={submitting}>
        {submitting ? 'Đang gửi…' : 'Gửi hướng dẫn qua email'}
      </button>
      <p className="login-footer recovery-footer"><a href={backUrl()}>Quay lại đăng nhập</a></p>
    </form>}
  </RecoveryShell>;
}

export function ResetPasswordPage() {
  const token = new URLSearchParams(window.location.hash.replace(/^#/, '')).get('token') || '';
  const [validation, setValidation] = useState<PasswordResetValidation | null>(null);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    if (!token) { setValidation({ valid: false, status: 'INVALID' }); return; }
    validatePasswordReset(token)
      .then(result => { if (active) setValidation(result); })
      .catch(() => { if (active) setValidation({ valid: false, status: 'INVALID' }); });
    return () => { active = false; };
  }, [token]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (newPassword.length < 8) { setError('Mật khẩu mới phải có ít nhất 8 ký tự.'); return; }
    if (newPassword !== confirmPassword) { setError('Mật khẩu xác nhận không khớp.'); return; }
    setSubmitting(true);
    setError('');
    try {
      await confirmPasswordReset(token, newPassword, confirmPassword);
      setSuccess(true);
      window.history.replaceState({}, '', window.location.pathname);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không thể đặt lại mật khẩu.');
      setValidation(await validatePasswordReset(token).catch(() => ({ valid: false, status: 'INVALID' as const })));
    } finally {
      setSubmitting(false);
    }
  }

  const invalidMessage: Record<string, string> = {
    INVALID: 'Link đặt lại mật khẩu không hợp lệ.',
    EXPIRED: 'Link đặt lại mật khẩu đã hết hạn.',
    USED: 'Link đặt lại mật khẩu đã được sử dụng hoặc đã bị thay thế.',
    DISABLED: 'Tính năng đặt lại mật khẩu qua email hiện chưa được bật.',
  };

  return <RecoveryShell title="Đặt mật khẩu mới" description="Link chỉ dùng một lần và có hiệu lực trong 15 phút.">
    {!validation && <div className="recovery-state"><span className="loading-dot">•••</span><p>Đang kiểm tra link…</p></div>}
    {success && <div className="recovery-state success" role="status"><span>✓</span><h2>Đổi mật khẩu thành công</h2>
      <p>Các phiên đăng nhập cũ đã hết hiệu lực. Hãy quay lại ứng dụng MyFschool hoặc cổng Giáo viên để đăng nhập bằng mật khẩu mới.</p>
      <a className="login-submit recovery-link-button" href="/teacher/login">Đăng nhập cổng Giáo viên</a></div>}
    {!success && validation && !validation.valid && <div className="recovery-state error" role="alert"><span>!</span>
      <h2>Không thể sử dụng link</h2><p>{invalidMessage[validation.status] || invalidMessage.INVALID}</p>
      <a className="login-submit recovery-link-button" href="/forgot-password">Yêu cầu link mới</a></div>}
    {!success && validation?.valid && <form onSubmit={submit} noValidate>
      {error && <div className="login-error" role="alert">{error}</div>}
      <div className="login-field"><label htmlFor="new-password">Mật khẩu mới</label><div className="login-field-wrap">
        <input id="new-password" type="password" autoComplete="new-password" minLength={8} maxLength={100}
          value={newPassword} onChange={event => setNewPassword(event.target.value)} autoFocus required />
      </div></div>
      <div className="login-field"><label htmlFor="confirm-password">Xác nhận mật khẩu</label><div className="login-field-wrap">
        <input id="confirm-password" type="password" autoComplete="new-password" minLength={8} maxLength={100}
          value={confirmPassword} onChange={event => setConfirmPassword(event.target.value)} required />
      </div></div>
      <button type="submit" className="login-submit" disabled={submitting}>{submitting ? 'Đang cập nhật…' : 'Đặt mật khẩu mới'}</button>
    </form>}
  </RecoveryShell>;
}

function RecoveryShell({ title, description, children }: { title: string; description: string; children: ReactNode }) {
  return <div className="login-page recovery-page">
    <section className="login-brand-panel" aria-label="MyFschool"><div className="login-brand-logo">
      <div className="login-brand-logo-mark" aria-hidden="true">MF</div><div className="login-brand-logo-name"><strong>MyFschool</strong><span>Bảo mật tài khoản</span></div>
    </div><div className="login-brand-content"><h2>Tài khoản của bạn, được bảo vệ đúng cách</h2>
      <p>MyFschool chỉ gửi hướng dẫn đến email đã xác minh và không tiết lộ tài khoản có tồn tại hay không.</p></div></section>
    <section className="login-form-panel"><div className="login-form-container"><header className="login-form-header"><h1>{title}</h1><p>{description}</p></header>{children}</div></section>
  </div>;
}
