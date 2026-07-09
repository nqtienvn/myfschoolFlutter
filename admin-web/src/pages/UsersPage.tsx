import { useState, useEffect, useRef } from 'react';
import { AdminUser } from '../api/auth';
import { getUsers, updateUserStatus, importTeachers } from '../api/user';

interface UsersPageData {
  content: AdminUser[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

const PAGE_SIZE = 20;

export default function UsersPage() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('');
  const [keyword, setKeyword] = useState('');
  const [appliedKeyword, setAppliedKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);

  // Excel Import states
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [importLoading, setImportLoading] = useState(false);
  const [importError, setImportError] = useState('');
  const [importSuccess, setImportSuccess] = useState('');
  const [importResult, setImportResult] = useState<any>(null);
  const [showImport, setShowImport] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  async function fetchUsers() {
    setLoading(true);
    try {
      const data: UsersPageData = await getUsers({
        role: role || undefined,
        status: status || undefined,
        keyword: appliedKeyword || undefined,
        page,
        size: PAGE_SIZE
      });
      setUsers(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err: any) {
      alert(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchUsers();
  }, [role, status, appliedKeyword, page]);

  function changeRole(value: string) {
    setRole(value);
    setPage(0);
  }

  function changeStatus(value: string) {
    setStatus(value);
    setPage(0);
  }

  function searchUsers() {
    setAppliedKeyword(keyword.trim());
    setPage(0);
  }

  async function toggleStatus(user: AdminUser) {
    const newStatus = user.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE';
    if (!confirm(`${newStatus === 'LOCKED' ? 'Khóa' : 'Mở'} tài khoản ${user.name}?`)) return;
    try {
      await updateUserStatus(user.id, newStatus);
      fetchUsers();
    } catch (err: any) {
      alert(err.message);
    }
  }

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) {
      if (!file.name.endsWith('.xlsx') && !file.name.endsWith('.xls')) {
        setImportError('Chỉ chấp nhận tệp Excel (.xlsx, .xls)');
        setSelectedFile(null);
        return;
      }
      setSelectedFile(file);
      setImportError('');
      setImportResult(null);
      setImportSuccess('');
    }
  }

  async function handleUpload() {
    setImportError('');
    setImportSuccess('');
    setImportResult(null);

    if (!selectedFile) {
      setImportError('Vui lòng chọn một tệp Excel để tải lên.');
      return;
    }

    const formData = new FormData();
    formData.append('file', selectedFile);

    setImportLoading(true);
    try {
      const data = await importTeachers(formData);
      setImportResult(data);
      setImportSuccess('Đã nhập danh sách giáo viên thành công!');
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
      fetchUsers(); // Refresh users list
    } catch (err: any) {
      setImportError(err.message || 'Gửi tệp thất bại');
    } finally {
      setImportLoading(false);
    }
  }

  function downloadTemplate() {
    const csvContent = 'employeeCode,name,phone,email,department\nGV001,Nguyễn Văn Tiến,0987654321,tiennv@school.edu.vn,Toán - Tin\nGV002,Lê Thị Mai,0912345678,mailt@school.edu.vn,Ngữ Văn\nGV003,Trần Văn Bình,0909090909,binhtv@school.edu.vn,Anh Văn\n';
    const blob = new Blob([new Uint8Array([0xEF, 0xBB, 0xBF]), csvContent], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'mau_nhap_giao_vien.csv';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '16px', borderBottom: '2px solid #000000', paddingBottom: '12px' }}>
        <h2 style={{ margin: 0, border: 'none', padding: 0 }}>Quản lý Giáo viên & Tài khoản</h2>
        <button 
          onClick={() => setShowImport(!showImport)}
          style={{ height: '34px', padding: '0 16px', background: '#000000', color: '#ffffff', border: 'none', fontWeight: 'bold', cursor: 'pointer', fontSize: '12px' }}
        >
          {showImport ? 'Ẩn bảng nhập Excel' : 'Nhập Giáo viên từ Excel'}
        </button>
      </div>

      {showImport && (
        <div style={{ background: '#ffffff', border: '1px solid #000000', padding: '20px', marginBottom: '24px' }}>
          <h3 style={{ fontSize: '14px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '16px', letterSpacing: '0.05em', borderBottom: '1px solid #e5e5e5', paddingBottom: '8px' }}>
            Nhập danh sách Giáo viên từ Excel
          </h3>

          {importError && <div className="error" style={{ marginBottom: 16 }}>{importError}</div>}
          {importSuccess && <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: '16px' }}>[SUCCESS] {importSuccess}</div>}

          <input
            type="file"
            ref={fileInputRef}
            style={{ display: 'none' }}
            accept=".xlsx,.xls"
            onChange={handleFileChange}
          />

          <div className="file-upload-zone" onClick={() => fileInputRef.current?.click()} style={{ marginBottom: '16px' }}>
            <p>{selectedFile ? `Đã chọn: ${selectedFile.name}` : 'Bấm vào đây để tải lên tệp Excel (.xlsx, .xls) giáo viên'}</p>
            <div className="csv-template-link" onClick={e => { e.stopPropagation(); downloadTemplate(); }}>Tải mẫu file CSV tại đây (.csv)</div>
          </div>

          <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
            <button 
              onClick={handleUpload} 
              disabled={importLoading || !selectedFile} 
              style={{ height: 38, padding: '0 20px', background: '#000000', color: '#ffffff', border: 'none', fontWeight: 'bold', cursor: 'pointer' }}
            >
              {importLoading ? 'Đang nhập...' : 'Tải lên & Nhập danh sách'}
            </button>
          </div>

          {importResult && (
            <div style={{ background: '#fafafa', padding: 16, border: '1px solid #d4d4d4', marginTop: 16 }}>
              <div style={{ fontWeight: 700, fontSize: 13, marginBottom: 12 }}>KẾT QUẢ ĐỒNG BỘ DỮ LIỆU</div>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: 12, marginBottom: 16 }}>
                <div style={{ border: '1px solid #d4d4d4', padding: 12, textAlign: 'center', background: '#fff' }}>
                  <div style={{ fontSize: 20, fontWeight: 800 }}>{importResult.total}</div>
                  <div style={{ fontSize: 10, color: '#666', textTransform: 'uppercase', marginTop: 4 }}>Tổng dòng Excel</div>
                </div>
                <div style={{ border: '1px solid #16a34a', padding: 12, textAlign: 'center', background: '#f0fdf4' }}>
                  <div style={{ fontSize: 20, fontWeight: 800, color: '#16a34a' }}>{importResult.success}</div>
                  <div style={{ fontSize: 10, color: '#16a34a', textTransform: 'uppercase', marginTop: 4 }}>Thành công</div>
                </div>
                <div style={{ border: '1px solid #ef4444', padding: 12, textAlign: 'center', background: '#fef2f2' }}>
                  <div style={{ fontSize: 20, fontWeight: 800, color: '#ef4444' }}>{importResult.failed}</div>
                  <div style={{ fontSize: 10, color: '#ef4444', textTransform: 'uppercase', marginTop: 4 }}>Bỏ qua / Lỗi</div>
                </div>
              </div>

              {importResult.errors && importResult.errors.length > 0 && (
                <div>
                  <h4 style={{ fontSize: 12, fontWeight: 700, color: '#ef4444', marginBottom: 6 }}>Chi tiết lỗi:</h4>
                  <ul style={{ paddingLeft: 16, fontSize: 11, color: '#525252', lineHeight: '1.5', maxHeight: 150, overflowY: 'auto' }}>
                    {importResult.errors.map((err: string, idx: number) => <li key={idx}>{err}</li>)}
                  </ul>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      <div className="filters">
        <select value={role} onChange={e => changeRole(e.target.value)}>
          <option value="">Tất cả vai trò</option>
          <option value="PARENT">Phụ huynh</option>
          <option value="STUDENT">Học sinh</option>
          <option value="TEACHER">Giáo viên</option>
        </select>
        <select value={status} onChange={e => changeStatus(e.target.value)}>
          <option value="">Tất cả trạng thái</option>
          <option value="ACTIVE">Hoạt động</option>
          <option value="LOCKED">Khóa</option>
        </select>
        <input
          type="text"
          placeholder="Tìm theo tên hoặc SĐT..."
          value={keyword}
          onChange={e => setKeyword(e.target.value)}
        />
        <button onClick={searchUsers} disabled={loading}>Tìm</button>
      </div>

      <div style={{ overflowX: 'auto' }}>
        <table>
          <thead>
            <tr>
              <th>ID</th><th>Tên</th><th>SĐT</th><th>Vai trò</th><th>Trạng thái</th><th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {users.map(u => (
              <tr key={u.id}>
                <td>{u.id}</td>
                <td>{u.name}</td>
                <td>{u.phone}</td>
                <td>{u.role}</td>
                <td>{u.status}</td>
                <td>
                  <button onClick={() => toggleStatus(u)}>
                    {u.status === 'ACTIVE' ? 'Khóa' : 'Mở'}
                  </button>
                </td>
              </tr>
            ))}
            {users.length === 0 && !loading && (
              <tr>
                <td colSpan={6}>Không có tài khoản phù hợp</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="filters">
        <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={loading || page === 0}>
          Trước
        </button>
        <span>
          Trang {totalPages === 0 ? 0 : page + 1}/{totalPages} - {totalElements} tài khoản
        </span>
        <button onClick={() => setPage(p => p + 1)} disabled={loading || page + 1 >= totalPages}>
          Sau
        </button>
      </div>
      {loading && <p>Đang tải...</p>}
    </div>
  );
}
