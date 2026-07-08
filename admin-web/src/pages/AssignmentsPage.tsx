import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

interface AcademicYearItem { id: number; name: string; status: string; }
interface ClassItem { id: number; name: string; academicYearId: number; academicYearName: string; }
interface SubjectItem { id: number; name: string; code: string; }
interface SemesterItem { id: number; name: string; academicYearId: number; academicYearName: string; isCurrent: boolean; order: number; }
interface TeacherItem { id: number; name: string; phone?: string; teacherProfile?: { id: number; employeeCode: string; } }
interface TeachingAssignmentItem {
  id: number;
  subjectName: string;
  subjectCode: string;
  teacherName: string;
  teacherCode: string;
  semesterName: string;
  effectiveFrom: string;
  effectiveTo: string | null;
  status: string;
}
interface HomeroomAssignmentItem { id: number; teacherName: string; effectiveFrom: string; effectiveTo: string | null; }

const today = () => new Date().toISOString().slice(0, 10);

export default function AssignmentsPage() {
  const [academicYears, setAcademicYears] = useState<AcademicYearItem[]>([]);
  const [academicYearId, setAcademicYearId] = useState('');
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [subjects, setSubjects] = useState<SubjectItem[]>([]);
  const [semesters, setSemesters] = useState<SemesterItem[]>([]);
  const [teachers, setTeachers] = useState<TeacherItem[]>([]);
  const [assignments, setAssignments] = useState<TeachingAssignmentItem[]>([]);
  const [homeroomTeacher, setHomeroomTeacher] = useState<HomeroomAssignmentItem | null>(null);
  const [loadingList, setLoadingList] = useState(false);
  const [classId, setClassId] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [teacherId, setTeacherId] = useState('');
  const [semesterId, setSemesterId] = useState('');
  const [effectiveFrom, setEffectiveFrom] = useState(today());
  const [isHomeroom, setIsHomeroom] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  useEffect(() => {
    fetchAcademicYears();
    apiFetch('/subjects').then((d: any) => setSubjects(d || [])).catch(() => {});
    apiFetch('/admin/users?role=TEACHER&page=0&size=100')
      .then((d: any) => setTeachers(d.content || d || []))
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (!academicYearId) return;
    setClassId('');
    setSemesterId('');
    setAssignments([]);
    setHomeroomTeacher(null);
    apiFetch(`/classes?academicYearId=${academicYearId}&page=0&size=100`)
      .then((d: any) => setClasses(d.content || []))
      .catch(() => {});
    apiFetch(`/semesters?academicYearId=${academicYearId}`)
      .then((d: any) => {
        const list = d || [];
        setSemesters(list);
        const current = list.find((s: SemesterItem) => s.isCurrent) || list[0];
        if (current) setSemesterId(String(current.id));
      })
      .catch(() => {});
  }, [academicYearId]);

  useEffect(() => {
    if (!classId || !semesterId || !academicYearId) {
      setAssignments([]);
      setHomeroomTeacher(null);
      return;
    }
    loadAssignments();
  }, [classId, semesterId, academicYearId]);

  async function fetchAcademicYears() {
    const data = await apiFetch('/academic-years') as AcademicYearItem[];
    setAcademicYears(data);
    const active = data.find(y => y.status === 'ACTIVE') || data[0];
    if (active) setAcademicYearId(String(active.id));
  }

  async function loadAssignments() {
    setLoadingList(true);
    try {
      const [teaching, homeroom] = await Promise.all([
        apiFetch(`/teaching-assignments?classId=${classId}&semesterId=${semesterId}`) as Promise<TeachingAssignmentItem[]>,
        apiFetch(`/homeroom-assignments/current?classId=${classId}&academicYearId=${academicYearId}`).catch(() => null) as Promise<HomeroomAssignmentItem | null>,
      ]);
      setAssignments(teaching || []);
      setHomeroomTeacher(homeroom || null);
    } catch (err: any) {
      setError(err.message || 'Không tải được danh sách phân công');
    } finally {
      setLoadingList(false);
    }
  }

  async function assign() {
    setError('');
    setSuccessMsg('');
    if (!classId || !subjectId || !teacherId || !semesterId || !effectiveFrom) {
      setError('Vui lòng chọn đủ Năm học, Lớp, Học kỳ, Môn học, Giáo viên và ngày hiệu lực.');
      return;
    }

    try {
      await apiFetch('/teaching-assignments', {
        method: 'POST',
        body: JSON.stringify({ classId: +classId, subjectId: +subjectId, teacherId: +teacherId, semesterId: +semesterId, effectiveFrom }),
      });
      if (isHomeroom) {
        const body = JSON.stringify({ classId: +classId, teacherId: +teacherId, academicYearId: +academicYearId, effectiveFrom });
        await apiFetch(homeroomTeacher ? `/homeroom-assignments/${homeroomTeacher.id}` : '/homeroom-assignments', {
          method: homeroomTeacher ? 'PUT' : 'POST',
          body,
        });
      }
      setSuccessMsg(isHomeroom ? 'Đã lưu phân công dạy và GVCN!' : 'Đã lưu phân công dạy!');
      setSubjectId('');
      setTeacherId('');
      setIsHomeroom(false);
      loadAssignments();
    } catch (err: any) {
      setError(err.message || 'Lỗi phân công giảng dạy');
    }
  }

  async function handleRemove(id: number) {
    if (!confirm('Bạn có chắc chắn muốn kết thúc phân công này?')) return;
    setError('');
    setSuccessMsg('');
    try {
      await apiFetch(`/teaching-assignments/${id}`, { method: 'DELETE' });
      setSuccessMsg('Đã kết thúc phân công.');
      loadAssignments();
    } catch (err: any) {
      setError(err.message || 'Lỗi khi kết thúc phân công');
    }
  }

  async function handleRemoveHomeroom() {
    if (!homeroomTeacher || !confirm('Bạn có chắc chắn muốn gỡ GVCN hiện tại?')) return;
    setError('');
    setSuccessMsg('');
    try {
      await apiFetch(`/homeroom-assignments/${homeroomTeacher.id}`, { method: 'DELETE' });
      setSuccessMsg('Đã gỡ GVCN.');
      loadAssignments();
    } catch (err: any) {
      setError(err.message || 'Lỗi khi gỡ GVCN');
    }
  }

  return (
    <div>
      <h2>Phân công giáo viên</h2>
      {error && <div className="error">{error}</div>}
      {successMsg && <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: '16px' }}>[SUCCESS] {successMsg}</div>}

      <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))' }}>
        <div className="form-group">
          <label>Năm học</label>
          <select value={academicYearId} onChange={e => setAcademicYearId(e.target.value)}>
            <option value="">Chọn năm học</option>
            {academicYears.map(y => <option key={y.id} value={y.id}>{y.name} {y.status === 'ACTIVE' ? '(Đang mở)' : ''}</option>)}
          </select>
          <span className="input-desc">Năm học áp dụng</span>
        </div>

        <div className="form-group">
          <label>Lớp học</label>
          <select value={classId} onChange={e => setClassId(e.target.value)} disabled={!academicYearId}>
            <option value="">Chọn lớp học</option>
            {classes.map(c => <option key={c.id} value={c.id}>{c.name} ({c.academicYearName})</option>)}
          </select>
          <span className="input-desc">Chọn lớp để phân công và xem danh sách</span>
        </div>

        <div className="form-group">
          <label>Học kỳ</label>
          <select value={semesterId} onChange={e => setSemesterId(e.target.value)} disabled={!academicYearId}>
            <option value="">Chọn học kỳ</option>
            {semesters.map(s => <option key={s.id} value={s.id}>{s.name} - {s.academicYearName} {s.isCurrent ? '(Hiện tại)' : ''}</option>)}
          </select>
          <span className="input-desc">Áp dụng cho học kỳ nào</span>
        </div>

        <div className="form-group">
          <label>Môn học</label>
          <select value={subjectId} onChange={e => setSubjectId(e.target.value)}>
            <option value="">Chọn môn học</option>
            {subjects.map(s => <option key={s.id} value={s.id}>{s.name} ({s.code})</option>)}
          </select>
          <span className="input-desc">Môn học giáo viên sẽ dạy</span>
        </div>

        <div className="form-group">
          <label>Giáo viên giảng dạy</label>
          <select value={teacherId} onChange={e => setTeacherId(e.target.value)}>
            <option value="">Chọn giáo viên</option>
            {teachers.map(t => t.teacherProfile?.id ? (
              <option key={t.id} value={t.teacherProfile.id}>{t.name} ({t.teacherProfile.employeeCode || 'GV'})</option>
            ) : null)}
          </select>
          <span className="input-desc">Chọn giáo viên trong danh sách trường</span>
        </div>

        <div className="form-group">
          <label>Hiệu lực từ</label>
          <input type="date" value={effectiveFrom} onChange={e => setEffectiveFrom(e.target.value)} />
          <span className="input-desc">Ngày bắt đầu phân công</span>
        </div>

        <div className="form-group" style={{ justifyContent: 'center' }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '12px' }}>
            <input type="checkbox" checked={isHomeroom} onChange={e => setIsHomeroom(e.target.checked)} style={{ width: '16px', height: '16px', cursor: 'pointer' }} />
            Đồng thời đặt làm Giáo viên chủ nhiệm
          </label>
          <span className="input-desc">GVCN lưu riêng ở HomeroomAssignment</span>
        </div>

        <div className="form-group">
          <label style={{ visibility: 'hidden' }}>Thao tác</label>
          <button onClick={assign} style={{ height: '38px', width: '100%' }}>Lưu phân công</button>
          <span className="input-desc" style={{ visibility: 'hidden' }}>&nbsp;</span>
        </div>
      </div>

      {classId && (
        <div style={{ marginTop: '24px', padding: '12px 16px', background: '#fafafa', borderLeft: '3px solid #000000', fontSize: '13px' }}>
          <strong>GVCN hiện tại:</strong> {homeroomTeacher ? `${homeroomTeacher.teacherName} (từ ${homeroomTeacher.effectiveFrom})` : 'Chưa có'}
          {homeroomTeacher && <button className="danger" onClick={handleRemoveHomeroom} style={{ marginLeft: '12px' }}>Gỡ GVCN</button>}
        </div>
      )}

      {classId && (
        <div style={{ marginTop: '32px' }}>
          <h3 style={{ fontSize: '15px', fontWeight: 700, textTransform: 'uppercase', marginBottom: '16px', borderBottom: '1px dashed #000000', paddingBottom: '8px' }}>
            Danh sách Phân công của Lớp: {classes.find(c => c.id === +classId)?.name}
          </h3>
          {loadingList ? <p style={{ fontSize: '13px', fontFamily: 'ui-monospace, monospace' }}>Đang tải danh sách phân công...</p> : assignments.length === 0 ? (
            <p style={{ fontSize: '13px', color: '#a3a3a3', fontStyle: 'italic' }}>Chưa có giáo viên nào được phân công giảng dạy cho lớp này.</p>
          ) : (
            <table>
              <thead>
                <tr><th>ID</th><th>Môn học</th><th>Mã môn</th><th>Giáo viên dạy</th><th>Học kỳ</th><th>Hiệu lực</th><th>Trạng thái</th><th>Thao tác</th></tr>
              </thead>
              <tbody>
                {assignments.map(item => (
                  <tr key={item.id}>
                    <td>{item.id}</td>
                    <td style={{ fontWeight: 600 }}>{item.subjectName}</td>
                    <td>{item.subjectCode}</td>
                    <td>{item.teacherName} ({item.teacherCode})</td>
                    <td>{item.semesterName}</td>
                    <td>{item.effectiveFrom}{item.effectiveTo ? ` → ${item.effectiveTo}` : ''}</td>
                    <td>{item.status === 'ACTIVE' ? <span style={{ background: '#000000', color: '#ffffff', padding: '2px 8px', fontSize: '11px', fontWeight: 700 }}>ĐANG DẠY</span> : <span style={{ color: '#737373' }}>Ngừng</span>}</td>
                    <td><button className="danger" onClick={() => handleRemove(item.id)}>Ngừng dạy</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
