import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

interface GradeLevel {
  id: number;
  name: string;
  code: string;
  order: number;
  description: string;
}

interface SchoolShift {
  id: number;
  name: string;
  code: string;
  order: number;
}

interface Period {
  id: number;
  name: string;
  order: number;
  shiftId: number;
  shiftName: string;
}

type TabKey = 'grade-levels' | 'shifts' | 'periods';

export default function MasterDataPage() {
  const [activeTab, setActiveTab] = useState<TabKey>('grade-levels');
  const [gradeLevels, setGradeLevels] = useState<GradeLevel[]>([]);
  const [shifts, setShifts] = useState<SchoolShift[]>([]);
  const [periods, setPeriods] = useState<Period[]>([]);
  
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [initializing, setInitializing] = useState(false);

  useEffect(() => {
    fetchMasterData();
  }, [activeTab]);

  async function fetchMasterData() {
    setError('');
    try {
      if (activeTab === 'grade-levels') {
        const data = await apiFetch('/master-data/grade-levels');
        setGradeLevels(data || []);
      } else if (activeTab === 'shifts') {
        const data = await apiFetch('/master-data/shifts');
        setShifts(data || []);
      } else if (activeTab === 'periods') {
        const data = await apiFetch('/master-data/periods');
        setPeriods(data || []);
      }
    } catch (err: any) {
      setError(err.message || 'Không thể tải dữ liệu danh mục');
    }
  }

  async function handleInitialize() {
    setError('');
    setSuccessMsg('');
    setInitializing(true);
    try {
      await apiFetch('/master-data/initialize', { method: 'POST' });
      setSuccessMsg('Khởi tạo nhanh dữ liệu danh mục mẫu thành công!');
      fetchMasterData();
    } catch (err: any) {
      setError(err.message || 'Lỗi khi khởi tạo danh mục mẫu');
    } finally {
      setInitializing(false);
    }
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '16px', borderBottom: '2px solid #000000', paddingBottom: '12px' }}>
        <h2 style={{ margin: 0, border: 'none', padding: 0 }}>Quản lý danh mục chung (Master Data)</h2>
        <button 
          onClick={handleInitialize} 
          disabled={initializing}
          style={{ height: '34px', padding: '0 16px', background: '#000000', color: '#ffffff', border: 'none', fontWeight: 'bold', cursor: 'pointer', fontSize: '12px' }}
        >
          {initializing ? 'Đang chạy...' : 'Khởi tạo nhanh danh mục mẫu'}
        </button>
      </div>

      <div className="tabs-container">
        <button className={`tab-btn ${activeTab === 'grade-levels' ? 'active' : ''}`} onClick={() => setActiveTab('grade-levels')}>Khối lớp</button>
        <button className={`tab-btn ${activeTab === 'shifts' ? 'active' : ''}`} onClick={() => setActiveTab('shifts')}>Ca học</button>
        <button className={`tab-btn ${activeTab === 'periods' ? 'active' : ''}`} onClick={() => setActiveTab('periods')}>Tiết học</button>
      </div>

      {error && <div className="error" style={{ marginBottom: 16 }}>{error}</div>}
      {successMsg && <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: '16px' }}>[SUCCESS] {successMsg}</div>}

      {/* Tab: Grade Levels Read-only */}
      {activeTab === 'grade-levels' && (
        <table>
          <thead>
            <tr>
              <th>Thứ tự</th>
              <th>Mã khối</th>
              <th>Tên khối lớp</th>
              <th>Mô tả</th>
            </tr>
          </thead>
          <tbody>
            {gradeLevels.map(gl => (
              <tr key={gl.id}>
                <td>{gl.order}</td>
                <td>{gl.code}</td>
                <td style={{ fontWeight: 600 }}>{gl.name}</td>
                <td>{gl.description}</td>
              </tr>
            ))}
            {gradeLevels.length === 0 && (
              <tr>
                <td colSpan={4} style={{ textAlign: 'center', color: '#737373', padding: '20px' }}>
                  Chưa có dữ liệu khối lớp. Vui lòng nhấn nút "Khởi tạo nhanh danh mục mẫu" ở phía trên.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {/* Tab: Shifts Read-only */}
      {activeTab === 'shifts' && (
        <table>
          <thead>
            <tr>
              <th>Thứ tự</th>
              <th>Mã ca</th>
              <th>Tên ca học</th>
            </tr>
          </thead>
          <tbody>
            {shifts.map(s => (
              <tr key={s.id}>
                <td>{s.order}</td>
                <td>{s.code}</td>
                <td style={{ fontWeight: 600 }}>{s.name}</td>
              </tr>
            ))}
            {shifts.length === 0 && (
              <tr>
                <td colSpan={3} style={{ textAlign: 'center', color: '#737373', padding: '20px' }}>
                  Chưa có dữ liệu ca học. Vui lòng nhấn nút "Khởi tạo nhanh danh mục mẫu" ở phía trên.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {/* Tab: Periods Read-only */}
      {activeTab === 'periods' && (
        <table>
          <thead>
            <tr>
              <th>Thứ tự tiết</th>
              <th>Tên tiết học</th>
              <th>Ca học thuộc</th>
            </tr>
          </thead>
          <tbody>
            {periods.map(p => (
              <tr key={p.id}>
                <td>{p.order}</td>
                <td style={{ fontWeight: 600 }}>{p.name}</td>
                <td>{p.shiftName}</td>
              </tr>
            ))}
            {periods.length === 0 && (
              <tr>
                <td colSpan={3} style={{ textAlign: 'center', color: '#737373', padding: '20px' }}>
                  Chưa có dữ liệu tiết học. Vui lòng nhấn nút "Khởi tạo nhanh danh mục mẫu" ở phía trên.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}
    </div>
  );
}
