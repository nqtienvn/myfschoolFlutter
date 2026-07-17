import { useEffect, useMemo, useState, type ChangeEvent } from 'react';
import readXlsxFile from 'read-excel-file/browser';
import {
  getTeacherAssignments,
  getTeacherGradeBook,
  getTeacherGradeStudents,
  saveTeacherScores,
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
    setBook(null); setScores([]); setError(''); setMessage('');
  }, [selectedSemesterId]);

  async function openBook() {
    if (!assignmentKey || !selectedSemesterId) return;
    const [classId, subjectId] = assignmentKey.split(':').map(Number);
    setError(''); setMessage('');
    try {
      const value = await getTeacherGradeBook(classId, subjectId, selectedSemesterId) as GradeBook;
      setBook(value); setScores(await getTeacherGradeStudents(value.id) as Score[]);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Không mở được bảng điểm.'); }
  }

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

  async function saveItem(item: GradeItem) {
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
      await saveTeacherScores(item.id, entries);
      setScores(await getTeacherGradeStudents(book.id) as Score[]);
      setMessage(`Đã lưu ${item.name}. Điểm sẽ hiển thị cho PH/HS sau khi Admin công bố.`);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Không lưu được điểm.'); }
    finally { setBusyItem(null); }
  }

  async function importFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file || !book) return;
    setError(''); setMessage('');
    try {
      const table = (file.name.toLowerCase().endsWith('.csv')
        ? parseCsv(await file.text())
        : await readXlsxFile(file)) as unknown[][];
      const [rawHeaders = [], ...dataRows] = table;
      const headers = rawHeaders.map(value => String(value || '').trim());
      const rows = dataRows.map(values => Object.fromEntries(headers.map((header, index) => [header, values[index] ?? ''])) as Record<string, unknown>);
      const byCode = new Map(rows.map(row => [String(row['Mã học sinh'] || row['studentCode'] || '').trim(), row]));
      let updatedColumns = 0;
      for (const item of book.items.filter(canEdit)) {
        const entries = students.map(student => {
          const raw = byCode.get(student.code)?.[item.name];
          if (item.assessmentType === 'SCORE') {
            const score = raw === '' || raw == null ? null : Number(raw);
            return { studentId: student.id, score: Number.isFinite(score) ? score : null, comment: null, isGraded: Number.isFinite(score) };
          }
          const comment = String(raw || '').trim();
          const normalized = item.assessmentType === 'PASS_FAIL'
            ? (/^(đ|đạt|pass)$/i.test(comment) ? 'PASS' : /^(cđ|chưa đạt|fail)$/i.test(comment) ? 'FAIL' : '')
            : comment;
          return { studentId: student.id, score: null, comment: normalized || null, isGraded: !!normalized };
        });
        await saveTeacherScores(item.id, entries); updatedColumns++;
      }
      setScores(await getTeacherGradeStudents(book.id) as Score[]);
      setMessage(`Đã nhập ${updatedColumns} cột từ ${file.name}.`);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'File không đúng định dạng.'); }
  }

  return <main className="teacher-page page-stack">
    <div className="page-heading"><div><span className="eyebrow">Lớp giảng dạy</span><h1>Nhập điểm</h1><p>Nhập trực tiếp trên lưới hoặc upload Excel/CSV; Admin công bố điểm linh hoạt theo từng đầu điểm.</p></div></div>
    {error && <div className="notice error">{error}</div>}{message && <div className="notice success">{message}</div>}
    <section className="panel teacher-filter-row">
      <label>Lớp · Môn<select value={assignmentKey} onChange={event => { setAssignmentKey(event.target.value); setBook(null); }}><option value="">Chọn phân công</option>{assignments.map(item => <option key={`${item.classId}:${item.subjectId}`} value={`${item.classId}:${item.subjectId}`}>{item.className} · {item.subjectName}</option>)}</select></label>
      <button disabled={!assignmentKey || !selectedSemesterId} onClick={openBook}>Mở bảng điểm</button>
      {book && <label className="teacher-file-button">Upload Excel/CSV<input type="file" accept=".xlsx,.xls,.csv" onChange={importFile}/></label>}
    </section>
    {book && <section className="panel">
      <div className="monitoring-actions"><h2>{book.className} · {book.subjectName}</h2><span className="badge-status active">{book.status}</span></div>
      <div className="table-responsive"><table className="teacher-grades-table"><thead><tr><th>Học sinh</th>{book.items.map(item => <th key={item.id}>{item.name}<small className="table-subtext">{item.assessmentType}{item.assessmentType === 'SCORE' ? ` · HS ${item.weight}` : ''}</small>{canEdit(item) && <button disabled={busyItem === item.id} onClick={() => saveItem(item)}>{busyItem === item.id ? 'Đang lưu…' : 'Lưu cột'}</button>}</th>)}<th>ĐTB</th></tr></thead><tbody>{students.map(student => <tr key={student.id}><td><strong>{student.name}</strong><small className="table-subtext">{student.code}</small></td>{book.items.map(item => { const value = student.values[item.id]; const disabled = !canEdit(item); return <td key={item.id}>{item.assessmentType === 'SCORE' ? <input type="number" min="0" max="10" step="0.1" disabled={disabled} value={value?.score ?? ''} onChange={event => patchScore(student.id, item.id, { score: event.target.value === '' ? null : Number(event.target.value), isGraded: !!event.target.value })}/> : item.assessmentType === 'PASS_FAIL' ? <select disabled={disabled} value={value?.comment || ''} onChange={event => patchScore(student.id, item.id, { comment: event.target.value || null, isGraded: !!event.target.value })}><option value="">—</option><option value="PASS">Đạt</option><option value="FAIL">Chưa đạt</option></select> : <textarea rows={2} disabled={disabled} value={value?.comment || ''} onChange={event => patchScore(student.id, item.id, { comment: event.target.value, isGraded: !!event.target.value.trim() })}/>}</td>; })}<td><strong>{student.average ?? '—'}</strong></td></tr>)}</tbody></table></div>
    </section>}
  </main>;
}

function parseCsv(value: string): string[][] {
  const rows: string[][] = [];
  let row: string[] = [];
  let cell = '';
  let quoted = false;
  for (let index = 0; index < value.length; index++) {
    const char = value[index];
    if (char === '"' && quoted && value[index + 1] === '"') { cell += '"'; index++; continue; }
    if (char === '"') { quoted = !quoted; continue; }
    if (!quoted && (char === ',' || char === ';')) { row.push(cell.trim()); cell = ''; continue; }
    if (!quoted && (char === '\n' || char === '\r')) {
      if (char === '\r' && value[index + 1] === '\n') index++;
      row.push(cell.trim()); if (row.some(Boolean)) rows.push(row); row = []; cell = ''; continue;
    }
    cell += char;
  }
  row.push(cell.trim()); if (row.some(Boolean)) rows.push(row);
  return rows;
}
