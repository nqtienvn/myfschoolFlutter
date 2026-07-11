import { useEffect, useMemo, useState } from 'react';
import { getClasses } from '../api/class';
import { getSemesters } from '../api/semester';
import { getTeachingAssignments } from '../api/teachingAssignment';
import { getAcademicYearMasterData } from '../api/academicYearConfig';
import { getPeriods, getShifts } from '../api/masterData';
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
interface AssignmentItem {
  id: number;
  subjectId: number;
  subjectName: string;
  teacherId: number;
  teacherName: string;
}
interface ShiftItem { id: number; name: string; code: 'MORNING' | 'AFTERNOON'; order: number; }
interface PeriodItem { id: number; name: string; order: number; shiftId: number; shiftName: string; shiftCode: 'MORNING' | 'AFTERNOON'; shiftOrder: number; }

const DAYS = [
  { value: 2, label: 'Thứ 2' },
  { value: 3, label: 'Thứ 3' },
  { value: 4, label: 'Thứ 4' },
  { value: 5, label: 'Thứ 5' },
  { value: 6, label: 'Thứ 6' },
  { value: 7, label: 'Thứ 7' },
  { value: 1, label: 'Chủ nhật' },
];
export default function TimetablesPage({ selectedYearId, selectedSemesterId }: { selectedYearId?: string; selectedSemesterId?: string }) {
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [semester, setSemester] = useState<SemesterItem | null>(null);
  const [grade, setGrade] = useState('10');
  const [classId, setClassId] = useState('');
  const [versions, setVersions] = useState<TimetableItem[]>([]);
  const [assignments, setAssignments] = useState<AssignmentItem[]>([]);
  const [periods, setPeriods] = useState<PeriodItem[]>([]);
  const [slots, setSlots] = useState<ScheduleSlotItem[]>([]);
  const [draftId, setDraftId] = useState<number | null>(null);
  const [publishDate, setPublishDate] = useState('');
  const [cellSubjects, setCellSubjects] = useState<Record<string, string>>({});
  const [savingCell, setSavingCell] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  useEffect(() => {
    setClassId('');
    setVersions([]);
    setAssignments([]);
    setPeriods([]);
    setSlots([]);
    setDraftId(null);
    setCellSubjects({});
    setError('');
    if (!selectedYearId) { setClasses([]); return; }
    Promise.all([
      getClasses({ academicYearId: selectedYearId, page: 0, size: 500 }) as any,
      getSemesters(selectedYearId) as any,
      getAcademicYearMasterData(selectedYearId),
      getPeriods() as Promise<Omit<PeriodItem, 'shiftCode' | 'shiftOrder'>[]>,
      getShifts() as Promise<ShiftItem[]>,
    ]).then(([page, semesterList, yearConfig, periodCatalog, shiftCatalog]) => {
      setClasses(page.content || []);
      const appliedIds = new Set(yearConfig.periodIds || []);
      const shiftMap = new Map(shiftCatalog.map(shift => [shift.id, shift]));
      setPeriods(periodCatalog
        .filter(period => appliedIds.has(period.id))
        .map(period => ({ ...period, shiftCode: shiftMap.get(period.shiftId)?.code || 'MORNING', shiftOrder: shiftMap.get(period.shiftId)?.order || 0 }))
        .sort((first, second) => first.shiftOrder - second.shiftOrder || first.order - second.order));
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
      const draft = (versionList || []).find(item => item.status === 'DRAFT');
      const displayed = draft
        || (versionList || []).find(item => item.status === 'SCHEDULED')
        || (versionList || []).find(item => item.status === 'ACTIVE');
      setVersions(versionList || []);
      setAssignments((assignmentList || []).filter(item => item.id && item.subjectId && item.teacherId));
      setDraftId(draft?.id || null);
      setSlots(displayed ? await getTimetableSlots(displayed.id) : []);
      setCellSubjects({});
    } catch (cause: any) {
      setError(cause.message || 'Không thể tải thời khóa biểu của lớp.');
    }
  }

  useEffect(() => { loadClassData(); }, [classId, selectedSemesterId]);

  const filteredClasses = useMemo(() => classes.filter(item => String(item.gradeLevel) === grade), [classes, grade]);
  const draft = versions.find(item => item.id === draftId);
  const active = versions.find(item => item.status === 'ACTIVE');
  const scheduled = versions.find(item => item.status === 'SCHEDULED');
  const subjects = useMemo(() => {
    const unique = new Map<number, string>();
    assignments.forEach(item => unique.set(item.subjectId, item.subjectName));
    return Array.from(unique, ([id, name]) => ({ id, name }));
  }, [assignments]);
  const slotsByCell = useMemo(() => new Map(slots.map(slot => [`${slot.dayOfWeek}-${slot.periodId}`, slot])), [slots]);

  async function ensureDraft() {
    if (draftId) return { id: draftId, slots };
    if (!classId || !selectedSemesterId || scheduled) throw new Error('Thời khóa biểu đang chờ phát hành nên chưa thể chỉnh sửa.');
    const created = await createTimetable({
      classId: Number(classId),
      semesterId: Number(selectedSemesterId),
      copyFromTimetableId: active?.id,
    });
    const copiedSlots = await getTimetableSlots(created.id);
    setVersions(current => [...current, created]);
    setDraftId(created.id);
    setSlots(copiedSlots);
    setMessage(active ? 'Đã tự động tạo bản chỉnh sửa từ lịch đang áp dụng.' : 'Đã tự động tạo thời khóa biểu mới.');
    return { id: created.id, slots: copiedSlots };
  }

  async function saveCell(dayOfWeek: number, period: PeriodItem, assignmentId: number) {
    const key = `${dayOfWeek}-${period.id}`;
    setSavingCell(key); setError(''); setMessage('');
    let removed: ScheduleSlotItem | undefined;
    let editableId: number | undefined;
    try {
      const editable = await ensureDraft();
      editableId = editable.id;
      removed = editable.slots.find(slot => slot.dayOfWeek === dayOfWeek && slot.periodId === period.id);
      if (removed) await deleteScheduleSlot(removed.id);
      await createScheduleSlot({
        timetableId: editable.id,
        assignmentId,
        dayOfWeek,
        periodId: period.id,
      });
      setSlots(await getTimetableSlots(editable.id));
      setCellSubjects(current => { const next = { ...current }; delete next[key]; return next; });
    } catch (cause: any) {
      if (removed && editableId) {
        await createScheduleSlot({ timetableId: editableId, assignmentId: removed.assignmentId, dayOfWeek, periodId: removed.periodId }).catch(() => undefined);
        setSlots(await getTimetableSlots(editableId).catch(() => slots));
      }
      setError(cause.message || 'Không thể lưu tiết học.');
    } finally { setSavingCell(''); }
  }

  async function clearCell(dayOfWeek: number, period: PeriodItem) {
    const key = `${dayOfWeek}-${period.id}`;
    setSavingCell(key); setError('');
    try {
      const editable = await ensureDraft();
      const slot = editable.slots.find(item => item.dayOfWeek === dayOfWeek && item.periodId === period.id);
      if (slot) await deleteScheduleSlot(slot.id);
      setSlots(await getTimetableSlots(editable.id));
      setCellSubjects(current => { const next = { ...current }; delete next[key]; return next; });
    } catch (cause: any) { setError(cause.message || 'Không thể xóa tiết học.'); }
    finally { setSavingCell(''); }
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
    if (!draftId || !confirm('Hủy tất cả thay đổi trong bản nháp này?')) return;
    setLoading(true); setError('');
    try { await deleteTimetable(draftId); await loadClassData(); }
    catch (cause: any) { setError(cause.message || 'Không thể hủy bản nháp.'); }
    finally { setLoading(false); }
  }

  return <div className="page-stack timetable-page">
    <div className="page-heading timetable-heading">
      <div><span className="eyebrow">Lập lịch giảng dạy</span><h1>Thời khóa biểu</h1><p>Chọn môn và giáo viên ngay trong từng ô Thứ × Tiết. Hệ thống tự tạo bản nháp khi có thay đổi đầu tiên.</p></div>
      {classId && <div className="timetable-summary"><strong>{slots.length}</strong><span>tiết đang hiển thị</span></div>}
    </div>

    {(!selectedYearId || !selectedSemesterId) && <div className="notice warning">Chọn năm học và học kỳ ở thanh phía trên.</div>}
    {error && <div className="notice error">{error}</div>}
    {message && <div className="notice success">{message}</div>}

    <section className="timetable-filter-card inline-only">
      <div className="form-group"><label>Khối</label><select value={grade} onChange={event => { setGrade(event.target.value); setClassId(''); }}>{Array.from({ length: 12 }, (_, index) => index + 1).map(value => <option key={value}>{value}</option>)}</select></div>
      <div className="form-group timetable-class-select"><label>Lớp học</label><select value={classId} onChange={event => setClassId(event.target.value)}><option value="">Chọn lớp để lập lịch</option>{filteredClasses.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div>
    </section>

    {scheduled && <div className="timetable-scheduled-banner"><span className="scheduled-icon">◷</span><div><strong>Đang chờ phát hành</strong><p>Phiên bản {scheduled.version} sẽ được cronjob phát hành ngày {formatDate(scheduled.effectiveFrom || '')}. Không thể sửa trong thời gian chờ.</p></div></div>}

    {classId && <section className="timetable-board-card">
      <div className="timetable-editor-heading">
        <div><div><h2>Thời khóa biểu theo thứ</h2><p>Chọn môn học, sau đó chọn giáo viên để lưu tiết ngay trong bảng.</p></div></div>
        {draft && <span className="status-badge preparing">Bản nháp · v{draft.version}</span>}
      </div>
      {assignments.length === 0 && <div className="notice warning">Lớp này chưa có phân công môn học và giáo viên. Hãy tạo phân công giảng dạy trước khi xếp tiết.</div>}
      {periods.length === 0 && <div className="notice warning">Năm học này chưa được cấu hình buổi học và tiết học.</div>}
      <div className="timetable-board-wrap"><table className="timetable-board inline-editor"><thead><tr><th className="period-heading">Tiết</th>{DAYS.map(day => <th key={day.value}>{day.label}</th>)}</tr></thead><tbody>{periods.map((period, periodIndex) => <tr key={period.id} className={periodIndex > 0 && periods[periodIndex - 1].shiftId !== period.shiftId ? 'afternoon-start' : ''}><th><span>{period.name}</span><small>{period.shiftName}</small></th>{DAYS.map(day => {
        const key = `${day.value}-${period.id}`;
        const slot = slotsByCell.get(key);
        const currentAssignment = slot ? assignments.find(item => item.id === slot.assignmentId) : undefined;
        const subjectId = cellSubjects[key] ?? (currentAssignment ? String(currentAssignment.subjectId) : '');
        const teacherAssignments = assignments.filter(item => String(item.subjectId) === subjectId);
        const teacherValue = currentAssignment && String(currentAssignment.subjectId) === subjectId ? String(currentAssignment.id) : '';
        const disabled = !!scheduled || savingCell === key || assignments.length === 0;
        return <td key={day.value}><div className={`schedule-cell inline-selects ${slot ? 'filled' : ''} ${savingCell === key ? 'saving' : ''}`}>
          <select aria-label={`Môn học ${day.label} ${period.name}`} disabled={disabled} value={subjectId} onChange={event => setCellSubjects(current => ({ ...current, [key]: event.target.value }))}>
            <option value="">Môn học</option>{subjects.map(subject => <option key={subject.id} value={subject.id}>{subject.name}</option>)}
          </select>
          <select aria-label={`Giáo viên ${day.label} ${period.name}`} disabled={disabled || !subjectId} value={teacherValue} onChange={event => event.target.value && saveCell(day.value, period, Number(event.target.value))}>
            <option value="">Giáo viên</option>{teacherAssignments.map(item => <option key={item.id} value={item.id}>{item.teacherName}</option>)}
          </select>
          {slot && <button type="button" className="clear-cell-button" title="Xóa tiết" disabled={disabled} onClick={() => clearCell(day.value, period)}>×</button>}
          {savingCell === key && <small className="cell-saving-label">Đang lưu…</small>}
        </div></td>;
      })}</tr>)}</tbody></table></div>
    </section>}

    {draft && <section className="timetable-publish-card">
      <div className="timetable-editor-heading"><div><div><h2>Phát hành thời khóa biểu</h2><p>Thông báo sẽ gửi cho học sinh, phụ huynh và giáo viên qua WebSocket.</p></div></div></div>
      <div className="publish-options">
        <div className="publish-option immediate"><div><strong>Phát hành ngay</strong><p>Kích hoạt lịch và gửi thông báo ngay lập tức.</p></div><button type="button" disabled={loading || slots.length === 0} onClick={publishNow}>Phát hành ngay</button></div>
        <div className="publish-divider"><span>hoặc</span></div>
        <div className="publish-option scheduled"><div><strong>Hẹn ngày phát hành</strong><p>Spring Scheduler tự phát hành lúc 00:00 ngày đã chọn.</p></div><div className="schedule-publish-controls"><input type="date" min={semester ? firstScheduleDate(semester) : undefined} max={semester?.endDate} value={publishDate} onChange={event => setPublishDate(event.target.value)} /><button type="button" className="secondary" disabled={loading || slots.length === 0 || !publishDate} onClick={schedulePublish}>Hẹn phát hành</button></div></div>
      </div>
      <div className="draft-danger-zone"><button type="button" className="danger" disabled={loading} onClick={removeDraft}>Hủy các thay đổi</button></div>
    </section>}

    {classId && <section className="timetable-history"><div className="class-list-heading"><div><h2>Lịch sử phiên bản</h2><p>Theo dõi lịch đang dùng, chờ phát hành và đã lưu trữ.</p></div></div><div className="table-responsive"><table><thead><tr><th>Phiên bản</th><th>Trạng thái</th><th>Ngày áp dụng/phát hành</th><th>Số tiết</th></tr></thead><tbody>{versions.map(item => <tr key={item.id}><td><strong>Phiên bản {item.version}</strong></td><td><span className={`badge-status ${statusClass(item.status)}`}>{statusLabel(item.status)}</span></td><td>{item.effectiveFrom ? formatDate(item.effectiveFrom) : '—'}{item.effectiveTo ? ` → ${formatDate(item.effectiveTo)}` : ''}</td><td>{item.slotCount}</td></tr>)}{versions.length === 0 && <tr><td colSpan={4}>Chưa có thời khóa biểu cho lớp và học kỳ này. Chọn môn và giáo viên trong bảng để bắt đầu.</td></tr>}</tbody></table></div></section>}
    {!classId && <div className="timetable-empty-state"><span>▦</span><h2>Chọn lớp để bắt đầu</h2><p>Bạn sẽ chọn môn và giáo viên trực tiếp trong từng ô thời khóa biểu.</p></div>}
  </div>;
}

function isoToday() { return new Date().toISOString().slice(0, 10); }
function isoTomorrow() { const date = new Date(); date.setDate(date.getDate() + 1); return date.toISOString().slice(0, 10); }
function firstScheduleDate(semester: SemesterItem) { return isoTomorrow() > semester.startDate ? isoTomorrow() : semester.startDate; }
function effectiveDateNow(semester: SemesterItem) { const today = isoToday(); return today < semester.startDate ? semester.startDate : today; }
function formatDate(value: string) { if (!value) return '—'; const [year, month, day] = value.split('-'); return `${day}/${month}/${year}`; }
function statusLabel(status: TimetableStatus) { return ({ DRAFT: 'Bản nháp', SCHEDULED: 'Chờ phát hành', ACTIVE: 'Đang áp dụng', ARCHIVED: 'Đã lưu trữ' })[status]; }
function statusClass(status: TimetableStatus) { return ({ DRAFT: 'preparing', SCHEDULED: 'scheduled', ACTIVE: 'active', ARCHIVED: 'completed' })[status]; }
