import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { getSubjects } from '../api/subject';
import {
  createTeacherAccount,
  getTeacherManagementSummary,
  getTeachers,
  updateTeacherProfile,
  updateTeacherSubjects,
  updateUserStatus,
  type TeacherAccountCredential,
  type TeacherItem,
  type TeacherManagementSummary,
} from '../api/user';
import StudentEnrollmentPage from './StudentEnrollmentPage';

interface SubjectItem { id: number; name: string; code: string; }
type AccountManagementTab = 'teachers' | 'families';

const emptyForm = { name: '', phone: '', email: '' };
const emptySummary: TeacherManagementSummary = { total: 0, active: 0, locked: 0, unassigned: 0, homeroom: 0 };

function readFilter(name: string, fallback = '') {
  return new URLSearchParams(window.location.search).get(name) ?? fallback;
}

function readPage() {
  const value = Number(readFilter('teacherPage', '1'));
  return Number.isInteger(value) && value > 0 ? value - 1 : 0;
}

function readPageSize() {
  const value = Number(readFilter('teacherSize', '20'));
  return [20, 50, 100].includes(value) ? value : 20;
}

function errorMessage(cause: unknown, fallback: string) {
  return cause instanceof Error && cause.message ? cause.message : fallback;
}

export default function UsersPage({ selectedYearId, studentAccountsEditable = true }: {
  selectedYearId?: string;
  studentAccountsEditable?: boolean;
}) {
  const [activeTab, setActiveTab] = useState<AccountManagementTab>('teachers');

  return (
    <div className="account-management-page">
      <div className="tabs-container" role="tablist" aria-label="Nhóm tài khoản cần quản lý">
        <button
          type="button"
          className={`tab-btn ${activeTab === 'teachers' ? 'active' : ''}`}
          role="tab"
          aria-selected={activeTab === 'teachers'}
          aria-controls="teacher-management-panel"
          onClick={() => setActiveTab('teachers')}
        >
          Quản lý giáo viên
        </button>
        <button
          type="button"
          className={`tab-btn ${activeTab === 'families' ? 'active' : ''}`}
          role="tab"
          aria-selected={activeTab === 'families'}
          aria-controls="family-account-management-panel"
          onClick={() => setActiveTab('families')}
        >
          Quản lý phụ huynh, học sinh
        </button>
      </div>

      {activeTab === 'teachers' ? (
        <div id="teacher-management-panel" role="tabpanel">
          <TeacherManagementTab selectedYearId={selectedYearId} />
        </div>
      ) : (
        <div id="family-account-management-panel" role="tabpanel">
          <StudentEnrollmentPage selectedYearId={selectedYearId} editable={studentAccountsEditable} />
        </div>
      )}
    </div>
  );
}

function TeacherManagementTab({ selectedYearId }: { selectedYearId?: string }) {
  const academicYearId = selectedYearId ? Number(selectedYearId) : undefined;
  const [teachers, setTeachers] = useState<TeacherItem[]>([]);
  const [subjects, setSubjects] = useState<SubjectItem[]>([]);
  const [summary, setSummary] = useState<TeacherManagementSummary>(emptySummary);
  const [form, setForm] = useState(emptyForm);
  const [subjectIds, setSubjectIds] = useState<number[]>([]);
  const initialKeyword = readFilter('teacherKeyword');
  const [draftKeyword, setDraftKeyword] = useState(initialKeyword);
  const [keyword, setKeyword] = useState(initialKeyword);
  const initialStatus = readFilter('teacherStatus', 'ACTIVE');
  const [status, setStatus] = useState(initialStatus === 'ALL' ? '' : initialStatus);
  const [selectedSubjectId, setSelectedSubjectId] = useState(readFilter('teacherSubject'));
  const [page, setPage] = useState(readPage);
  const [pageSize, setPageSize] = useState(readPageSize);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [editingSubjectsFor, setEditingSubjectsFor] = useState<TeacherItem | null>(null);
  const [editingSubjects, setEditingSubjects] = useState<number[]>([]);
  const [subjectSearch, setSubjectSearch] = useState('');
  const [editingProfile, setEditingProfile] = useState<TeacherItem | null>(null);
  const [profileForm, setProfileForm] = useState(emptyForm);
  const [credential, setCredential] = useState<TeacherAccountCredential | null>(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    getSubjects()
      .then((items: SubjectItem[]) => { if (active) setSubjects(items || []); })
      .catch((cause: Error) => { if (active) setError(cause.message || 'Không thể tải danh mục môn học.'); });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setPage(0);
      setKeyword(draftKeyword.trim());
    }, 400);
    return () => window.clearTimeout(timer);
  }, [draftKeyword]);

  useEffect(() => {
    setPage(0);
    setEditingSubjectsFor(null);
    setEditingProfile(null);
  }, [selectedYearId]);

  useEffect(() => {
    const url = new URL(window.location.href);
    const params = url.searchParams;
    keyword ? params.set('teacherKeyword', keyword) : params.delete('teacherKeyword');
    params.set('teacherStatus', status || 'ALL');
    selectedSubjectId ? params.set('teacherSubject', selectedSubjectId) : params.delete('teacherSubject');
    params.set('teacherPage', String(page + 1));
    params.set('teacherSize', String(pageSize));
    window.history.replaceState(window.history.state, '', `${url.pathname}?${params.toString()}${url.hash}`);
  }, [keyword, status, selectedSubjectId, page, pageSize]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    getTeachers({
      status: status || undefined,
      keyword: keyword || undefined,
      subjectId: selectedSubjectId ? Number(selectedSubjectId) : undefined,
      academicYearId,
      page,
      size: pageSize,
    })
      .then(result => {
        if (!active) return;
        setTeachers((result.content || []).map(teacher => ({
          ...teacher,
          subjects: teacher.subjects || [],
          teachingAssignments: teacher.teachingAssignments || [],
          homeroomClasses: teacher.homeroomClasses || [],
        })));
        setTotalElements(result.totalElements || 0);
        setTotalPages(result.totalPages || 0);
        if (result.totalPages > 0 && page >= result.totalPages) setPage(result.totalPages - 1);
      })
      .catch((cause: Error) => { if (active) setError(cause.message || 'Không thể tải danh sách giáo viên.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [status, keyword, selectedSubjectId, academicYearId, page, pageSize, reloadKey]);

  useEffect(() => {
    let active = true;
    getTeacherManagementSummary(academicYearId)
      .then(result => { if (active) setSummary(result); })
      .catch((cause: Error) => { if (active) setError(cause.message || 'Không thể tải thống kê giáo viên.'); });
    return () => { active = false; };
  }, [academicYearId, reloadKey]);

  const filteredSubjects = useMemo(() => {
    const query = subjectSearch.trim().toLocaleLowerCase('vi');
    if (!query) return subjects;
    return subjects.filter(subject => `${subject.name} ${subject.code}`.toLocaleLowerCase('vi').includes(query));
  }, [subjects, subjectSearch]);

  function refresh() {
    setReloadKey(value => value + 1);
  }

  function clearNotices() {
    setError('');
    setMessage('');
  }

  function commitSearch() {
    setPage(0);
    setKeyword(draftKeyword.trim());
  }

  async function create(event: FormEvent) {
    event.preventDefault();
    if (subjectIds.length === 0) {
      setError('Chọn ít nhất một môn giáo viên có thể giảng dạy.');
      return;
    }
    clearNotices();
    setSubmitting(true);
    try {
      const result = await createTeacherAccount({ ...form, subjectIds });
      setForm(emptyForm);
      setSubjectIds([]);
      setShowForm(false);
      setCredential(result);
      setMessage(`Đã tạo giáo viên ${result.teacher.employeeCode}.`);
      setPage(0);
      refresh();
    } catch (cause: unknown) {
      setError(errorMessage(cause, 'Không thể tạo giáo viên.'));
    } finally {
      setSubmitting(false);
    }
  }

  function openProfileEditor(teacher: TeacherItem) {
    clearNotices();
    setEditingSubjectsFor(null);
    setEditingProfile(teacher);
    setProfileForm({ name: teacher.name, phone: teacher.phone, email: teacher.email || '' });
  }

  async function saveProfile(event: FormEvent) {
    event.preventDefault();
    if (!editingProfile) return;
    clearNotices();
    setSubmitting(true);
    try {
      await updateTeacherProfile(editingProfile.id, profileForm, academicYearId);
      setMessage(`Đã cập nhật thông tin ${editingProfile.employeeCode}.`);
      setEditingProfile(null);
      refresh();
    } catch (cause: unknown) {
      setError(errorMessage(cause, 'Không thể cập nhật thông tin giáo viên.'));
    } finally {
      setSubmitting(false);
    }
  }

  function openSubjectEditor(teacher: TeacherItem) {
    clearNotices();
    setEditingProfile(null);
    setEditingSubjectsFor(teacher);
    setEditingSubjects(teacher.subjects.map(subject => subject.id));
    setSubjectSearch('');
  }

  async function saveSubjects() {
    if (!editingSubjectsFor) return;
    if (editingSubjects.length === 0) {
      setError('Giáo viên phải có ít nhất một môn có thể giảng dạy.');
      return;
    }
    clearNotices();
    setSubmitting(true);
    try {
      await updateTeacherSubjects(editingSubjectsFor.id, editingSubjects);
      setMessage(`Đã cập nhật môn giảng dạy của ${editingSubjectsFor.employeeCode}.`);
      setEditingSubjectsFor(null);
      refresh();
    } catch (cause: unknown) {
      setError(errorMessage(cause, 'Không thể cập nhật môn giảng dạy.'));
    } finally {
      setSubmitting(false);
    }
  }

  async function toggleStatus(teacher: TeacherItem) {
    const locking = teacher.status === 'ACTIVE';
    const teaching = teacher.teachingAssignments.length
      ? teacher.teachingAssignments.map(item => `${item.subjectCode} - ${item.className}`).join(', ')
      : 'không có phân công dạy';
    const homeroom = teacher.homeroomClasses.length
      ? teacher.homeroomClasses.map(item => item.className).join(', ')
      : 'không chủ nhiệm lớp nào';
    const confirmation = locking
      ? `Khóa tài khoản ${teacher.name}?\n\nNăm đang chọn: ${teaching}; ${homeroom}.\nGiáo viên sẽ không thể đăng nhập, nhưng dữ liệu và phân công vẫn được giữ nguyên.`
      : `Mở khóa tài khoản ${teacher.name}? Giáo viên sẽ có thể đăng nhập lại.`;
    if (!window.confirm(confirmation)) return;

    clearNotices();
    setSubmitting(true);
    try {
      await updateUserStatus(teacher.userId, locking ? 'LOCKED' : 'ACTIVE');
      setMessage(locking ? `Đã khóa tài khoản ${teacher.employeeCode}.` : `Đã mở khóa tài khoản ${teacher.employeeCode}.`);
      refresh();
    } catch (cause: unknown) {
      setError(errorMessage(cause, 'Không thể đổi trạng thái tài khoản.'));
    } finally {
      setSubmitting(false);
    }
  }

  const firstItem = totalElements === 0 ? 0 : page * pageSize + 1;
  const lastItem = Math.min((page + 1) * pageSize, totalElements);

  return (
    <div className="page-stack teacher-management-page">
      <div className="page-heading">
        <div><span className="eyebrow">Phân hệ giáo viên</span><h1>Quản lý giáo viên</h1></div>
        <div className="page-heading-actions">
          <button type="button" onClick={() => { setShowForm(value => !value); setEditingProfile(null); setEditingSubjectsFor(null); }}>
            {showForm ? '✕ Đóng' : '＋ Tạo giáo viên'}
          </button>
        </div>
      </div>

      <div className="teacher-summary-grid" aria-label="Thống kê giáo viên">
        {[
          ['Tổng giáo viên', summary.total],
          ['Đang hoạt động', summary.active],
          ['Đã khóa', summary.locked],
          ['Chưa phân công', summary.unassigned],
          ['Đang chủ nhiệm', summary.homeroom],
        ].map(([label, value]) => <div className="teacher-summary-card" key={label}><span>{label}</span><strong>{value}</strong></div>)}
      </div>

      {!academicYearId && <div className="notice">Chọn năm học trên header để xem phân công giảng dạy và chủ nhiệm.</div>}
      {error && <div className="notice error">{error}</div>}
      {message && <div className="notice success">{message}</div>}

      {credential && <section className="teacher-credential-card" aria-live="polite">
        <div>
          <span className="eyebrow">Tài khoản vừa tạo</span>
          <h2>{credential.teacher.name} · {credential.teacher.employeeCode}</h2>
          <p>Mật khẩu tạm và tên đăng nhập đã được gửi đến email đã xác minh của giáo viên.</p>
        </div>
        <dl>
          <div><dt>Tài khoản</dt><dd>{credential.teacher.phone}</dd></div>
          <div><dt>Gửi email</dt><dd>{credential.credentialsEmailed ? 'Đã xếp hàng gửi' : 'Chưa gửi được'}</dd></div>
        </dl>
        <div className="teacher-action-row">
          <button type="button" className="secondary-button" onClick={() => setCredential(null)}>Đóng</button>
        </div>
      </section>}

      {showForm && <form className="teacher-create-form" onSubmit={create}>
        <div className="teacher-form-heading">
          <div><span className="teacher-form-index">01</span><div><h2>Thông tin giáo viên</h2><p>Tài khoản đăng nhập là số điện thoại.</p></div></div>
          <span className="teacher-required-note">* Thông tin bắt buộc</span>
        </div>
        <div className="teacher-primary-fields">
          <div className="form-group"><label>Họ và tên *</label><input required maxLength={100} placeholder="Ví dụ: Nguyễn Văn An" value={form.name} onChange={event => setForm({ ...form, name: event.target.value })} /></div>
          <div className="form-group"><label>Số điện thoại *</label><input required inputMode="numeric" pattern="0[0-9]{9}" placeholder="10 chữ số, bắt đầu bằng 0" value={form.phone} onChange={event => setForm({ ...form, phone: event.target.value })} /></div>
          <div className="form-group"><label>Email *</label><input required type="email" maxLength={255} placeholder="giaovien@myfschool.edu.vn" value={form.email} onChange={event => setForm({ ...form, email: event.target.value })} /></div>
        </div>
        <SubjectPicker subjects={subjects} selectedIds={subjectIds} onChange={setSubjectIds} />
        <div className="teacher-form-footer">
          <button disabled={submitting}>{submitting ? 'Đang tạo…' : 'Tạo giáo viên'}</button>
          <button type="button" className="secondary-button" onClick={() => { setShowForm(false); setForm(emptyForm); setSubjectIds([]); }}>Hủy</button>
        </div>
      </form>}

      {editingProfile && <form className="teacher-edit-panel" onSubmit={saveProfile}>
        <div><span className="eyebrow">Sửa thông tin</span><h2>{editingProfile.name} · {editingProfile.employeeCode}</h2></div>
        <div className="teacher-primary-fields">
          <div className="form-group"><label>Họ và tên *</label><input required maxLength={100} value={profileForm.name} onChange={event => setProfileForm({ ...profileForm, name: event.target.value })} /></div>
          <div className="form-group"><label>Số điện thoại *</label><input required inputMode="numeric" pattern="0[0-9]{9}" value={profileForm.phone} onChange={event => setProfileForm({ ...profileForm, phone: event.target.value })} /></div>
          <div className="form-group"><label>Email</label><input type="email" maxLength={255} value={profileForm.email} onChange={event => setProfileForm({ ...profileForm, email: event.target.value })} /></div>
        </div>
        <div className="teacher-action-row"><button disabled={submitting}>Lưu thông tin</button><button type="button" className="secondary-button" onClick={() => setEditingProfile(null)}>Hủy</button></div>
      </form>}

      {editingSubjectsFor && <section className="teacher-edit-panel">
        <div><span className="eyebrow">Môn có thể giảng dạy</span><h2>{editingSubjectsFor.name} · {editingSubjectsFor.employeeCode}</h2></div>
        <div className="form-group teacher-subject-search"><label>Tìm môn</label><input value={subjectSearch} onChange={event => setSubjectSearch(event.target.value)} placeholder="Tên hoặc mã môn" /></div>
        <SubjectPicker subjects={filteredSubjects} selectedIds={editingSubjects} onChange={setEditingSubjects} />
        <div className="teacher-action-row"><button type="button" disabled={submitting} onClick={saveSubjects}>Lưu môn giảng dạy</button><button type="button" className="secondary-button" onClick={() => setEditingSubjectsFor(null)}>Hủy</button></div>
      </section>}

      <form className="filters teacher-filters" onSubmit={event => { event.preventDefault(); commitSearch(); }}>
        <div className="form-group"><label>Tìm kiếm</label><input value={draftKeyword} onChange={event => setDraftKeyword(event.target.value)} placeholder="Tên, số điện thoại, mã GV" /></div>
        <div className="form-group"><label>Trạng thái</label><select value={status} onChange={event => { setStatus(event.target.value); setPage(0); }}><option value="ACTIVE">Đang hoạt động</option><option value="LOCKED">Đã khóa</option><option value="">Tất cả</option></select></div>
        <div className="form-group"><label>Bộ môn</label><select value={selectedSubjectId} onChange={event => { setSelectedSubjectId(event.target.value); setPage(0); }}><option value="">Tất cả</option>{subjects.map(subject => <option key={subject.id} value={subject.id}>{subject.name}</option>)}</select></div>
        <button type="submit" disabled={loading}>Tìm</button>
      </form>

      <div className="teacher-table-toolbar">
        <span>Hiển thị {firstItem}–{lastItem} trong {totalElements} giáo viên</span>
        <label>Số dòng <select value={pageSize} onChange={event => { setPageSize(Number(event.target.value)); setPage(0); }}><option value={20}>20</option><option value={50}>50</option><option value={100}>100</option></select></label>
      </div>

      <div className="table-responsive"><table className="teacher-table"><thead><tr><th>Mã GV</th><th>Họ tên</th><th>Liên hệ</th><th>Môn có thể dạy</th><th>Phân công năm đang chọn</th><th>Chủ nhiệm</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>
        {teachers.map(teacher => <tr key={teacher.id}>
          <td><strong>{teacher.employeeCode}</strong></td>
          <td>{teacher.name}</td>
          <td>{teacher.phone}{teacher.email && <><br /><small>{teacher.email}</small></>}</td>
          <td><div className="teacher-chip-list">{teacher.subjects.map(subject => <span className="teacher-chip" key={subject.id} title={subject.name}>{subject.code}</span>)}{teacher.subjects.length === 0 && <span className="muted-text">Chưa chọn môn</span>}</div></td>
          <td><div className="teacher-assignment-list">{teacher.teachingAssignments.map(item => <span key={item.id}><strong>{item.subjectCode}</strong> · {item.className}</span>)}{teacher.teachingAssignments.length === 0 && <span className="muted-text">Chưa phân công</span>}</div></td>
          <td>{teacher.homeroomClasses.length ? teacher.homeroomClasses.map(item => <span className="teacher-chip homeroom" key={item.id}>{item.className}</span>) : <span className="muted-text">Không</span>}</td>
          <td><span className={`badge-status ${teacher.status === 'ACTIVE' ? 'active' : ''}`}>{teacher.status === 'ACTIVE' ? 'Đang hoạt động' : 'Đã khóa'}</span></td>
          <td><div className="teacher-row-actions">
            <button type="button" className="secondary-button" onClick={() => openProfileEditor(teacher)}>Sửa</button>
            <button type="button" className="secondary-button" onClick={() => openSubjectEditor(teacher)}>Môn dạy</button>
            <button type="button" className={teacher.status === 'ACTIVE' ? 'danger' : 'secondary-button'} onClick={() => toggleStatus(teacher)} disabled={submitting}>{teacher.status === 'ACTIVE' ? 'Khóa' : 'Mở khóa'}</button>
          </div></td>
        </tr>)}
        {!teachers.length && !loading && <tr><td colSpan={8}>Chưa có giáo viên phù hợp.</td></tr>}
        {loading && <tr><td colSpan={8}>Đang tải danh sách giáo viên…</td></tr>}
      </tbody></table></div>

      <div className="teacher-pagination" aria-label="Phân trang giáo viên">
        <button type="button" className="secondary-button" disabled={page === 0 || loading} onClick={() => setPage(value => value - 1)}>Trang trước</button>
        <span>Trang {totalPages === 0 ? 0 : page + 1}/{totalPages}</span>
        <button type="button" className="secondary-button" disabled={page + 1 >= totalPages || loading} onClick={() => setPage(value => value + 1)}>Trang sau</button>
      </div>
    </div>
  );
}

function SubjectPicker({ subjects, selectedIds, onChange }: {
  subjects: SubjectItem[];
  selectedIds: number[];
  onChange: (ids: number[]) => void;
}) {
  return <fieldset className="teacher-subject-picker">
    <legend>Môn có thể giảng dạy *</legend>
    <div className="teacher-subject-grid">
      {subjects.map(subject => {
        const selected = selectedIds.includes(subject.id);
        return <label key={subject.id} className={selected ? 'selected' : ''}>
          <input type="checkbox" checked={selected} onChange={() => onChange(selected ? selectedIds.filter(id => id !== subject.id) : [...selectedIds, subject.id])} />
          <span><strong>{subject.name}</strong><small>{subject.code}</small></span>
        </label>;
      })}
      {subjects.length === 0 && <span className="input-desc">Không có môn học phù hợp.</span>}
    </div>
  </fieldset>;
}
