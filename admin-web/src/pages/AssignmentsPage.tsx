import { useEffect, useMemo, useState } from 'react';
import { getClasses } from '../api/class';
import { getAcademicYearMasterData } from '../api/academicYearConfig';
import { getSubjects } from '../api/subject';
import { getTeachers, type TeacherItem } from '../api/user';
import { createTeachingAssignment, deleteTeachingAssignment, getTeachingAssignments, updateTeachingAssignment } from '../api/teachingAssignment';

interface ClassItem { id: number; name: string; gradeLevel: number; }
interface SubjectItem { id: number; name: string; code: string; }
interface AssignmentItem { id: number; subjectId: number; teacherId: number; status: string; }

export default function AssignmentsPage({ selectedYearId }: { selectedYearId?: string }) {
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [subjects, setSubjects] = useState<SubjectItem[]>([]);
  const [teachers, setTeachers] = useState<TeacherItem[]>([]);
  const [assignments, setAssignments] = useState<AssignmentItem[]>([]);
  const [classId, setClassId] = useState('');
  const [grade, setGrade] = useState('10');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    getTeachers({ status: 'ACTIVE', page: 0, size: 500 })
      .then((teacherPage: any) => setTeachers(teacherPage.content || []));
  }, []);

  useEffect(() => {
    setClassId(''); setAssignments([]);
    if (!selectedYearId) { setClasses([]); setSubjects([]); return; }
    Promise.all([
      getClasses({ academicYearId: selectedYearId, page: 0, size: 500 }) as any,
      getSubjects() as Promise<SubjectItem[]>,
      getAcademicYearMasterData(selectedYearId),
    ]).then(([page, subjectList, config]) => {
      setClasses(page.content || []);
      setSubjects((subjectList || []).filter(subject => config.subjectIds.includes(subject.id)));
    }).catch((cause: any) => setError(cause.message || 'Không thể tải dữ liệu phân công.'));
  }, [selectedYearId]);

  async function loadAssignments(targetClassId = classId) {
    if (!targetClassId) return setAssignments([]);
    const data = await getTeachingAssignments({ classId: targetClassId }) as AssignmentItem[];
    setAssignments(data || []);
  }
  useEffect(() => { loadAssignments(); }, [classId]);

  const filteredClasses = useMemo(() => classes.filter(item => String(item.gradeLevel) === grade), [classes, grade]);
  const selectedClass = classes.find(item => String(item.id) === classId);

  async function assign(subjectId: number, teacherId: string) {
    if (!classId) return;
    setLoading(true); setError(''); setMessage('');
    const existing = assignments.find(item => item.subjectId === subjectId);
    try {
      if (!teacherId && existing) await deleteTeachingAssignment(existing.id);
      if (teacherId) {
        const body = { classId: Number(classId), subjectId, teacherId: Number(teacherId) };
        if (existing) await updateTeachingAssignment(existing.id, body); else await createTeachingAssignment(body);
      }
      setMessage('Đã lưu phân công giảng dạy.');
      await loadAssignments();
    } catch (cause: any) { setError(cause.message || 'Không thể lưu phân công.'); }
    finally { setLoading(false); }
  }

  return <div className="page-stack">
    <div className="page-heading"><div><span className="eyebrow">Bước 6</span><h1>Phân công giảng dạy</h1><p>Mỗi lớp chỉ có một giáo viên phụ trách một môn trong suốt năm học.</p></div></div>
    {!selectedYearId && <div className="notice warning">Chọn năm học ở thanh phía trên.</div>}
    {error && <div className="notice error">{error}</div>}{message && <div className="notice success">{message}</div>}
    <div className="form-inline">
      <div className="form-group"><label>Khối</label><select value={grade} onChange={e => { setGrade(e.target.value); setClassId(''); }}>{Array.from({ length: 12 }, (_, index) => index + 1).map(value => <option key={value}>{value}</option>)}</select></div>
      <div className="form-group"><label>Lớp</label><select value={classId} onChange={e => setClassId(e.target.value)}><option value="">Chọn lớp</option>{filteredClasses.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div>
    </div>
    {selectedClass && <div className="table-responsive"><table><thead><tr><th>Môn học</th><th>Giáo viên đủ chuyên môn</th><th>Trạng thái</th></tr></thead><tbody>{subjects.map(subject => {
      const assignment = assignments.find(item => item.subjectId === subject.id);
      const eligible = teachers.filter(teacher => teacher.subjects.some(item => item.id === subject.id));
      return <tr key={subject.id}><td><strong>{subject.name}</strong><br /><small>{subject.code}</small></td><td><select disabled={loading} value={assignment?.teacherId || ''} onChange={event => assign(subject.id, event.target.value)}><option value="">Chưa phân công</option>{eligible.map(teacher => <option key={teacher.id} value={teacher.id}>{teacher.name} · {teacher.employeeCode}</option>)}</select></td><td><span className={`badge-status ${assignment ? 'active' : ''}`}>{assignment ? 'ĐÃ PHÂN CÔNG' : 'CÒN THIẾU'}</span></td></tr>;
    })}</tbody></table></div>}
    {!selectedClass && <div className="notice">Chọn một lớp để xem và phân công toàn bộ môn học.</div>}
  </div>;
}
