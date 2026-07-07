import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

interface Semester { id: number; name: string; academicYear: string; startDate: string; endDate: string; isCurrent: boolean; }

export default function SemestersPage() {
  const [items, setItems] = useState<Semester[]>([]);
  const [name, setName] = useState('');
  const [academicYear, setAcademicYear] = useState('2026-2027');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  async function fetchItems() {
    try { setItems(await apiFetch('/semesters')); } catch (err: any) { alert(err.message); }
  }
  useEffect(() => { fetchItems(); }, []);

  async function createItem() {
    try {
      await apiFetch('/semesters', { method: 'POST', body: JSON.stringify({ name, academicYear, startDate, endDate }) });
      setName(''); fetchItems();
    } catch (err: any) { alert(err.message); }
  }

  async function setCurrent(id: number) {
    try { await apiFetch(`/semesters/${id}/set-current`, { method: 'PUT' }); fetchItems(); } catch (err: any) { alert(err.message); }
  }

  return (
    <div>
      <h2>Quản lý học kỳ</h2>
      <div className="form-inline">
        <input placeholder="Tên học kỳ" value={name} onChange={e => setName(e.target.value)} />
        <input type="date" value={startDate} onChange={e => setStartDate(e.target.value)} />
        <input type="date" value={endDate} onChange={e => setEndDate(e.target.value)} />
        <button onClick={createItem}>Tạo học kỳ</button>
      </div>
      <table>
        <thead><tr><th>ID</th><th>Tên</th><th>Năm học</th><th>Hiện tại</th><th></th></tr></thead>
        <tbody>
          {items.map(s => (
            <tr key={s.id}><td>{s.id}</td><td>{s.name}</td><td>{s.academicYear}</td><td>{s.isCurrent ? '✓' : ''}</td>
              <td>{!s.isCurrent && <button onClick={() => setCurrent(s.id)}>Đặt hiện tại</button>}</td></tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
