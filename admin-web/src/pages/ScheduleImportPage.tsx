import { useState, useEffect, useRef } from 'react';
import { getAcademicYears } from '../api/academicYear';
import { getSemesters } from '../api/semester';
import { importSchedules } from '../api/schedule';

interface AcademicYearItem {
  id: number;
  name: string;
  status: string;
}

interface SemesterItem {
  id: number;
  name: string;
  academicYearId: number;
  academicYearName: string;
  isCurrent: boolean;
}

interface ImportResult {
  total: number;
  success: number;
  failed: number;
  errors: string[];
}

export default function ScheduleImportPage({ selectedYearId, selectedSemesterId }: { selectedYearId?: string; selectedSemesterId?: string }) {
  const [academicYears, setAcademicYears] = useState<AcademicYearItem[]>([]);
  const [academicYearId, setAcademicYearId] = useState(selectedYearId || '');
  const [semesters, setSemesters] = useState<SemesterItem[]>([]);
  const [semesterId, setSemesterId] = useState(selectedSemesterId || '');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [result, setResult] = useState<ImportResult | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    fetchAcademicYears();
  }, []);

  useEffect(() => {
    if (selectedYearId) {
      setAcademicYearId(selectedYearId);
    }
  }, [selectedYearId]);

  useEffect(() => {
    if (selectedSemesterId) {
      setSemesterId(selectedSemesterId);
    }
  }, [selectedSemesterId]);

  useEffect(() => {
    if (academicYearId) {
      fetchSemesters();
    } else {
      setSemesters([]);
      if (!selectedSemesterId) setSemesterId('');
    }
  }, [academicYearId, selectedSemesterId]);

  async function fetchAcademicYears() {
    try {
      const data = await getAcademicYears() as AcademicYearItem[];
      setAcademicYears(data || []);
      const active = data.find(y => y.status === 'ACTIVE') || data[0];
      if (active) setAcademicYearId(String(active.id));
    } catch (err: any) {
      setError(err.message || 'Không thể tải danh sách năm học');
    }
  }

  async function fetchSemesters() {
    try {
      const data = await getSemesters(academicYearId) as SemesterItem[];
      setSemesters(data || []);
      if (!selectedSemesterId) {
        const current = data.find(s => s.isCurrent) || data[0];
        if (current) setSemesterId(String(current.id));
      }
    } catch (err: any) {
      setError(err.message || 'Không thể tải danh sách học kỳ');
    }
  }

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) {
      if (!file.name.endsWith('.xlsx') && !file.name.endsWith('.xls')) {
        setError('Chỉ chấp nhận tệp Excel (.xlsx, .xls)');
        setSelectedFile(null);
        return;
      }
      setSelectedFile(file);
      setError('');
      setResult(null);
      setSuccessMsg('');
    }
  }

  async function handleUpload() {
    setError('');
    setSuccessMsg('');
    setResult(null);

    if (!semesterId) {
      setError('Vui lòng chọn học kỳ.');
      return;
    }
    if (!selectedFile) {
      setError('Vui lòng chọn một tệp Excel để tải lên.');
      return;
    }

    const formData = new FormData();
    formData.append('file', selectedFile);
    formData.append('semesterId', semesterId);

    setLoading(true);
    try {
      const data = await importSchedules(formData);
      setResult(data);
      setSuccessMsg('Đã nhập thời khóa biểu thành công!');
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
    } catch (err: any) {
      setError(err.message || 'Gửi tệp thất bại');
    } finally {
      setLoading(false);
    }
  }

  function downloadTemplate() {
    const csvContent = 'assignmentId,dayOfWeek,period,room,shift\n1,2,1,Phòng A101,MORNING\n2,2,2,Phòng A101,MORNING\n3,3,4,Phòng Lab 3,AFTERNOON\n';
    const blob = new Blob([new Uint8Array([0xEF, 0xBB, 0xBF]), csvContent], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'mau_nhap_thoi_khoa_bieu.csv';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  return (
    <div>
      <h2>Nhập thời khóa biểu từ Excel</h2>

      {error && <div className="error" style={{ marginBottom: 16 }}>{error}</div>}
      {successMsg && <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: '16px' }}>[SUCCESS] {successMsg}</div>}

      <div className="form-grid" style={{ gridTemplateColumns: '1fr 1fr 150px' }}>
        <div className="form-group">
          <label>Năm học</label>
          <select value={academicYearId} onChange={e => setAcademicYearId(e.target.value)}>
            <option value="">Chọn năm học</option>
            {academicYears.map(y => (
              <option key={y.id} value={y.id}>
                {y.name} {y.status === 'ACTIVE' ? '(Đang mở)' : ''}
              </option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label>Học kỳ áp dụng</label>
          <select value={semesterId} onChange={e => setSemesterId(e.target.value)}>
            <option value="">Chọn học kỳ</option>
            {semesters.map(s => (
              <option key={s.id} value={s.id}>
                {s.name} {s.isCurrent ? '(Hiện tại)' : ''}
              </option>
            ))}
          </select>
        </div>

        <div className="form-group" style={{ display: 'flex', justifyContent: 'flex-end', height: '100%' }}>
          <label style={{ visibility: 'hidden' }}>Thao tác</label>
          <button onClick={handleUpload} disabled={loading || !selectedFile} style={{ width: '100%', height: 38 }}>
            {loading ? 'Đang tải lên...' : 'Nhập Excel'}
          </button>
        </div>
      </div>

      <input
        type="file"
        ref={fileInputRef}
        style={{ display: 'none' }}
        accept=".xlsx,.xls"
        onChange={handleFileChange}
      />

      <div className="file-upload-zone" onClick={() => fileInputRef.current?.click()}>
        <p>{selectedFile ? `Đã chọn: ${selectedFile.name}` : 'Bấm vào đây để chọn tệp Excel (.xlsx, .xls) chứa thời khóa biểu giảng dạy'}</p>
        <span>Cột tiêu đề bắt buộc ở dòng 1: assignmentId (ID phân công), dayOfWeek (thứ 2-8), period (tiết học 1-10), room (phòng, tùy chọn), shift (MORNING hoặc AFTERNOON)</span>
        <div className="csv-template-link" onClick={e => { e.stopPropagation(); downloadTemplate(); }}>Tải mẫu file CSV tại đây (.csv)</div>
      </div>

      {result && (
        <div style={{ background: '#ffffff', padding: 24, border: '1px solid #e5e5e5', marginTop: 24 }}>
          <h3 style={{ fontSize: 16, fontWeight: 700, marginBottom: 16, borderBottom: '1px solid #000', paddingBottom: 6 }}>KẾT QUẢ IMPORT</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
            <div style={{ border: '1px solid #d4d4d4', padding: 16, textAlign: 'center' }}>
              <div style={{ fontSize: 24, fontWeight: 800 }}>{result.total}</div>
              <div style={{ fontSize: 11, color: '#666666', textTransform: 'uppercase', marginTop: 4 }}>Tổng số bản ghi</div>
            </div>
            <div style={{ border: '1px solid #16a34a', padding: 16, textAlign: 'center', background: '#f0fdf4' }}>
              <div style={{ fontSize: 24, fontWeight: 800, color: '#16a34a' }}>{result.success}</div>
              <div style={{ fontSize: 11, color: '#16a34a', textTransform: 'uppercase', marginTop: 4 }}>Thành công</div>
            </div>
            <div style={{ border: '1px solid #ef4444', padding: 16, textAlign: 'center', background: '#fef2f2' }}>
              <div style={{ fontSize: 24, fontWeight: 800, color: '#ef4444' }}>{result.failed}</div>
              <div style={{ fontSize: 11, color: '#ef4444', textTransform: 'uppercase', marginTop: 4 }}>Thất bại</div>
            </div>
          </div>

          {result.errors && result.errors.length > 0 && (
            <div>
              <h4 style={{ fontSize: 13, fontWeight: 700, color: '#ef4444', marginBottom: 8 }}>Chi tiết lỗi thời khóa biểu:</h4>
              <ul style={{ paddingLeft: 20, fontSize: 12, color: '#525252', lineHeight: '1.6', maxHeight: 200, overflowY: 'auto' }}>
                {result.errors.map((err, idx) => <li key={idx}>{err}</li>)}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
