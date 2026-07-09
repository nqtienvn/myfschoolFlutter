import { useState, useEffect, useRef } from 'react';
import { updateUserStatus, importTeachers, createTeacherAccount, getTeachers, updateTeacherSubjects } from '../api/user';
import type { TeacherItem } from '../api/user';
import { getSubjects } from '../api/subject';

interface TeacherPageData {
  content: TeacherItem[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface SubjectItem { id: number; name: string; code: string; }

const PAGE_SIZE = 20;
const DEFAULT_TEACHER_PASSWORD = '12345678';
const EMPTY_TEACHER_FORM = { employeeCode: '', name: '', phone: '', email: '', department: '' };

export default function UsersPage() {
  const [teachers, setTeachers] = useState<TeacherItem[]>([]);
  const [subjects, setSubjects] = useState<SubjectItem[]>([]);
  const [status, setStatus] = useState('');
  const [subjectFilter, setSubjectFilter] = useState('');
  const [keyword, setKeyword] = useState('');
  const [appliedKeyword, setAppliedKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [editingTeacherId, setEditingTeacherId] = useState<number | null>(null);
  const [editSubjectIds, setEditSubjectIds] = useState<number[]>([]);
  const [editSaving, setEditSaving] = useState(false);

  // Excel Import states
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [importLoading, setImportLoading] = useState(false);
  const [importError, setImportError] = useState('');
  const [importSuccess, setImportSuccess] = useState('');
  const [importResult, setImportResult] = useState<any>(null);
  const [showImport, setShowImport] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState('');
  const [createSuccess, setCreateSuccess] = useState('');
  const [teacherForm, setTeacherForm] = useState(EMPTY_TEACHER_FORM);
  const [teacherSubjectIds, setTeacherSubjectIds] = useState<number[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    getSubjects().then((d: any) => setSubjects(d || [])).catch(() => {});
  }, []);

  async function fetchTeachers() {
    setLoading(true);
    try {
      const data: TeacherPageData = await getTeachers({
        status: status || undefined,
        keyword: appliedKeyword || undefined,
        subjectId: subjectFilter ? Number(subjectFilter) : undefined,
        page,
        size: PAGE_SIZE
      });
      setTeachers(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err: any) {
      alert(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchTeachers();
  }, [status, subjectFilter, appliedKeyword, page]);

  function changeStatus(value: string) {
    setStatus(value);
    setPage(0);
  }

  function searchUsers() {
    setAppliedKeyword(keyword.trim());
    setPage(0);
  }

  async function toggleStatus(teacher: TeacherItem) {
    const newStatus = teacher.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE';
    if (!confirm(`${newStatus === 'LOCKED' ? 'Khóa' : 'Mở'} giáo viên ${teacher.name}?`)) return;
    try {
      await updateUserStatus(teacher.userId, newStatus);
      fetchTeachers();
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
      fetchTeachers();
    } catch (err: any) {
      setImportError(err.message || 'Gửi tệp thất bại');
    } finally {
      setImportLoading(false);
    }
  }

  async function handleCreateTeacher(e: { preventDefault(): void }) {
    e.preventDefault();
    setCreateError('');
    setCreateSuccess('');

    const payload = {
      employeeCode: teacherForm.employeeCode.trim(),
      name: teacherForm.name.trim(),
      phone: teacherForm.phone.trim(),
      email: teacherForm.email.trim() || undefined,
      department: teacherForm.department.trim() || undefined,
      subjectIds: teacherSubjectIds,
    };

    if (!payload.employeeCode || !payload.name || !payload.phone) {
      setCreateError('Vui lòng nhập mã giáo viên, họ tên và số điện thoại.');
      return;
    }
    if (payload.phone.length < 10 || payload.phone.length > 15) {
      setCreateError('Số điện thoại phải từ 10 đến 15 ký tự.');
      return;
    }
    if (payload.subjectIds.length === 0) {
      setCreateError('Vui lòng chọn ít nhất một môn học.');
      return;
    }

    setCreateLoading(true);
    try {
      await createTeacherAccount(payload);
      setTeacherForm(EMPTY_TEACHER_FORM);
      setTeacherSubjectIds([]);
      setCreateSuccess(`Tạo giáo viên thành công. Mật khẩu mặc định: ${DEFAULT_TEACHER_PASSWORD}`);
      setPage(0);
      await fetchTeachers();
    } catch (err: any) {
      setCreateError(err.message || 'Không thể tạo giáo viên');
    } finally {
      setCreateLoading(false);
    }
  }

  async function handleSaveSubject(teacherId: number) {
    setEditSaving(true);
    try {
      await updateTeacherSubjects(teacherId, editSubjectIds);
      setEditingTeacherId(null);
      await fetchTeachers();
    } catch (err: any) {
      alert(err.message);
    } finally {
      setEditSaving(false);
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
        <h2 style={{ margin: 0, border: 'none', padding: 0 }}>Quản lý Giáo viên</h2>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <button
            onClick={() => setShowCreate(!showCreate)}
            style={{ height: '34px', padding: '0 16px', background: '#000000', color: '#ffffff', border: 'none', fontWeight: 'bold', cursor: 'pointer', fontSize: '12px' }}
          >
            {showCreate ? 'Ẩn form thêm giáo viên' : 'Thêm giáo viên'}
          </button>
          <button
            onClick={() => setShowImport(!showImport)}
            style={{ height: '34px', padding: '0 16px', background: '#ffffff', color: '#000000', border: '1px solid #000000', fontWeight: 'bold', cursor: 'pointer', fontSize: '12px' }}
          >
            {showImport ? 'Ẩn bảng nhập Excel' : 'Nhập Giáo viên từ Excel'}
          </button>
        </div>
      </div>

      {showCreate && (
        <form onSubmit={handleCreateTeacher} style={{ background: '#ffffff', border: '1px solid #000000', padding: '20px', marginBottom: '24px' }}>
          <h3 style={{ fontSize: '14px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '16px', letterSpacing: '0.05em', borderBottom: '1px solid #e5e5e5', paddingBottom: '8px' }}>
            Thêm giáo viên thủ công
          </h3>

          {createError && <div className="error" style={{ marginBottom: 16 }}>{createError}</div>}
          {createSuccess && <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: '16px' }}>[SUCCESS] {createSuccess}</div>}

          <div className="form-grid" style={{ marginBottom: 12, padding: 0, border: 'none' }}>
            <div className="form-group">
              <label>Mã giáo viên</label>
              <input value={teacherForm.employeeCode} onChange={e => setTeacherForm({ ...teacherForm, employeeCode: e.target.value })} placeholder="GV001" />
            </div>
            <div className="form-group">
              <label>Họ tên</label>
              <input value={teacherForm.name} onChange={e => setTeacherForm({ ...teacherForm, name: e.target.value })} placeholder="Nguyễn Văn A" />
            </div>
            <div className="form-group">
              <label>Số điện thoại</label>
              <input value={teacherForm.phone} onChange={e => setTeacherForm({ ...teacherForm, phone: e.target.value })} placeholder="0987654321" />
            </div>
            <div className="form-group">
              <label>Email</label>
              <input type="email" value={teacherForm.email} onChange={e => setTeacherForm({ ...teacherForm, email: e.target.value })} placeholder="giaovien@school.edu.vn" />
            </div>
            <div className="form-group">
              <label>Tổ chuyên môn</label>
              <input value={teacherForm.department} onChange={e => setTeacherForm({ ...teacherForm, department: e.target.value })} placeholder="Tổ Toán - Tin" />
            </div>
            <div className="form-group" style={{ gridColumn: '1 / -1' }}>
              <label>Môn học phụ trách <span style={{ color: '#ef4444' }}>*</span></label>
              <select
                multiple
                value={teacherSubjectIds.map(String)}
                onChange={e => setTeacherSubjectIds(Array.from(e.target.selectedOptions, o => Number(o.value)))}
                style={{ minHeight: 80, width: '100%', padding: 8, border: '1px solid #d4d4d4', fontSize: 13 }}
              >
                {subjects.map(s => (
                  <option key={s.id} value={s.id}>{s.name} ({s.code})</option>
                ))}
              </select>
              <span className="input-desc">Giữ Ctrl/Cmd để chọn nhiều môn</span>
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap', alignItems: 'center' }}>
            <span style={{ fontSize: 12, color: '#737373', fontFamily: 'ui-monospace, monospace' }}>Mật khẩu mặc định: {DEFAULT_TEACHER_PASSWORD}</span>
            <button type="submit" disabled={createLoading} style={{ height: 38, padding: '0 20px', background: '#000000', color: '#ffffff', border: 'none', fontWeight: 'bold', cursor: 'pointer' }}>
              {createLoading ? 'Đang tạo...' : 'Tạo giáo viên'}
            </button>
          </div>
        </form>
      )}

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
        <select value={status} onChange={e => changeStatus(e.target.value)}>
          <option value="">Tất cả trạng thái</option>
          <option value="ACTIVE">Hoạt động</option>
          <option value="LOCKED">Khóa</option>
        </select>
        <select value={subjectFilter} onChange={e => { setSubjectFilter(e.target.value); setPage(0); }}>
          <option value="">Tất cả môn học</option>
          {subjects.map(s => (
            <option key={s.id} value={s.id}>{s.name}</option>
          ))}
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
              <th>Mã GV</th><th>Tên giáo viên</th><th>SĐT</th><th>Môn học</th><th>Trạng thái</th><th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {teachers.map(t => (
              <tr key={t.id}>
                <td>{t.employeeCode}</td>
                <td>{t.name}</td>
                <td>{t.phone}</td>
                <td>
                  {editingTeacherId === t.id ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                      <select
                        multiple
                        value={editSubjectIds.map(String)}
                        onChange={e => setEditSubjectIds(Array.from(e.target.selectedOptions, o => Number(o.value)))}
                        style={{ minHeight: 60, fontSize: 11, padding: 4, border: '1px solid #d4d4d4' }}
                      >
                        {subjects.map(s => (
                          <option key={s.id} value={s.id}>{s.name}</option>
                        ))}
                      </select>
                      <div style={{ display: 'flex', gap: 4 }}>
                        <button onClick={() => handleSaveSubject(t.id)} disabled={editSaving} style={{ fontSize: 11, padding: '2px 8px' }}>
                          {editSaving ? '...' : 'Lưu'}
                        </button>
                        <button onClick={() => setEditingTeacherId(null)} className="danger" style={{ fontSize: 11, padding: '2px 8px' }}>
                          Hủy
                        </button>
                      </div>
                    </div>
                  ) : (
                    <span
                      style={{ cursor: 'pointer', textDecoration: 'underline dotted', color: '#666' }}
                      title="Bấm để chỉnh sửa"
                      onClick={() => { setEditingTeacherId(t.id); setEditSubjectIds(t.subjects.map(s => s.id)); }}
                    >
                      {t.subjects.length > 0 ? t.subjects.map(s => s.code).join(', ') : <em style={{ color: '#aaa' }}>Chưa có môn</em>}
                    </span>
                  )}
                </td>
                <td>{t.status}</td>
                <td>
                  <button onClick={() => toggleStatus(t)}>
                    {t.status === 'ACTIVE' ? 'Khóa' : 'Mở'}
                  </button>
                </td>
              </tr>
            ))}
            {teachers.length === 0 && !loading && (
              <tr>
                <td colSpan={6}>Không có giáo viên phù hợp</td>
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
          Trang {totalPages === 0 ? 0 : page + 1}/{totalPages} - {totalElements} giáo viên
        </span>
        <button onClick={() => setPage(p => p + 1)} disabled={loading || page + 1 >= totalPages}>
          Sau
        </button>
      </div>
      {loading && <p>Đang tải...</p>}
    </div>
  );
}
