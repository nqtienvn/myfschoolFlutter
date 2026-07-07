import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

export default function SchedulesPage() {
  const [items, setItems] = useState<any[]>([]);
  const [classes, setClasses] = useState<any[]>([]);
  const [subjects, setSubjects] = useState<any[]>([]);
  const [classId, setClassId] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [teacherId, setTeacherId] = useState('');
  const [semesterId, setSemesterId] = useState('');
  const [dayOfWeek, setDayOfWeek] = useState(2);
  const [period, setPeriod] = useState(1);
  const [room, setRoom] = useState('');
  const [shift, setShift] = useState('MORNING');

  useEffect(() => {
    apiFetch('/classes?page=0&size=100').then(d => setClasses(d.content || [])).catch(() => {});
    apiFetch('/subjects').then(setSubjects).catch(() => {});
  }, []);

  async function fetchItems() {
    if (!classId || !semesterId) return;
    try {
      const data = await apiFetch(`/schedules/class?classId=${classId}&semesterId=${semesterId}`);
      const allDays = data.days || [];
      setItems(allDays.flatMap((d: any) => [...(d.morningSlots || []), ...(d.afternoonSlots || [])]));
    } catch (err: any) { alert(err.message); }
  }

  async function createItem() {
    try {
      await apiFetch('/schedules', {
        method: 'POST',
        body: JSON.stringify({ classId: +classId, subjectId: +subjectId, teacherId: +teacherId, semesterId: +semesterId, dayOfWeek, period, room, shift }),
      });
      fetchItems();
    } catch (err: any) { alert(err.message); }
  }

  async function deleteItem(id: number) {
    if (!confirm('Xóa tiết này?')) return;
    try { await apiFetch(`/schedules/${id}`, { method: 'DELETE' }); fetchItems(); } catch (err: any) { alert(err.message); }
  }

  return (
    <div>
      <h2>Thời khóa biểu</h2>
      <div className="form-inline">
        <select value={classId} onChange={e => setClassId(e.target.value)}>
          <option value="">Chọn lớp</option>
          {classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <input placeholder="Semester ID" type="number" value={semesterId} onChange={e => setSemesterId(e.target.value)} />
        <button onClick={fetchItems}>Xem TKB</button>
      </div>
      <div className="form-inline">
        <select value={subjectId} onChange={e => setSubjectId(e.target.value)}>
          <option value="">Chọn môn</option>
          {subjects.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
        </select>
        <input placeholder="Teacher ID" type="number" value={teacherId} onChange={e => setTeacherId(e.target.value)} />
        <input type="number" placeholder="Thứ" value={dayOfWeek} onChange={e => setDayOfWeek(+e.target.value)} min={2} max={7} />
        <input type="number" placeholder="Tiết" value={period} onChange={e => setPeriod(+e.target.value)} min={1} max={10} />
        <input placeholder="Phòng" value={room} onChange={e => setRoom(e.target.value)} />
        <select value={shift} onChange={e => setShift(e.target.value)}>
          <option value="MORNING">Sáng</option>
          <option value="AFTERNOON">Chiều</option>
        </select>
        <button onClick={createItem}>Thêm tiết</button>
      </div>
      <table>
        <thead><tr><th>ID</th><th>Môn</th><th>Thứ</th><th>Tiết</th><th>Phòng</th><th></th></tr></thead>
        <tbody>
          {items.map((s: any) => (
            <tr key={s.id}><td>{s.id}</td><td>{s.subjectName}</td><td>{s.dayOfWeek}</td><td>{s.period}</td><td>{s.room}</td>
              <td><button onClick={() => deleteItem(s.id)}>Xóa</button></td></tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
