import { useEffect, useMemo, useState } from 'react';
import { getClasses } from '../api/class';
import { getSemesters } from '../api/semester';
import { getTeachingAssignments } from '../api/teachingAssignment';
import { createScheduleSlot, deleteScheduleSlot, getTimetableSlots, type ScheduleSlotItem } from '../api/schedule';
import { createTimetable, deleteTimetable, getTimetables, publishTimetable, type TimetableItem } from '../api/timetable';

interface ClassItem { id: number; name: string; gradeLevel: number; }
interface SemesterItem { id: number; startDate: string; endDate: string; }
interface AssignmentItem { id: number; subjectName: string; teacherName: string; }

export default function TimetablesPage({ selectedYearId, selectedSemesterId }: { selectedYearId?: string; selectedSemesterId?: string }) {
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [semester, setSemester] = useState<SemesterItem | null>(null);
  const [grade, setGrade] = useState('10');
  const [classId, setClassId] = useState('');
  const [versions, setVersions] = useState<TimetableItem[]>([]);
  const [assignments, setAssignments] = useState<AssignmentItem[]>([]);
  const [slots, setSlots] = useState<ScheduleSlotItem[]>([]);
  const [draftId, setDraftId] = useState<number | null>(null);
  const [effectiveFrom, setEffectiveFrom] = useState('');
  const [slotForm, setSlotForm] = useState({ assignmentId: '', dayOfWeek: '2', period: '1' });
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
        setEffectiveFrom(selected?.startDate || '');
      }).catch((cause: any) => setError(cause.message || 'Không thể tải dữ liệu thời khóa biểu.'));
  }, [selectedYearId, selectedSemesterId]);

  async function loadClassData(targetClassId = classId) {
    if (!targetClassId || !selectedSemesterId) return;
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
    } catch (cause: any) { setError(cause.message || 'Không thể tải thời khóa biểu của lớp.'); }
  }

  useEffect(() => { loadClassData(); }, [classId, selectedSemesterId]);

  const filteredClasses = useMemo(() => classes.filter(item => String(item.gradeLevel) === grade), [classes, grade]);
  const draft = versions.find(item => item.id === draftId);
  const active = versions.find(item => item.status === 'ACTIVE');

  async function createDraft() {
    if (!classId || !selectedSemesterId || !effectiveFrom) return;
    setLoading(true); setError(''); setMessage('');
    try {
      await createTimetable({ classId: Number(classId), semesterId: Number(selectedSemesterId), effectiveFrom,
        copyFromTimetableId: active?.id });
      setMessage(active ? 'Đã sao chép thời khóa biểu hiện tại thành bản nháp mới.' : 'Đã tạo thời khóa biểu nháp.');
      await loadClassData();
    } catch (cause: any) { setError(cause.message || 'Không thể tạo thời khóa biểu nháp.'); }
    finally { setLoading(false); }
  }

  async function addSlot() {
    if (!draftId || !slotForm.assignmentId) return;
    setLoading(true); setError(''); setMessage('');
    try {
      const period = Number(slotForm.period);
      await createScheduleSlot({ timetableId: draftId, assignmentId: Number(slotForm.assignmentId),
        dayOfWeek: Number(slotForm.dayOfWeek), period,
        shift: period <= 5 ? 'MORNING' : 'AFTERNOON' });
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

  async function publish() {
    if (!draftId || !effectiveFrom || !confirm(`Phát hành thời khóa biểu từ ngày ${effectiveFrom}?`)) return;
    setLoading(true); setError(''); setMessage('');
    try {
      await publishTimetable(draftId, effectiveFrom);
      setMessage('Đã phát hành thời khóa biểu mới. Bản cũ được lưu trong lịch sử.');
      await loadClassData();
    } catch (cause: any) { setError(cause.message || 'Không thể phát hành thời khóa biểu.'); }
    finally { setLoading(false); }
  }

  async function removeDraft() {
    if (!draftId || !confirm('Xóa thời khóa biểu nháp này?')) return;
    setLoading(true);
    try { await deleteTimetable(draftId); await loadClassData(); }
    catch (cause: any) { setError(cause.message || 'Không thể xóa bản nháp.'); }
    finally { setLoading(false); }
  }

  return <div className="page-stack">
    <div className="page-heading"><div><span className="eyebrow">Phân hệ thời khóa biểu</span><h1>Thời khóa biểu</h1><p>Có thể tạo bất kỳ lúc nào; không ảnh hưởng điều kiện kích hoạt năm học hoặc học kỳ.</p></div></div>
    {(!selectedYearId || !selectedSemesterId) && <div className="notice warning">Chọn năm học và học kỳ ở thanh phía trên.</div>}
    {error && <div className="notice error">{error}</div>}{message && <div className="notice success">{message}</div>}
    <div className="form-inline">
      <div className="form-group"><label>Khối</label><select value={grade} onChange={event => { setGrade(event.target.value); setClassId(''); }}>{Array.from({ length: 12 }, (_, index) => index + 1).map(value => <option key={value}>{value}</option>)}</select></div>
      <div className="form-group"><label>Lớp</label><select value={classId} onChange={event => setClassId(event.target.value)}><option value="">Chọn lớp</option>{filteredClasses.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div>
      <div className="form-group"><label>Ngày áp dụng bản mới</label><input type="date" min={semester?.startDate} max={semester?.endDate} value={effectiveFrom} onChange={event => setEffectiveFrom(event.target.value)} /></div>
      <button type="button" disabled={!classId || !!draft || loading} onClick={createDraft}>{active ? 'Tạo bản thay thế' : 'Tạo bản nháp'}</button>
    </div>

    {classId && <div className="table-responsive"><table><thead><tr><th>Phiên bản</th><th>Trạng thái</th><th>Hiệu lực</th><th>Số tiết</th></tr></thead><tbody>
      {versions.map(item => <tr key={item.id}><td><strong>Phiên bản {item.version}</strong></td><td><span className={`badge-status ${item.status === 'ACTIVE' ? 'active' : ''}`}>{item.status}</span></td><td>{item.effectiveFrom} → {item.effectiveTo || 'Không giới hạn'}</td><td>{item.slotCount}</td></tr>)}
      {versions.length === 0 && <tr><td colSpan={4}>Chưa có thời khóa biểu cho lớp và học kỳ này.</td></tr>}
    </tbody></table></div>}

    {draft && <section className="page-stack">
      <div className="class-list-heading"><div><h2>Chỉnh sửa phiên bản {draft.version}</h2><p>Mỗi khung thứ/tiết chỉ được chọn một môn học.</p></div><span>{slots.length} tiết</span></div>
      <div className="form-inline">
        <div className="form-group"><label>Môn học · Giáo viên</label><select value={slotForm.assignmentId} onChange={event => setSlotForm(current => ({ ...current, assignmentId: event.target.value }))}><option value="">Chọn phân công</option>{assignments.map(item => <option key={item.id} value={item.id}>{item.subjectName} · {item.teacherName}</option>)}</select></div>
        <div className="form-group"><label>Thứ</label><select value={slotForm.dayOfWeek} onChange={event => setSlotForm(current => ({ ...current, dayOfWeek: event.target.value }))}>{[2,3,4,5,6,7,1].map(day => <option key={day} value={day}>{day === 1 ? 'Chủ nhật' : `Thứ ${day}`}</option>)}</select></div>
        <div className="form-group"><label>Tiết</label><select value={slotForm.period} onChange={event => setSlotForm(current => ({ ...current, period: event.target.value }))}>{Array.from({ length: 10 }, (_, index) => index + 1).map(period => <option key={period}>{period}</option>)}</select></div>
        <button type="button" disabled={!slotForm.assignmentId || loading} onClick={addSlot}>Thêm tiết</button>
      </div>
      <div className="table-responsive"><table><thead><tr><th>Thứ</th><th>Tiết</th><th>Môn học</th><th>Giáo viên</th><th>Phòng</th><th></th></tr></thead><tbody>
        {slots.map(item => <tr key={item.id}><td>{item.dayOfWeekName}</td><td>{item.period}</td><td><strong>{item.subjectName}</strong></td><td>{item.teacherName}</td><td>{item.room || '—'}</td><td><button type="button" className="danger" disabled={loading} onClick={() => removeSlot(item.id)}>Xóa</button></td></tr>)}
        {slots.length === 0 && <tr><td colSpan={6}>Bản nháp chưa có tiết học.</td></tr>}
      </tbody></table></div>
      <div className="page-actions"><button type="button" className="danger" disabled={loading} onClick={removeDraft}>Xóa bản nháp</button><button type="button" disabled={loading || slots.length === 0} onClick={publish}>Phát hành từ {effectiveFrom || 'ngày đã chọn'}</button></div>
    </section>}
    {!classId && <div className="notice">Chọn lớp để quản lý các phiên bản thời khóa biểu.</div>}
  </div>;
}
