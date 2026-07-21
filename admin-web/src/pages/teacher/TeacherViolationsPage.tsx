import { useEffect, useState, type FormEvent } from 'react';
import {
  deleteTeacherViolation,
  getHomeroomClass,
  getTeacherDashboard,
  getTeacherViolations,
  saveTeacherViolation,
  submitHomeroomClassViolations,
  submitTeacherViolations,
} from '../../api/teacher';
import { useTeacherAcademic } from '../../teacher/TeacherAcademicContext';

interface Student { id: number; name: string; studentCode: string }
interface Violation {
  id: number;
  title: string;
  category: string | null;
  description: string | null;
  eventDate: string;
  status: 'DRAFT' | 'SUBMITTED';
}

const emptyViolation = { title: '', category: '', description: '', eventDate: '' };

export default function TeacherViolationsPage() {
  const { selectedYearId, selectedSemesterId } = useTeacherAcademic();
  const [classId, setClassId] = useState<number | null>(null);
  const [className, setClassName] = useState('');
  const [students, setStudents] = useState<Student[]>([]);
  const [selectedStudent, setSelectedStudent] = useState<Student | null>(null);
  const [violations, setViolations] = useState<Violation[]>([]);
  const [editingViolation, setEditingViolation] = useState<Violation | null>(null);
  const [violation, setViolation] = useState(emptyViolation);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setClassId(null); setClassName(''); setStudents([]); setSelectedStudent(null);
    setViolations([]); setEditingViolation(null); setViolation(emptyViolation);
    setError(''); setMessage('');
    if (!selectedYearId || !selectedSemesterId) return;

    void (async () => {
      try {
        const dashboard: any = await getTeacherDashboard(selectedYearId, selectedSemesterId);
        const detail: any = await getHomeroomClass(dashboard.classId);
        if (cancelled) return;
        setClassId(dashboard.classId);
        setClassName(dashboard.className || detail.name || '');
        setStudents(detail.students || []);
      } catch (cause) {
        if (!cancelled) setError(cause instanceof Error ? cause.message : 'Không có lớp chủ nhiệm trong phạm vi đã chọn.');
      }
    })();

    return () => { cancelled = true; };
  }, [selectedYearId, selectedSemesterId]);

  function showError(cause: unknown) {
    setMessage('');
    setError(cause instanceof Error ? cause.message : 'Có lỗi xảy ra.');
  }

  async function loadViolations(student: Student) {
    if (!selectedYearId || !selectedSemesterId || !classId) return;
    setSelectedStudent(student); setViolations([]); setEditingViolation(null);
    setViolation(emptyViolation); setError(''); setMessage('');
    try {
      setViolations(await getTeacherViolations(
        student.id, selectedYearId, selectedSemesterId, classId,
      ) as Violation[]);
    } catch (cause) { showError(cause); }
  }

  async function reloadSelected() {
    if (!selectedStudent || !selectedYearId || !selectedSemesterId || !classId) return;
    setViolations(await getTeacherViolations(
      selectedStudent.id, selectedYearId, selectedSemesterId, classId,
    ) as Violation[]);
  }

  async function saveViolation(event: FormEvent) {
    event.preventDefault();
    if (!selectedStudent || !selectedYearId || !selectedSemesterId || !classId) return;
    setBusy(true); setError(''); setMessage('');
    try {
      await saveTeacherViolation(selectedStudent.id, {
        academicYearId: selectedYearId,
        semesterId: selectedSemesterId,
        classId,
        ...violation,
      }, editingViolation?.id);
      await reloadSelected();
      setEditingViolation(null); setViolation(emptyViolation);
      setMessage('Đã lưu vi phạm ở trạng thái nháp.');
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  async function removeViolation(id: number) {
    if (!selectedYearId || !confirm('Xóa vi phạm này?')) return;
    setBusy(true); setError(''); setMessage('');
    try {
      await deleteTeacherViolation(id, selectedYearId);
      await reloadSelected();
      setMessage('Đã xóa vi phạm.');
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  async function submitStudent() {
    if (!selectedStudent || !selectedYearId || !selectedSemesterId || !classId
      || !confirm(`Submit các vi phạm nháp của ${selectedStudent.name} cho Admin?`)) return;
    setBusy(true); setError(''); setMessage('');
    try {
      setViolations(await submitTeacherViolations(selectedStudent.id, {
        academicYearId: selectedYearId, semesterId: selectedSemesterId, classId,
      }) as Violation[]);
      setEditingViolation(null); setViolation(emptyViolation);
      setMessage('Đã Submit vi phạm cho Admin. Dữ liệu đã gửi không thể sửa hoặc xóa.');
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  async function submitClass() {
    if (!selectedYearId || !selectedSemesterId || !classId
      || !confirm(`Submit toàn bộ vi phạm nháp của lớp ${className} cho Admin?`)) return;
    setBusy(true); setError(''); setMessage('');
    try {
      await submitHomeroomClassViolations({
        academicYearId: selectedYearId, semesterId: selectedSemesterId, classId,
      });
      if (selectedStudent) await reloadSelected();
      setEditingViolation(null); setViolation(emptyViolation);
      setMessage('Đã Submit toàn bộ vi phạm nháp của lớp cho Admin.');
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  const hasDrafts = violations.some(item => item.status === 'DRAFT');

  return <main className="teacher-page page-stack">
    <div className="page-heading"><div><span className="eyebrow">Công tác chủ nhiệm</span><h1>Vi phạm học sinh</h1><p>GVCN ghi nhận vi phạm và Submit cho Admin thống kê kết quả rèn luyện.</p></div></div>
    {error && <div className="notice error">{error}</div>}
    {message && <div className="notice success">{message}</div>}
    {classId && <div className="discipline-grid">
      <section className="panel result-panel">
        <div className="result-panel-heading"><div><h2>Lớp {className}</h2><p>Chọn học sinh để ghi nhận vi phạm.</p></div>
          <button disabled={busy || !students.length} onClick={submitClass}>Submit cả lớp</button></div>
        <div className="table-responsive"><table><thead><tr><th>Học sinh</th><th>Mã HS</th><th /></tr></thead><tbody>
          {students.map(student => <tr key={student.id}><td><strong>{student.name}</strong></td><td>{student.studentCode}</td><td><button className="secondary-button" onClick={() => loadViolations(student)}>Chọn</button></td></tr>)}
        </tbody></table></div>
      </section>
      <section className="panel result-panel">
        <div className="result-panel-heading"><div><h2>{selectedStudent?.name || 'Chi tiết vi phạm'}</h2><p>{selectedStudent ? selectedStudent.studentCode : 'Chọn học sinh ở danh sách bên trái.'}</p></div>
          {selectedStudent && <button disabled={busy || !hasDrafts} onClick={submitStudent}>Submit cho Admin</button>}</div>
        {selectedStudent && <>
          <form className="violation-form" onSubmit={saveViolation}>
            <label>Tiêu đề<input required maxLength={200} value={violation.title} onChange={event => setViolation(value => ({ ...value, title: event.target.value }))}/></label>
            <label>Phân loại<input maxLength={100} value={violation.category} onChange={event => setViolation(value => ({ ...value, category: event.target.value }))}/></label>
            <label>Ngày vi phạm<input required type="date" value={violation.eventDate} onChange={event => setViolation(value => ({ ...value, eventDate: event.target.value }))}/></label>
            <label className="wide">Mô tả<textarea rows={3} maxLength={2000} value={violation.description} onChange={event => setViolation(value => ({ ...value, description: event.target.value }))}/></label>
            <div className="monitoring-actions wide"><button disabled={busy}>{editingViolation ? 'Lưu thay đổi' : 'Thêm vi phạm'}</button>{editingViolation && <button type="button" className="secondary-button" onClick={() => { setEditingViolation(null); setViolation(emptyViolation); }}>Hủy</button>}</div>
          </form>
          <div className="violation-list">{violations.map(item => <article key={item.id}><div><strong>{item.title}</strong><small>{item.category || 'Vi phạm'} · {item.eventDate} · {item.status === 'SUBMITTED' ? 'Đã Submit' : 'Bản nháp'}</small><p>{item.description}</p></div>{item.status === 'DRAFT' && <div className="table-actions"><button className="secondary-button" disabled={busy} onClick={() => { setEditingViolation(item); setViolation({ title: item.title, category: item.category || '', description: item.description || '', eventDate: item.eventDate }); }}>Sửa</button><button className="danger" disabled={busy} onClick={() => removeViolation(item.id)}>Xóa</button></div>}</article>)}</div>
          {!violations.length && <div className="empty-state">Học sinh chưa có vi phạm trong học kỳ.</div>}
        </>}
      </section>
    </div>}
  </main>;
}
