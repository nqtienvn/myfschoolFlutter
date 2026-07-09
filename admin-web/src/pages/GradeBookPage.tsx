import { useEffect, useMemo, useState } from 'react';
import { getAcademicYears } from '../api/academicYear';
import { getSemesters } from '../api/semester';
import { getClasses } from '../api/class';
import { getSubjects } from '../api/subject';
import { 
  getGradeBook, 
  getGradeBookStudents, 
  saveScores as apiSaveScores, 
  finalizeGradeBook 
} from '../api/gradeBook';

interface AcademicYearItem { id: number; name: string; status: string; }
interface ClassItem { id: number; name: string; academicYearId: number; academicYearName: string; }
interface SubjectItem { id: number; name: string; code: string; }
interface SemesterItem { id: number; name: string; academicYearId: number; academicYearName: string; isCurrent: boolean; order: number; }
interface GradeItem { id: number; name: string; weight: number; maxScore: number; order: number; }
interface GradeBook { id: number; classId: number; className: string; subjectId: number; subjectName: string; semesterId: number; semesterName: string; isFinalized: boolean; items: GradeItem[]; }
interface StudentScore { id: number | null; studentId: number; studentName: string; studentCode: string; gradeItemId: number; score: number | null; isGraded: boolean; note: string | null; isCommentBased: boolean; comment: string | null; average: number | null; }

export default function GradeBookPage({ selectedYearId, selectedSemesterId }: { selectedYearId?: string; selectedSemesterId?: string }) {
  const [academicYears, setAcademicYears] = useState<AcademicYearItem[]>([]);
  const [academicYearId, setAcademicYearId] = useState(selectedYearId || '');
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [subjects, setSubjects] = useState<SubjectItem[]>([]);
  const [semesters, setSemesters] = useState<SemesterItem[]>([]);
  const [classId, setClassId] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [semesterId, setSemesterId] = useState(selectedSemesterId || '');
  const [book, setBook] = useState<GradeBook | null>(null);
  const [scores, setScores] = useState<StudentScore[]>([]);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchAcademicYears();
    getSubjects().then((d: SubjectItem[]) => setSubjects(d || [])).catch(() => {});
  }, []);

  useEffect(() => {
    if (selectedYearId) {
      setAcademicYearId(selectedYearId);
    }
  }, [selectedYearId]);

  useEffect(() => {
    if (selectedSemesterId) {
      setSemesterId(selectedSemesterId);
    }
  }, [selectedSemesterId]);

  useEffect(() => {
    if (!academicYearId) return;
    setClassId('');
    setSubjectId('');
    setBook(null);
    setScores([]);
    getClasses({ academicYearId, page: 0, size: 100 }).then((d: any) => setClasses(d.content || [])).catch(() => {});
    getSemesters(academicYearId).then((d: SemesterItem[]) => {
      const list = d || [];
      setSemesters(list);
      if (!selectedSemesterId) {
        const current = list.find(s => s.isCurrent) || list[0];
        if (current) setSemesterId(String(current.id));
      }
    }).catch(() => {});
  }, [academicYearId, selectedSemesterId]);

  const rows = useMemo(() => {
    const map = new Map<number, { studentId: number; studentName: string; studentCode: string; average: number | null; byItem: Record<number, StudentScore> }>();
    for (const score of scores) {
      const row = map.get(score.studentId) || { studentId: score.studentId, studentName: score.studentName, studentCode: score.studentCode, average: score.average, byItem: {} };
      row.byItem[score.gradeItemId] = score;
      row.average = score.average;
      map.set(score.studentId, row);
    }
    return Array.from(map.values());
  }, [scores]);

  async function fetchAcademicYears() {
    const data = await getAcademicYears() as AcademicYearItem[];
    setAcademicYears(data);
    const active = data.find(y => y.status === 'ACTIVE') || data[0];
    if (active) setAcademicYearId(String(active.id));
  }

  async function loadBook() {
    if (!classId || !subjectId || !semesterId) return setError('Chọn đủ lớp, môn, học kỳ.');
    setError('');
    setSuccessMsg('');
    setLoading(true);
    try {
      const gb = await getGradeBook(classId, subjectId, semesterId) as GradeBook;
      setBook(gb);
      setScores(await getGradeBookStudents(gb.id) as StudentScore[]);
    } catch (err: any) {
      setError(err.message || 'Không tải được bảng điểm');
    } finally {
      setLoading(false);
    }
  }

  function setScore(studentId: number, gradeItemId: number, value: string) {
    setScores(prev => prev.map(s => s.studentId === studentId && s.gradeItemId === gradeItemId
      ? { ...s, score: value === '' ? null : Number(value), isGraded: value !== '' }
      : s));
  }

  async function saveScores() {
    if (!book) return;
    setError('');
    setSuccessMsg('');
    setLoading(true);
    try {
      for (const item of book.items) {
        await apiSaveScores(item.id, scores.filter(s => s.gradeItemId === item.id).map(s => ({
          studentId: s.studentId,
          score: s.score !== null ? s.score : undefined,
          isGraded: s.isGraded,
          note: s.note !== null ? s.note : undefined,
          isCommentBased: s.isCommentBased,
          comment: s.comment !== null ? s.comment : undefined
        })));
      }
      setSuccessMsg('Đã lưu điểm.');
      loadBook();
    } catch (err: any) {
      setError(err.message || 'Lỗi lưu điểm');
    } finally {
      setLoading(false);
    }
  }

  async function finalizeBook() {
    if (!book || !confirm('Khóa bảng điểm? Sau đó không thể sửa điểm.')) return;
    setError('');
    setSuccessMsg('');
    setLoading(true);
    try {
      await finalizeGradeBook(book.id);
      setSuccessMsg('Đã khóa bảng điểm.');
      loadBook();
    } catch (err: any) {
      setError(err.message || 'Lỗi khóa bảng điểm');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <h2>Bảng điểm</h2>
      {error && <div className="error">{error}</div>}
      {successMsg && <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: '16px' }}>[SUCCESS] {successMsg}</div>}

      <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))' }}>
        <div className="form-group"><label>Năm học</label><select value={academicYearId} onChange={e => setAcademicYearId(e.target.value)}><option value="">Chọn năm học</option>{academicYears.map(y => <option key={y.id} value={y.id}>{y.name} {y.status === 'ACTIVE' ? '(Đang mở)' : ''}</option>)}</select></div>
        <div className="form-group"><label>Lớp</label><select value={classId} onChange={e => setClassId(e.target.value)} disabled={!academicYearId}><option value="">Chọn lớp</option>{classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></div>
        <div className="form-group"><label>Học kỳ</label><select value={semesterId} onChange={e => setSemesterId(e.target.value)} disabled={!academicYearId}><option value="">Chọn học kỳ</option>{semesters.map(s => <option key={s.id} value={s.id}>{s.name} {s.isCurrent ? '(Hiện tại)' : ''}</option>)}</select></div>
        <div className="form-group"><label>Môn</label><select value={subjectId} onChange={e => setSubjectId(e.target.value)}><option value="">Chọn môn</option>{subjects.map(s => <option key={s.id} value={s.id}>{s.name} ({s.code})</option>)}</select></div>
        <div className="form-group"><label style={{ visibility: 'hidden' }}>Tải</label><button onClick={loadBook} disabled={loading}>{loading ? 'Đang tải...' : 'Tải bảng điểm'}</button></div>
      </div>

      {book && <div style={{ marginTop: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3 style={{ fontSize: '15px', fontWeight: 700, textTransform: 'uppercase', margin: 0 }}>{book.className} — {book.subjectName} — {book.semesterName}</h3>
          <span style={{ padding: '4px 12px', fontSize: '12px', fontWeight: 700, background: book.isFinalized ? '#737373' : '#16a34a', color: '#fff' }}>{book.isFinalized ? 'ĐÃ KHÓA' : 'ĐANG MỞ'}</span>
        </div>

        <table>
          <thead>
            <tr>
              <th>Mã HS</th>
              <th>Họ tên</th>
              {book.items.map(i => <th key={i.id}>{i.name} (x{i.weight})</th>)}
              <th>TBM</th>
            </tr>
          </thead>
          <tbody>
            {rows.map(row => <tr key={row.studentId}>
              <td>{row.studentCode}</td>
              <td style={{ fontWeight: 600 }}>{row.studentName}</td>
              {book.items.map(item => <td key={item.id}>
                {book.isFinalized ? (row.byItem[item.id]?.score ?? '') : <input type="number" min="0" max="10" step="0.25" value={row.byItem[item.id]?.score ?? ''} onChange={e => setScore(row.studentId, item.id, e.target.value)} style={{ width: '72px' }} />}
              </td>)}
              <td>{row.average ?? '-'}</td>
            </tr>)}
          </tbody>
        </table>

        {!book.isFinalized && <div style={{ marginTop: '16px', display: 'flex', gap: '12px' }}>
          <button onClick={saveScores} disabled={loading}>{loading ? 'Đang lưu...' : 'Lưu điểm'}</button>
          <button onClick={finalizeBook} disabled={loading} style={{ border: '1px solid #737373' }}>Khóa bảng điểm</button>
        </div>}
      </div>}
    </div>
  );
}
