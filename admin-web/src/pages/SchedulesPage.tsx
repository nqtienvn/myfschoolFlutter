import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

interface AcademicYearItem { id: number; name: string; status: string; }
interface ClassItem { id: number; name: string; academicYearId: number; academicYearName: string; }
interface SemesterItem { id: number; name: string; academicYearId: number; academicYearName: string; isCurrent: boolean; order: number; }
interface TeachingAssignmentItem { id: number; subjectName: string; subjectCode: string; teacherName: string; teacherCode: string; }

export default function SchedulesPage() {
  const [academicYears, setAcademicYears] = useState<AcademicYearItem[]>([]);
  const [academicYearId, setAcademicYearId] = useState('');
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [semesters, setSemesters] = useState<SemesterItem[]>([]);
  const [assignments, setAssignments] = useState<TeachingAssignmentItem[]>([]);
  const [items, setItems] = useState<any[]>([]);
  const [loadingTkb, setLoadingTkb] = useState(false);
  const [classId, setClassId] = useState('');
  const [semesterId, setSemesterId] = useState('');
  const [assignmentId, setAssignmentId] = useState('');
  const [dayOfWeek, setDayOfWeek] = useState(2);
  const [period, setPeriod] = useState(1);
  const [room, setRoom] = useState('');
  const [shift, setShift] = useState('MORNING');
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const periodOptions = shift === 'MORNING' ? [1, 2, 3, 4, 5] : [6, 7, 8, 9, 10];

  useEffect(() => { fetchAcademicYears(); }, []);
  useEffect(() => {
    if (!academicYearId) return;
    setClassId('');
    setSemesterId('');
    setAssignments([]);
    apiFetch(`/classes?academicYearId=${academicYearId}&page=0&size=100`).then((d: any) => setClasses(d.content || [])).catch(() => {});
    apiFetch(`/semesters?academicYearId=${academicYearId}`).then((d: any) => {
      const list = d || [];
      setSemesters(list);
      const current = list.find((s: SemesterItem) => s.isCurrent) || list[0];
      if (current) setSemesterId(String(current.id));
    }).catch(() => {});
  }, [academicYearId]);

  useEffect(() => {
    if (!classId || !semesterId) {
      setItems([]);
      setAssignments([]);
      return;
    }
    fetchItems();
    fetchAssignments();
  }, [classId, semesterId]);

  async function fetchAcademicYears() {
    const data = await apiFetch('/academic-years') as AcademicYearItem[];
    setAcademicYears(data);
    const active = data.find(y => y.status === 'ACTIVE') || data[0];
    if (active) setAcademicYearId(String(active.id));
  }

  async function fetchItems() {
    setLoadingTkb(true);
    try {
      const data = await apiFetch(`/schedules/class?classId=${classId}&semesterId=${semesterId}`) as any;
      const allDays = data.days || [];
      const slots = allDays.flatMap((d: any) => [
        ...(d.morningSlots || []).map((s: any) => ({ ...s, shiftLabel: 'Sáng', dayName: d.dayOfWeekName })),
        ...(d.afternoonSlots || []).map((s: any) => ({ ...s, shiftLabel: 'Chiều', dayName: d.dayOfWeekName }))
      ]);
      slots.sort((a: any, b: any) => a.dayOfWeek !== b.dayOfWeek ? a.dayOfWeek - b.dayOfWeek : a.period - b.period);
      setItems(slots);
    } catch (err: any) {
      setError(err.message || 'Không thể tải thời khóa biểu');
    } finally {
      setLoadingTkb(false);
    }
  }

  async function fetchAssignments() {
    try {
      const data = await apiFetch(`/teaching-assignments?classId=${classId}&semesterId=${semesterId}`) as TeachingAssignmentItem[];
      setAssignments(data || []);
    } catch (err: any) {
      console.error('Không tải được DS giáo viên phân công: ', err.message);
    }
  }

  async function createItem() {
    setError('');
    setSuccessMsg('');
    if (!classId || !semesterId) return setError('Vui lòng chọn Lớp học và Học kỳ.');
    if (!assignmentId) return setError('Vui lòng chọn phân công giảng dạy.');
    if (!room.trim()) return setError('Vui lòng nhập Phòng học.');

    try {
      await apiFetch('/schedules', {
        method: 'POST',
        body: JSON.stringify({ assignmentId: +assignmentId, dayOfWeek, period, room: room.trim(), shift }),
      });
      setSuccessMsg('Đã thêm lịch học vào thời khóa biểu thành công!');
      setRoom('');
      setAssignmentId('');
      fetchItems();
    } catch (err: any) {
      setError(err.message || 'Lỗi thêm lịch thời khóa biểu');
    }
  }

  async function deleteItem(id: number) {
    if (!confirm('Bạn có muốn xóa tiết học này khỏi thời khóa biểu?')) return;
    setError('');
    setSuccessMsg('');
    try {
      await apiFetch(`/schedules/${id}`, { method: 'DELETE' });
      setSuccessMsg('Đã xóa tiết học.');
      fetchItems();
    } catch (err: any) {
      setError(err.message || 'Lỗi khi xóa tiết học');
    }
  }

  return (
    <div>
      <h2>Thời khóa biểu</h2>
      {error && <div className="error">{error}</div>}
      {successMsg && <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: '16px' }}>[SUCCESS] {successMsg}</div>}

      <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))' }}>
        <div className="form-group">
          <label>Năm học</label>
          <select value={academicYearId} onChange={e => setAcademicYearId(e.target.value)}>
            <option value="">Chọn năm học</option>
            {academicYears.map(y => <option key={y.id} value={y.id}>{y.name} {y.status === 'ACTIVE' ? '(Đang mở)' : ''}</option>)}
          </select>
          <span className="input-desc">Năm học áp dụng</span>
        </div>
        <div className="form-group"><label>Lớp học</label><select value={classId} onChange={e => setClassId(e.target.value)} disabled={!academicYearId}><option value="">Chọn lớp học</option>{classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select><span className="input-desc">Chọn lớp học để quản lý</span></div>
        <div className="form-group"><label>Học kỳ</label><select value={semesterId} onChange={e => setSemesterId(e.target.value)} disabled={!academicYearId}><option value="">Chọn học kỳ</option>{semesters.map(s => <option key={s.id} value={s.id}>{s.name} - {s.academicYearName} {s.isCurrent ? '(Hiện tại)' : ''}</option>)}</select><span className="input-desc">Xem lịch học của kỳ nào</span></div>
      </div>

      {classId && semesterId && (
        <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', marginTop: '16px', background: '#fbfbfb' }}>
          <div style={{ gridColumn: 'span 4', fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', marginBottom: '8px' }}>[ THÊM LỊCH HỌC MỚI CHO LỚP ]</div>
          <div className="form-group"><label>Môn & Giáo viên</label><select value={assignmentId} onChange={e => setAssignmentId(e.target.value)}><option value="">Chọn phân công giảng dạy</option>{assignments.map(t => <option key={t.id} value={t.id}>{t.subjectName} - {t.teacherName} ({t.teacherCode})</option>)}</select><span className="input-desc">Chỉ hiển thị giáo viên đã gán dạy lớp</span></div>
          <div className="form-group"><label>Thứ</label><select value={dayOfWeek} onChange={e => setDayOfWeek(+e.target.value)}><option value="2">Thứ Hai</option><option value="3">Thứ Ba</option><option value="4">Thứ Tư</option><option value="5">Thứ Năm</option><option value="6">Thứ Sáu</option><option value="7">Thứ Bảy</option></select><span className="input-desc">Ngày diễn ra tiết học</span></div>
          <div className="form-group"><label>Ca học</label><select value={shift} onChange={e => { const next = e.target.value; setShift(next); setPeriod(next === 'MORNING' ? 1 : 6); }}><option value="MORNING">Buổi Sáng</option><option value="AFTERNOON">Buổi Chiều</option></select><span className="input-desc">Ca sáng hoặc ca chiều</span></div>
          <div className="form-group"><label>Tiết học</label><select value={period} onChange={e => setPeriod(+e.target.value)}>{periodOptions.map(p => <option key={p} value={p}>Tiết {p}</option>)}</select><span className="input-desc">Số tiết trong buổi</span></div>
          <div className="form-group"><label>Phòng học</label><input placeholder="VD: 101, A2-402..." value={room} onChange={e => setRoom(e.target.value)} /><span className="input-desc">Vị trí phòng học diễn ra</span></div>
          <div className="form-group" style={{ justifyContent: 'center' }}><label style={{ visibility: 'hidden' }}>Thao tác</label><button onClick={createItem} style={{ height: '38px', width: '100%' }}>Thêm lịch</button><span className="input-desc" style={{ visibility: 'hidden' }}>&nbsp;</span></div>
        </div>
      )}

      {classId && semesterId && (
        <div style={{ marginTop: '32px' }}>
          <h3 style={{ fontSize: '15px', fontWeight: 700, textTransform: 'uppercase', marginBottom: '16px', borderBottom: '1px dashed #000000', paddingBottom: '8px' }}>Thời khóa biểu lớp: {classes.find(c => c.id === +classId)?.name}</h3>
          {loadingTkb ? <p style={{ fontSize: '13px', fontFamily: 'ui-monospace, monospace' }}>Đang tải dữ liệu thời khóa biểu...</p> : items.length === 0 ? <p style={{ fontSize: '13px', color: '#a3a3a3', fontStyle: 'italic' }}>Lớp học này chưa được xếp lịch học cho học kỳ đã chọn.</p> : (
            <table><thead><tr><th>Thứ</th><th>Buổi</th><th>Tiết</th><th>Môn học</th><th>Giáo viên</th><th>Phòng học</th><th>Thao tác</th></tr></thead><tbody>{items.map((s: any) => <tr key={s.id}><td style={{ fontWeight: 700 }}>{s.dayOfWeekName || `Thứ ${s.dayOfWeek}`}</td><td>{s.shiftLabel || s.shift}</td><td style={{ fontFamily: 'ui-monospace, monospace' }}>Tiết {s.period}</td><td style={{ fontWeight: 600 }}>{s.subjectName}</td><td>{s.teacherName}</td><td>{s.room}</td><td><button className="danger" onClick={() => deleteItem(s.id)}>Xóa</button></td></tr>)}</tbody></table>
          )}
        </div>
      )}
    </div>
  );
}
