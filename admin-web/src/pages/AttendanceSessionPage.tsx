import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

interface AcademicYearItem { id: number; name: string; status: string; }
interface ClassItem { id: number; name: string; academicYearId: number; academicYearName: string; }
interface TeacherItem { id: number; name: string; teacherProfile?: { id: number; employeeCode: string; }; }

interface AttendanceDetailItem {
  id: number; sessionId: number;
  studentId: number; studentName: string; studentCode: string;
  status: string; note: string;
}
interface AttendanceSessionItem {
  id: number;
  classId: number; className: string;
  teacherId: number; teacherName: string;
  date: string; shift: string;
  scheduleId: number | null;
  total: number; present: number; late: number; absent: number;
  isClosed: boolean;
  details: AttendanceDetailItem[];
}

const today = () => new Date().toISOString().slice(0, 10);

const STATUS_OPTIONS = [
  { value: 'PRESENT', label: 'Có mặt' },
  { value: 'LATE', label: 'Muộn' },
  { value: 'ABSENT_WITH_LEAVE', label: 'Vắng (có phép)' },
  { value: 'ABSENT_WITHOUT_LEAVE', label: 'Vắng (không phép)' },
];

export default function AttendanceSessionPage() {
  const [academicYears, setAcademicYears] = useState<AcademicYearItem[]>([]);
  const [academicYearId, setAcademicYearId] = useState('');
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [teachers, setTeachers] = useState<TeacherItem[]>([]);

  const [classId, setClassId] = useState('');
  const [date, setDate] = useState(today());
  const [shift, setShift] = useState('MORNING');
  const [teacherId, setTeacherId] = useState('');

  const [session, setSession] = useState<AttendanceSessionItem | null>(null);
  const [details, setDetails] = useState<AttendanceDetailItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  useEffect(() => {
    fetchAcademicYears();
    apiFetch('/admin/users?role=TEACHER&page=0&size=100')
      .then((d: any) => setTeachers(d.content || d || []))
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (!academicYearId) return;
    setClassId('');
    setSession(null);
    setDetails([]);
    apiFetch(`/classes?academicYearId=${academicYearId}&page=0&size=100`)
      .then((d: any) => setClasses(d.content || []))
      .catch(() => {});
  }, [academicYearId]);

  async function fetchAcademicYears() {
    const data = await apiFetch('/academic-years') as AcademicYearItem[];
    setAcademicYears(data);
    const active = data.find(y => y.status === 'ACTIVE') || data[0];
    if (active) setAcademicYearId(String(active.id));
  }

  async function loadSession() {
    if (!classId || !date || !shift) {
      setError('Chọn lớp, ngày, buổi trước.');
      return;
    }
    setError('');
    setSuccessMsg('');
    setLoading(true);
    try {
      const data = await apiFetch(`/attendance-sessions?classId=${classId}&date=${date}&shift=${shift}`) as AttendanceSessionItem[];
      if (data.length > 0) {
        const s = data[0];
        setSession(s);
        setDetails(s.details || []);
      } else {
        setSession(null);
        setDetails([]);
      }
    } catch (err: any) {
      setError(err.message || 'Lỗi tải dữ liệu');
    } finally {
      setLoading(false);
    }
  }

  async function createSession() {
    if (!classId || !date || !shift || !teacherId) {
      setError('Chọn đủ lớp, ngày, buổi, giáo viên.');
      return;
    }
    setError('');
    setSuccessMsg('');
    setLoading(true);
    try {
      const newSession = await apiFetch('/attendance-sessions', {
        method: 'POST',
        body: JSON.stringify({ classId: +classId, teacherId: +teacherId, date, shift }),
      }) as AttendanceSessionItem;
      setSession(newSession);
      setDetails(newSession.details || []);
      setSuccessMsg(`Đã tạo buổi điểm danh cho ${newSession.className}`);
    } catch (err: any) {
      setError(err.message || 'Lỗi tạo buổi điểm danh');
    } finally {
      setLoading(false);
    }
  }

  function updateDetailStatus(studentId: number, newStatus: string) {
    setDetails(prev => prev.map(d => d.studentId === studentId ? { ...d, status: newStatus } : d));
  }

  async function saveDetails() {
    if (!session) return;
    setError('');
    setSuccessMsg('');
    setLoading(true);
    try {
      const entries = details.map(d => ({
        studentId: d.studentId,
        status: d.status,
        note: d.note || '',
      }));
      const updated = await apiFetch(`/attendance-sessions/${session.id}/details`, {
        method: 'PUT',
        body: JSON.stringify({ sessionId: session.id, entries }),
      }) as AttendanceDetailItem[];
      setDetails(updated);
      // Reload session to get updated counts
      loadSession();
      setSuccessMsg('Đã lưu điểm danh.');
    } catch (err: any) {
      setError(err.message || 'Lỗi lưu điểm danh');
    } finally {
      setLoading(false);
    }
  }

  async function closeSession() {
    if (!session) return;
    if (!confirm('Kết thúc buổi điểm danh? Sau đó không thể chỉnh sửa.')) return;
    setError('');
    setSuccessMsg('');
    setLoading(true);
    try {
      const closed = await apiFetch(`/attendance-sessions/${session.id}/close`, {
        method: 'POST',
      }) as AttendanceSessionItem;
      setSession(closed);
      setSuccessMsg('Đã kết thúc buổi điểm danh.');
    } catch (err: any) {
      setError(err.message || 'Lỗi kết thúc buổi điểm danh');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <h2>Điểm danh theo buổi</h2>
      {error && <div className="error">{error}</div>}
      {successMsg && <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: '16px' }}>[SUCCESS] {successMsg}</div>}

      {/* Filters */}
      <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))' }}>
        <div className="form-group">
          <label>Năm học</label>
          <select value={academicYearId} onChange={e => setAcademicYearId(e.target.value)}>
            <option value="">Chọn năm học</option>
            {academicYears.map(y => <option key={y.id} value={y.id}>{y.name} {y.status === 'ACTIVE' ? '(Đang mở)' : ''}</option>)}
          </select>
        </div>

        <div className="form-group">
          <label>Lớp học</label>
          <select value={classId} onChange={e => setClassId(e.target.value)} disabled={!academicYearId}>
            <option value="">Chọn lớp</option>
            {classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>

        <div className="form-group">
          <label>Ngày</label>
          <input type="date" value={date} onChange={e => setDate(e.target.value)} />
        </div>

        <div className="form-group">
          <label>Buổi</label>
          <select value={shift} onChange={e => setShift(e.target.value)}>
            <option value="MORNING">Sáng</option>
            <option value="AFTERNOON">Chiều</option>
          </select>
        </div>

        <div className="form-group">
          <label>Giáo viên</label>
          <select value={teacherId} onChange={e => setTeacherId(e.target.value)}>
            <option value="">Chọn giáo viên</option>
            {teachers.map(t => t.teacherProfile ? (
              <option key={t.teacherProfile.id} value={t.teacherProfile.id}>{t.name} ({t.teacherProfile.employeeCode})</option>
            ) : null)}
          </select>
        </div>

        <div className="form-group">
          <label style={{ visibility: 'hidden' }}>Thao tác</label>
          <button onClick={loadSession} disabled={loading || !classId} style={{ height: '38px' }}>
            {loading ? 'Đang tải...' : 'Tìm kiếm'}
          </button>
        </div>
      </div>

      {/* No session → create button */}
      {!session && !loading && classId && (
        <div style={{ marginTop: '24px', padding: '16px', background: '#fafafa', borderLeft: '3px solid #000' }}>
          <p style={{ fontSize: '13px', marginBottom: '12px' }}>Chưa có buổi điểm danh cho lớp/ngày/buổi này.</p>
          <button onClick={createSession} disabled={loading || !teacherId}>
            {loading ? 'Đang tạo...' : 'Bắt đầu điểm danh'}
          </button>
        </div>
      )}

      {/* Session details table */}
      {session && (
        <div style={{ marginTop: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 style={{ fontSize: '15px', fontWeight: 700, textTransform: 'uppercase', borderBottom: '1px dashed #000', paddingBottom: '8px', margin: 0 }}>
              {session.className} — {session.date} — {session.shift === 'MORNING' ? 'Sáng' : 'Chiều'}
            </h3>
            <div style={{ display: 'flex', gap: '8px' }}>
              <span style={{
                display: 'inline-block', padding: '4px 12px', fontSize: '12px', fontWeight: 700,
                background: session.isClosed ? '#737373' : '#16a34a', color: '#fff',
              }}>
                {session.isClosed ? 'ĐÃ KẾT THÚC' : 'ĐANG MỞ'}
              </span>
            </div>
          </div>

          {/* Stats */}
          <div style={{ display: 'flex', gap: '16px', marginBottom: '16px', fontSize: '13px', fontFamily: 'ui-monospace, monospace' }}>
            <span>Tổng: {session.total}</span>
            <span>Có mặt: {session.present}</span>
            <span>Muộn: {session.late}</span>
            <span>Vắng: {session.absent}</span>
            <span>GV: {session.teacherName}</span>
          </div>

          {/* Student table */}
          <table>
            <thead>
              <tr>
                <th>#</th>
                <th>Mã HS</th>
                <th>Họ tên</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {details.map((d, i) => (
                <tr key={d.studentId}>
                  <td>{i + 1}</td>
                  <td>{d.studentCode}</td>
                  <td style={{ fontWeight: 600 }}>{d.studentName}</td>
                  <td>
                    {session.isClosed ? (
                      <span style={{ fontSize: '12px' }}>{STATUS_OPTIONS.find(o => o.value === d.status)?.label || d.status}</span>
                    ) : (
                      <select
                        value={d.status}
                        onChange={e => updateDetailStatus(d.studentId, e.target.value)}
                        style={{ fontSize: '12px', padding: '4px 8px' }}
                      >
                        {STATUS_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                      </select>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Action buttons */}
          {!session.isClosed && (
            <div style={{ marginTop: '16px', display: 'flex', gap: '12px' }}>
              <button onClick={saveDetails} disabled={loading}>
                {loading ? 'Đang lưu...' : 'Lưu điểm danh'}
              </button>
              <button onClick={closeSession} disabled={loading} style={{ border: '1px solid #737373' }}>
                Kết thúc buổi
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
