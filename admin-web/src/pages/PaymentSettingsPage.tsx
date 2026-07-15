import { type FormEvent, useEffect, useRef, useState } from 'react';
import {
  getPaymentConfiguration,
  updatePaymentConfiguration,
  type PaymentConfigurationInput,
} from '../api/paymentConfiguration';
import PaymentReconciliationPanel from '../components/PaymentReconciliationPanel';

const EMPTY_FORM: PaymentConfigurationInput = {
  bankCode: '',
  bankName: '',
  accountNumber: '',
  accountHolder: '',
  branch: '',
  transferContentTemplate: 'MFS {studentCode} {semester}',
  enabled: true,
};

interface Props {
  selectedYearId: string;
  selectedYearStatus?: string;
}

export default function PaymentSettingsPage({ selectedYearId, selectedYearStatus }: Props) {
  const [form, setForm] = useState<PaymentConfigurationInput>(EMPTY_FORM);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [configured, setConfigured] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const selectedYearRef = useRef(selectedYearId);

  useEffect(() => {
    selectedYearRef.current = selectedYearId;
    let alive = true;
    setForm(EMPTY_FORM);
    setConfigured(false);
    setError('');
    setMessage('');
    setLoading(false);
    if (!selectedYearId) return () => { alive = false; };

    setLoading(true);
    void getPaymentConfiguration(selectedYearId)
      .then(configuration => {
        if (!alive || !configuration) return;
        setConfigured(true);
        setForm({
          bankCode: configuration.bankCode ?? '',
          bankName: configuration.bankName,
          accountNumber: configuration.accountNumber,
          accountHolder: configuration.accountHolder,
          branch: configuration.branch ?? '',
          transferContentTemplate: configuration.transferContentTemplate,
          enabled: configuration.enabled,
        });
      })
      .catch((reason: Error) => {
        if (alive) setError(reason.message);
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => { alive = false; };
  }, [selectedYearId]);

  const patch = <K extends keyof PaymentConfigurationInput>(
    key: K,
    value: PaymentConfigurationInput[K],
  ) => setForm(current => ({ ...current, [key]: value }));

  async function save(event: FormEvent) {
    event.preventDefault();
    setError('');
    setMessage('');
    if (!selectedYearId) {
      setError('Hãy chọn năm học ở header trước khi lưu.');
      return;
    }
    if (!/^\d{6,30}$/.test(form.accountNumber.trim())) {
      setError('Số tài khoản phải gồm 6 đến 30 chữ số.');
      return;
    }
    if (!form.transferContentTemplate.includes('{studentCode}')) {
      setError('Nội dung chuyển khoản phải chứa biến {studentCode}.');
      return;
    }

    const yearBeingSaved = selectedYearId;
    setSaving(true);
    try {
      const saved = await updatePaymentConfiguration(yearBeingSaved, {
        ...form,
        bankCode: form.bankCode.trim().toUpperCase(),
        bankName: form.bankName.trim(),
        accountNumber: form.accountNumber.trim(),
        accountHolder: form.accountHolder.trim().toUpperCase(),
        branch: form.branch.trim(),
        transferContentTemplate: form.transferContentTemplate.trim(),
      });
      if (yearBeingSaved !== selectedYearRef.current) return;
      setConfigured(true);
      setForm({
        bankCode: saved.bankCode ?? '',
        bankName: saved.bankName,
        accountNumber: saved.accountNumber,
        accountHolder: saved.accountHolder,
        branch: saved.branch ?? '',
        transferContentTemplate: saved.transferContentTemplate,
        enabled: saved.enabled,
      });
      setMessage('Đã lưu cấu hình chuyển khoản cho năm học đang chọn.');
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Không thể lưu cấu hình.');
    } finally {
      setSaving(false);
    }
  }

  const readOnly = selectedYearStatus === 'COMPLETED';

  return (
    <main className="page-stack" role="main" aria-label="Cấu hình thanh toán">
      <section className="page-heading">
        <div>
          <span className="eyebrow">Thanh toán</span>
          <h1>Tài khoản nhận chuyển khoản</h1>
          <p>Thông tin được lưu riêng theo năm học và hiển thị trực tiếp trên app phụ huynh, học sinh.</p>
        </div>
      </section>

      {!selectedYearId && <div className="notice warning">Hãy chọn một năm học ở header để xem cấu hình.</div>}
      {loading && <div className="notice info">Đang tải cấu hình thanh toán…</div>}
      {error && <div className="notice error" role="alert">{error}</div>}
      {message && <div className="notice success" role="status">{message}</div>}
      {selectedYearId && !loading && !configured && !error && (
        <div className="notice warning">
          Năm học này chưa có tài khoản nhận chuyển khoản. App sẽ chưa cho người dùng gửi xác nhận thanh toán.
        </div>
      )}
      <div className="notice info">
        Hiện tại hệ thống dùng chuyển khoản thủ công, không hiển thị QR. Mã ngân hàng được lưu từ bây giờ để có thể tạo VietQR ở giai đoạn mở rộng mà không phải nhập lại dữ liệu.
      </div>
      {readOnly && <div className="notice warning">Năm học đã hoàn tất nên cấu hình chỉ được xem, không thể sửa.</div>}

      <form onSubmit={save}>
        <section className="panel" style={{ padding: 24, display: 'grid', gap: 22 }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 18 }}>
            <div className="form-group">
              <label htmlFor="payment-bank-name">Tên ngân hàng <em>*</em></label>
              <input id="payment-bank-name" required maxLength={150} disabled={readOnly || loading} value={form.bankName} onChange={event => patch('bankName', event.target.value)} placeholder="Ví dụ: TPBank" />
            </div>
            <div className="form-group">
              <label htmlFor="payment-bank-code">Mã ngân hàng (dùng cho QR sau này)</label>
              <input id="payment-bank-code" maxLength={30} disabled={readOnly || loading} value={form.bankCode} onChange={event => patch('bankCode', event.target.value.toUpperCase())} placeholder="Ví dụ: TPB, VCB" />
            </div>
            <div className="form-group">
              <label htmlFor="payment-account-number">Số tài khoản <em>*</em></label>
              <input id="payment-account-number" required inputMode="numeric" maxLength={30} disabled={readOnly || loading} value={form.accountNumber} onChange={event => patch('accountNumber', event.target.value.replace(/\D/g, ''))} placeholder="Chỉ nhập chữ số" />
            </div>
            <div className="form-group">
              <label htmlFor="payment-account-holder">Tên chủ tài khoản <em>*</em></label>
              <input id="payment-account-holder" required maxLength={150} disabled={readOnly || loading} value={form.accountHolder} onChange={event => patch('accountHolder', event.target.value.toUpperCase())} placeholder="FPT SCHOOLS" />
            </div>
            <div className="form-group">
              <label htmlFor="payment-branch">Chi nhánh</label>
              <input id="payment-branch" maxLength={150} disabled={readOnly || loading} value={form.branch} onChange={event => patch('branch', event.target.value)} placeholder="Không bắt buộc" />
            </div>
            <div className="form-group">
              <label htmlFor="payment-content">Mẫu nội dung chuyển khoản <em>*</em></label>
              <input id="payment-content" required maxLength={255} disabled={readOnly || loading} value={form.transferContentTemplate} onChange={event => patch('transferContentTemplate', event.target.value)} />
              <small>Dùng được: {'{studentCode}'}, {'{academicYear}'}, {'{semester}'}. Bắt buộc có {'{studentCode}'}.</small>
            </div>
          </div>

          <label style={{ display: 'flex', alignItems: 'center', gap: 10, fontWeight: 700 }}>
            <input type="checkbox" checked={form.enabled} disabled={readOnly || loading} onChange={event => patch('enabled', event.target.checked)} />
            Kích hoạt nhận xác nhận chuyển khoản trên app
          </label>

          <div style={{ border: '1px solid var(--border)', borderRadius: 12, padding: 18, background: 'var(--surface-muted, #f8fafc)' }}>
            <strong>Xem trước thông tin trên app</strong>
            <div style={{ display: 'grid', gap: 6, marginTop: 12, fontSize: 14 }}>
              <span>Ngân hàng: {form.bankName || '—'}</span>
              <span>Số tài khoản: {form.accountNumber || '—'}</span>
              <span>Chủ tài khoản: {form.accountHolder || '—'}</span>
              <span>Nội dung mẫu: {form.transferContentTemplate || '—'}</span>
              <span>Hình thức: Chuyển khoản thủ công · Chưa sử dụng QR</span>
            </div>
          </div>

          {!readOnly && (
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button type="submit" disabled={!selectedYearId || loading || saving}>
                {saving ? 'Đang lưu…' : 'Lưu cấu hình chuyển khoản'}
              </button>
            </div>
          )}
        </section>
      </form>
      <PaymentReconciliationPanel selectedYearId={selectedYearId} />
    </main>
  );
}
