import { useEffect, useState } from 'react';
import AdminGradeImportPanel from '../components/AdminGradeImportPanel';
import { completeAcademicYear } from '../api/academicYear';
import { getAcademicYearMasterData } from '../api/academicYearConfig';
import { getClasses } from '../api/class';
import {
  calculateSchoolSemesterResults,
  getGradeComponentOverview,
} from '../api/gradeBook';
import type { AssessmentType } from '../api/gradeConfiguration';
import {
  calculateAcademicYearResults,
  closeSemesterResults,
  downloadResultExport,
  getAcademicYearResults,
  getResultSummary,
  overrideResult,
  publishAcademicYearResults,
  publishSchoolSemesterResults,
  type AcademicYearResultItem,
  type ResultSummaryItem,
} from '../api/resultManagement';
import { getSubjects } from '../api/subject';
import { numericAssessmentItems } from '../utils/gradeAssessment';

type MainView = 'semester' | 'annual';
type SemesterSection = 'import' | 'grades' | 'summary';

interface SemesterContextItem {
  id: number;
  name: string;
  order: number;
  status: 'NOT_STARTED' | 'ACTIVE' | 'COMPLETED';
}

interface GradeComponentColumn {
  code: string;
  name: string;
  weight: number;
  assessmentType: AssessmentType;
}

interface GradeComponentCell {
  score: number | null;
  comment: string | null;
  isGraded: boolean;
}

interface GradeComponentRow {
  classId: number;
  className: string;
  subjectId: number;
  subjectName: string;
  studentId: number;
  studentName: string;
  studentCode: string;
  values: Record<string, GradeComponentCell>;
  average: number | null;
}

interface GradeComponentOverview {
  columns: GradeComponentColumn[];
  rows: GradeComponentRow[];
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
  const [componentOverview, setComponentOverview] = useState<GradeComponentOverview | null>(null);
  const [summary, setSummary] = useState<ResultSummaryItem[]>([]);
  const [annualResults, setAnnualResults] = useState<AcademicYearResultItem[]>([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [gradePage, setGradePage] = useState(1);
  const [summaryPage, setSummaryPage] = useState(1);
  const [annualPage, setAnnualPage] = useState(1);

  const locked = selectedYearStatus === 'COMPLETED'
    || (view === 'semester' && selectedSemesterStatus === 'COMPLETED');
  const bothSemestersClosed = semesters.length >= 2 && semesters.every(item => item.status === 'COMPLETED');

  const showError = (cause: unknown) => {
    setMessage('');
    setError(cause instanceof Error ? cause.message : 'Có lỗi xảy ra. Vui lòng thử lại.');
  };
  const beginAction = () => { setError(''); setMessage(''); };

  useEffect(() => {
    setClasses([]); setSubjects([]); setClassId(''); setSubjectId(''); setComponentOverview(null);
    setSummary([]); setAnnualResults([]);
    setGradePage(1); setSummaryPage(1); setAnnualPage(1); beginAction();
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
    setSummary([]); setAnnualResults([]);
    setGradePage(1); setSummaryPage(1); setAnnualPage(1);
    if (!classId || !selectedYearId) return;
    if (view === 'annual') {
      getAcademicYearResults(selectedYearId, classId).then(setAnnualResults).catch(showError);
      return;
    }
    if (!selectedSemesterId) return;
    getResultSummary(selectedYearId, selectedSemesterId, classId)
      .then(resultRows => setSummary(resultRows || []))
      .catch(showError);
  }, [selectedYearId, selectedSemesterId, classId, view]);

  useEffect(() => {
    setComponentOverview(null); setSubjectId('');
  }, [selectedSemesterId]);

  useEffect(() => {
    let cancelled = false;
    setComponentOverview(null); setGradePage(1); setBusy(false);
    if (view !== 'semester' || section !== 'grades' || !selectedYearId || !selectedSemesterId) return;

    beginAction(); setBusy(true);
    void (async () => {
      try {
        const overview = await getGradeComponentOverview(
          Number(selectedYearId), Number(selectedSemesterId),
          classId ? Number(classId) : undefined, subjectId ? Number(subjectId) : undefined,
        ) as GradeComponentOverview;
        if (!cancelled) setComponentOverview(overview);
      } catch (cause) {
        if (!cancelled) showError(cause);
      } finally {
        if (!cancelled) setBusy(false);
      }
    })();

    return () => { cancelled = true; };
  }, [view, section, selectedYearId, selectedSemesterId, classId, subjectId]);

  useEffect(() => {
    if (view !== 'semester' || section !== 'grades' || !selectedYearId || !selectedSemesterId) return;
    let cancelled = false;
    const refresh = () => {
      void getGradeComponentOverview(
        Number(selectedYearId), Number(selectedSemesterId),
        classId ? Number(classId) : undefined, subjectId ? Number(subjectId) : undefined,
      ).then(data => { if (!cancelled) setComponentOverview(data as GradeComponentOverview); }).catch(() => undefined);
    };
    const intervalId = window.setInterval(refresh, 10000);
    return () => { cancelled = true; window.clearInterval(intervalId); };
  }, [view, section, selectedYearId, selectedSemesterId, classId, subjectId]);

  async function calculateSemester() {
    beginAction(); setBusy(true);
    try {
      const result: any = await calculateSchoolSemesterResults(Number(selectedYearId), Number(selectedSemesterId));
      if (classId) setSummary(await getResultSummary(selectedYearId, selectedSemesterId, classId));
      setMessage(`Đã tính kết quả học kỳ toàn trường cho ${result?.updated ?? 0} học sinh.`);
    } catch (cause) { showError(cause); } finally { setBusy(false); }
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
    if (locked || !confirm('Công bố kết quả học kỳ của TOÀN TRƯỜNG cho học sinh và phụ huynh?')) return;
    beginAction(); setBusy(true);
    try {
      await publishSchoolSemesterResults(Number(selectedYearId), Number(selectedSemesterId));
      if (classId) setSummary(await getResultSummary(selectedYearId, selectedSemesterId, classId));
      setMessage('Đã công bố kết quả học kỳ cho toàn trường.');
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
  const numericItems = numericAssessmentItems(componentOverview?.columns || []);

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
      <span aria-hidden="true">✓</span><div><strong>Kết quả đã hoàn thành</strong><p>Dữ liệu được khóa ở chế độ chỉ xem. Import, sửa và tính lại đã bị vô hiệu hóa.</p></div>
    </div>}

    {!(view === 'semester' && section === 'import') && <section className="panel result-toolbar">
      <div className="result-filter-grid">
        <label>{view === 'semester' && section === 'grades' ? 'Lọc lớp' : 'Lớp'}<select value={classId} onChange={event => setClassId(event.target.value)}>
          <option value="">{view === 'semester' && section === 'grades' ? 'Tất cả lớp' : 'Chọn lớp'}</option>{classes.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
        {view === 'semester' && <label>{section === 'grades' ? 'Lọc môn' : 'Môn học'}<select value={subjectId} onChange={event => setSubjectId(event.target.value)}>
          <option value="">{section === 'grades' ? 'Tất cả môn' : 'Chọn môn'}</option>{subjects.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>}
      </div>
      <div className="result-file-actions">
        <button className="excel-action primary" disabled={busy} onClick={exportExcel}>
          <span>⇩</span><div><strong>Export toàn bộ</strong><small>Điểm · học tập · rèn luyện</small></div></button>
      </div>
    </section>}

    {view === 'semester' && <>
      <nav className="result-section-tabs" aria-label="Các phần kết quả học kỳ">
        <button className={section === 'import' ? 'active' : ''} onClick={() => setSection('import')}><b>0</b> Import đầu điểm</button>
        <button className={section === 'grades' ? 'active' : ''} onClick={() => setSection('grades')}><b>1</b> Điểm thành phần</button>
        <button className={section === 'summary' ? 'active' : ''} onClick={() => setSection('summary')}><b>2</b> Tổng kết học kỳ</button>
      </nav>

      {section === 'import' && <AdminGradeImportPanel disabled={locked} />}

      {section === 'grades' && <section className="panel result-panel">
        <div className="result-panel-heading"><div><h2>Điểm thành phần</h2><p>Mặc định hiển thị toàn bộ lớp, môn và học sinh theo thứ tự. Dùng bộ lọc để thu hẹp danh sách; bảng tự làm mới mỗi 10 giây khi đang mở.</p></div></div>
        {!componentOverview ? <div className="result-empty"><span>01</span><strong>Đang tải bảng điểm thành phần</strong></div>
          : componentOverview.rows.length === 0 ? <div className="result-empty"><span>01</span><strong>Chưa có dữ liệu điểm theo bộ lọc hiện tại</strong></div> : <>
          <div className="result-formula">Công thức cấu hình: {numericItems.length ? numericItems.map(item => `${item.name} × ${item.weight}`).join(' + ') : 'Không có đầu điểm số'}</div>
          <div className="table-responsive"><table className="result-grade-table"><thead><tr><th>#</th><th>Lớp</th><th>Môn học</th><th>Học sinh</th>{componentOverview.columns.map(item => <th key={item.code}>
            <span>{item.name}</span><small>{assessmentLabel[item.assessmentType]} · HS {item.weight}</small>
          </th>)}<th>ĐTB</th></tr></thead>
            <tbody>{pageOf(componentOverview.rows, gradePage).map((row, index) => <tr key={`${row.classId}-${row.subjectId}-${row.studentId}`}><td>{(gradePage - 1) * PAGE_SIZE + index + 1}</td><td>{row.className}</td><td>{row.subjectName}</td><td><strong>{row.studentName}</strong><small>{row.studentCode}</small></td>{componentOverview.columns.map(item => {
              const value = row.values[item.code];
              return <td key={item.code}>{item.assessmentType === 'SCORE' && <input type="number" disabled value={value?.score ?? ''} />} {item.assessmentType === 'PASS_FAIL' && <select disabled value={value?.comment || ''}>
                  <option value="">—</option><option value="PASS">Đạt</option><option value="FAIL">Chưa đạt</option></select>}
                {item.assessmentType === 'COMMENT' && <textarea disabled rows={2} value={value?.comment || ''} />}</td>;
            })}<td><strong>{row.average ?? '—'}</strong></td></tr>)}</tbody></table></div>
          <Pagination total={componentOverview.rows.length} page={gradePage} onChange={setGradePage} />
        </>}
      </section>}

      {section === 'summary' && <section className="panel result-panel"><div className="result-panel-heading"><div><h2>Tổng kết học kỳ</h2><p>Tính và công bố áp dụng cho toàn trường. Hạnh kiểm được điền sẵn từ chuyên cần; chọn lớp bên trên chỉ để xem hoặc điều chỉnh từng học sinh trước khi công bố.</p></div>
        <div className="monitoring-actions"><button className="secondary-button" disabled={locked || busy} onClick={calculateSemester}>Tính kết quả toàn trường</button>
          <button disabled={locked || busy} onClick={publishSummary}>Công bố toàn trường</button>
          <button className="danger" disabled={locked || busy || selectedSemesterStatus !== 'ACTIVE'} onClick={closeSemester}>Đóng học kỳ toàn trường</button></div></div>
        {!classId ? <div className="result-empty"><strong>Chọn lớp để xem và điều chỉnh kết quả từng học sinh</strong><p>Nút tính và công bố bên trên luôn áp dụng cho toàn trường.</p></div> : <><div className="table-responsive"><table className="summary-result-table"><thead><tr><th>Học sinh</th><th>ĐTB / Hạng</th><th>Học tập gợi ý</th><th>Học tập cuối</th><th>Rèn luyện cuối</th><th>Danh hiệu</th><th>Trạng thái</th><th /></tr></thead><tbody>
          {pageOf(summary, summaryPage).map(row => <tr key={row.studentId}><td><strong>{row.studentName}</strong><small>{row.studentCode}</small></td><td>{row.gpa ?? '—'} / {row.rank ?? '—'}</td><td>{row.suggestedAcademicAbility || '—'}</td>
            <td><select disabled={locked} value={row.academicAbility || ''} onChange={event => patchSummary(row.studentId, { academicAbility: event.target.value })}><option value="">—</option>{[...new Set([row.academicAbility, ...resultLevels].filter(Boolean))].map(value => <option key={value!}>{value}</option>)}</select></td>
            <td><select disabled={locked} value={row.conduct || ''} onChange={event => patchSummary(row.studentId, { conduct: event.target.value })}><option value="">—</option>{[...new Set([row.conduct, ...resultLevels].filter(Boolean))].map(value => <option key={value!}>{value}</option>)}</select></td>
            <td><select disabled={locked} value={row.honor || ''} onChange={event => patchSummary(row.studentId, { honor: event.target.value })}><option value="">—</option>{[...new Set([row.honor, ...honorOptions].filter(Boolean))].map(value => <option key={value!}>{value}</option>)}</select></td>
            <td><span className={`badge-status ${row.status === 'PUBLISHED' ? 'active' : 'draft'}`}>{row.status === 'PUBLISHED' ? 'Đã công bố' : 'Bản nháp'}</span></td><td><button className="secondary-button" disabled={locked} onClick={() => saveFinal(row)}>Lưu</button></td></tr>)}</tbody></table></div>
          <Pagination total={summary.length} page={summaryPage} onChange={setSummaryPage} /></>}
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
        <Pagination total={annualResults.length} page={annualPage} onChange={setAnnualPage} /></>}
    </section>}
  </main>;
}
