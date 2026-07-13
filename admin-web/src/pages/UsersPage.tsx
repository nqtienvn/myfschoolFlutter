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

const emptyForm = { name: '', phone: '', email: '' };

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
  const [showForm, setShowForm] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [selectedSubjectId, setSelectedSubjectId] = useState<string>('');
  // useState trả về một mảng đúng 2 phần tử
  //1 là biến lưu giá trị hiện tại
  //2 là hàm để thay đổi giá trị
  //'' là giá trị khởi tạo ban đầu --> khi web vừa load xong thì selectedSubjectId sẽ là chuỗi rỗng
  //useState là một hook để tạo và quản lý một biến có thể thay đổi dữ liệu trên giao diện
  //khi giá trị buêns này thay đổi UI sẽ tự động vẽ lại để hiển thị giá trị mới ngay lập tức


  async function load() {
    setLoading(true);
    setError('');
    try {
      const [teacherPage, subjectList] = await Promise.all([
        getTeachers({
          status: status || undefined,
          keyword: keyword || undefined,
          subjectId: selectedSubjectId ? Number(selectedSubjectId) : undefined,
          page: 0, size: 500
        }) as any,
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

  useEffect(
    () => { load(); },
    [status, selectedSubjectId]);

  async function create(event: React.FormEvent) {
    event.preventDefault();
    if (subjectIds.length === 0) {
      setError('Chọn ít nhất một môn giáo viên có thể giảng dạy.');
      return;
    }
    setError(''); setMessage(''); setLoading(true);
    try {
      const created = await createTeacherAccount({ ...form, subjectIds });
      setForm(emptyForm); setSubjectIds([]);
      setShowForm(false);
      setMessage(`Đã tạo giáo viên ${created.employeeCode}. Giáo viên phải đổi mật khẩu ở lần đăng nhập đầu tiên.`);
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
      <div className="page-heading"><div><span className="eyebrow">Phân hệ giáo viên</span><h1>Quản lý giáo viên</h1></div><div className="page-heading-actions"><button type="button" onClick={() => setShowForm(v => !v)}>{showForm ? '✕ Đóng' : '＋ Tạo giáo viên'}</button></div></div>
      {error && <div className="notice error">{error}</div>}
      {message && <div className="notice success">{message}</div>}

      {showForm && <form className="teacher-create-form" onSubmit={create}>
        <div className="teacher-form-heading">
          <div><span className="teacher-form-index">01</span><div><h2>Thông tin giáo viên</h2></div></div>
          <span className="teacher-required-note">* Thông tin bắt buộc</span>
        </div>

        <div className="teacher-primary-fields">
          <div className="form-group"><label>Họ và tên *</label><input required placeholder="Ví dụ: Nguyễn Văn An" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></div>
          <div className="form-group"><label>Số điện thoại *</label><input required inputMode="numeric" pattern="0[0-9]{9}" placeholder="10 chữ số, bắt đầu bằng 0" value={form.phone} onChange={e => setForm({ ...form, phone: e.target.value })} /></div>
          <div className="form-group"><label>Email</label><input type="email" placeholder="giaovien@myfschool.edu.vn" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} /></div>
        </div>

        <fieldset className="teacher-subject-picker">
          <legend>Môn có thể giảng dạy *</legend>
          <div className="teacher-subject-grid">
            {subjects.map(subject => {
              const selected = subjectIds.includes(subject.id);
              return <label key={subject.id} className={selected ? 'selected' : ''}>
                <input
                  type="checkbox"
                  checked={selected}
                  onChange={() => setSubjectIds(current => selected ? current.filter(id => id !== subject.id) : [...current, subject.id])}
                />
                <span><strong>{subject.name}</strong><small>{subject.code}</small></span>
              </label>;
            })}
            {subjects.length === 0 && <span className="input-desc">Chưa có môn học. Hãy khởi tạo danh mục trước.</span>}
          </div>
        </fieldset>

        <div className="teacher-form-footer">
          <button disabled={loading}>{loading ? 'Đang tạo…' : 'Tạo giáo viên'}</button>
          <button type="button" className="secondary-button" onClick={() => { setShowForm(false); setForm(emptyForm); setSubjectIds([]); }}>Đóng</button>
        </div>
      </form>}

      <div className="filters">
        <div className="form-group"><label>Tìm kiếm</label><input value={keyword} onChange={e => setKeyword(e.target.value)} placeholder="Tìm theo Tên, SĐT, Mã GV" /></div>
        <div className="form-group"><label>Trạng thái</label><select value={status} onChange={e => setStatus(e.target.value)}><option value="ACTIVE">Đang hoạt động</option><option value="LOCKED">Đã khóa</option><option value="">Tất cả</option></select></div>
        <div className="form-group"><label>Bộ Môn</label>
          <select value={selectedSubjectId} onChange={e => setSelectedSubjectId(e.target.value)}>
            <option value="">Tất cả</option>
            {subjects.map(subject => (
              <option key={subject.id} value={subject.id}>
                {subject.name}
              </option>
            ))}
          </select>
        </div>
        <button type="button" onClick={load} disabled={loading}>Tìm</button>
      </div>

      <div className="table-responsive"><table><thead><tr><th>Mã GV</th><th>Họ tên</th><th>Liên hệ</th><th>Môn phụ trách</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>
        {teachers.map(teacher => <tr key={teacher.id}>
          <td>{teacher.employeeCode}</td><td>{teacher.name}</td><td>{teacher.phone}<br /><small>{teacher.email}</small></td>
          <td>{editingId === teacher.id ? <div className="form-group"><select multiple value={editingSubjects.map(String)} onChange={e => setEditingSubjects(Array.from(e.target.selectedOptions, option => Number(option.value)))}>{subjects.map(subject => <option key={subject.id} value={subject.id}>{subject.name}</option>)}</select><div><button onClick={() => saveSubjects(teacher.id)}>Lưu</button> <button className="secondary-button" onClick={() => setEditingId(null)}>Hủy</button></div></div> : <button className="secondary-button" onClick={() => { setEditingId(teacher.id); setEditingSubjects(teacher.subjects.map(subject => subject.id)); }}>{teacher.subjects.map(subject => subject.code).join(', ') || 'Chọn môn'}</button>}</td>
          <td><span className={`badge-status ${teacher.status === 'ACTIVE' ? 'active' : ''}`}>{teacher.status}</span></td>
          <td><button className="secondary-button" onClick={() => toggle(teacher)}>{teacher.status === 'ACTIVE' ? 'Khóa' : 'Mở khóa'}</button></td>
        </tr>)}
        {!teachers.length && !loading && <tr><td colSpan={6}>Chưa có giáo viên phù hợp.</td></tr>}
      </tbody></table></div>
    </div>
  );
}
