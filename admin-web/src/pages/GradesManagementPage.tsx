import { useEffect, useMemo, useState } from 'react';
import { getClasses } from '../api/class';
import { getSubjects } from '../api/subject';
import { getAcademicYearMasterData } from '../api/academicYearConfig';
import {
  calculateSemesterResults,
  calculateSubjectAverages,
  changeGradeBookStatus,
  getGradeBook,
  getGradeBookStudents,
  updateScores,
} from '../api/gradeBook';
import type { AssessmentType, GradeEntryRole } from '../api/gradeConfiguration';
import { gradeEntryPayload, numericAssessmentItems } from '../utils/gradeAssessment';

interface GradeItem {
  id: number;
  name: string;
  weight: number;
  entryRole: GradeEntryRole;
  assessmentType: AssessmentType;
  requiredEntry: boolean;
}

interface GradeBook {
  id: number;
  status: string;
  items: GradeItem[];
}

interface ScoreRow {
  studentId: number;
  studentName: string;
  studentCode: string;
  gradeItemId: number;
  score: number | null;
  comment: string | null;
  isGraded: boolean;
  average: number | null;
}

interface StudentRow {
  id: number;
  name: string;
  code: string;
  values: Record<number, Pick<ScoreRow, 'score' | 'comment' | 'isGraded'>>;
  average: number | null;
}

const assessmentLabel = (type: AssessmentType) => ({
  SCORE: 'Điểm số',
  PASS_FAIL: 'Đạt / Chưa đạt',
  COMMENT: 'Nhận xét',
}[type]);

export default function GradesManagementPage({
  selectedYearId,
  selectedSemesterId,
}: {
  selectedYearId: string;
  selectedSemesterId: string;
}) {
  const [classes, setClasses] = useState<any[]>([]);
  const [subjects, setSubjects] = useState<any[]>([]);
  const [classId, setClassId] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [book, setBook] = useState<GradeBook | null>(null);
  const [scores, setScores] = useState<ScoreRow[]>([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [savingItemId, setSavingItemId] = useState<number | null>(null);

  const showError = (cause: unknown) => {
    setMessage('');
    setError(cause instanceof Error ? cause.message : 'Có lỗi xảy ra khi xử lý điểm. Vui lòng thử lại.');
  };
  const beginAction = () => {
    setError('');
    setMessage('');
  };

  useEffect(() => {
    setBook(null);
    setScores([]);
    setClassId('');
    setSubjectId('');
    setError('');
    setMessage('');
    if (!selectedYearId) return;
    Promise.all([
      getClasses({ academicYearId: selectedYearId, size: 200 }),
      getSubjects(),
      getAcademicYearMasterData(selectedYearId),
    ]).then(([classData, allSubjects, config]: any[]) => {
      setClasses(classData.content || classData || []);
      setSubjects((allSubjects || []).filter((subject: any) => config.subjectIds.includes(subject.id)));
    }).catch(showError);
  }, [selectedYearId]);

  async function load() {
    if (!classId || !subjectId || !selectedSemesterId) return;
    beginAction();
    try {
      const loadedBook = await getGradeBook(Number(classId), Number(subjectId), Number(selectedSemesterId)) as GradeBook;
      setBook(loadedBook);
      setScores(await getGradeBookStudents(loadedBook.id) as ScoreRow[]);
    } catch (cause) {
      showError(cause);
    }
  }

  const students = useMemo(() => {
    const map = new Map<number, StudentRow>();
    for (const row of scores) {
      const student = map.get(row.studentId) || {
        id: row.studentId,
        name: row.studentName,
        code: row.studentCode,
        values: {},
        average: row.average,
      };
      student.values[row.gradeItemId] = {
        score: row.score,
        comment: row.comment,
        isGraded: row.isGraded,
      };
      student.average = row.average;
      map.set(row.studentId, student);
    }
    return [...map.values()];
  }, [scores]);

  function changeValue(studentId: number, itemId: number, patch: Partial<ScoreRow>) {
    setScores(rows => rows.map(row => row.studentId === studentId && row.gradeItemId === itemId
      ? { ...row, ...patch }
      : row));
  }

  async function saveItem(item: GradeItem) {
    const entries = students.map(student => {
      const value = student.values[item.id] || { score: null, comment: null, isGraded: false };
      return gradeEntryPayload(item.assessmentType, student.id, value.score, value.comment);
    });
    beginAction();
    setSavingItemId(item.id);
    try {
      await updateScores(item.id, entries, `Admin cập nhật đầu điểm ${item.name}`);
      await load();
      setMessage(`Đã lưu cột ${item.name}.`);
    } catch (cause) {
      showError(cause);
    } finally {
      setSavingItemId(null);
    }
  }

  async function calculateSubject() {
    if (!book) return;
    beginAction();
    try {
      await calculateSubjectAverages(book.id);
      await load();
      setMessage('Đã tính điểm trung bình môn từ các đầu điểm số.');
    } catch (cause) {
      showError(cause);
    }
  }

  async function calculateSemester() {
    beginAction();
    try {
      const result: any = await calculateSemesterResults(Number(classId), Number(selectedSemesterId));
      const warning = result?.warnings?.length ? ` Cảnh báo: ${result.warnings.join(', ')}` : '';
      setMessage(`Đã tính kết quả học kỳ cho ${result?.updated ?? 0} học sinh.${warning}`);
    } catch (cause) {
      showError(cause);
    }
  }

  async function changeStatus(status: 'PUBLISHED' | 'LOCKED') {
    if (!book) return;
    beginAction();
    try {
      await changeGradeBookStatus(book.id, status);
      await load();
      setMessage(status === 'PUBLISHED' ? 'Đã công bố bảng điểm.' : 'Đã khóa bảng điểm.');
    } catch (cause) {
      showError(cause);
    }
  }

  if (!selectedYearId || !selectedSemesterId) {
    return <div className="notice warning">Chọn năm học và học kỳ để quản lý điểm.</div>;
  }

  const numericItems = numericAssessmentItems(book?.items || []);
  const canAdminEdit = (item: GradeItem) => item.entryRole !== 'SUBJECT_TEACHER' && book?.status !== 'LOCKED';

  return <div>
    <div className="page-header">
      <div>
        <h1>Quản lý điểm</h1>
        <p>Điểm được cô lập theo năm học, học kỳ, lớp và môn.</p>
      </div>
    </div>
    {error && <div className="notice error" role="alert">{error}</div>}
    {message && <div className="notice success">{message}</div>}
    <section className="form-grid">
      <label className="form-group">Lớp
        <select value={classId} onChange={event => { setClassId(event.target.value); setBook(null); beginAction(); }}>
          <option value="">Chọn lớp</option>
          {classes.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
        </select>
      </label>
      <label className="form-group">Môn
        <select value={subjectId} onChange={event => { setSubjectId(event.target.value); setBook(null); beginAction(); }}>
          <option value="">Chọn môn</option>
          {subjects.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
        </select>
      </label>
      <button onClick={load}>Mở bảng điểm</button>
    </section>

    {book && <div className="notice" style={{ marginTop: 16 }}>
      <strong>Cách hệ thống tính điểm</strong>
      <div style={{ marginTop: 6 }}>ĐTB môn = Tổng (điểm số × hệ số) / Tổng hệ số, làm tròn 1 chữ số thập phân.</div>
      <div style={{ marginTop: 4 }}>{numericItems.length > 0 ? numericItems.map(item => `${item.name} × ${item.weight}`).join(' + ') : 'Chưa cấu hình đầu điểm số.'}</div>
      <div style={{ marginTop: 6 }}>Nhận xét và Đạt/Chưa đạt là dữ liệu học tập bắt buộc nếu được cấu hình, nhưng không tham gia tính ĐTB.</div>
    </div>}

    {book && <>
      <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', margin: '16px 0', flexWrap: 'wrap' }}>
        <span className="badge-status active">{book.status}</span>
        <button className="secondary-button" onClick={calculateSubject}>Tính ĐTB môn</button>
        <button className="secondary-button" onClick={calculateSemester}>Tính kết quả học kỳ</button>
        <button onClick={() => changeStatus('PUBLISHED')}>Công bố</button>
        <button onClick={() => changeStatus('LOCKED')}>Khóa điểm</button>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Học sinh</th>
              {book.items.map(item => <th key={item.id} style={{ minWidth: item.assessmentType === 'COMMENT' ? 260 : 150 }}>
                {item.name}
                <small style={{ display: 'block' }}>
                  {assessmentLabel(item.assessmentType)}{item.assessmentType === 'SCORE' ? ` · HS ${item.weight}` : ''} · {item.entryRole === 'ADMIN' ? 'Nhà trường' : item.entryRole === 'SUBJECT_TEACHER' ? 'Giáo viên' : 'GV/Admin'}
                </small>
                {canAdminEdit(item) && <button className="secondary-button" disabled={savingItemId === item.id} onClick={() => saveItem(item)} style={{ marginTop: 8 }}>
                  {savingItemId === item.id ? 'Đang lưu…' : 'Lưu cột'}
                </button>}
              </th>)}
              <th>ĐTB</th>
            </tr>
          </thead>
          <tbody>
            {students.map(student => <tr key={student.id}>
              <td><strong>{student.name}</strong><small style={{ display: 'block' }}>{student.code}</small></td>
              {book.items.map(item => {
                const value = student.values[item.id] || { score: null, comment: null, isGraded: false };
                const disabled = !canAdminEdit(item);
                return <td key={item.id}>
                  {item.assessmentType === 'SCORE' && <input
                    aria-label={`${item.name} của ${student.name}`}
                    type="number"
                    min="0"
                    max="10"
                    step="0.1"
                    disabled={disabled}
                    value={value.score ?? ''}
                    onChange={event => changeValue(student.id, item.id, {
                      score: event.target.value === '' ? null : Number(event.target.value),
                      comment: null,
                      isGraded: event.target.value !== '',
                    })}
                  />}
                  {item.assessmentType === 'PASS_FAIL' && <select
                    aria-label={`${item.name} của ${student.name}`}
                    disabled={disabled}
                    value={value.comment || ''}
                    onChange={event => changeValue(student.id, item.id, {
                      score: null,
                      comment: event.target.value || null,
                      isGraded: event.target.value !== '',
                    })}
                  >
                    <option value="">Chưa nhập</option>
                    <option value="PASS">Đạt</option>
                    <option value="FAIL">Chưa đạt</option>
                  </select>}
                  {item.assessmentType === 'COMMENT' && <textarea
                    aria-label={`${item.name} của ${student.name}`}
                    disabled={disabled}
                    maxLength={255}
                    rows={3}
                    value={value.comment || ''}
                    onChange={event => changeValue(student.id, item.id, {
                      score: null,
                      comment: event.target.value,
                      isGraded: event.target.value.trim() !== '',
                    })}
                  />}
                </td>;
              })}
              <td><strong>{student.average ?? '—'}</strong></td>
            </tr>)}
          </tbody>
        </table>
      </div>
    </>}
  </div>;
}
