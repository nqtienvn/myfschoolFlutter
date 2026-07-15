import { useEffect, useRef, useState } from 'react';
import {
  confirmPayment,
  getPendingPaymentRequests,
  rejectPayment,
  type PendingTuitionBill,
} from '../api/tuitionPayment';

export default function PaymentReconciliationPanel({
  selectedYearId,
}: {
  selectedYearId: string;
}) {
  const [rows, setRows] = useState<PendingTuitionBill[]>([]);
  const [loading, setLoading] = useState(false);
  const [actionBillId, setActionBillId] = useState<number | null>(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const selectedYearRef = useRef(selectedYearId);

  useEffect(() => {
    let alive = true;
    selectedYearRef.current = selectedYearId;
    setRows([]);
    setError('');
    setMessage('');
    setLoading(false);
    if (!selectedYearId) return () => { alive = false; };

    setLoading(true);
    void getPendingPaymentRequests(selectedYearId)
      .then(data => {
        if (alive) setRows(data ?? []);
      })
      .catch((reason: Error) => {
        if (alive) setError(reason.message);
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => { alive = false; };
  }, [selectedYearId]);

  async function reconcile(bill: PendingTuitionBill, accepted: boolean) {
    const action = accepted ? 'xác nhận đã nhận tiền' : 'trả về chưa đóng';
    if (!window.confirm(`Bạn chắc chắn muốn ${action} cho khoản “${bill.name}” của ${bill.studentName}?`)) {
      return;
    }
    const yearBeingChanged = selectedYearId;
    setActionBillId(bill.id);
    setError('');
    setMessage('');
    try {
      if (accepted) await confirmPayment(bill.id);
      else await rejectPayment(bill.id);
      if (yearBeingChanged !== selectedYearRef.current) return;
      setRows(current => current.filter(row => row.id !== bill.id));
      setMessage(
        accepted
          ? `Đã xác nhận thanh toán cho ${bill.studentName}.`
          : `Đã trả khoản phí của ${bill.studentName} về trạng thái chưa đóng.`,
      );
    } catch (reason) {
      if (yearBeingChanged === selectedYearRef.current) {
        setError(reason instanceof Error ? reason.message : 'Không thể cập nhật giao dịch.');
      }
    } finally {
      setActionBillId(null);
    }
  }

  return (
    <section className="panel" style={{ padding: 24, display: 'grid', gap: 18 }}>
      <div>
        <h2 style={{ margin: 0, fontSize: 18 }}>Chờ đối soát chuyển khoản</h2>
        <p style={{ margin: '6px 0 0', color: 'var(--text-secondary)', fontSize: 13 }}>
          Chỉ bấm “Đã nhận tiền” sau khi kiểm tra giao dịch trong tài khoản ngân hàng của nhà trường.
        </p>
      </div>

      {error && <div className="notice error" role="alert">{error}</div>}
      {message && <div className="notice success" role="status">{message}</div>}
      {!selectedYearId && <div className="notice warning">Hãy chọn năm học để xem các yêu cầu đang chờ.</div>}
      {loading && <div className="notice info">Đang tải danh sách đối soát…</div>}
      {selectedYearId && !loading && rows.length === 0 && !error && (
        <div className="notice success">Không có xác nhận chuyển khoản nào đang chờ.</div>
      )}

      {rows.length > 0 && (
        <div className="table-responsive">
          <table>
            <thead>
              <tr>
                <th>Học sinh</th>
                <th>Khoản phí</th>
                <th>Số tiền</th>
                <th>Gửi xác nhận</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {rows.map(row => {
                const pending = row.transactions.find(item => item.status === 'PENDING');
                const busy = actionBillId === row.id;
                return (
                  <tr key={row.id}>
                    <td><strong>{row.studentName}</strong><small style={{ display: 'block' }}>{row.studentCode} · {row.className}</small></td>
                    <td><strong>{row.name}</strong><small style={{ display: 'block' }}>{row.semesterName}</small></td>
                    <td><strong>{money(row.amount)}</strong></td>
                    <td>{pending?.createdAt ? dateTime(pending.createdAt) : '—'}</td>
                    <td>
                      <div className="table-actions">
                        <button disabled={busy} onClick={() => reconcile(row, true)}>
                          {busy ? 'Đang xử lý…' : 'Đã nhận tiền'}
                        </button>
                        <button className="secondary-button" disabled={busy} onClick={() => reconcile(row, false)}>
                          Không tìm thấy giao dịch
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function money(value: number) {
  return `${new Intl.NumberFormat('vi-VN').format(value)} đ`;
}

function dateTime(value: string) {
  const parsed = new Date(value);
  return Number.isNaN(parsed.valueOf())
    ? value
    : new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(parsed);
}
