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
interface GradeStudent { id: number; name: string; code: string; values: Record<number, Score> }

function getSavedItemIds(scores: Score[]) {
  return [...new Set(scores.filter(score => score.isGraded).map(score => score.gradeItemId))];
}

export default function TeacherGradesPage() {
  const { selectedYearId, selectedSemesterId } = useTeacherAcademic();
  const [assignments, setAssignments] = useState<any[]>([]);
  const [assignmentKey, setAssignmentKey] = useState('');
  const [book, setBook] = useState<GradeBook | null>(null);
  const [scores, setScores] = useState<Score[]>([]);
  const [savedItemIds, setSavedItemIds] = useState<number[]>([]);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [busyItem, setBusyItem] = useState<number | null>(null);
  const [selectedItemId, setSelectedItemId] = useState<number | null>(null);
  const [editingItemId, setEditingItemId] = useState<number | null>(null);

  useEffect(() => {
    if (!message) return;
    const timeoutId = window.setTimeout(() => setMessage(''), 7000);
    return () => window.clearTimeout(timeoutId);
  }, [message]);

  useEffect(() => {
    setAssignments([]); setAssignmentKey(''); setBook(null); setScores([]); setSavedItemIds([]); setSelectedItemId(null); setEditingItemId(null); setError(''); setMessage('');
    if (!selectedYearId) return;
    getTeacherAssignments(selectedYearId).then(rows => setAssignments(rows || []))
      .catch(cause => setError(cause instanceof Error ? cause.message : 'Không tải được phân công.'));
  }, [selectedYearId]);

  useEffect(() => {
    let cancelled = false;
    setBook(null); setScores([]); setSavedItemIds([]); setSelectedItemId(null); setEditingItemId(null); setError(''); setMessage('');
    if (!assignmentKey || !selectedSemesterId) return;

    const [classId, subjectId] = assignmentKey.split(':').map(Number);
    void (async () => {
      try {
        const value = await getTeacherGradeBook(classId, subjectId, selectedSemesterId) as GradeBook;
        const loadedScores = await getTeacherGradeStudents(value.id) as Score[];
        if (!cancelled) { setBook(value); setScores(loadedScores); setSavedItemIds(getSavedItemIds(loadedScores)); }
      } catch (cause) {
        if (!cancelled) setError(cause instanceof Error ? cause.message : 'Không mở được bảng điểm.');
      }
    })();

    return () => { cancelled = true; };
  }, [assignmentKey, selectedSemesterId]);

  const students = useMemo<GradeStudent[]>(() => {
    const map = new Map<number, GradeStudent>();
    scores.forEach(row => {
      const student = map.get(row.studentId) || { id: row.studentId, name: row.studentName, code: row.studentCode, values: {} };
      student.values[row.gradeItemId] = row; map.set(row.studentId, student);
    });
    return [...map.values()];
  }, [scores]);

  const isTeacherItem = (item: GradeItem) =>
    item.entryRole === 'SUBJECT_TEACHER' || item.entryRole === 'SUBJECT_TEACHER_AND_ADMIN';

  const canEdit = (item: GradeItem) => book?.status !== 'LOCKED' && isTeacherItem(item);

  const teacherItems = useMemo(() => book?.items.filter(isTeacherItem) || [], [book]);
  const selectedItem = teacherItems.find(item => item.id === selectedItemId) || null;
  const hasSubmittedScores = (item: GradeItem) => savedItemIds.includes(item.id);
  const isSelectedItemEditable = !!selectedItem && canEdit(selectedItem);
  const isEditingSelectedItem = !!selectedItem && isSelectedItemEditable
    && (editingItemId === selectedItem.id || !hasSubmittedScores(selectedItem));

  useEffect(() => {
    setSelectedItemId(current => teacherItems.some(item => item.id === current) ? current : teacherItems[0]?.id || null);
    setEditingItemId(null);
  }, [book]);

  function patchScore(studentId: number, gradeItemId: number, patch: Partial<Score>) {
    setScores(rows => rows.map(row => row.studentId === studentId && row.gradeItemId === gradeItemId ? { ...row, ...patch } : row));
  }

  async function submitItem(item: GradeItem) {
    if (!book) return false;
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
      const refreshedScores = await getTeacherGradeStudents(book.id) as Score[];
      setScores(refreshedScores);
      setSavedItemIds(getSavedItemIds(refreshedScores));
      setMessage(`Đã lưu ${item.name}. Điểm hiện đã hiển thị cho phụ huynh và học sinh.`);
      return true;
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không lưu được điểm.');
      return false;
    }
    finally { setBusyItem(null); }
  }

  async function handleItemAction(item: GradeItem) {
    if (hasSubmittedScores(item) && editingItemId !== item.id) {
      setEditingItemId(item.id);
      return;
    }
    if (await submitItem(item)) setEditingItemId(null);
  }

  function renderScoreInput(student: GradeStudent, item: GradeItem) {
    const value = student.values[item.id];
    const disabled = !isEditingSelectedItem;
    if (item.assessmentType === 'SCORE') {
      return <input aria-label={`Điểm ${item.name} của ${student.name}`} type="number" min="0" max="10" step="0.1" disabled={disabled}
        value={value?.score ?? ''} onChange={event => patchScore(student.id, item.id, { score: event.target.value === '' ? null : Number(event.target.value), isGraded: event.target.value !== '' })} />;
    }
    if (item.assessmentType === 'PASS_FAIL') {
      return <select aria-label={`Kết quả ${item.name} của ${student.name}`} disabled={disabled} value={value?.comment || ''}
        onChange={event => patchScore(student.id, item.id, { comment: event.target.value || null, isGraded: event.target.value !== '' })}>
        <option value="">—</option><option value="PASS">Đạt</option><option value="FAIL">Chưa đạt</option>
      </select>;
    }
    return <textarea aria-label={`Nhận xét ${item.name} của ${student.name}`} rows={2} disabled={disabled} value={value?.comment || ''}
      onChange={event => patchScore(student.id, item.id, { comment: event.target.value, isGraded: event.target.value.trim() !== '' })} />;
  }

  return <main className="teacher-page page-stack">
    <div className="page-heading"><div><span className="eyebrow">Lớp giảng dạy</span><h1>Nhập điểm</h1></div></div>
    {error && <div className="notice error">{error}</div>}
    {message && <div className="success-toast" role="status"><span>{message}</span><button type="button" aria-label="Đóng thông báo" onClick={() => setMessage('')}>×</button></div>}
    <section className="panel teacher-filter-row">
      <label>Lớp · Môn<select value={assignmentKey} onChange={event => { setAssignmentKey(event.target.value); setBook(null); setScores([]); setSavedItemIds([]); setSelectedItemId(null); setEditingItemId(null); }}><option value="">Chọn phân công</option>{assignments.map(item => <option key={`${item.classId}:${item.subjectId}`} value={`${item.classId}:${item.subjectId}`}>{item.className} · {item.subjectName}</option>)}</select></label>
    </section>
    {book && <section className="panel">
      <div className="teacher-grade-entry-heading"><div><h2>{book.className} · {book.subjectName}</h2><p>Chọn đầu điểm để nhập hoặc sửa điểm cho cả lớp.</p></div><span className={`badge-status ${book.status === 'LOCKED' ? 'completed' : 'active'}`}>{book.status}</span></div>
      {!teacherItems.length ? <div className="empty-state">Môn này không có đầu điểm thuộc quyền nhập của bạn.</div> : <>
        <div className="teacher-grade-item-picker">
          <label>Đầu điểm
            <select value={selectedItemId ?? ''} onChange={event => { setSelectedItemId(Number(event.target.value)); setEditingItemId(null); }}>
              {teacherItems.map(item => <option key={item.id} value={item.id}>{item.name}{hasSubmittedScores(item) ? ' · Đã nhập' : ''}</option>)}
            </select>
          </label>
        </div>
        {selectedItem && <div className="teacher-grade-entry">
          <div className="teacher-grade-entry-context"><div><span className="eyebrow">Đầu điểm đang chọn</span><h3>{selectedItem.name}</h3></div>
            <span>{selectedItem.assessmentType === 'SCORE' ? `Điểm số · Hệ số ${selectedItem.weight}` : selectedItem.assessmentType === 'PASS_FAIL' ? 'Đạt / Chưa đạt' : 'Nhận xét'}</span>
          </div>
          <div className="table-responsive"><table className="teacher-grade-entry-table"><thead><tr><th>Học sinh</th><th>{selectedItem.name}</th></tr></thead><tbody>
            {students.map(student => <tr key={student.id}><td><strong>{student.name}</strong><small className="table-subtext">{student.code}</small></td><td>{renderScoreInput(student, selectedItem)}</td></tr>)}
          </tbody></table></div>
          <div className="teacher-grade-entry-actions"><p>{isEditingSelectedItem ? 'Kiểm tra điểm trước khi lưu.' : 'Điểm đã lưu. Chọn “Sửa điểm” để thay đổi.'}</p>
            {isSelectedItemEditable && <button type="button" disabled={busyItem === selectedItem.id} onClick={() => void handleItemAction(selectedItem)}>
              {busyItem === selectedItem.id ? 'Đang lưu…' : isEditingSelectedItem ? hasSubmittedScores(selectedItem) ? 'Lưu thay đổi' : 'Thêm điểm' : 'Sửa điểm'}
            </button>}
          </div>
        </div>}
      </>}
    </section>}
  </main>;
}
