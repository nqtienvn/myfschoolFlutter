import { useEffect, useState } from 'react';
import { getSubjects } from '../api/subject';
import {
  createTeacherAccount,
  getTeachers,
  updateTeacherSubjects,
  updateUserStatus,
  type TeacherItem,
} from '../api/user';

interface SubjectItem { id: number; name: string; code: string; }

const emptyForm = { employeeCode: '', name: '', phone: '', email: '', department: '' };

export default function UsersPage() {
  const [teachers, setTeachers] = useState<TeacherItem[]>([]);
  const [subjects, setSubjects] = useState<SubjectItem[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [subjectIds, setSubjectIds] = useState<number[]>([]);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('ACTIVE');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editingSubjects, setEditingSubjects] = useState<number[]>([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  async function load() {
    setLoading(true);
    setError('');
    try {
      const [teacherPage, subjectList] = await Promise.all([
        getTeachers({ status: status || undefined, keyword: keyword || undefined, page: 0, size: 500 }) as any,
        getSubjects() as Promise<SubjectItem[]>,
      ]);
      setTeachers(teacherPage.content || []);
      setSubjects(subjectList || []);
    } catch (cause: any) {
      setError(cause.message || 'Không thể tải danh sách giáo viên.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, [status]);

  async function create(event: React.FormEvent) {
    event.preventDefault();
    setError(''); setMessage(''); setLoading(true);
    try {
      await createTeacherAccount({ ...form, subjectIds });
      setForm(emptyForm); setSubjectIds([]);
      setMessage('Đã tạo tài khoản giáo viên. Giáo viên phải đổi mật khẩu ở lần đăng nhập đầu tiên.');
      await load();
    } catch (cause: any) {
      setError(cause.message || 'Không thể tạo giáo viên.');
    } finally { setLoading(false); }
  }

  async function saveSubjects(teacherId: number) {
    try {
      await updateTeacherSubjects(teacherId, editingSubjects);
      setEditingId(null);
      await load();
    } catch (cause: any) { setError(cause.message || 'Không thể cập nhật môn phụ trách.'); }
  }

  async function toggle(teacher: TeacherItem) {
    try {
      await updateUserStatus(teacher.userId, teacher.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE');
      await load();
    } catch (cause: any) { setError(cause.message || 'Không thể đổi trạng thái.'); }
  }

  return (
    <div className="page-stack">
      <div className="page-heading"><div><span className="eyebrow">Bước 3</span><h1>Quản lý giáo viên</h1><p>Hồ sơ giáo viên dùng lâu dài; phân công lớp và môn được quản lý riêng theo năm học.</p></div></div>
      {error && <div className="notice error">{error}</div>}
      {message && <div className="notice success">{message}</div>}

      <form className="form-grid" onSubmit={create}>
        <div className="form-group"><label>Mã giáo viên</label><input required value={form.employeeCode} onChange={e => setForm({ ...form, employeeCode: e.target.value })} /></div>
        <div className="form-group"><label>Họ và tên</label><input required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></div>
        <div className="form-group"><label>Số điện thoại</label><input required pattern="0[0-9]{9}" value={form.phone} onChange={e => setForm({ ...form, phone: e.target.value })} /></div>
        <div className="form-group"><label>Email</label><input type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} /></div>
        <div className="form-group"><label>Tổ chuyên môn</label><input value={form.department} onChange={e => setForm({ ...form, department: e.target.value })} /></div>
        <div className="form-group"><label>Môn có thể giảng dạy</label><select required multiple value={subjectIds.map(String)} onChange={e => setSubjectIds(Array.from(e.target.selectedOptions, option => Number(option.value)))}>{subjects.map(subject => <option key={subject.id} value={subject.id}>{subject.name} ({subject.code})</option>)}</select><small className="input-desc">Giữ Ctrl/Cmd để chọn nhiều môn.</small></div>
        <div className="form-actions" style={{ gridColumn: '1 / -1' }}><button disabled={loading}>Tạo giáo viên</button></div>
      </form>

      <div className="filters">
        <div className="form-group"><label>Tìm kiếm</label><input value={keyword} onChange={e => setKeyword(e.target.value)} placeholder="Tên, SĐT hoặc mã giáo viên" /></div>
        <div className="form-group"><label>Trạng thái</label><select value={status} onChange={e => setStatus(e.target.value)}><option value="ACTIVE">Đang hoạt động</option><option value="LOCKED">Đã khóa</option><option value="">Tất cả</option></select></div>
        <button type="button" onClick={load} disabled={loading}>Tìm</button>
      </div>

      <div className="table-responsive"><table><thead><tr><th>Mã GV</th><th>Họ tên</th><th>Liên hệ</th><th>Môn phụ trách</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>
        {teachers.map(teacher => <tr key={teacher.id}>
          <td>{teacher.employeeCode}</td><td>{teacher.name}<br /><small>{teacher.department}</small></td><td>{teacher.phone}<br /><small>{teacher.email}</small></td>
          <td>{editingId === teacher.id ? <div className="form-group"><select multiple value={editingSubjects.map(String)} onChange={e => setEditingSubjects(Array.from(e.target.selectedOptions, option => Number(option.value)))}>{subjects.map(subject => <option key={subject.id} value={subject.id}>{subject.name}</option>)}</select><div><button onClick={() => saveSubjects(teacher.id)}>Lưu</button> <button className="secondary-button" onClick={() => setEditingId(null)}>Hủy</button></div></div> : <button className="secondary-button" onClick={() => { setEditingId(teacher.id); setEditingSubjects(teacher.subjects.map(subject => subject.id)); }}>{teacher.subjects.map(subject => subject.code).join(', ') || 'Chọn môn'}</button>}</td>
          <td><span className={`badge-status ${teacher.status === 'ACTIVE' ? 'active' : ''}`}>{teacher.status}</span></td>
          <td><button className="secondary-button" onClick={() => toggle(teacher)}>{teacher.status === 'ACTIVE' ? 'Khóa' : 'Mở khóa'}</button></td>
        </tr>)}
        {!teachers.length && !loading && <tr><td colSpan={6}>Chưa có giáo viên phù hợp.</td></tr>}
      </tbody></table></div>
    </div>
  );
}
