import { useEffect, useMemo, useState } from 'react';
import { getClasses } from '../api/class';
import { getSemesters } from '../api/semester';
import { getTeachingAssignments } from '../api/teachingAssignment';
import { createScheduleSlot, deleteScheduleSlot, getTimetableSlots, type ScheduleSlotItem } from '../api/schedule';
import {
  createTimetable,
  deleteTimetable,
  getTimetables,
  publishTimetable,
  scheduleTimetable,
  type TimetableItem,
  type TimetableStatus,
} from '../api/timetable';

interface ClassItem { id: number; name: string; gradeLevel: number; }
interface SemesterItem { id: number; startDate: string; endDate: string; }
interface AssignmentItem { id: number; subjectName: string; teacherName: string; }

const DAYS = [
  { value: 2, label: 'Thứ 2', short: 'T2' },
  { value: 3, label: 'Thứ 3', short: 'T3' },
  { value: 4, label: 'Thứ 4', short: 'T4' },
  { value: 5, label: 'Thứ 5', short: 'T5' },
  { value: 6, label: 'Thứ 6', short: 'T6' },
  { value: 7, label: 'Thứ 7', short: 'T7' },
  { value: 1, label: 'Chủ nhật', short: 'CN' },
];
const PERIODS = Array.from({ length: 10 }, (_, index) => index + 1);

export default function TimetablesPage({ selectedYearId, selectedSemesterId }: { selectedYearId?: string; selectedSemesterId?: string }) {
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [semester, setSemester] = useState<SemesterItem | null>(null);
  const [grade, setGrade] = useState('10');
  const [classId, setClassId] = useState('');
  const [versions, setVersions] = useState<TimetableItem[]>([]);
  const [assignments, setAssignments] = useState<AssignmentItem[]>([]);
  const [slots, setSlots] = useState<ScheduleSlotItem[]>([]);
  const [draftId, setDraftId] = useState<number | null>(null);
  const [publishDate, setPublishDate] = useState('');
  const [slotForm, setSlotForm] = useState({ assignmentId: '', dayOfWeek: 2, period: 1 });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  useEffect(() => {
    setClassId(''); setVersions([]); setAssignments([]); setSlots([]); setDraftId(null); setError('');
    if (!selectedYearId) { setClasses([]); return; }
    Promise.all([getClasses({ academicYearId: selectedYearId, page: 0, size: 500 }) as any, getSemesters(selectedYearId) as any])
      .then(([page, semesterList]) => {
        setClasses(page.content || []);
        const selected = (semesterList || []).find((item: SemesterItem) => String(item.id) === selectedSemesterId) || null;
        setSemester(selected);
        setPublishDate(selected ? firstScheduleDate(selected) : '');
      }).catch((cause: any) => setError(cause.message || 'Không thể tải dữ liệu thời khóa biểu.'));
  }, [selectedYearId, selectedSemesterId]);

  async function loadClassData(targetClassId = classId) {
    if (!targetClassId || !selectedSemesterId) return;
    setError('');
    try {
      const [versionList, assignmentList] = await Promise.all([
        getTimetables(targetClassId, selectedSemesterId),
        getTeachingAssignments({ classId: targetClassId }) as Promise<AssignmentItem[]>,
      ]);
      setVersions(versionList || []);
      setAssignments(assignmentList || []);
      const draft = (versionList || []).find(item => item.status === 'DRAFT');
      setDraftId(draft?.id || null);
      setSlots(draft ? await getTimetableSlots(draft.id) : []);
    } catch (cause: any) {
      setError(cause.message || 'Không thể tải thời khóa biểu của lớp.');
    }
  }

  useEffect(() => { loadClassData(); }, [classId, selectedSemesterId]);

  const filteredClasses = useMemo(() => classes.filter(item => String(item.gradeLevel) === grade), [classes, grade]);
  const draft = versions.find(item => item.id === draftId);
  const active = versions.find(item => item.status === 'ACTIVE');
  const scheduled = versions.find(item => item.status === 'SCHEDULED');
  const selectedAssignment = assignments.find(item => item.id === Number(slotForm.assignmentId));
  const slotsByCell = useMemo(() => new Map(slots.map(slot => [`${slot.dayOfWeek}-${slot.period}`, slot])), [slots]);

  async function createDraft() {
    if (!classId || !selectedSemesterId) return;
    setLoading(true); setError(''); setMessage('');
    try {
      await createTimetable({
        classId: Number(classId), semesterId: Number(selectedSemesterId),
        copyFromTimetableId: active?.id,
      });
      setMessage(active ? 'Đã tạo bản nháp từ thời khóa biểu đang dùng.' : 'Đã tạo bản nháp mới.');
      await loadClassData();
    } catch (cause: any) { setError(cause.message || 'Không thể tạo bản nháp.'); }
    finally { setLoading(false); }
  }

  async function addSlot() {
    if (!draftId || !slotForm.assignmentId) return;
    setLoading(true); setError(''); setMessage('');
    try {
      await createScheduleSlot({
        timetableId: draftId,
        assignmentId: Number(slotForm.assignmentId),
        dayOfWeek: slotForm.dayOfWeek,
        period: slotForm.period,
        shift: slotForm.period <= 5 ? 'MORNING' : 'AFTERNOON',
      });
      setSlots(await getTimetableSlots(draftId));
      setSlotForm(current => ({ ...current, assignmentId: '' }));
    } catch (cause: any) { setError(cause.message || 'Không thể thêm tiết học.'); }
    finally { setLoading(false); }
  }

  async function removeSlot(id: number) {
    if (!draftId) return;
    setLoading(true); setError('');
    try { await deleteScheduleSlot(id); setSlots(await getTimetableSlots(draftId)); }
    catch (cause: any) { setError(cause.message || 'Không thể xóa tiết học.'); }
    finally { setLoading(false); }
  }

  async function publishNow() {
    if (!draftId || !semester || !confirm('Phát hành thời khóa biểu ngay và gửi thông báo đến phụ huynh, học sinh, giáo viên?')) return;
    setLoading(true); setError(''); setMessage('');
    try {
      await publishTimetable(draftId, effectiveDateNow(semester));
      setMessage('Đã phát hành và gửi thông báo realtime đến các tài khoản liên quan.');
      await loadClassData();
    } catch (cause: any) { setError(cause.message || 'Không thể phát hành thời khóa biểu.'); }
    finally { setLoading(false); }
  }

  async function schedulePublish() {
    if (!draftId || !publishDate || !confirm(`Hẹn phát hành vào ngày ${formatDate(publishDate)}?`)) return;
    setLoading(true); setError(''); setMessage('');
    try {
      await scheduleTimetable(draftId, publishDate);
      setMessage(`Đã hẹn phát hành ngày ${formatDate(publishDate)}. Cronjob sẽ tự phát hành và gửi thông báo.`);
      await loadClassData();
    } catch (cause: any) { setError(cause.message || 'Không thể hẹn ngày phát hành.'); }
    finally { setLoading(false); }
  }

  async function removeDraft() {
    if (!draftId || !confirm('Xóa bản nháp này?')) return;
    setLoading(true);
    try { await deleteTimetable(draftId); await loadClassData(); }
    catch (cause: any) { setError(cause.message || 'Không thể xóa bản nháp.'); }
    finally { setLoading(false); }
  }

  return <div className="page-stack timetable-page">
    <div className="page-heading timetable-heading">
      <div><span className="eyebrow">Lập lịch giảng dạy</span><h1>Thời khóa biểu</h1><p>Chọn lớp, bấm trực tiếp vào ô Thứ × Tiết và phân công môn học.</p></div>
      {classId && <div className="timetable-summary"><strong>{slots.length}</strong><span>tiết trong bản nháp</span></div>}
    </div>

    {(!selectedYearId || !selectedSemesterId) && <div className="notice warning">Chọn năm học và học kỳ ở thanh phía trên.</div>}
    {error && <div className="notice error">{error}</div>}
    {message && <div className="notice success">{message}</div>}

    <section className="timetable-filter-card">
      <div className="form-group"><label>Khối</label><select value={grade} onChange={event => { setGrade(event.target.value); setClassId(''); }}>{Array.from({ length: 12 }, (_, index) => index + 1).map(value => <option key={value}>{value}</option>)}</select></div>
      <div className="form-group timetable-class-select"><label>Lớp học</label><select value={classId} onChange={event => setClassId(event.target.value)}><option value="">Chọn lớp để lập lịch</option>{filteredClasses.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div>
      <div className="timetable-filter-action">
        <button type="button" disabled={!classId || !!draft || !!scheduled || loading} onClick={createDraft}>{active ? 'Tạo lịch thay thế' : 'Tạo thời khóa biểu'}</button>
      </div>
    </section>

    {scheduled && <div className="timetable-scheduled-banner"><span className="scheduled-icon">◷</span><div><strong>Đang chờ phát hành</strong><p>Phiên bản {scheduled.version} sẽ được cronjob phát hành ngày {formatDate(scheduled.effectiveFrom || '')}.</p></div></div>}

    {draft && <>
      <section className="timetable-editor-card">
        <div className="timetable-editor-heading">
          <div><span className="step-number">1</span><div><h2>Chọn môn học và vị trí</h2><p>Bấm ô trống trong bảng hoặc chọn Thứ/Tiết bên dưới.</p></div></div>
          <span className="status-badge preparing">Bản nháp · v{draft.version}</span>
        </div>
        <div className="timetable-slot-form">
          <div className="form-group assignment-field"><label>Môn học · Giáo viên</label><select value={slotForm.assignmentId} onChange={event => setSlotForm(current => ({ ...current, assignmentId: event.target.value }))}><option value="">Chọn phân công giảng dạy</option>{assignments.map(item => <option key={item.id} value={item.id}>{item.subjectName} · {item.teacherName}</option>)}</select></div>
          <div className="day-selector"><label>Thứ</label><div>{DAYS.map(day => <button type="button" key={day.value} className={slotForm.dayOfWeek === day.value ? 'selected' : ''} onClick={() => setSlotForm(current => ({ ...current, dayOfWeek: day.value }))}>{day.short}</button>)}</div></div>
          <div className="period-selector"><label>Tiết</label><div>{PERIODS.map(period => <button type="button" key={period} className={slotForm.period === period ? 'selected' : ''} onClick={() => setSlotForm(current => ({ ...current, period }))}>{period}</button>)}</div></div>
          <button type="button" className="add-slot-button" disabled={!selectedAssignment || loading || slotsByCell.has(`${slotForm.dayOfWeek}-${slotForm.period}`)} onClick={addSlot}>+ Thêm vào {DAYS.find(day => day.value === slotForm.dayOfWeek)?.label}, tiết {slotForm.period}</button>
        </div>
      </section>

      <section className="timetable-board-card">
        <div className="timetable-editor-heading"><div><span className="step-number">2</span><div><h2>Thời khóa biểu theo thứ</h2><p>Chọn ô trống để thêm nhanh; chọn × để xóa tiết.</p></div></div></div>
        <div className="timetable-board-wrap"><table className="timetable-board"><thead><tr><th className="period-heading">Tiết</th>{DAYS.map(day => <th key={day.value}>{day.label}</th>)}</tr></thead><tbody>{PERIODS.map(period => <tr key={period} className={period === 6 ? 'afternoon-start' : ''}><th><span>{period}</span><small>{period <= 5 ? 'Sáng' : 'Chiều'}</small></th>{DAYS.map(day => { const slot = slotsByCell.get(`${day.value}-${period}`); return <td key={day.value}>{slot ? <div className="schedule-cell filled"><strong>{slot.subjectName}</strong><span>{slot.teacherName}</span><small>{slot.room}</small><button type="button" title="Xóa tiết" disabled={loading} onClick={() => removeSlot(slot.id)}>×</button></div> : <button type="button" className={`schedule-cell empty ${slotForm.dayOfWeek === day.value && slotForm.period === period ? 'selected' : ''}`} onClick={() => setSlotForm(current => ({ ...current, dayOfWeek: day.value, period }))}><span>+</span><small>Thêm tiết</small></button>}</td>; })}</tr>)}</tbody></table></div>
      </section>

      <section className="timetable-publish-card">
        <div className="timetable-editor-heading"><div><span className="step-number">3</span><div><h2>Phát hành thời khóa biểu</h2><p>Thông báo sẽ gửi cho học sinh, phụ huynh và giáo viên qua WebSocket.</p></div></div></div>
        <div className="publish-options">
          <div className="publish-option immediate"><div><strong>Phát hành ngay</strong><p>Kích hoạt lịch và gửi thông báo ngay lập tức.</p></div><button type="button" disabled={loading || slots.length === 0} onClick={publishNow}>Phát hành ngay</button></div>
          <div className="publish-divider"><span>hoặc</span></div>
          <div className="publish-option scheduled"><div><strong>Hẹn ngày phát hành</strong><p>Spring Scheduler tự phát hành lúc 00:00 ngày đã chọn.</p></div><div className="schedule-publish-controls"><input type="date" min={semester ? firstScheduleDate(semester) : undefined} max={semester?.endDate} value={publishDate} onChange={event => setPublishDate(event.target.value)} /><button type="button" className="secondary" disabled={loading || slots.length === 0 || !publishDate} onClick={schedulePublish}>Hẹn phát hành</button></div></div>
        </div>
        <div className="draft-danger-zone"><button type="button" className="danger" disabled={loading} onClick={removeDraft}>Xóa bản nháp</button></div>
      </section>
    </>}

    {classId && <section className="timetable-history"><div className="class-list-heading"><div><h2>Lịch sử phiên bản</h2><p>Theo dõi lịch đang dùng, chờ phát hành và đã lưu trữ.</p></div></div><div className="table-responsive"><table><thead><tr><th>Phiên bản</th><th>Trạng thái</th><th>Ngày áp dụng/phát hành</th><th>Số tiết</th></tr></thead><tbody>{versions.map(item => <tr key={item.id}><td><strong>Phiên bản {item.version}</strong></td><td><span className={`badge-status ${statusClass(item.status)}`}>{statusLabel(item.status)}</span></td><td>{item.effectiveFrom ? formatDate(item.effectiveFrom) : '—'}{item.effectiveTo ? ` → ${formatDate(item.effectiveTo)}` : ''}</td><td>{item.slotCount}</td></tr>)}{versions.length === 0 && <tr><td colSpan={4}>Chưa có thời khóa biểu cho lớp và học kỳ này.</td></tr>}</tbody></table></div></section>}
    {!classId && <div className="timetable-empty-state"><span>▦</span><h2>Chọn lớp để bắt đầu</h2><p>Bạn sẽ lập lịch trực tiếp theo từng Thứ và Tiết.</p></div>}
  </div>;
}

function isoToday() { return new Date().toISOString().slice(0, 10); }
function isoTomorrow() { const date = new Date(); date.setDate(date.getDate() + 1); return date.toISOString().slice(0, 10); }
function firstScheduleDate(semester: SemesterItem) { return isoTomorrow() > semester.startDate ? isoTomorrow() : semester.startDate; }
function effectiveDateNow(semester: SemesterItem) { const today = isoToday(); return today < semester.startDate ? semester.startDate : today; }
function formatDate(value: string) { if (!value) return '—'; const [year, month, day] = value.split('-'); return `${day}/${month}/${year}`; }
function statusLabel(status: TimetableStatus) { return ({ DRAFT: 'Bản nháp', SCHEDULED: 'Chờ phát hành', ACTIVE: 'Đang áp dụng', ARCHIVED: 'Đã lưu trữ' })[status]; }
function statusClass(status: TimetableStatus) { return ({ DRAFT: 'preparing', SCHEDULED: 'scheduled', ACTIVE: 'active', ARCHIVED: 'completed' })[status]; }
