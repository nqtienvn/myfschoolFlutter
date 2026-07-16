import { useEffect, useMemo, useState } from 'react';
import { getAcademicYearMasterData } from '../api/academicYearConfig';
import { getClasses } from '../api/class';
import {
  calculateSemesterResults,
  calculateSubjectAverages,
  changeGradeBookStatus,
  getGradeBook,
  getGradeBookStudents,
  publishGradeItem,
  updateScores,
} from '../api/gradeBook';
import type { AssessmentType, GradeEntryRole } from '../api/gradeConfiguration';
import { getPeriodicReports, type PeriodicReportItem } from '../api/periodicReview';
import {
  deleteViolation,
  getResultSummary,
  getStudentViolations,
  overrideResult,
  publishSemesterResults,
  saveViolation,
  type ResultSummaryItem,
  type ViolationItem,
} from '../api/resultManagement';
import { getSubjects } from '../api/subject';
import { gradeEntryPayload, numericAssessmentItems } from '../utils/gradeAssessment';

type ResultTab = 'grades' | 'discipline' | 'summary';

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
  subjectName: string;
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
  SCORE: 'Điểm số', PASS_FAIL: 'Đạt / Chưa đạt', COMMENT: 'Nhận xét',
}[type]);

const abilityOptions = ['Giỏi', 'Khá', 'Trung bình', 'Yếu'];
const conductOptions = ['Tốt', 'Khá', 'Trung bình', 'Yếu'];
const honorOptions = ['Học sinh xuất sắc', 'Học sinh giỏi', 'Học sinh tiên tiến', 'Giỏi', 'Khá', 'Trung bình', 'Yếu', 'Không'];

export default function GradesManagementPage({
  selectedYearId,
  selectedSemesterId,
}: {
  selectedYearId: string;
  selectedSemesterId: string;
}) {
  const [tab, setTab] = useState<ResultTab>('grades');
  const [classes, setClasses] = useState<any[]>([]);
  const [subjects, setSubjects] = useState<any[]>([]);
  const [classId, setClassId] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [book, setBook] = useState<GradeBook | null>(null);
  const [scores, setScores] = useState<ScoreRow[]>([]);
  const [reports, setReports] = useState<PeriodicReportItem[]>([]);
  const [summary, setSummary] = useState<ResultSummaryItem[]>([]);
  const [selectedStudentId, setSelectedStudentId] = useState('');
  const [violations, setViolations] = useState<ViolationItem[]>([]);
  const [editingViolation, setEditingViolation] = useState<ViolationItem | null>(null);
  const [violationDraft, setViolationDraft] = useState({ title: '', category: '', description: '', eventDate: '' });
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [savingItemId, setSavingItemId] = useState<number | null>(null);

  const showError = (cause: unknown) => {
    setMessage('');
    setError(cause instanceof Error ? cause.message : 'Có lỗi xảy ra. Vui lòng thử lại.');
  };
  const beginAction = () => { setError(''); setMessage(''); };

  useEffect(() => {
    setClasses([]); setSubjects([]); setClassId(''); setSubjectId(''); setBook(null);
    setScores([]); setReports([]); setSummary([]); setSelectedStudentId(''); setViolations([]);
    beginAction();
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

  useEffect(() => {
    setBook(null); setScores([]); setReports([]); setSummary([]); setViolations([]); setSelectedStudentId('');
    if (!classId || !selectedYearId || !selectedSemesterId) return;
    Promise.all([
      getResultSummary(selectedYearId, selectedSemesterId, classId),
      getPeriodicReports({ academicYearId: selectedYearId, semesterId: selectedSemesterId, classId }),
    ]).then(([resultRows, reportRows]) => {
      setSummary(resultRows || []);
      setReports(reportRows || []);
    }).catch(showError);
  }, [selectedYearId, selectedSemesterId, classId]);

  useEffect(() => {
    setViolations([]); setEditingViolation(null);
    setViolationDraft({ title: '', category: '', description: '', eventDate: '' });
    if (!selectedStudentId || !classId || !selectedYearId || !selectedSemesterId) return;
    getStudentViolations(Number(selectedStudentId), Number(selectedYearId), Number(selectedSemesterId), Number(classId))
      .then(items => setViolations((items || []).filter(item => item.eventType === 'VIOLATION')))
      .catch(showError);
  }, [selectedStudentId, classId, selectedYearId, selectedSemesterId]);

  async function loadGradeBook() {
    if (!classId || !subjectId || !selectedSemesterId) return;
    beginAction(); setBusy(true);
    try {
      const loadedBook = await getGradeBook(Number(classId), Number(subjectId), Number(selectedSemesterId)) as GradeBook;
      setBook(loadedBook);
      setScores(await getGradeBookStudents(loadedBook.id) as ScoreRow[]);
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  const students = useMemo(() => {
    const map = new Map<number, StudentRow>();
    for (const row of scores) {
      const student = map.get(row.studentId) || { id: row.studentId, name: row.studentName, code: row.studentCode, values: {}, average: row.average };
      student.values[row.gradeItemId] = { score: row.score, comment: row.comment, isGraded: row.isGraded };
      student.average = row.average; map.set(row.studentId, student);
    }
    return [...map.values()];
  }, [scores]);

  function changeValue(studentId: number, itemId: number, patch: Partial<ScoreRow>) {
    setScores(rows => rows.map(row => row.studentId === studentId && row.gradeItemId === itemId ? { ...row, ...patch } : row));
  }

  async function saveItem(item: GradeItem) {
    const entries = students.map(student => {
      const value = student.values[item.id] || { score: null, comment: null, isGraded: false };
      return gradeEntryPayload(item.assessmentType, student.id, value.score, value.comment);
    });
    beginAction(); setSavingItemId(item.id);
    try {
      await updateScores(item.id, entries, `Admin cập nhật đầu điểm ${item.name}`);
      await loadGradeBook(); setMessage(`Đã lưu cột ${item.name}.`);
    } catch (cause) { showError(cause); } finally { setSavingItemId(null); }
  }

  async function publishItem(item: GradeItem) {
    if (!book) return;
    beginAction(); setSavingItemId(item.id);
    try {
      await publishGradeItem(book.id, item.id);
      await loadGradeBook();
      setMessage(`Đã công bố ${item.name}. PH/HS có thể xem ngay các giá trị đã nhập.`);
    } catch (cause) { showError(cause); } finally { setSavingItemId(null); }
  }

  async function calculateSubject() {
    if (!book) return;
    beginAction();
    try { await calculateSubjectAverages(book.id); await loadGradeBook(); setMessage('Đã tính điểm trung bình môn.'); }
    catch (cause) { showError(cause); }
  }

  async function calculateSemester() {
    if (!classId) return;
    beginAction(); setBusy(true);
    try {
      const result: any = await calculateSemesterResults(Number(classId), Number(selectedSemesterId));
      setSummary(await getResultSummary(selectedYearId, selectedSemesterId, classId));
      setMessage(`Đã tính kết quả cho ${result?.updated ?? 0} học sinh.`);
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  async function publishWholeBook() {
    if (!book) return;
    beginAction();
    try { await changeGradeBookStatus(book.id, 'PUBLISHED'); await loadGradeBook(); setMessage('Đã công bố toàn bộ bảng điểm môn.'); }
    catch (cause) { showError(cause); }
  }

  function subjectComment(studentId: number) {
    const subjectName = subjects.find(item => String(item.id) === subjectId)?.name;
    const report = reports.find(item => item.studentId === studentId);
    return report?.subjectReviews.find(item => item.subjectName === subjectName)?.comment || '—';
  }

  async function submitViolation(event: React.FormEvent) {
    event.preventDefault();
    if (!selectedStudentId || !violationDraft.title.trim() || !violationDraft.eventDate) return;
    beginAction(); setBusy(true);
    try {
      await saveViolation(Number(selectedStudentId), {
        academicYearId: Number(selectedYearId), semesterId: Number(selectedSemesterId), classId: Number(classId),
        title: violationDraft.title.trim(), category: violationDraft.category.trim(),
        description: violationDraft.description.trim(), eventDate: violationDraft.eventDate,
      }, editingViolation?.id);
      setViolations(await getStudentViolations(Number(selectedStudentId), Number(selectedYearId), Number(selectedSemesterId), Number(classId)));
      setSummary(await getResultSummary(selectedYearId, selectedSemesterId, classId));
      setEditingViolation(null); setViolationDraft({ title: '', category: '', description: '', eventDate: '' });
      setMessage('Đã lưu vi phạm.');
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  async function removeViolation(id: number) {
    if (!confirm('Xóa vi phạm này?')) return;
    beginAction();
    try {
      await deleteViolation(id, Number(selectedYearId));
      setViolations(items => items.filter(item => item.id !== id));
      setSummary(await getResultSummary(selectedYearId, selectedSemesterId, classId));
      setMessage('Đã xóa vi phạm.');
    } catch (cause) { showError(cause); }
  }

  function patchSummary(studentId: number, patch: Partial<ResultSummaryItem>) {
    setSummary(rows => rows.map(row => row.studentId === studentId ? { ...row, ...patch } : row));
  }

  async function saveFinal(row: ResultSummaryItem) {
    if (!row.academicAbility || !row.conduct || !row.honor) return;
    beginAction();
    try {
      const updated = await overrideResult(row.studentId, {
        academicYearId: Number(selectedYearId), semesterId: Number(selectedSemesterId), classId: Number(classId),
        academicAbility: row.academicAbility, conduct: row.conduct, honor: row.honor,
      });
      patchSummary(row.studentId, updated); setMessage(`Đã lưu kết quả cuối của ${row.studentName}.`);
    } catch (cause) { showError(cause); }
  }

  async function publishSummary() {
    if (!classId || !confirm('Công bố toàn bộ kết quả học kỳ của lớp này cho PH/HS?')) return;
    beginAction(); setBusy(true);
    try {
      setSummary(await publishSemesterResults({ academicYearId: Number(selectedYearId), semesterId: Number(selectedSemesterId), classId: Number(classId) }));
      setReports(await getPeriodicReports({ academicYearId: selectedYearId, semesterId: selectedSemesterId, classId }));
      setMessage('Đã công bố điểm, tổng kết và toàn bộ nhận xét cho PH/HS.');
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  if (!selectedYearId || !selectedSemesterId) return <div className="notice warning">Chọn năm học và học kỳ để quản lý kết quả.</div>;
  const numericItems = numericAssessmentItems(book?.items || []);
  const canAdminEdit = (item: GradeItem) => item.entryRole !== 'SUBJECT_TEACHER' && book?.status !== 'LOCKED';

  return <main className="page-stack result-management" aria-label="Quản lý kết quả">
    <div className="page-heading"><div><h1>Quản lý kết quả</h1><p>Điểm, nhận xét, vi phạm, chuyên cần và tổng kết dùng chung phạm vi năm học.</p></div></div>
    {error && <div className="notice error" role="alert">{error}</div>}
    {message && <div className="notice success">{message}</div>}
    <div className="result-tabs" role="tablist">
      <button className={tab === 'grades' ? 'active' : ''} onClick={() => setTab('grades')}>Điểm & nhận xét môn</button>
      <button className={tab === 'discipline' ? 'active' : ''} onClick={() => setTab('discipline')}>Vi phạm & chuyên cần</button>
      <button className={tab === 'summary' ? 'active' : ''} onClick={() => setTab('summary')}>Tổng kết học kỳ</button>
    </div>
    <section className="form-grid result-scope">
      <label className="form-group">Lớp<select value={classId} onChange={event => setClassId(event.target.value)}><option value="">Chọn lớp</option>{classes.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
      {tab === 'grades' && <label className="form-group">Môn<select value={subjectId} onChange={event => { setSubjectId(event.target.value); setBook(null); }}><option value="">Chọn môn</option>{subjects.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>}
      {tab === 'grades' && <button disabled={!classId || !subjectId || busy} onClick={loadGradeBook}>Mở bảng điểm</button>}
    </section>

    {tab === 'grades' && book && <section className="panel result-panel">
      <div className="monitoring-actions">
        <span className="badge-status active">{book.status}</span>
        <button className="secondary-button" onClick={calculateSubject}>Tính ĐTB môn</button>
        <button onClick={publishWholeBook}>Công bố toàn bộ môn</button>
      </div>
      <p className="table-subtext">{numericItems.length ? `Công thức: ${numericItems.map(item => `${item.name} × ${item.weight}`).join(' + ')}` : 'Chưa có đầu điểm số.'}</p>
      <div className="table-responsive"><table><thead><tr><th>Học sinh</th>{book.items.map(item => <th key={item.id} style={{ minWidth: item.assessmentType === 'COMMENT' ? 240 : 150 }}>{item.name}<small className="table-subtext">{assessmentLabel(item.assessmentType)} · HS {item.weight}</small><div className="grade-column-actions">{canAdminEdit(item) && <button className="secondary-button" disabled={savingItemId === item.id} onClick={() => saveItem(item)}>Lưu</button>}<button disabled={savingItemId === item.id} onClick={() => publishItem(item)}>Công bố</button></div></th>)}<th>ĐTB</th><th>Nhận xét GVBM</th></tr></thead>
      <tbody>{students.map(student => <tr key={student.id}><td><strong>{student.name}</strong><small className="table-subtext">{student.code}</small></td>{book.items.map(item => { const value = student.values[item.id] || { score: null, comment: null, isGraded: false }; const disabled = !canAdminEdit(item); return <td key={item.id}>{item.assessmentType === 'SCORE' && <input type="number" min="0" max="10" step="0.1" disabled={disabled} value={value.score ?? ''} onChange={event => changeValue(student.id, item.id, { score: event.target.value === '' ? null : Number(event.target.value), comment: null, isGraded: event.target.value !== '' })}/>} {item.assessmentType === 'PASS_FAIL' && <select disabled={disabled} value={value.comment || ''} onChange={event => changeValue(student.id, item.id, { score: null, comment: event.target.value || null, isGraded: !!event.target.value })}><option value="">—</option><option value="PASS">Đạt</option><option value="FAIL">Chưa đạt</option></select>} {item.assessmentType === 'COMMENT' && <textarea disabled={disabled} rows={2} value={value.comment || ''} onChange={event => changeValue(student.id, item.id, { score: null, comment: event.target.value, isGraded: !!event.target.value.trim() })}/>}</td>; })}<td><strong>{student.average ?? '—'}</strong></td><td className="subject-comment-cell">{subjectComment(student.id)}</td></tr>)}</tbody></table></div>
    </section>}

    {tab === 'discipline' && classId && <div className="discipline-grid">
      <section className="panel"><h2>Chuyên cần & vi phạm</h2><div className="table-responsive"><table><thead><tr><th>Học sinh</th><th>Vi phạm</th><th>Nghỉ có phép</th><th>Nghỉ không phép</th><th /></tr></thead><tbody>{summary.map(row => <tr key={row.studentId}><td><strong>{row.studentName}</strong><small className="table-subtext">{row.studentCode}</small></td><td>{row.violationCount}</td><td>{row.absentWithLeave}</td><td>{row.absentWithoutLeave}</td><td><button className="secondary-button" onClick={() => setSelectedStudentId(String(row.studentId))}>Quản lý</button></td></tr>)}</tbody></table></div></section>
      <section className="panel"><h2>Quản lý vi phạm</h2>{!selectedStudentId ? <div className="empty-state">Chọn một học sinh để thêm/sửa/xóa vi phạm.</div> : <><form className="violation-form" onSubmit={submitViolation}><label>Tiêu đề<input required maxLength={200} value={violationDraft.title} onChange={event => setViolationDraft(d => ({ ...d, title: event.target.value }))}/></label><label>Phân loại<input maxLength={100} value={violationDraft.category} onChange={event => setViolationDraft(d => ({ ...d, category: event.target.value }))}/></label><label>Ngày vi phạm<input required type="date" value={violationDraft.eventDate} onChange={event => setViolationDraft(d => ({ ...d, eventDate: event.target.value }))}/></label><label className="wide">Mô tả<textarea rows={3} maxLength={2000} value={violationDraft.description} onChange={event => setViolationDraft(d => ({ ...d, description: event.target.value }))}/></label><div className="monitoring-actions wide"><button disabled={busy}>{editingViolation ? 'Lưu thay đổi' : 'Thêm vi phạm'}</button>{editingViolation && <button type="button" className="secondary-button" onClick={() => { setEditingViolation(null); setViolationDraft({ title: '', category: '', description: '', eventDate: '' }); }}>Hủy sửa</button>}</div></form><div className="violation-list">{violations.map(item => <article key={item.id}><div><strong>{item.title}</strong><small>{item.category || 'Vi phạm'} · {item.eventDate} · {item.status}</small><p>{item.description}</p></div><div className="table-actions"><button className="secondary-button" onClick={() => { setEditingViolation(item); setViolationDraft({ title: item.title, category: item.category || '', description: item.description || '', eventDate: item.eventDate }); }}>Sửa</button><button className="danger" onClick={() => removeViolation(item.id)}>Xóa</button></div></article>)}</div></>}</section>
    </div>}

    {tab === 'summary' && classId && <section className="panel result-panel">
      <div className="monitoring-actions"><button className="secondary-button" disabled={busy} onClick={calculateSemester}>Tính lại GPA & gợi ý</button><button disabled={busy || !summary.length} onClick={publishSummary}>CÔNG BỐ KẾT QUẢ HỌC KỲ</button></div>
      <div className="table-responsive"><table className="summary-result-table"><thead><tr><th>Học sinh</th><th>GPA / Hạng</th><th>Vi phạm</th><th>Nghỉ P/KP</th><th>Học lực gợi ý</th><th>Hạnh kiểm gợi ý</th><th>Học lực cuối</th><th>Hạnh kiểm cuối</th><th>Danh hiệu</th><th>Nhận xét GVCN</th><th>Trạng thái</th><th /></tr></thead><tbody>{summary.map(row => <tr key={row.studentId}><td><strong>{row.studentName}</strong><small className="table-subtext">{row.studentCode}</small></td><td>{row.gpa ?? '—'} / {row.rank ?? '—'}</td><td>{row.violationCount}</td><td>{row.absentWithLeave} / {row.absentWithoutLeave}</td><td>{row.suggestedAcademicAbility || '—'}</td><td>{row.suggestedConduct || '—'}</td><td><select value={row.academicAbility || ''} onChange={event => patchSummary(row.studentId, { academicAbility: event.target.value })}><option value="">—</option>{abilityOptions.map(value => <option key={value}>{value}</option>)}</select></td><td><select value={row.conduct || ''} onChange={event => patchSummary(row.studentId, { conduct: event.target.value })}><option value="">—</option>{conductOptions.map(value => <option key={value}>{value}</option>)}</select></td><td><select value={row.honor || ''} onChange={event => patchSummary(row.studentId, { honor: event.target.value })}><option value="">—</option>{[...new Set([row.honor, ...honorOptions].filter(Boolean))].map(value => <option key={value!}>{value}</option>)}</select></td><td className="subject-comment-cell">{row.generalComment || '—'}</td><td><span className={`badge-status ${row.status === 'PUBLISHED' ? 'active' : 'draft'}`}>{row.status === 'PUBLISHED' ? 'Đã công bố' : row.reportStatus === 'SUBMITTED' ? 'Chờ công bố' : 'Chờ GVCN'}</span></td><td><button className="secondary-button" onClick={() => saveFinal(row)}>Lưu</button></td></tr>)}</tbody></table></div>
    </section>}
  </main>;
}
