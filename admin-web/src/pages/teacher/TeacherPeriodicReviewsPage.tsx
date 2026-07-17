import { useEffect, useState, type FormEvent } from 'react';
import {
  deleteTeacherViolation,
  getHomeroomReports,
  getReviewAssignments,
  getSubjectReviews,
  getTeacherDashboard,
  getTeacherViolations,
  saveHomeroomReport,
  saveSubjectReview,
  saveTeacherViolation,
  submitHomeroomClass,
  submitHomeroomReport,
  submitSubjectReviews,
} from '../../api/teacher';
import { useTeacherAcademic } from '../../teacher/TeacherAcademicContext';

interface Review { id: number | null; studentId: number; studentName: string; studentCode: string; comment: string | null; strengths: string | null; improvements: string | null; status: 'DRAFT' | 'SUBMITTED' }
interface Report { id: number | null; classId: number; className: string; studentId: number; studentName: string; studentCode: string; generalComment: string | null; status: 'DRAFT' | 'SUBMITTED' | 'PUBLISHED' }
interface Violation { id: number; title: string; category: string | null; description: string | null; eventDate: string; status: 'DRAFT' | 'SUBMITTED' }

export default function TeacherPeriodicReviewsPage() {
  const { selectedYearId, selectedSemesterId } = useTeacherAcademic();
  const [assignments, setAssignments] = useState<any[]>([]);
  const [assignmentKey, setAssignmentKey] = useState('');
  const [reviews, setReviews] = useState<Review[]>([]);
  const [homeroomClassId, setHomeroomClassId] = useState<number | null>(null);
  const [reports, setReports] = useState<Report[]>([]);
  const [selectedReport, setSelectedReport] = useState<Report | null>(null);
  const [generalComment, setGeneralComment] = useState('');
  const [violations, setViolations] = useState<Violation[]>([]);
  const [editingViolation, setEditingViolation] = useState<Violation | null>(null);
  const [violation, setViolation] = useState({ title: '', category: '', description: '', eventDate: '' });
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setAssignments([]); setAssignmentKey(''); setReviews([]); setHomeroomClassId(null); setReports([]); setSelectedReport(null); setError(''); setMessage('');
    if (!selectedYearId || !selectedSemesterId) return;
    Promise.all([
      getReviewAssignments(selectedYearId),
      getTeacherDashboard(selectedYearId, selectedSemesterId).catch(() => null),
    ]).then(async ([rows, dashboard]) => {
      setAssignments(rows || []);
      if (dashboard?.classId) {
        setHomeroomClassId(dashboard.classId);
        setReports(await getHomeroomReports(selectedYearId, selectedSemesterId, dashboard.classId) as Report[]);
      }
    }).catch(showError);
  }, [selectedYearId, selectedSemesterId]);

  async function loadReviews(value = assignmentKey) {
    if (!selectedYearId || !selectedSemesterId || !value) return;
    const [classId, subjectId] = value.split(':').map(Number);
    try { setReviews(await getSubjectReviews(selectedYearId, selectedSemesterId, classId, subjectId) as Review[]); }
    catch (cause) { showError(cause); }
  }

  function showError(cause: unknown) { setMessage(''); setError(cause instanceof Error ? cause.message : 'Có lỗi xảy ra.'); }
  function patchReview(studentId: number, patch: Partial<Review>) { setReviews(rows => rows.map(row => row.studentId === studentId ? { ...row, ...patch } : row)); }

  async function saveReview(review: Review, submit: boolean) {
    if (!selectedYearId || !selectedSemesterId || !assignmentKey) return;
    const [classId, subjectId] = assignmentKey.split(':').map(Number);
    setBusy(true); setError(''); setMessage('');
    try {
      await saveSubjectReview(review.studentId, {
        academicYearId: selectedYearId, semesterId: selectedSemesterId, classId, subjectId,
        comment: review.comment || '', strengths: review.strengths || '', improvements: review.improvements || '',
      });
      if (submit) await submitSubjectReviews({ academicYearId: selectedYearId, semesterId: selectedSemesterId, classId, subjectId, studentIds: [review.studentId] });
      await loadReviews(); setMessage(submit ? 'Đã gửi nhận xét thẳng lên hệ thống.' : 'Đã lưu bản nháp.');
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  async function selectHomeroomReport(report: Report) {
    if (!selectedYearId || !selectedSemesterId) return;
    setSelectedReport(report); setGeneralComment(report.generalComment || ''); setEditingViolation(null);
    setViolation({ title: '', category: '', description: '', eventDate: '' });
    try { setViolations((await getTeacherViolations(report.studentId, selectedYearId, selectedSemesterId, report.classId) as Violation[]).filter(item => item.status === 'DRAFT' || item.status === 'SUBMITTED')); }
    catch (cause) { showError(cause); }
  }

  async function saveGeneral(submit: boolean) {
    if (!selectedReport || !selectedYearId || !selectedSemesterId) return;
    const payload = { academicYearId: selectedYearId, semesterId: selectedSemesterId, classId: selectedReport.classId, generalComment };
    setBusy(true); setError(''); setMessage('');
    try {
      await saveHomeroomReport(selectedReport.studentId, payload);
      if (submit) await submitHomeroomReport(selectedReport.studentId, { academicYearId: selectedYearId, semesterId: selectedSemesterId, classId: selectedReport.classId });
      const rows = await getHomeroomReports(selectedYearId, selectedSemesterId, selectedReport.classId) as Report[];
      setReports(rows); const current = rows.find(row => row.studentId === selectedReport.studentId) || null; setSelectedReport(current);
      if (current) await selectHomeroomReport(current);
      setMessage(submit ? 'Đã Submit nhận xét chung và danh sách vi phạm cho Admin.' : 'Đã lưu bản nháp GVCN.');
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  async function submitClass() {
    if (!selectedYearId || !selectedSemesterId || !homeroomClassId) return;
    setBusy(true); setError(''); setMessage('');
    try {
      await submitHomeroomClass({ academicYearId: selectedYearId, semesterId: selectedSemesterId, classId: homeroomClassId });
      setReports(await getHomeroomReports(selectedYearId, selectedSemesterId, homeroomClassId) as Report[]);
      setMessage('Đã Submit cả lớp cho Admin.');
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  async function saveViolation(event: FormEvent) {
    event.preventDefault();
    if (!selectedReport || !selectedYearId || !selectedSemesterId) return;
    setBusy(true); setError('');
    try {
      await saveTeacherViolation(selectedReport.studentId, {
        academicYearId: selectedYearId, semesterId: selectedSemesterId, classId: selectedReport.classId,
        ...violation,
      }, editingViolation?.id);
      setViolations(await getTeacherViolations(selectedReport.studentId, selectedYearId, selectedSemesterId, selectedReport.classId) as Violation[]);
      setEditingViolation(null); setViolation({ title: '', category: '', description: '', eventDate: '' }); setMessage('Đã lưu vi phạm.');
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  async function removeViolation(id: number) {
    if (!selectedReport || !selectedYearId || !selectedSemesterId || !confirm('Xóa vi phạm này?')) return;
    try {
      await deleteTeacherViolation(id, selectedYearId);
      setViolations(await getTeacherViolations(selectedReport.studentId, selectedYearId, selectedSemesterId, selectedReport.classId) as Violation[]);
    } catch (cause) { showError(cause); }
  }

  return <main className="teacher-page page-stack">
    <div className="page-heading"><div><span className="eyebrow">Đánh giá học sinh</span><h1>Nhận xét định kỳ</h1><p>GVBM và GVCN Submit độc lập; GVCN không duyệt nhận xét môn và không chọn hạnh kiểm.</p></div></div>
    {error && <div className="notice error">{error}</div>}{message && <div className="notice success">{message}</div>}
    <section className="panel"><h2>Nhận xét bộ môn</h2><div className="teacher-filter-row"><label>Lớp · Môn<select value={assignmentKey} onChange={event => { setAssignmentKey(event.target.value); setReviews([]); void loadReviews(event.target.value); }}><option value="">Chọn phân công</option>{assignments.map(item => <option key={`${item.classId}:${item.subjectId}`} value={`${item.classId}:${item.subjectId}`}>{item.className} · {item.subjectName}</option>)}</select></label></div>
      <div className="review-editor-list">{reviews.map(review => <article key={review.studentId}><header><div><strong>{review.studentName}</strong><small>{review.studentCode}</small></div><span className={`badge-status ${review.status === 'SUBMITTED' ? 'active' : 'draft'}`}>{review.status === 'SUBMITTED' ? 'Đã gửi hệ thống' : 'Bản nháp'}</span></header><label>Nhận xét<textarea disabled={review.status === 'SUBMITTED'} rows={3} value={review.comment || ''} onChange={event => patchReview(review.studentId, { comment: event.target.value })}/></label><div className="review-two-cols"><label>Điểm mạnh<textarea disabled={review.status === 'SUBMITTED'} rows={2} value={review.strengths || ''} onChange={event => patchReview(review.studentId, { strengths: event.target.value })}/></label><label>Cần cải thiện<textarea disabled={review.status === 'SUBMITTED'} rows={2} value={review.improvements || ''} onChange={event => patchReview(review.studentId, { improvements: event.target.value })}/></label></div>{review.status !== 'SUBMITTED' && <div className="monitoring-actions"><button className="secondary-button" disabled={busy} onClick={() => saveReview(review, false)}>Lưu nháp</button><button disabled={busy || !review.comment?.trim()} onClick={() => saveReview(review, true)}>Gửi hệ thống</button></div>}</article>)}</div>
    </section>
    {homeroomClassId && <section className="panel homeroom-review-panel"><div className="monitoring-actions"><div><h2>Nhận xét chủ nhiệm</h2><p>Chỉ gồm nhận xét chung và vi phạm của học sinh.</p></div><button disabled={busy || !reports.length} onClick={submitClass}>Submit cả lớp</button></div><div className="homeroom-review-layout"><aside>{reports.map(report => <button className={selectedReport?.studentId === report.studentId ? 'active' : ''} key={report.studentId} onClick={() => selectHomeroomReport(report)}><span><strong>{report.studentName}</strong><small>{report.studentCode}</small></span><b>{report.status}</b></button>)}</aside>{selectedReport ? <div className="homeroom-student-editor"><div className="monitoring-actions"><h3>{selectedReport.studentName}</h3><span className={`badge-status ${selectedReport.status === 'DRAFT' ? 'draft' : 'active'}`}>{selectedReport.status}</span></div><label>Nhận xét chung của GVCN<textarea rows={5} disabled={selectedReport.status !== 'DRAFT'} value={generalComment} onChange={event => setGeneralComment(event.target.value)}/></label><div className="monitoring-actions"><h3>Vi phạm trong học kỳ</h3></div>{selectedReport.status === 'DRAFT' && <form className="violation-form" onSubmit={saveViolation}><label>Tiêu đề<input required value={violation.title} onChange={event => setViolation(v => ({ ...v, title: event.target.value }))}/></label><label>Phân loại<input value={violation.category} onChange={event => setViolation(v => ({ ...v, category: event.target.value }))}/></label><label>Ngày<input required type="date" value={violation.eventDate} onChange={event => setViolation(v => ({ ...v, eventDate: event.target.value }))}/></label><label className="wide">Mô tả<textarea rows={2} value={violation.description} onChange={event => setViolation(v => ({ ...v, description: event.target.value }))}/></label><div className="monitoring-actions wide"><button disabled={busy}>{editingViolation ? 'Lưu vi phạm' : 'Thêm vi phạm'}</button></div></form>}<div className="violation-list">{violations.map(item => <article key={item.id}><div><strong>{item.title}</strong><small>{item.category || 'Vi phạm'} · {item.eventDate} · {item.status}</small><p>{item.description}</p></div>{selectedReport.status === 'DRAFT' && item.status === 'DRAFT' && <div className="table-actions"><button className="secondary-button" onClick={() => { setEditingViolation(item); setViolation({ title: item.title, category: item.category || '', description: item.description || '', eventDate: item.eventDate }); }}>Sửa</button><button className="danger" onClick={() => removeViolation(item.id)}>Xóa</button></div>}</article>)}</div>{selectedReport.status === 'DRAFT' && <div className="monitoring-actions"><button className="secondary-button" disabled={busy} onClick={() => saveGeneral(false)}>Lưu nháp</button><button disabled={busy || !generalComment.trim()} onClick={() => saveGeneral(true)}>Submit</button></div>}</div> : <div className="empty-state">Chọn học sinh để nhập nhận xét chủ nhiệm.</div>}</div></section>}
  </main>;
}
