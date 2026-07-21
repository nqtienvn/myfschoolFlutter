import { useEffect, useMemo, useRef, useState } from 'react';
import { completeAcademicYear } from '../api/academicYear';
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
  calculateAcademicYearResults,
  closeSemesterResults,
  deleteViolation,
  downloadGradeTemplate,
  downloadResultExport,
  getAcademicYearResults,
  getResultSummary,
  getStudentViolations,
  importAdminScores,
  overrideResult,
  publishAcademicYearResults,
  publishSemesterResults,
  saveViolation,
  type AcademicYearResultItem,
  type ResultSummaryItem,
  type ViolationItem,
} from '../api/resultManagement';
import { getSubjects } from '../api/subject';
import { gradeEntryPayload, numericAssessmentItems } from '../utils/gradeAssessment';

type MainView = 'semester' | 'annual';
type SemesterSection = 'grades' | 'discipline' | 'summary';

interface SemesterContextItem {
  id: number;
  name: string;
  order: number;
  status: 'NOT_STARTED' | 'ACTIVE' | 'COMPLETED';
}

interface GradeItem {
  id: number;
  code: string;
  name: string;
  weight: number;
  entryRole: GradeEntryRole;
  assessmentType: AssessmentType;
  requiredEntry: boolean;
}

interface GradeBook {
  id: number;
  status: 'DRAFT' | 'SUBMITTED' | 'PUBLISHED' | 'LOCKED';
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

const PAGE_SIZE = 15;
const resultLevels = ['Tốt', 'Khá', 'Đạt', 'Chưa đạt'];
const honorOptions = ['Học sinh Xuất sắc', 'Học sinh Giỏi', 'Không'];
const assessmentLabel: Record<AssessmentType, string> = {
  SCORE: 'Điểm số', PASS_FAIL: 'Đạt / Chưa đạt', COMMENT: 'Nhận xét',
};

function pageOf<T>(rows: T[], page: number) {
  return rows.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);
}

function downloadBlob(blob: Blob, suggestedName: string, responseName?: string | null) {
  const href = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = href;
  anchor.download = responseName || suggestedName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(href);
}

function Pagination({ total, page, onChange }: { total: number; page: number; onChange: (value: number) => void }) {
  const pages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  if (total <= PAGE_SIZE) return null;
  return <div className="result-pagination" aria-label="Phân trang">
    <span>{(page - 1) * PAGE_SIZE + 1}–{Math.min(page * PAGE_SIZE, total)} / {total}</span>
    <div><button className="secondary-button" disabled={page <= 1} onClick={() => onChange(page - 1)}>Trước</button>
      <strong>{page}/{pages}</strong>
      <button className="secondary-button" disabled={page >= pages} onClick={() => onChange(page + 1)}>Sau</button></div>
  </div>;
}

export default function GradesManagementPage({
  selectedYearId,
  selectedSemesterId,
  selectedYearStatus,
  selectedSemesterStatus,
  semesters,
  onContextRefresh,
}: {
  selectedYearId: string;
  selectedSemesterId: string;
  selectedYearStatus?: 'DRAFT' | 'ACTIVE' | 'COMPLETED';
  selectedSemesterStatus?: 'NOT_STARTED' | 'ACTIVE' | 'COMPLETED';
  semesters: SemesterContextItem[];
  onContextRefresh?: () => Promise<void> | void;
}) {
  const [view, setView] = useState<MainView>('semester');
  const [section, setSection] = useState<SemesterSection>('grades');
  const [classes, setClasses] = useState<any[]>([]);
  const [subjects, setSubjects] = useState<any[]>([]);
  const [classId, setClassId] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [book, setBook] = useState<GradeBook | null>(null);
  const [scores, setScores] = useState<ScoreRow[]>([]);
  const [reports, setReports] = useState<PeriodicReportItem[]>([]);
  const [summary, setSummary] = useState<ResultSummaryItem[]>([]);
  const [annualResults, setAnnualResults] = useState<AcademicYearResultItem[]>([]);
  const [selectedStudentId, setSelectedStudentId] = useState('');
  const [violations, setViolations] = useState<ViolationItem[]>([]);
  const [editingViolation, setEditingViolation] = useState<ViolationItem | null>(null);
  const [violationDraft, setViolationDraft] = useState({ title: '', category: '', description: '', eventDate: '' });
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [savingItemId, setSavingItemId] = useState<number | null>(null);
  const [gradePage, setGradePage] = useState(1);
  const [disciplinePage, setDisciplinePage] = useState(1);
  const [summaryPage, setSummaryPage] = useState(1);
  const [annualPage, setAnnualPage] = useState(1);
  const importRef = useRef<HTMLInputElement>(null);

  const locked = selectedYearStatus === 'COMPLETED'
    || (view === 'semester' && selectedSemesterStatus === 'COMPLETED');
  const bothSemestersClosed = semesters.length >= 2 && semesters.every(item => item.status === 'COMPLETED');
  const selectedClass = classes.find(item => String(item.id) === classId);

  const showError = (cause: unknown) => {
    setMessage('');
    setError(cause instanceof Error ? cause.message : 'Có lỗi xảy ra. Vui lòng thử lại.');
  };
  const beginAction = () => { setError(''); setMessage(''); };

  useEffect(() => {
    setClasses([]); setSubjects([]); setClassId(''); setSubjectId(''); setBook(null);
    setScores([]); setReports([]); setSummary([]); setAnnualResults([]); setViolations([]);
    setGradePage(1); setDisciplinePage(1); setSummaryPage(1); setAnnualPage(1); beginAction();
    if (!selectedYearId) return;
    Promise.all([
      getClasses({ academicYearId: selectedYearId, size: 500 }),
      getSubjects(),
      getAcademicYearMasterData(selectedYearId),
    ]).then(([classData, allSubjects, config]: any[]) => {
      setClasses(classData.content || classData || []);
      setSubjects((allSubjects || []).filter((subject: any) => config.subjectIds.includes(subject.id)));
    }).catch(showError);
  }, [selectedYearId]);

  useEffect(() => {
    setBook(null); setScores([]); setReports([]); setSummary([]); setAnnualResults([]);
    setViolations([]); setSelectedStudentId(''); setGradePage(1); setDisciplinePage(1);
    setSummaryPage(1); setAnnualPage(1);
    if (!classId || !selectedYearId) return;
    if (view === 'annual') {
      getAcademicYearResults(selectedYearId, classId).then(setAnnualResults).catch(showError);
      return;
    }
    if (!selectedSemesterId) return;
    Promise.all([
      getResultSummary(selectedYearId, selectedSemesterId, classId),
      getPeriodicReports({ academicYearId: selectedYearId, semesterId: selectedSemesterId, classId }),
    ]).then(([resultRows, reportRows]) => {
      setSummary(resultRows || []); setReports(reportRows || []);
    }).catch(showError);
  }, [selectedYearId, selectedSemesterId, classId, view]);

  useEffect(() => {
    setBook(null); setScores([]); setSubjectId('');
  }, [selectedSemesterId]);

  useEffect(() => {
    setViolations([]); setEditingViolation(null);
    setViolationDraft({ title: '', category: '', description: '', eventDate: '' });
    if (!selectedStudentId || !classId || !selectedYearId || !selectedSemesterId) return;
    getStudentViolations(Number(selectedStudentId), Number(selectedYearId), Number(selectedSemesterId), Number(classId))
      .then(items => setViolations((items || []).filter(item => item.eventType === 'VIOLATION')))
      .catch(showError);
  }, [selectedStudentId, classId, selectedYearId, selectedSemesterId]);

  const students = useMemo(() => {
    const map = new Map<number, StudentRow>();
    for (const row of scores) {
      const student = map.get(row.studentId) || {
        id: row.studentId, name: row.studentName, code: row.studentCode, values: {}, average: row.average,
      };
      student.values[row.gradeItemId] = { score: row.score, comment: row.comment, isGraded: row.isGraded };
      student.average = row.average; map.set(row.studentId, student);
    }
    return [...map.values()];
  }, [scores]);

  const kpis = useMemo(() => ({
    students: summary.length,
    published: summary.filter(row => row.status === 'PUBLISHED').length,
    missing: summary.filter(row => row.gpa == null).length,
    attention: summary.filter(row => row.absentWithoutLeave >= 5 || row.violationCount >= 2).length,
  }), [summary]);

  async function loadGradeBook() {
    if (!classId || !subjectId || !selectedSemesterId) return;
    beginAction(); setBusy(true);
    try {
      const loaded = await getGradeBook(Number(classId), Number(subjectId), Number(selectedSemesterId)) as GradeBook;
      setBook(loaded); setScores(await getGradeBookStudents(loaded.id) as ScoreRow[]); setGradePage(1);
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  function changeValue(studentId: number, itemId: number, patch: Partial<ScoreRow>) {
    setScores(rows => rows.map(row => row.studentId === studentId && row.gradeItemId === itemId
      ? { ...row, ...patch } : row));
  }

  const canAdminEdit = (item: GradeItem) => !locked && book?.status !== 'LOCKED'
    && item.entryRole !== 'SUBJECT_TEACHER';

  async function saveItem(item: GradeItem) {
    const entries = students.map(student => {
      const value = student.values[item.id] || { score: null, comment: null, isGraded: false };
      const score = item.assessmentType === 'SCORE' ? (value.score ?? 0) : value.score;
      return gradeEntryPayload(item.assessmentType, student.id, score, value.comment);
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
      await publishGradeItem(book.id, item.id); await loadGradeBook();
      setMessage(`Đã công bố ${item.name} cho học sinh và phụ huynh.`);
    } catch (cause) { showError(cause); } finally { setSavingItemId(null); }
  }

  async function calculateSubject() {
    if (!book) return;
    beginAction(); setBusy(true);
    try { await calculateSubjectAverages(book.id); await loadGradeBook(); setMessage('Đã đối soát và tính ĐTB môn.'); }
    catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  async function calculateSemester() {
    if (!classId) return;
    beginAction(); setBusy(true);
    try {
      const result: any = await calculateSemesterResults(Number(classId), Number(selectedSemesterId));
      setSummary(await getResultSummary(selectedYearId, selectedSemesterId, classId));
      setMessage(`Đã tính kết quả cho ${result?.updated ?? 0} học sinh. Xếp mức theo kết quả từng môn.`);
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  async function publishWholeBook() {
    if (!book) return;
    beginAction(); setBusy(true);
    try { await changeGradeBookStatus(book.id, 'PUBLISHED'); await loadGradeBook(); setMessage('Đã công bố toàn bộ bảng điểm môn.'); }
    catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  function subjectComment(studentId: number) {
    const subjectName = subjects.find(item => String(item.id) === subjectId)?.name;
    return reports.find(item => item.studentId === studentId)?.subjectReviews
      .find(item => item.subjectName === subjectName)?.comment || '—';
  }

  async function submitViolation(event: React.FormEvent) {
    event.preventDefault();
    if (!selectedStudentId || !violationDraft.title.trim() || !violationDraft.eventDate || locked) return;
    beginAction(); setBusy(true);
    try {
      await saveViolation(Number(selectedStudentId), {
        academicYearId: Number(selectedYearId), semesterId: Number(selectedSemesterId), classId: Number(classId),
        title: violationDraft.title.trim(), category: violationDraft.category.trim(),
        description: violationDraft.description.trim(), eventDate: violationDraft.eventDate,
      }, editingViolation?.id);
      setViolations(await getStudentViolations(Number(selectedStudentId), Number(selectedYearId),
        Number(selectedSemesterId), Number(classId)));
      setSummary(await getResultSummary(selectedYearId, selectedSemesterId, classId));
      setEditingViolation(null); setViolationDraft({ title: '', category: '', description: '', eventDate: '' });
      setMessage('Đã lưu vi phạm và cập nhật gợi ý rèn luyện.');
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  async function removeViolation(id: number) {
    if (locked || !confirm('Xóa vi phạm này?')) return;
    beginAction();
    try {
      await deleteViolation(id, Number(selectedYearId)); setViolations(items => items.filter(item => item.id !== id));
      setSummary(await getResultSummary(selectedYearId, selectedSemesterId, classId)); setMessage('Đã xóa vi phạm.');
    } catch (cause) { showError(cause); }
  }

  function patchSummary(studentId: number, patch: Partial<ResultSummaryItem>) {
    setSummary(rows => rows.map(row => row.studentId === studentId ? { ...row, ...patch } : row));
  }

  async function saveFinal(row: ResultSummaryItem) {
    if (!row.academicAbility || !row.conduct || !row.honor || locked) return;
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
    if (!classId || locked || !confirm('Công bố kết quả học kỳ của lớp cho học sinh và phụ huynh?')) return;
    beginAction(); setBusy(true);
    try {
      setSummary(await publishSemesterResults({
        academicYearId: Number(selectedYearId), semesterId: Number(selectedSemesterId), classId: Number(classId),
      }));
      setReports(await getPeriodicReports({ academicYearId: selectedYearId, semesterId: selectedSemesterId, classId }));
      setMessage('Đã công bố điểm, tổng kết và nhận xét cho các vai trò liên quan.');
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  async function closeSemester() {
    if (locked || !confirm('Đóng kết quả học kỳ cho TOÀN TRƯỜNG? Sau khi đóng sẽ không thể sửa điểm, chuyên cần hoặc kết quả.')) return;
    beginAction(); setBusy(true);
    try {
      await closeSemesterResults(Number(selectedYearId), Number(selectedSemesterId));
      await onContextRefresh?.(); setMessage('Đã đóng kết quả học kỳ. Trạng thái chuyển sang Đã hoàn thành.');
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  async function calculateAnnual() {
    if (!classId || !bothSemestersClosed || selectedYearStatus === 'COMPLETED') return;
    beginAction(); setBusy(true);
    try { setAnnualResults(await calculateAcademicYearResults(Number(selectedYearId), Number(classId))); setMessage('Đã tính tổng kết năm học.'); }
    catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  async function publishAnnual() {
    if (!classId || !annualResults.length || selectedYearStatus === 'COMPLETED'
      || !confirm('Công bố kết quả năm học của lớp cho học sinh và phụ huynh?')) return;
    beginAction(); setBusy(true);
    try { setAnnualResults(await publishAcademicYearResults(Number(selectedYearId), Number(classId))); setMessage('Đã công bố kết quả năm học.'); }
    catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  async function finishAcademicYear() {
    if (!confirm('Hoàn tất năm học cho TOÀN TRƯỜNG? Thao tác này khóa vĩnh viễn toàn bộ kết quả.')) return;
    beginAction(); setBusy(true);
    try { await completeAcademicYear(Number(selectedYearId)); await onContextRefresh?.(); setMessage('Năm học đã hoàn thành và chuyển sang chế độ chỉ xem.'); }
    catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  async function getTemplate() {
    if (!classId || !subjectId || !selectedSemesterId) return;
    beginAction(); setBusy(true);
    try {
      const file = await downloadGradeTemplate(Number(selectedYearId), Number(selectedSemesterId), Number(classId), Number(subjectId));
      downloadBlob(file.blob, `template-diem-${selectedClass?.name || 'lop'}.xlsx`, file.filename);
      setMessage('Đã tải template khớp cấu hình đầu điểm của năm học.');
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  async function importExcel(file?: File) {
    if (!file || !classId || !subjectId || !selectedSemesterId || locked) return;
    beginAction(); setBusy(true);
    try {
      const result = await importAdminScores(Number(selectedYearId), Number(selectedSemesterId),
        Number(classId), Number(subjectId), file);
      await loadGradeBook();
      setMessage(`Đã import ${result.updatedScores} điểm; ${result.zeroFilledScores} ô trống được ghi là 0.`);
    } catch (cause) { showError(cause); } finally {
      setBusy(false); if (importRef.current) importRef.current.value = '';
    }
  }

  async function exportExcel() {
    if (!selectedYearId) return;
    beginAction(); setBusy(true);
    try {
      const file = await downloadResultExport(Number(selectedYearId),
        view === 'semester' && selectedSemesterId ? Number(selectedSemesterId) : undefined,
        classId ? Number(classId) : undefined);
      downloadBlob(file.blob, 'ket-qua-hoc-tap.xlsx', file.filename); setMessage('Đã xuất báo cáo Excel 3 sheet.');
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  if (!selectedYearId) return <div className="notice warning">Chọn năm học để quản lý kết quả.</div>;
  const numericItems = numericAssessmentItems(book?.items || []);

  return <main className="page-stack result-management" aria-label="Quản lý kết quả">
    <header className="result-hero">
      <div><span className="result-eyebrow">Học vụ · Kết quả</span><h1>Quản lý kết quả học tập</h1>
        <p>Đối soát đầu điểm, chuyên cần, rèn luyện và phát hành kết quả theo từng mốc khóa.</p></div>
      <div className="result-view-switch" role="tablist">
        <button className={view === 'semester' ? 'active' : ''} onClick={() => setView('semester')}>Học kỳ</button>
        <button className={view === 'annual' ? 'active' : ''} onClick={() => setView('annual')}>Năm học</button>
      </div>
    </header>

    {error && <div className="notice error" role="alert">{error}</div>}
    {message && <div className="notice success">{message}</div>}
    {locked && <div className="result-lock-banner">
      <span aria-hidden="true">✓</span><div><strong>Kết quả đã hoàn thành</strong><p>Dữ liệu được khóa ở chế độ chỉ xem. Import, sửa, tính lại và công bố lại đã bị vô hiệu hóa.</p></div>
    </div>}

    <section className="panel result-toolbar">
      <div className="result-filter-grid">
        <label>Lớp<select value={classId} onChange={event => setClassId(event.target.value)}>
          <option value="">Tất cả lớp</option>{classes.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
        {view === 'semester' && <label>Môn học<select value={subjectId} onChange={event => { setSubjectId(event.target.value); setBook(null); setScores([]); }}>
          <option value="">Chọn môn</option>{subjects.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>}
        {view === 'semester' && <button className="secondary-button" disabled={!classId || !subjectId || busy} onClick={loadGradeBook}>Mở bảng điểm</button>}
      </div>
      <div className="result-file-actions">
        {view === 'semester' && <button className="excel-action" disabled={!classId || !subjectId || busy} onClick={getTemplate}>
          <span>↓</span><div><strong>Template Excel</strong><small>Đúng cấu hình năm học</small></div></button>}
        {view === 'semester' && <button className="excel-action" disabled={!classId || !subjectId || locked || busy} onClick={() => importRef.current?.click()}>
          <span>↑</span><div><strong>Import Excel</strong><small>Chỉ đầu điểm Admin</small></div></button>}
        <input ref={importRef} hidden type="file" accept=".xlsx" onChange={event => importExcel(event.target.files?.[0])}/>
        <button className="excel-action primary" disabled={busy} onClick={exportExcel}>
          <span>⇩</span><div><strong>Export toàn bộ</strong><small>Điểm · học tập · rèn luyện</small></div></button>
      </div>
    </section>

    {view === 'semester' && classId && <section className="result-kpi-grid">
      <article><span>Học sinh</span><strong>{kpis.students}</strong><small>trong lớp đã chọn</small></article>
      <article><span>Đã công bố</span><strong>{kpis.published}/{kpis.students}</strong><small>kết quả học kỳ</small></article>
      <article><span>Chưa tính</span><strong>{kpis.missing}</strong><small>cần hoàn thiện điểm</small></article>
      <article className={kpis.attention ? 'attention' : ''}><span>Cần đối soát</span><strong>{kpis.attention}</strong><small>nghỉ KP hoặc vi phạm</small></article>
    </section>}

    {view === 'semester' && <>
      <nav className="result-section-tabs" aria-label="Các phần kết quả học kỳ">
        <button className={section === 'grades' ? 'active' : ''} onClick={() => setSection('grades')}><b>1</b> Điểm thành phần</button>
        <button className={section === 'discipline' ? 'active' : ''} onClick={() => setSection('discipline')}><b>2</b> Chuyên cần & vi phạm</button>
        <button className={section === 'summary' ? 'active' : ''} onClick={() => setSection('summary')}><b>3</b> Tổng kết học kỳ</button>
      </nav>

      {section === 'grades' && <section className="panel result-panel">
        <div className="result-panel-heading"><div><h2>Điểm thành phần</h2><p>Cột điểm sinh từ cấu hình năm học; ô số chưa nhập hiển thị 0 để Admin đối soát.</p></div>
          {book && <div className="monitoring-actions"><span className={`badge-status ${book.status === 'LOCKED' ? 'completed' : 'active'}`}>{book.status}</span>
            <button className="secondary-button" disabled={busy} onClick={calculateSubject}>Tính ĐTB môn</button>
            <button disabled={locked || busy} onClick={publishWholeBook}>Công bố môn</button></div>}</div>
        {!book ? <div className="result-empty"><span>01</span><strong>Chọn lớp và môn, sau đó mở bảng điểm</strong><p>Admin sẽ thấy cả điểm giáo viên đã nhập và các cột mình phụ trách.</p></div> : <>
          <div className="result-formula">Công thức cấu hình: {numericItems.length ? numericItems.map(item => `${item.name} × ${item.weight}`).join(' + ') : 'Không có đầu điểm số'}</div>
          <div className="table-responsive"><table className="result-grade-table"><thead><tr><th>Học sinh</th>{book.items.map(item => <th key={item.id}>
            <span>{item.name}</span><small>{assessmentLabel[item.assessmentType]} · HS {item.weight}</small>
            <div className="grade-column-actions">{canAdminEdit(item) && <button className="secondary-button" disabled={savingItemId === item.id} onClick={() => saveItem(item)}>Lưu</button>}
              <button disabled={locked || savingItemId === item.id} onClick={() => publishItem(item)}>Công bố</button></div></th>)}<th>ĐTB</th><th>Nhận xét GVBM</th></tr></thead>
            <tbody>{pageOf(students, gradePage).map(student => <tr key={student.id}><td><strong>{student.name}</strong><small>{student.code}</small></td>{book.items.map(item => {
              const value = student.values[item.id] || { score: null, comment: null, isGraded: false };
              const disabled = !canAdminEdit(item);
              return <td key={item.id}>{item.assessmentType === 'SCORE' && <input type="number" min="0" max="10" step="0.1" disabled={disabled} value={value.score ?? 0}
                onChange={event => changeValue(student.id, item.id, { score: Number(event.target.value || 0), comment: null, isGraded: true })}/>} {item.assessmentType === 'PASS_FAIL' && <select disabled={disabled} value={value.comment || ''}
                  onChange={event => changeValue(student.id, item.id, { score: null, comment: event.target.value || null, isGraded: !!event.target.value })}><option value="">—</option><option value="PASS">Đạt</option><option value="FAIL">Chưa đạt</option></select>}
                {item.assessmentType === 'COMMENT' && <textarea disabled={disabled} rows={2} value={value.comment || ''}
                  onChange={event => changeValue(student.id, item.id, { score: null, comment: event.target.value, isGraded: !!event.target.value.trim() })}/>}</td>;
            })}<td><strong>{student.average ?? '—'}</strong></td><td className="subject-comment-cell">{subjectComment(student.id)}</td></tr>)}</tbody></table></div>
          <Pagination total={students.length} page={gradePage} onChange={setGradePage}/>
        </>}
      </section>}

      {section === 'discipline' && <div className="discipline-grid">
        <section className="panel result-panel"><div className="result-panel-heading"><div><h2>Chuyên cần & vi phạm</h2><p>Gợi ý: Tốt 0 VP/≤2 nghỉ KP; Khá ≤1/≤4; Đạt ≤2/≤9; còn lại Chưa đạt.</p></div></div>
          {!classId ? <div className="result-empty"><strong>Chọn lớp để xem dữ liệu</strong></div> : <><div className="table-responsive"><table><thead><tr><th>Học sinh</th><th>Vi phạm</th><th>Nghỉ có phép</th><th>Nghỉ không phép</th><th>Gợi ý</th><th /></tr></thead><tbody>
            {pageOf(summary, disciplinePage).map(row => <tr key={row.studentId}><td><strong>{row.studentName}</strong><small className="table-subtext">{row.studentCode}</small></td><td>{row.violationCount}</td><td>{row.absentWithLeave}</td><td>{row.absentWithoutLeave}</td><td><span className="result-level">{row.suggestedConduct || '—'}</span></td><td><button className="secondary-button" onClick={() => setSelectedStudentId(String(row.studentId))}>Chi tiết</button></td></tr>)}</tbody></table></div>
            <Pagination total={summary.length} page={disciplinePage} onChange={setDisciplinePage}/></>}
        </section>
        <section className="panel result-panel"><div className="result-panel-heading"><div><h2>Quản lý vi phạm</h2><p>{selectedStudentId ? 'Chỉnh sửa hồ sơ đã chọn.' : 'Chọn học sinh ở bảng bên trái.'}</p></div></div>
          {selectedStudentId && <><form className="violation-form" onSubmit={submitViolation}><label>Tiêu đề<input required disabled={locked} maxLength={200} value={violationDraft.title} onChange={event => setViolationDraft(d => ({ ...d, title: event.target.value }))}/></label>
            <label>Phân loại<input disabled={locked} maxLength={100} value={violationDraft.category} onChange={event => setViolationDraft(d => ({ ...d, category: event.target.value }))}/></label>
            <label>Ngày vi phạm<input required disabled={locked} type="date" value={violationDraft.eventDate} onChange={event => setViolationDraft(d => ({ ...d, eventDate: event.target.value }))}/></label>
            <label className="wide">Mô tả<textarea disabled={locked} rows={3} maxLength={2000} value={violationDraft.description} onChange={event => setViolationDraft(d => ({ ...d, description: event.target.value }))}/></label>
            <div className="monitoring-actions wide"><button disabled={locked || busy}>{editingViolation ? 'Lưu thay đổi' : 'Thêm vi phạm'}</button>{editingViolation && <button type="button" className="secondary-button" onClick={() => { setEditingViolation(null); setViolationDraft({ title: '', category: '', description: '', eventDate: '' }); }}>Hủy</button>}</div></form>
            <div className="violation-list">{violations.map(item => <article key={item.id}><div><strong>{item.title}</strong><small>{item.category || 'Vi phạm'} · {item.eventDate}</small><p>{item.description}</p></div><div className="table-actions"><button className="secondary-button" disabled={locked} onClick={() => { setEditingViolation(item); setViolationDraft({ title: item.title, category: item.category || '', description: item.description || '', eventDate: item.eventDate }); }}>Sửa</button><button className="danger" disabled={locked} onClick={() => removeViolation(item.id)}>Xóa</button></div></article>)}</div></>}
        </section>
      </div>}

      {section === 'summary' && <section className="panel result-panel"><div className="result-panel-heading"><div><h2>Tổng kết học kỳ</h2><p>Điểm TB chỉ tham khảo; mức học tập được xét theo từng môn theo Thông tư 22/2021/TT-BGDĐT.</p></div>
        <div className="monitoring-actions"><button className="secondary-button" disabled={!classId || locked || busy} onClick={calculateSemester}>Tính kết quả lớp</button>
          <button disabled={!classId || locked || busy || !summary.length} onClick={publishSummary}>Công bố lớp</button>
          <button className="danger" disabled={locked || busy || selectedSemesterStatus !== 'ACTIVE'} onClick={closeSemester}>Đóng học kỳ toàn trường</button></div></div>
        {!classId ? <div className="result-empty"><strong>Chọn lớp để tổng kết</strong></div> : <><div className="table-responsive"><table className="summary-result-table"><thead><tr><th>Học sinh</th><th>ĐTB / Hạng</th><th>VP · nghỉ KP</th><th>Học tập gợi ý</th><th>Rèn luyện gợi ý</th><th>Học tập cuối</th><th>Rèn luyện cuối</th><th>Danh hiệu</th><th>Nhận xét GVCN</th><th>Trạng thái</th><th /></tr></thead><tbody>
          {pageOf(summary, summaryPage).map(row => <tr key={row.studentId}><td><strong>{row.studentName}</strong><small>{row.studentCode}</small></td><td>{row.gpa ?? '—'} / {row.rank ?? '—'}</td><td>{row.violationCount} · {row.absentWithoutLeave}</td><td>{row.suggestedAcademicAbility || '—'}</td><td>{row.suggestedConduct || '—'}</td>
            <td><select disabled={locked} value={row.academicAbility || ''} onChange={event => patchSummary(row.studentId, { academicAbility: event.target.value })}><option value="">—</option>{[...new Set([row.academicAbility, ...resultLevels].filter(Boolean))].map(value => <option key={value!}>{value}</option>)}</select></td>
            <td><select disabled={locked} value={row.conduct || ''} onChange={event => patchSummary(row.studentId, { conduct: event.target.value })}><option value="">—</option>{[...new Set([row.conduct, ...resultLevels].filter(Boolean))].map(value => <option key={value!}>{value}</option>)}</select></td>
            <td><select disabled={locked} value={row.honor || ''} onChange={event => patchSummary(row.studentId, { honor: event.target.value })}><option value="">—</option>{[...new Set([row.honor, ...honorOptions].filter(Boolean))].map(value => <option key={value!}>{value}</option>)}</select></td>
            <td className="subject-comment-cell">{row.generalComment || '—'}</td><td><span className={`badge-status ${row.status === 'PUBLISHED' ? 'active' : 'draft'}`}>{row.status === 'PUBLISHED' ? 'Đã công bố' : row.reportStatus === 'SUBMITTED' ? 'Chờ công bố' : 'Chờ GVCN'}</span></td><td><button className="secondary-button" disabled={locked} onClick={() => saveFinal(row)}>Lưu</button></td></tr>)}</tbody></table></div>
          <Pagination total={summary.length} page={summaryPage} onChange={setSummaryPage}/></>}
      </section>}
    </>}

    {view === 'annual' && <section className="panel result-panel annual-result-panel">
      <div className="result-panel-heading"><div><h2>Tổng kết năm học</h2><p>ĐTB môn cả năm = (HK1 + 2 × HK2) / 3. Kết quả rèn luyện dùng ma trận ưu tiên học kỳ II.</p></div>
        <div className="monitoring-actions">{bothSemestersClosed ? <span className="result-ready-chip">✓ Đã đóng đủ 2 học kỳ</span> : <span className="result-wait-chip">Cần đóng đủ 2 học kỳ</span>}
          {bothSemestersClosed && <button className="secondary-button" disabled={!classId || selectedYearStatus === 'COMPLETED' || busy} onClick={calculateAnnual}>Tính tổng kết lớp</button>}
          <button disabled={!classId || !annualResults.length || selectedYearStatus === 'COMPLETED' || busy} onClick={publishAnnual}>Công bố lớp</button>
          {bothSemestersClosed && <button className="danger" disabled={selectedYearStatus === 'COMPLETED' || busy} onClick={finishAcademicYear}>Hoàn tất năm học</button>}</div></div>
      {!classId ? <div className="result-empty"><span>02</span><strong>Chọn lớp để xem tổng kết năm học</strong><p>Nút tính chỉ xuất hiện khi cả hai học kỳ đã được đóng.</p></div> : <><div className="table-responsive"><table className="annual-result-table"><thead><tr><th>Học sinh</th><th>HK1</th><th>Học tập HK1</th><th>Rèn luyện HK1</th><th>HK2</th><th>Học tập HK2</th><th>Rèn luyện HK2</th><th>ĐTB năm</th><th>Học tập cả năm</th><th>Rèn luyện cả năm</th><th>Danh hiệu</th><th>Hạng</th><th>Trạng thái</th></tr></thead><tbody>
        {pageOf(annualResults, annualPage).map(row => <tr key={row.studentId}><td><strong>{row.studentName}</strong><small>{row.studentCode}</small></td><td>{row.semester1Average ?? '—'}</td><td>{row.semester1AcademicAbility || '—'}</td><td>{row.semester1Conduct || '—'}</td><td>{row.semester2Average ?? '—'}</td><td>{row.semester2AcademicAbility || '—'}</td><td>{row.semester2Conduct || '—'}</td><td><strong>{row.annualAverage ?? '—'}</strong></td><td><span className="result-level">{row.academicAbility || '—'}</span></td><td><span className="result-level">{row.conduct || '—'}</span></td><td>{row.honor || '—'}</td><td>{row.rank ?? '—'}</td><td><span className={`badge-status ${row.status === 'PUBLISHED' ? 'active' : 'draft'}`}>{row.status === 'PUBLISHED' ? 'Đã công bố' : 'Bản nháp'}</span></td></tr>)}</tbody></table></div>
        <Pagination total={annualResults.length} page={annualPage} onChange={setAnnualPage}/></>}
    </section>}
  </main>;
}
