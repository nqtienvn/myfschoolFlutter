import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

interface AcademicYearItem {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  status: string;
}

interface SemestersPageProps {
  selectedYearId?: string;
}

export default function SemestersPage({ selectedYearId }: SemestersPageProps) {
  const [academicYears, setAcademicYears] = useState<AcademicYearItem[]>([]);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Creation state (stored as string to avoid locking when editing)
  const [startYearStr, setStartYearStr] = useState<string>('');
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    fetchAcademicYears();
  }, []);

  async function fetchAcademicYears() {
    try {
      const data = await apiFetch('/academic-years') as AcademicYearItem[];
      setAcademicYears(data || []);

      // Automatically set the next logical year
      const years = data.map(y => {
        const parts = y.name.split('-');
        return parseInt(parts[0]);
      }).filter(y => !isNaN(y));
      const nextStartYear = years.length > 0 ? Math.max(...years) + 1 : new Date().getFullYear();
      setStartYearStr(String(nextStartYear));
    } catch (err: any) {
      setError(err.message || 'Không thể tải danh sách năm học');
    }
  }

  // Calculate dates based on startYear (opening date fixed at 05/09 to 30/05 next year)
  function getCalculatedDates(year: number) {
    const name = `${year}-${year + 1}`;
    const yearStart = `${year}-09-05`;
    const yearEnd = `${year + 1}-05-30`;
    const endHK1 = `${year}-12-31`;
    const startHK2 = `${year + 1}-01-01`;
    const endHK2 = `${year + 1}-05-30`;
    return {
      name,
      yearStart,
      yearEnd,
      hk1: { start: yearStart, end: endHK1 },
      hk2: { start: startHK2, end: endHK2 }
    };
  }

  const calculated = getCalculatedDates(parseInt(startYearStr) || new Date().getFullYear());

  async function handleCreateYearAndSemesters() {
    setError('');
    setSuccessMsg('');

    if (!startYearStr.trim()) {
      setError('Vui lòng nhập năm bắt đầu.');
      return;
    }

    const year = parseInt(startYearStr);
    if (isNaN(year) || year < 2000 || year > 2100) {
      setError('Năm bắt đầu không hợp lệ (phải từ 2000 đến 2100).');
      return;
    }

    const calculated = getCalculatedDates(year);

    setCreating(true);
    try {
      // 1. Create Academic Year
      const yearRes = await apiFetch('/academic-years', {
        method: 'POST',
        body: JSON.stringify({
          name: calculated.name,
          startDate: calculated.yearStart,
          endDate: calculated.yearEnd,
          status: 'DRAFT'
        })
      }) as any;

      if (!yearRes || !yearRes.id) {
        throw new Error('Không nhận được thông tin năm học mới tạo');
      }

      const yearId = yearRes.id;

      // 2. Create Semester 1
      await apiFetch('/semesters', {
        method: 'POST',
        body: JSON.stringify({
          name: 'Học kỳ 1',
          academicYearId: yearId,
          order: 1,
          startDate: calculated.hk1.start,
          endDate: calculated.hk1.end
        })
      });

      // 3. Create Semester 2
      await apiFetch('/semesters', {
        method: 'POST',
        body: JSON.stringify({
          name: 'Học kỳ 2',
          academicYearId: yearId,
          order: 2,
          startDate: calculated.hk2.start,
          endDate: calculated.hk2.end
        })
      });

      setSuccessMsg(`Tạo thành công năm học ${calculated.name} và tự động khởi tạo 2 học kỳ liên quan!`);

      // Reload list
      const updatedYears = await apiFetch('/academic-years') as AcademicYearItem[];
      setAcademicYears(updatedYears || []);

      // Refresh the next default year
      const years = updatedYears.map(y => {
        const parts = y.name.split('-');
        return parseInt(parts[0]);
      }).filter(y => !isNaN(y));
      const nextStartYear = years.length > 0 ? Math.max(...years) + 1 : new Date().getFullYear();
      setStartYearStr(String(nextStartYear));
    } catch (err: any) {
      setError(err.message || 'Lỗi trong quá trình tạo năm học & học kỳ');
    } finally {
      setCreating(false);
    }
  }

  return (
    <div style={{ padding: '8px 0', maxWidth: '600px' }}>
      <h2>Quản lý dòng thời gian học tập</h2>

      {error && <div className="error" style={{ marginBottom: 16 }}>{error}</div>}
      {successMsg && <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: '16px' }}>[SUCCESS] {successMsg}</div>}

      <div style={{ background: '#ffffff', border: '1px solid #000000', padding: '24px', marginTop: '16px' }}>
        <h3 style={{ fontSize: '14px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '16px', letterSpacing: '0.05em', borderBottom: '1px solid #e5e5e5', paddingBottom: '8px' }}>
          Khởi tạo năm học mới
        </h3>

        <div className="form-group" style={{ marginBottom: '16px' }}>
          <label>Năm bắt đầu (Khai giảng)</label>
          <input
            type="number"
            value={startYearStr}
            onChange={e => setStartYearStr(e.target.value)}
            min="2000"
            max="2100"
            placeholder="Ví dụ: 2026"
            style={{ width: '100%', height: '38px', padding: '8px', border: '1px solid #d4d4d4' }}
          />
          <span className="input-desc">Nhập năm học bắt đầu, hệ thống tự động sinh niên khóa</span>
        </div>

        <div style={{ background: '#fafafa', padding: '16px', borderLeft: '3px solid #000000', marginBottom: '20px', fontSize: '12px', fontFamily: 'ui-monospace, monospace', color: '#404040', lineHeight: '1.6' }}>
          <div style={{ fontWeight: 700, marginBottom: '8px', color: '#000000' }}>[VÍ DỤ CẤU HÌNH THỜI GIAN]</div>
          <div>Nếu năm bắt đầu nhập là <strong>2026</strong>:</div>
          <div style={{ marginTop: '4px' }}>• <strong>Niên khóa:</strong> 2026-2027</div>
          <div>• <strong>Thời gian năm học:</strong> 05/09/2026 → 30/05/2027</div>
          <div style={{ margin: '6px 0', borderTop: '1px dashed #d4d4d4' }}></div>
          <div>• <strong>Học kỳ 1:</strong> 05/09/2026 → 31/12/2026</div>
          <div>• <strong>Học kỳ 2:</strong> 01/01/2027 → 30/05/2027</div>
        </div>

        <button
          onClick={handleCreateYearAndSemesters}
          disabled={creating}
          style={{ width: '100%', height: '38px', background: '#000000', color: '#ffffff', border: 'none', fontWeight: 'bold', cursor: 'pointer' }}
        >
          {creating ? 'Đang khởi tạo...' : 'Tạo Năm học & Học kỳ'}
        </button>
      </div>
    </div>
  );
}
