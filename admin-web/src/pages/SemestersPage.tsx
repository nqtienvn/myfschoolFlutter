import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

interface Semester { 
  id: number; 
  name: string; 
  academicYear: string; 
  startDate: string; 
  endDate: string; 
  isCurrent: boolean; 
}

export default function SemestersPage() {
  const [items, setItems] = useState<Semester[]>([]);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Form State: Chỉ cần Năm học và Ngày bắt đầu
  const [academicYear, setAcademicYear] = useState('2025-2026');
  const [startYearDate, setStartYearDate] = useState('2025-09-01');
  const [autoSetCurrent, setAutoSetCurrent] = useState(true);

  // Tính toán tự động Ngày kết thúc HK1, bắt đầu HK2 và kết thúc HK2 dựa trên Ngày bắt đầu
  const calculateSemesters = () => {
    if (!startYearDate) return null;
    
    const start = new Date(startYearDate);
    if (isNaN(start.getTime())) return null;

    // HK1 kéo dài 4.5 tháng
    const endHK1 = new Date(start);
    endHK1.setMonth(start.getMonth() + 4);
    endHK1.setDate(start.getDate() + 14);

    // HK2 bắt đầu ngay ngày hôm sau
    const startHK2 = new Date(endHK1);
    startHK2.setDate(endHK1.getDate() + 1);

    // HK2 kéo dài thêm 4.5 tháng (tổng năm học khoảng 9 tháng)
    const endHK2 = new Date(startHK2);
    endHK2.setMonth(startHK2.getMonth() + 4);
    endHK2.setDate(startHK2.getDate() + 14);

    const formatDate = (date: Date) => date.toISOString().split('T')[0];

    return {
      hk1: {
        start: formatDate(start),
        end: formatDate(endHK1)
      },
      hk2: {
        start: formatDate(startHK2),
        end: formatDate(endHK2)
      }
    };
  };

  const calculated = calculateSemesters();

  async function fetchItems() {
    try { 
      const data = await apiFetch('/semesters');
      // Sắp xếp học kỳ theo ID giảm dần để đưa học kỳ mới lên đầu
      if (Array.isArray(data)) {
        data.sort((a, b) => b.id - a.id);
      }
      setItems(data); 
    } catch (err: any) { 
      setError(err.message || 'Không thể tải danh sách học kỳ'); 
    }
  }

  useEffect(() => { 
    fetchItems(); 
  }, []);

  async function handleAutoInitialize() {
    setError('');
    setSuccessMsg('');

    if (!academicYear.trim()) {
      setError('Vui lòng nhập Năm học.');
      return;
    }
    if (academicYear.length > 9) {
      setError('Năm học không được dài quá 9 ký tự (Ví dụ: 2025-2026).');
      return;
    }
    if (!calculated) {
      setError('Ngày bắt đầu năm học không hợp lệ.');
      return;
    }

    try {
      // 1. Tạo Học kỳ 1
      const res1 = await apiFetch('/semesters', { 
        method: 'POST', 
        body: JSON.stringify({ 
          name: 'Học kỳ 1', 
          academicYear: academicYear.trim(), 
          startDate: calculated.hk1.start, 
          endDate: calculated.hk1.end 
        }) 
      }) as any;

      // 2. Tạo Học kỳ 2
      await apiFetch('/semesters', { 
        method: 'POST', 
        body: JSON.stringify({ 
          name: 'Học kỳ 2', 
          academicYear: academicYear.trim(), 
          startDate: calculated.hk2.start, 
          endDate: calculated.hk2.end 
        }) 
      });

      // 3. Tự động đặt Học kỳ 1 làm hiện tại (nếu check)
      if (autoSetCurrent && res1 && res1.id) {
        await apiFetch(`/semesters/${res1.id}/set-current`, { method: 'PUT' });
      }

      setSuccessMsg(`Khởi tạo thành công 2 học kỳ cho năm học ${academicYear}!`);
      fetchItems();
    } catch (err: any) { 
      setError(err.message || 'Lỗi khởi tạo năm học'); 
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
    if (!window.confirm('Bạn có chắc chắn muốn xóa học kỳ này không? (Học kỳ bị xóa sẽ được ẩn khỏi hệ thống)')) return;
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
      {successMsg && (
        <div style={{ 
          color: '#16a34a', 
          fontFamily: 'ui-monospace, monospace', 
          fontSize: '13px', 
          marginBottom: '16px' 
        }}>
          [SUCCESS] {successMsg}
        </div>
      )}

      {/* Form tự động sinh 2 học kỳ */}
      <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))' }}>
        <div className="form-group">
          <label>Năm học mới</label>
          <input 
            placeholder="VD: 2025-2026" 
            value={academicYear} 
            onChange={e => setAcademicYear(e.target.value)} 
          />
          <span className="input-desc">Định dạng YYYY-YYYY (Tối đa 9 ký tự)</span>
        </div>

        <div className="form-group">
          <label>Ngày khai giảng năm học</label>
          <input 
            type="date" 
            value={startYearDate} 
            onChange={e => setStartYearDate(e.target.value)} 
          />
          <span className="input-desc">Ngày bắt đầu của Học kỳ 1</span>
        </div>

        <div className="form-group" style={{ justifyContent: 'center' }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '12px' }}>
            <input 
              type="checkbox" 
              checked={autoSetCurrent} 
              onChange={e => setAutoSetCurrent(e.target.checked)} 
              style={{ width: '16px', height: '16px', cursor: 'pointer' }}
            />
            Đặt ngay Học kỳ 1 làm hiện tại
          </label>
          <span className="input-desc">Kích hoạt TKB/điểm danh ngay</span>
        </div>

        <div className="form-group">
          <label style={{ visibility: 'hidden' }}>Thao tác</label>
          <button onClick={handleAutoInitialize} style={{ height: '38px', width: '100%' }}>
            Khởi tạo năm học mới
          </button>
          <span className="input-desc" style={{ visibility: 'hidden' }}>&nbsp;</span>
        </div>

        {/* Khung hiển thị thông tin tính toán trước */}
        {calculated && (
          <div style={{ 
            gridColumn: 'span 4', 
            background: '#fafafa', 
            padding: '12px 16px', 
            borderLeft: '3px solid #000000',
            fontSize: '12px',
            fontFamily: 'ui-monospace, monospace',
            color: '#666666'
          }}>
            <div>[HỆ THỐNG TỰ TÍNH TOÁN] Thiết lập thời gian học kỳ:</div>
            <div style={{ marginTop: '4px' }}>
              • <strong>Học kỳ 1:</strong> Bắt đầu {calculated.hk1.start} → Kết thúc {calculated.hk1.end} (4.5 tháng học)
            </div>
            <div>
              • <strong>Học kỳ 2:</strong> Bắt đầu {calculated.hk2.start} → Kết thúc {calculated.hk2.end} (4.5 tháng học)
            </div>
          </div>
        )}
      </div>

      {/* Danh sách học kỳ */}
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Tên học kỳ</th>
            <th>Năm học</th>
            <th>Thời gian hoạt động</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {items.map(s => (
            <tr key={s.id}>
              <td>{s.id}</td>
              <td>{s.name}</td>
              <td>{s.academicYear}</td>
              <td style={{ fontFamily: 'ui-monospace, monospace', fontSize: '12px' }}>
                {s.startDate} → {s.endDate}
              </td>
              <td>
                {s.isCurrent ? (
                  <span style={{ 
                    background: '#000000', 
                    color: '#ffffff', 
                    padding: '2px 8px', 
                    fontSize: '11px',
                    fontWeight: 700 
                  }}>
                    HIỆN TẠI
                  </span>
                ) : (
                  <span style={{ color: '#a3a3a3' }}>Lịch sử</span>
                )}
              </td>
              <td>
                {!s.isCurrent && (
                  <>
                    <button onClick={() => setCurrent(s.id)} style={{ marginRight: '8px' }}>
                      Đặt làm hiện tại
                    </button>
                    <button className="danger" onClick={() => handleDelete(s.id)}>
                      Xóa
                    </button>
                  </>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
