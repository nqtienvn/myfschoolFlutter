import { useEffect, useState } from 'react';
import { activateAcademicYear, getAcademicYearReadiness } from '../api/readiness';
import type { AcademicYearReadiness } from '../api/readiness';

interface Props {
  academicYearId?: string;
  academicYearStatus?: string;
  onChanged: () => Promise<void> | void;
  onNavigate: (key: string) => void;
}

export default function ActivationPage({ academicYearId, academicYearStatus, onChanged, onNavigate }: Props) {
  const [readiness, setReadiness] = useState<AcademicYearReadiness | null>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    setMessage('');
    setError('');
    if (!academicYearId) return;
    getAcademicYearReadiness(academicYearId).then(setReadiness).catch((cause: any) => setError(cause.message));
  }, [academicYearId]);

  async function activate() {
    if (!academicYearId || !readiness?.ready) return;
    if (!confirm('Kích hoạt năm học? Sau thao tác này, cấu hình quan trọng sẽ bị khóa.')) return;
    setLoading(true);
    setError('');
    try {
      await activateAcademicYear(academicYearId);
      setMessage('Năm học đã được kích hoạt và Học kỳ 1 đã mở.');
      await onChanged();
    } catch (cause: any) {
      setError(cause.message || 'Không thể kích hoạt năm học.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page-stack">
      <section className="page-heading"><div><span className="eyebrow">Bước 7</span><h1>Kích hoạt năm học</h1><p>Đây là bước cuối cùng của quy trình thiết lập.</p></div></section>
      {error && <div className="notice error">{error}</div>}
      {message && <div className="notice success">{message}</div>}
      {academicYearStatus === 'ACTIVE' && <div className="notice success">Năm học này đã được kích hoạt.</div>}
      {academicYearStatus === 'COMPLETED' && <div className="notice warning">Năm học này đã hoàn tất và không thể kích hoạt lại.</div>}
      <section className="activation-card">
        <div className={`activation-seal ${readiness?.ready ? 'ready' : ''}`}>{readiness?.ready ? '✓' : '!'}</div>
        <div>
          <h2>{readiness?.ready ? 'Sẵn sàng kích hoạt' : 'Chưa thể kích hoạt'}</h2>
          <p>{readiness?.ready ? 'Tất cả điều kiện dữ liệu đã đạt.' : 'Quay lại bước kiểm tra để xử lý các điều kiện chưa đạt.'}</p>
        </div>
      </section>
      <div className="notice warning"><strong>Lưu ý:</strong> Năm học ACTIVE không được sửa ngày, xóa hoặc thay đổi cấu hình quan trọng.</div>
      <div className="page-actions">
        <button className="secondary-button" onClick={() => onNavigate('validation')}>Xem kết quả kiểm tra</button>
        <button onClick={activate} disabled={!readiness?.ready || academicYearStatus !== 'DRAFT' || loading}>{loading ? 'Đang kích hoạt…' : 'Kích hoạt năm học'}</button>
      </div>
    </div>
  );
}
