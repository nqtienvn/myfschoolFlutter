import { useEffect, useState } from 'react';
import { getAcademicYearReadiness } from '../api/readiness';
import type { AcademicYearReadiness } from '../api/readiness';

interface Props {
  academicYearId?: string;
  onNavigate: (key: string) => void;
}

export default function ValidationPage({ academicYearId, onNavigate }: Props) {
  const [data, setData] = useState<AcademicYearReadiness | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function validate() {
    if (!academicYearId) return;
    setLoading(true);
    setError('');
    try {
      setData(await getAcademicYearReadiness(academicYearId));
    } catch (cause: any) {
      setError(cause.message || 'Không thể kiểm tra dữ liệu năm học.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { validate(); }, [academicYearId]);

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div><span className="eyebrow">Bước 8</span><h1>Kiểm tra dữ liệu</h1><p>Hệ thống kiểm tra dữ liệu bắt buộc trước khi cho phép ACTIVE.</p></div>
        <button className="secondary-button" onClick={validate} disabled={!academicYearId || loading}>{loading ? 'Đang kiểm tra…' : 'Kiểm tra lại'}</button>
      </section>
      {!academicYearId && <div className="notice warning">Hãy chọn năm học cần cấu hình ở thanh phía trên.</div>}
      {error && <div className="notice error">{error}</div>}
      {data && (
        <>
          <div className={`readiness-summary ${data.ready ? 'success' : 'warning'}`}>
            <strong>{data.ready ? 'Dữ liệu đã sẵn sàng' : 'Dữ liệu chưa đủ điều kiện'}</strong>
            <span>{data.ready ? 'Có thể chuyển sang bước kích hoạt.' : 'Hoàn thành các mục chưa đạt rồi kiểm tra lại.'}</span>
          </div>
          <div className="check-list">
            {data.checks.map(check => (
              <div key={check.code} className={`check-row ${check.passed ? 'passed' : 'failed'}`}>
                <span className="check-icon">{check.passed ? '✓' : '!'}</span>
                <span><strong>{check.label}</strong><small>{check.detail}</small></span>
              </div>
            ))}
          </div>
          <div className="page-actions">
            <button onClick={() => onNavigate('assignments')} className="secondary-button">Quay lại phân công</button>
            <button onClick={() => onNavigate('activation')} disabled={!data.ready}>Tiếp tục kích hoạt</button>
          </div>
        </>
      )}
    </div>
  );
}
