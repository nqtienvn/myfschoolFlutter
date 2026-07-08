import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

interface ClassItem { id: number; name: string; academicYear: string; }
interface SemesterItem { id: number; name: string; academicYear: string; isCurrent: boolean; }
interface AssignedTeacherSubject { id: number; subject: { id: number; name: string; code: string; }; teacher: { id: number; name: string; employeeCode: string; } }

export default function SchedulesPage() {
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [semesters, setSemesters] = useState<SemesterItem[]>([]);
  const [assignedTeachers, setAssignedTeachers] = useState<AssignedTeacherSubject[]>([]);

  const [items, setItems] = useState<any[]>([]);
  const [loadingTkb, setLoadingTkb] = useState(false);

  // Form State
  const [classId, setClassId] = useState('');
  const [semesterId, setSemesterId] = useState('');
  const [assignedId, setAssignedId] = useState(''); // Lưu classSubjectId để lấy subjectId & teacherId
  const [dayOfWeek, setDayOfWeek] = useState(2);
  const [period, setPeriod] = useState(1);
  const [room, setRoom] = useState('');
  const [shift, setShift] = useState('MORNING');

  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // 1. Tải dữ liệu ban đầu
  useEffect(() => {
    apiFetch('/classes?page=0&size=100')
      .then((d: any) => setClasses(d.content || []))
      .catch(() => {});

    apiFetch('/semesters')
      .then((d: any) => {
        const list = d || [];
        setSemesters(list);
        const current = list.find((s: SemesterItem) => s.isCurrent);
        if (current) setSemesterId(current.id.toString());
      })
      .catch(() => {});
  }, []);

  // 2. Tự động tải TKB và Giáo viên phân công khi Class / Semester thay đổi
  useEffect(() => {
    if (!classId || !semesterId) {
      setItems([]);
      setAssignedTeachers([]);
      return;
    }
    fetchItems();
    fetchAssignedTeachers();
  }, [classId, semesterId]);

  async function fetchItems() {
    setLoadingTkb(true);
    try {
      const data = await apiFetch(`/schedules/class?classId=${classId}&semesterId=${semesterId}`) as any;
      const allDays = data.days || [];
      const slots = allDays.flatMap((d: any) => [
        ...(d.morningSlots || []).map((s: any) => ({ ...s, shift: 'Sáng', dayName: d.dayOfWeekName })),
        ...(d.afternoonSlots || []).map((s: any) => ({ ...s, shift: 'Chiều', dayName: d.dayOfWeekName }))
      ]);
      // Sắp xếp TKB theo Thứ và Tiết tăng dần
      slots.sort((a: any, b: any) => {
        if (a.dayOfWeek !== b.dayOfWeek) return a.dayOfWeek - b.dayOfWeek;
        if (a.shift !== b.shift) return a.shift === 'Sáng' ? -1 : 1;
        return a.period - b.period;
      });
      setItems(slots);
    } catch (err: any) {
      setError(err.message || 'Không thể tải thời khóa biểu');
    } finally {
      setLoadingTkb(false);
    }
  }

  async function fetchAssignedTeachers() {
    try {
      const data = await apiFetch(`/classes/${classId}`) as any;
      setAssignedTeachers(data.subjects || []);
    } catch (err: any) {
      console.error('Không tải được DS giáo viên phân công: ', err.message);
    }
  }

  async function createItem() {
    setError('');
    setSuccessMsg('');

    if (!classId || !semesterId) {
      setError('Vui lòng chọn Lớp học và Học kỳ.');
      return;
    }
    if (!assignedId) {
      setError('Vui lòng chọn Môn học & Giáo viên giảng dạy.');
      return;
    }
    if (!room.trim()) {
      setError('Vui lòng nhập Phòng học.');
      return;
    }

    const selectedAssignment = assignedTeachers.find(t => t.id === +assignedId);
    if (!selectedAssignment) {
      setError('Thông tin phân công không hợp lệ.');
      return;
    }

    try {
      await apiFetch('/schedules', {
        method: 'POST',
        body: JSON.stringify({
          classId: +classId,
          subjectId: selectedAssignment.subject.id,
          teacherId: selectedAssignment.teacher.id,
          semesterId: +semesterId,
          dayOfWeek,
          period,
          room: room.trim(),
          shift,
        }),
      });
      setSuccessMsg('Đã thêm lịch học vào thời khóa biểu thành công!');
      setRoom('');
      setAssignedId('');
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
      {successMsg && (
        <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: '16px' }}>
          [SUCCESS] {successMsg}
        </div>
      )}

      {/* Bộ lọc xem TKB và thiết lập */}
      <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))' }}>
        <div className="form-group">
          <label>Lớp học</label>
          <select value={classId} onChange={e => setClassId(e.target.value)}>
            <option value="">Chọn lớp học</option>
            {classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <span className="input-desc">Chọn lớp học để quản lý</span>
        </div>

        <div className="form-group">
          <label>Học kỳ</label>
          <select value={semesterId} onChange={e => setSemesterId(e.target.value)}>
            <option value="">Chọn học kỳ</option>
            {semesters.map(s => <option key={s.id} value={s.id}>{s.name} - {s.academicYear} {s.isCurrent ? '(Hiện tại)' : ''}</option>)}
          </select>
          <span className="input-desc">Xem lịch học của kỳ nào</span>
        </div>
      </div>

      {/* Form thêm tiết học mới */}
      {classId && semesterId && (
        <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', marginTop: '16px', background: '#fbfbfb' }}>
          <div style={{ gridColumn: 'span 4', fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', marginBottom: '8px' }}>
            [ THÊM LỊCH HỌC MỚI CHO LỚP ]
          </div>
          
          <div className="form-group">
            <label>Môn & Giáo viên</label>
            <select value={assignedId} onChange={e => setAssignedId(e.target.value)}>
              <option value="">Chọn phân công giảng dạy</option>
              {assignedTeachers.map(t => (
                <option key={t.id} value={t.id}>
                  {t.subject.name} - {t.teacher.name} ({t.teacher.employeeCode})
                </option>
              ))}
            </select>
            <span className="input-desc">Chỉ hiển thị giáo viên đã gán dạy lớp</span>
          </div>

          <div className="form-group">
            <label>Thứ</label>
            <select value={dayOfWeek} onChange={e => setDayOfWeek(+e.target.value)}>
              <option value="2">Thứ Hai</option>
              <option value="3">Thứ Ba</option>
              <option value="4">Thứ Tư</option>
              <option value="5">Thứ Năm</option>
              <option value="6">Thứ Sáu</option>
              <option value="7">Thứ Bảy</option>
            </select>
            <span className="input-desc">Ngày diễn ra tiết học</span>
          </div>

          <div className="form-group">
            <label>Ca học</label>
            <select value={shift} onChange={e => setShift(e.target.value)}>
              <option value="MORNING">Buổi Sáng</option>
              <option value="AFTERNOON">Buổi Chiều</option>
            </select>
            <span className="input-desc">Ca sáng hoặc ca chiều</span>
          </div>

          <div className="form-group">
            <label>Tiết học</label>
            <select value={period} onChange={e => setPeriod(+e.target.value)}>
              {[1, 2, 3, 4, 5].map(p => (
                <option key={p} value={p}>Tiết {p} ({shift === 'MORNING' ? `Ca Sáng` : `Ca Chiều`})</option>
              ))}
            </select>
            <span className="input-desc">Số tiết trong buổi (1 - 5)</span>
          </div>

          <div className="form-group">
            <label>Phòng học</label>
            <input 
              placeholder="VD: 101, A2-402..." 
              value={room} 
              onChange={e => setRoom(e.target.value)} 
            />
            <span className="input-desc">Vị trí phòng học diễn ra</span>
          </div>

          <div className="form-group" style={{ justifyContent: 'center' }}>
            <label style={{ visibility: 'hidden' }}>Thao tác</label>
            <button onClick={createItem} style={{ height: '38px', width: '100%' }}>Thêm lịch</button>
            <span className="input-desc" style={{ visibility: 'hidden' }}>&nbsp;</span>
          </div>
        </div>
      )}

      {/* Bảng danh sách TKB */}
      {classId && semesterId && (
        <div style={{ marginTop: '32px' }}>
          <h3 style={{ fontSize: '15px', fontWeight: 700, textTransform: 'uppercase', marginBottom: '16px', borderBottom: '1px dashed #000000', paddingBottom: '8px' }}>
            Thời khóa biểu lớp: {classes.find(c => c.id === +classId)?.name}
          </h3>

          {loadingTkb ? (
            <p style={{ fontSize: '13px', fontFamily: 'ui-monospace, monospace' }}>Đang tải dữ liệu thời khóa biểu...</p>
          ) : items.length === 0 ? (
            <p style={{ fontSize: '13px', color: '#a3a3a3', fontStyle: 'italic' }}>Lớp học này chưa được xếp lịch học cho học kỳ đã chọn.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Thứ</th>
                  <th>Buổi</th>
                  <th>Tiết</th>
                  <th>Môn học</th>
                  <th>Giáo viên</th>
                  <th>Phòng học</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {items.map((s: any) => (
                  <tr key={s.id}>
                    <td style={{ fontWeight: 700 }}>{s.dayOfWeekName || `Thứ ${s.dayOfWeek}`}</td>
                    <td>{s.shift}</td>
                    <td style={{ fontFamily: 'ui-monospace, monospace' }}>Tiết {s.period}</td>
                    <td style={{ fontWeight: 600 }}>{s.subjectName}</td>
                    <td>{s.teacherName}</td>
                    <td>{s.room}</td>
                    <td>
                      <button className="danger" onClick={() => deleteItem(s.id)}>
                        Xóa
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
