import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

interface AcademicYearItem {
  id: number;
  name: string;
  status: string;
}

interface InitResult {
  classesCreated: number;
  teachingAssignmentsCopied: number;
  feeTemplatesCopied: number;
  warnings: string[];
}

export default function AcademicYearInitPage() {
  const [academicYears, setAcademicYears] = useState<AcademicYearItem[]>([]);
  const [targetYearId, setTargetYearId] = useState('');
  const [sourceYearId, setSourceYearId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [result, setResult] = useState<InitResult | null>(null);

  useEffect(() => {
    fetchAcademicYears();
  }, []);

  async function fetchAcademicYears() {
    try {
      const data = await apiFetch('/academic-years') as AcademicYearItem[];
      setAcademicYears(data || []);
      // Set default target year to active or first year
      const active = data.find(y => y.status === 'ACTIVE') || data[0];
      if (active) {
        setTargetYearId(String(active.id));
      }
    } catch (err: any) {
      setError(err.message || 'Không thể tải danh sách năm học');
    }
  }

  async function handleInit() {
    setError('');
    setSuccessMsg('');
    setResult(null);

    if (!targetYearId || !sourceYearId) {
      setError('Vui lòng chọn đầy đủ năm học nguồn và năm học đích.');
      return;
    }
    if (targetYearId === sourceYearId) {
      setError('Năm học đích phải khác năm học nguồn.');
      return;
    }

    if (!confirm('Hệ thống sẽ sao chép danh sách lớp, mẫu phân công giảng dạy và mẫu học phí từ năm học nguồn sang năm học đích. Bạn có chắc chắn muốn tiếp tục?')) {
      return;
    }

    setLoading(true);
    try {
      const data = await apiFetch(`/academic-years/${targetYearId}/initialize`, {
        method: 'POST',
        body: JSON.stringify({ fromAcademicYearId: +sourceYearId }),
      }) as InitResult;

      setResult(data);
      setSuccessMsg('Khởi tạo cấu trúc năm học thành công!');
    } catch (err: any) {
      setError(err.message || 'Khởi tạo thất bại.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <h2>Khởi tạo cấu trúc năm học mới</h2>
      
      {error && <div className="error" style={{ marginBottom: 16 }}>{error}</div>}
      {successMsg && <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: '16px' }}>[SUCCESS] {successMsg}</div>}

      <div className="form-grid" style={{ gridTemplateColumns: '1fr 1fr 150px' }}>
        <div className="form-group">
          <label>Năm học đích (Mới cần khởi tạo)</label>
          <select value={targetYearId} onChange={e => setTargetYearId(e.target.value)}>
            <option value="">Chọn năm học đích</option>
            {academicYears.map(y => (
              <option key={y.id} value={y.id}>
                {y.name} {y.status === 'ACTIVE' ? '(Đang mở)' : ''}
              </option>
            ))}
          </select>
          <span className="input-desc">Năm học trống hoặc mới tạo cần sao chép cấu trúc sang</span>
        </div>

        <div className="form-group">
          <label>Năm học nguồn (Năm trước đó)</label>
          <select value={sourceYearId} onChange={e => setSourceYearId(e.target.value)}>
            <option value="">Chọn năm học nguồn</option>
            {academicYears
              .filter(y => String(y.id) !== targetYearId)
              .map(y => (
                <option key={y.id} value={y.id}>
                  {y.name}
                </option>
              ))}
          </select>
          <span className="input-desc">Năm học cũ chứa dữ liệu gốc để copy</span>
        </div>

        <div className="form-group" style={{ display: 'flex', justifyContent: 'flex-end', height: '100%' }}>
          <label style={{ visibility: 'hidden' }}>Thao tác</label>
          <button onClick={handleInit} disabled={loading} style={{ width: '100%', height: 38 }}>
            {loading ? 'Đang chạy...' : 'Khởi tạo'}
          </button>
        </div>
      </div>

      {result && (
        <div style={{ background: '#ffffff', padding: 24, border: '1px solid #e5e5e5' }}>
          <h3 style={{ fontSize: 16, fontWeight: 700, marginBottom: 16, borderBottom: '1px solid #000', paddingBottom: 6 }}>KẾT QUẢ KHỞI TẠO</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
            <div style={{ border: '1px solid #d4d4d4', padding: 16, textAlign: 'center' }}>
              <div style={{ fontSize: 24, fontWeight: 800 }}>{result.classesCreated}</div>
              <div style={{ fontSize: 11, color: '#666666', textTransform: 'uppercase', marginTop: 4 }}>Lớp học đã tạo</div>
            </div>
            <div style={{ border: '1px solid #d4d4d4', padding: 16, textAlign: 'center' }}>
              <div style={{ fontSize: 24, fontWeight: 800 }}>{result.teachingAssignmentsCopied}</div>
              <div style={{ fontSize: 11, color: '#666666', textTransform: 'uppercase', marginTop: 4 }}>Mẫu phân công giảng dạy</div>
            </div>
            <div style={{ border: '1px solid #d4d4d4', padding: 16, textAlign: 'center' }}>
              <div style={{ fontSize: 24, fontWeight: 800 }}>{result.feeTemplatesCopied}</div>
              <div style={{ fontSize: 11, color: '#666666', textTransform: 'uppercase', marginTop: 4 }}>Mẫu học phí đã copy</div>
            </div>
          </div>

          {result.warnings && result.warnings.length > 0 && (
            <div>
              <h4 style={{ fontSize: 13, fontWeight: 700, color: '#ef4444', marginBottom: 8 }}>Cảnh báo / Bỏ qua:</h4>
              <ul style={{ paddingLeft: 20, fontSize: 12, color: '#525252', lineHeight: '1.6' }}>
                {result.warnings.map((w, idx) => <li key={idx}>{w}</li>)}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
