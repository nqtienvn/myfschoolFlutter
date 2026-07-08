import { useState, useEffect, useRef } from 'react';
import { apiFetch } from '../api/client';

interface AcademicYearItem {
  id: number;
  name: string;
  status: string;
}

interface ImportResult {
  total: number;
  success: number;
  failed: number;
  errors: string[];
}

export default function EnrollmentImportPage({ selectedYearId }: { selectedYearId?: string }) {
  const [academicYears, setAcademicYears] = useState<AcademicYearItem[]>([]);
  const [academicYearId, setAcademicYearId] = useState(selectedYearId || '');
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

  async function fetchAcademicYears() {
    try {
      const data = await apiFetch('/academic-years') as AcademicYearItem[];
      setAcademicYears(data || []);
      if (!selectedYearId) {
        const active = data.find(y => y.status === 'ACTIVE') || data[0];
        if (active) setAcademicYearId(String(active.id));
      }
    } catch (err: any) {
      setError(err.message || 'Không thể tải danh sách năm học');
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

    if (!academicYearId) {
      setError('Vui lòng chọn năm học.');
      return;
    }
    if (!selectedFile) {
      setError('Vui lòng chọn một tệp Excel để tải lên.');
      return;
    }

    const token = localStorage.getItem('admin_token');
    const formData = new FormData();
    formData.append('file', selectedFile);
    formData.append('academicYearId', academicYearId);

    setLoading(true);
    try {
      // In production, this maps to the updated student bulk import endpoint.
      // For now, it calls our endpoint /import/enrollments
      const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
      const headers: Record<string, string> = {};
      if (token) headers['Authorization'] = `Bearer ${token}`;

      const res = await fetch(`${API_BASE}/import/enrollments`, {
        method: 'POST',
        headers,
        body: formData
      });

      const json = await res.json();
      if (!res.ok || !json.success) {
        throw new Error(json.message || 'Lỗi tải tệp lên');
      }

      setResult(json.data);
      setSuccessMsg('Đã nhập danh sách học sinh và xếp lớp thành công!');
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
    } catch (err: any) {
      setError(err.message || 'Gửi tệp thất bại');
    } finally {
      setLoading(false);
    }
  }

  function downloadTemplate() {
    const csvContent = 'Mã học sinh,Họ và tên,Ngày sinh,Giới tính,Mã lớp,Tên phụ huynh,Số điện thoại phụ huynh\nHS202601,Nguyễn Văn Hoàng,2016-05-15,Nam,10A1,Nguyễn Văn Hùng,0987654321\nHS202602,Trần Thị Mai,2016-08-20,Nữ,10A1,Trần Văn Thịnh,0912345678\nHS202603,Lê Hoàng Nam,2016-11-02,Nam,10A2,Lê Thị Hạnh,0909090909\n';
    const blob = new Blob([new Uint8Array([0xEF, 0xBB, 0xBF]), csvContent], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'mau_nhap_hoc_sinh_phu_huynh.csv';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  return (
    <div>
      <h2>Bước 5: Nhập Học sinh, xếp Lớp & liên kết Phụ huynh</h2>

      {error && <div className="error" style={{ marginBottom: 16 }}>{error}</div>}
      {successMsg && <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: '16px' }}>[SUCCESS] {successMsg}</div>}

      <div className="form-grid" style={{ gridTemplateColumns: '2fr 1fr' }}>
        <div className="form-group">
          <label>Năm học xếp lớp (Chọn ở Header trên cùng)</label>
          <div style={{ padding: '8px 12px', background: '#f3f4f6', border: '1px solid #d1d5db', fontSize: '13px', fontWeight: 'bold', fontFamily: 'ui-monospace, monospace' }}>
            NĂM HỌC {academicYears.find(y => String(y.id) === academicYearId)?.name || 'CHƯA CHỌN'}
          </div>
          <span className="input-desc">Học sinh sẽ được xếp vào lớp thuộc năm học hoạt động hiện tại.</span>
        </div>

        <div className="form-group" style={{ display: 'flex', justifyContent: 'flex-end', height: '100%' }}>
          <label style={{ visibility: 'hidden' }}>Thao tác</label>
          <button onClick={handleUpload} disabled={loading || !selectedFile} style={{ width: '100%', height: 38 }}>
            {loading ? 'Đang xử lý...' : 'Nhập danh sách Excel'}
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
        <p>{selectedFile ? `Đã chọn: ${selectedFile.name}` : 'Bấm vào đây để chọn tệp Excel (.xlsx, .xls) chứa danh sách học sinh xếp lớp & phụ huynh'}</p>
        <span>Tệp Excel phải chứa các cột: Mã học sinh, Họ và tên, Ngày sinh, Giới tính, Mã lớp, Tên phụ huynh, Số điện thoại phụ huynh</span>
        <div className="csv-template-link" onClick={e => { e.stopPropagation(); downloadTemplate(); }}>Tải mẫu file CSV tại đây (.csv)</div>
      </div>

      {result && (
        <div style={{ background: '#ffffff', padding: 24, border: '1px solid #e5e5e5', marginTop: 24 }}>
          <h3 style={{ fontSize: 16, fontWeight: 700, marginBottom: 16, borderBottom: '1px solid #000', paddingBottom: 6 }}>KẾT QUẢ ĐỒNG BỘ DỮ LIỆU</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
            <div style={{ border: '1px solid #d4d4d4', padding: 16, textAlign: 'center' }}>
              <div style={{ fontSize: 24, fontWeight: 800 }}>{result.total}</div>
              <div style={{ fontSize: 11, color: '#666666', textTransform: 'uppercase', marginTop: 4 }}>Tổng bản ghi trong Excel</div>
            </div>
            <div style={{ border: '1px solid #16a34a', padding: 16, textAlign: 'center', background: '#f0fdf4' }}>
              <div style={{ fontSize: 24, fontWeight: 800, color: '#16a34a' }}>{result.success}</div>
              <div style={{ fontSize: 11, color: '#16a34a', textTransform: 'uppercase', marginTop: 4 }}>Tài khoản đã tạo & xếp lớp</div>
            </div>
            <div style={{ border: '1px solid #ef4444', padding: 16, textAlign: 'center', background: '#fef2f2' }}>
              <div style={{ fontSize: 24, fontWeight: 800, color: '#ef4444' }}>{result.failed}</div>
              <div style={{ fontSize: 11, color: '#ef4444', textTransform: 'uppercase', marginTop: 4 }}>Lỗi dòng bỏ qua</div>
            </div>
          </div>

          {result.errors && result.errors.length > 0 && (
            <div>
              <h4 style={{ fontSize: 13, fontWeight: 700, color: '#ef4444', marginBottom: 8 }}>Chi tiết lỗi:</h4>
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
