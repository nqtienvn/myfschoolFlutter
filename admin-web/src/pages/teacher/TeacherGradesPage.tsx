import { useEffect, useMemo, useState } from 'react';
import {
  getTeacherAssignments,
  getTeacherGradeBook,
  getTeacherGradeStudents,
  submitTeacherScores,
} from '../../api/teacher';
import { useTeacherAcademic } from '../../teacher/TeacherAcademicContext';

interface GradeItem { id: number; name: string; assessmentType: 'SCORE' | 'PASS_FAIL' | 'COMMENT'; entryRole: string; weight: number }
interface GradeBook { id: number; className: string; subjectName: string; status: string; items: GradeItem[] }
interface Score { studentId: number; studentName: string; studentCode: string; gradeItemId: number; score: number | null; comment: string | null; isGraded: boolean; average: number | null }

export default function TeacherGradesPage() {
  const { selectedYearId, selectedSemesterId } = useTeacherAcademic();
  const [assignments, setAssignments] = useState<any[]>([]);
  const [assignmentKey, setAssignmentKey] = useState('');
  const [book, setBook] = useState<GradeBook | null>(null);
  const [scores, setScores] = useState<Score[]>([]);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [busyItem, setBusyItem] = useState<number | null>(null);

  useEffect(() => {
    setAssignments([]); setAssignmentKey(''); setBook(null); setScores([]); setError(''); setMessage('');
    if (!selectedYearId) return;
    getTeacherAssignments(selectedYearId).then(rows => setAssignments(rows || []))
      .catch(cause => setError(cause instanceof Error ? cause.message : 'Không tải được phân công.'));
  }, [selectedYearId]);

  useEffect(() => {
    let cancelled = false;
    setBook(null); setScores([]); setError(''); setMessage('');
    if (!assignmentKey || !selectedSemesterId) return;

    const [classId, subjectId] = assignmentKey.split(':').map(Number);
    void (async () => {
      try {
        const value = await getTeacherGradeBook(classId, subjectId, selectedSemesterId) as GradeBook;
        const loadedScores = await getTeacherGradeStudents(value.id) as Score[];
        if (!cancelled) { setBook(value); setScores(loadedScores); }
      } catch (cause) {
        if (!cancelled) setError(cause instanceof Error ? cause.message : 'Không mở được bảng điểm.');
      }
    })();

    return () => { cancelled = true; };
  }, [assignmentKey, selectedSemesterId]);

  const students = useMemo(() => {
    const map = new Map<number, { id: number; name: string; code: string; average: number | null; values: Record<number, Score> }>();
    scores.forEach(row => {
      const student = map.get(row.studentId) || { id: row.studentId, name: row.studentName, code: row.studentCode, average: row.average, values: {} };
      student.values[row.gradeItemId] = row; student.average = row.average; map.set(row.studentId, student);
    });
    return [...map.values()];
  }, [scores]);

  function patchScore(studentId: number, gradeItemId: number, patch: Partial<Score>) {
    setScores(rows => rows.map(row => row.studentId === studentId && row.gradeItemId === gradeItemId ? { ...row, ...patch } : row));
  }

  const canEdit = (item: GradeItem) => book?.status !== 'LOCKED'
    && (item.entryRole === 'SUBJECT_TEACHER' || item.entryRole === 'SUBJECT_TEACHER_AND_ADMIN');

  async function submitItem(item: GradeItem) {
    if (!book) return;
    setBusyItem(item.id); setError(''); setMessage('');
    try {
      const entries = students.map(student => {
        const value = student.values[item.id];
        return {
          studentId: student.id,
          score: item.assessmentType === 'SCORE' ? value?.score ?? null : null,
          comment: item.assessmentType === 'SCORE' ? null : value?.comment || null,
          isGraded: item.assessmentType === 'SCORE' ? value?.score != null : !!value?.comment?.trim(),
        };
      });
      await submitTeacherScores(item.id, entries);
      setScores(await getTeacherGradeStudents(book.id) as Score[]);
      setMessage(`Đã submit ${item.name}. Điểm hiện đã hiển thị cho phụ huynh và học sinh.`);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Không lưu được điểm.'); }
    finally { setBusyItem(null); }
  }

  return <main className="teacher-page page-stack">
    <div className="page-heading"><div><span className="eyebrow">Lớp giảng dạy</span><h1>Nhập điểm</h1><p>Nhập trực tiếp trên lưới; khi submit, điểm được công bố ngay cho phụ huynh và học sinh.</p></div></div>
    {error && <div className="notice error">{error}</div>}{message && <div className="notice success">{message}</div>}
    <section className="panel teacher-filter-row">
      <label>Lớp · Môn<select value={assignmentKey} onChange={event => { setAssignmentKey(event.target.value); setBook(null); setScores([]); }}><option value="">Chọn phân công</option>{assignments.map(item => <option key={`${item.classId}:${item.subjectId}`} value={`${item.classId}:${item.subjectId}`}>{item.className} · {item.subjectName}</option>)}</select></label>
    </section>
    {book && <section className="panel">
      <div className="monitoring-actions"><h2>{book.className} · {book.subjectName}</h2><span className="badge-status active">{book.status}</span></div>
      <div className="table-responsive"><table className="teacher-grades-table"><thead><tr><th>Học sinh</th>{book.items.map(item => <th key={item.id}>{item.name}<small className="table-subtext">{item.assessmentType}{item.assessmentType === 'SCORE' ? ` · HS ${item.weight}` : ''}</small>{canEdit(item) && <button disabled={busyItem === item.id} onClick={() => submitItem(item)}>{busyItem === item.id ? 'Đang submit…' : 'Submit & công bố'}</button>}</th>)}<th>ĐTB</th></tr></thead><tbody>{students.map(student => <tr key={student.id}><td><strong>{student.name}</strong><small className="table-subtext">{student.code}</small></td>{book.items.map(item => { const value = student.values[item.id]; const disabled = !canEdit(item); return <td key={item.id}>{item.assessmentType === 'SCORE' ? <input type="number" min="0" max="10" step="0.1" disabled={disabled} value={value?.score ?? ''} onChange={event => patchScore(student.id, item.id, { score: event.target.value === '' ? null : Number(event.target.value), isGraded: !!event.target.value })}/> : item.assessmentType === 'PASS_FAIL' ? <select disabled={disabled} value={value?.comment || ''} onChange={event => patchScore(student.id, item.id, { comment: event.target.value || null, isGraded: !!event.target.value })}><option value="">—</option><option value="PASS">Đạt</option><option value="FAIL">Chưa đạt</option></select> : <textarea rows={2} disabled={disabled} value={value?.comment || ''} onChange={event => patchScore(student.id, item.id, { comment: event.target.value, isGraded: !!event.target.value.trim() })}/>}</td>; })}<td><strong>{student.average ?? '—'}</strong></td></tr>)}</tbody></table></div>
    </section>}
  </main>;
}
