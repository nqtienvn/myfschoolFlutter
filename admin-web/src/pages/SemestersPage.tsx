import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

interface AcademicYearItem { id: number; name: string; startDate: string; endDate: string; status: string; }
interface Semester { id: number; name: string; academicYearId: number; academicYearName: string; order: number; startDate: string; endDate: string; isCurrent: boolean; }

export default function SemestersPage() {
  const [academicYears, setAcademicYears] = useState<AcademicYearItem[]>([]);
  const [academicYearId, setAcademicYearId] = useState('');
  const [items, setItems] = useState<Semester[]>([]);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [startYearDate, setStartYearDate] = useState('2025-09-01');
  const [autoSetCurrent, setAutoSetCurrent] = useState(true);

  const calculated = calculateSemesters();

  useEffect(() => { fetchAcademicYears(); }, []);
  useEffect(() => { if (academicYearId) fetchItems(); }, [academicYearId]);

  function calculateSemesters() {
    if (!startYearDate) return null;
    const start = new Date(startYearDate);
    if (isNaN(start.getTime())) return null;
    const endHK1 = new Date(start);
    endHK1.setMonth(start.getMonth() + 4);
    endHK1.setDate(start.getDate() + 14);
    const startHK2 = new Date(endHK1);
    startHK2.setDate(endHK1.getDate() + 1);
    const endHK2 = new Date(startHK2);
    endHK2.setMonth(startHK2.getMonth() + 4);
    endHK2.setDate(startHK2.getDate() + 14);
    const formatDate = (date: Date) => date.toISOString().split('T')[0];
    return { hk1: { start: formatDate(start), end: formatDate(endHK1) }, hk2: { start: formatDate(startHK2), end: formatDate(endHK2) } };
  }

  async function fetchAcademicYears() {
    const data = await apiFetch('/academic-years') as AcademicYearItem[];
    setAcademicYears(data);
    const active = data.find(y => y.status === 'ACTIVE') || data[0];
    if (active) {
      setAcademicYearId(String(active.id));
      setStartYearDate(active.startDate);
    }
  }

  async function fetchItems() {
    try {
      const data = await apiFetch(`/semesters?academicYearId=${academicYearId}`);
      setItems(Array.isArray(data) ? data.sort((a, b) => a.order - b.order) : []);
    } catch (err: any) {
      setError(err.message || 'Không thể tải danh sách học kỳ');
    }
  }

  async function handleAutoInitialize() {
    setError('');
    setSuccessMsg('');
    if (!academicYearId) return setError('Vui lòng chọn năm học.');
    if (!calculated) return setError('Ngày bắt đầu năm học không hợp lệ.');

    try {
      const res1 = await apiFetch('/semesters', {
        method: 'POST',
        body: JSON.stringify({ name: 'Học kỳ 1', academicYearId: +academicYearId, order: 1, startDate: calculated.hk1.start, endDate: calculated.hk1.end })
      }) as any;
      await apiFetch('/semesters', {
        method: 'POST',
        body: JSON.stringify({ name: 'Học kỳ 2', academicYearId: +academicYearId, order: 2, startDate: calculated.hk2.start, endDate: calculated.hk2.end })
      });
      if (autoSetCurrent && res1?.id) await apiFetch(`/semesters/${res1.id}/set-current`, { method: 'PUT' });
      setSuccessMsg(`Khởi tạo thành công 2 học kỳ cho năm học ${academicYears.find(y => y.id === +academicYearId)?.name || ''}!`);
      fetchItems();
    } catch (err: any) {
      setError(err.message || 'Lỗi khởi tạo học kỳ');
    }
  }

  async function setCurrent(id: number) {
    setError('');
    setSuccessMsg('');
    try {
      await apiFetch(`/semesters/${id}/set-current`, { method: 'PUT' });
      fetchItems();
    } catch (err: any) {
      setError(err.message || 'Lỗi thiết lập học kỳ hiện tại');
    }
  }

  async function handleDelete(id: number) {
    if (!window.confirm('Bạn có chắc chắn muốn xóa học kỳ này không?')) return;
    setError('');
    setSuccessMsg('');
    try {
      await apiFetch(`/semesters/${id}`, { method: 'DELETE' });
      setSuccessMsg('Xóa học kỳ thành công!');
      fetchItems();
    } catch (err: any) {
      setError(err.message || 'Lỗi khi xóa học kỳ');
    }
  }

  return (
    <div>
      <h2>Quản lý học kỳ</h2>
      {error && <div className="error">{error}</div>}
      {successMsg && <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: '16px' }}>[SUCCESS] {successMsg}</div>}

      <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))' }}>
        <div className="form-group">
          <label>Năm học</label>
          <select value={academicYearId} onChange={e => setAcademicYearId(e.target.value)}>
            <option value="">Chọn năm học</option>
            {academicYears.map(y => <option key={y.id} value={y.id}>{y.name} {y.status === 'ACTIVE' ? '(Đang mở)' : ''}</option>)}
          </select>
          <span className="input-desc">Năm học cần khởi tạo học kỳ</span>
        </div>
        <div className="form-group">
          <label>Ngày khai giảng năm học</label>
          <input type="date" value={startYearDate} onChange={e => setStartYearDate(e.target.value)} />
          <span className="input-desc">Ngày bắt đầu của Học kỳ 1</span>
        </div>
        <div className="form-group" style={{ justifyContent: 'center' }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '12px' }}>
            <input type="checkbox" checked={autoSetCurrent} onChange={e => setAutoSetCurrent(e.target.checked)} style={{ width: '16px', height: '16px', cursor: 'pointer' }} />
            Đặt ngay Học kỳ 1 làm hiện tại
          </label>
          <span className="input-desc">Kích hoạt TKB/điểm danh ngay</span>
        </div>
        <div className="form-group">
          <label style={{ visibility: 'hidden' }}>Thao tác</label>
          <button onClick={handleAutoInitialize} style={{ height: '38px', width: '100%' }}>Khởi tạo học kỳ</button>
          <span className="input-desc" style={{ visibility: 'hidden' }}>&nbsp;</span>
        </div>
        {calculated && <div style={{ gridColumn: 'span 4', background: '#fafafa', padding: '12px 16px', borderLeft: '3px solid #000000', fontSize: '12px', fontFamily: 'ui-monospace, monospace', color: '#666666' }}>
          <div>[HỆ THỐNG TỰ TÍNH TOÁN] Thiết lập thời gian học kỳ:</div>
          <div style={{ marginTop: '4px' }}>• <strong>Học kỳ 1:</strong> {calculated.hk1.start} → {calculated.hk1.end}</div>
          <div>• <strong>Học kỳ 2:</strong> {calculated.hk2.start} → {calculated.hk2.end}</div>
        </div>}
      </div>

      <table>
        <thead><tr><th>ID</th><th>Tên học kỳ</th><th>Thứ tự</th><th>Năm học</th><th>Thời gian hoạt động</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
        <tbody>
          {items.map(s => <tr key={s.id}>
            <td>{s.id}</td><td>{s.name}</td><td>{s.order}</td><td>{s.academicYearName}</td>
            <td style={{ fontFamily: 'ui-monospace, monospace', fontSize: '12px' }}>{s.startDate} → {s.endDate}</td>
            <td>{s.isCurrent ? <span style={{ background: '#000000', color: '#ffffff', padding: '2px 8px', fontSize: '11px', fontWeight: 700 }}>HIỆN TẠI</span> : <span style={{ color: '#a3a3a3' }}>Lịch sử</span>}</td>
            <td>{!s.isCurrent && <><button onClick={() => setCurrent(s.id)} style={{ marginRight: '8px' }}>Đặt làm hiện tại</button><button className="danger" onClick={() => handleDelete(s.id)}>Xóa</button></>}</td>
          </tr>)}
        </tbody>
      </table>
    </div>
  );
}
