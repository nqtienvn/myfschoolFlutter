import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

interface ClassItem { id: number; name: string; gradeLevel: number; academicYear: string; }

export default function ClassesPage() {
  const [items, setItems] = useState<ClassItem[]>([]);
  const [name, setName] = useState('');
  const [gradeLevel, setGradeLevel] = useState(10);
  const [academicYear, setAcademicYear] = useState('2026-2027');

  async function fetchItems() {
    try {
      const data = await apiFetch('/classes?page=0&size=100');
      setItems(data.content || []);
    } catch (err: any) { alert(err.message); }
  }

  useEffect(() => { fetchItems(); }, []);

  async function createItem() {
    try {
      await apiFetch('/classes', {
        method: 'POST',
        body: JSON.stringify({ name, gradeLevel, academicYear, schoolName: 'FPT Schools' }),
      });
      setName(''); fetchItems();
    } catch (err: any) { alert(err.message); }
  }

  async function deleteItem(id: number) {
    if (!confirm('Xóa lớp này?')) return;
    try { await apiFetch(`/classes/${id}`, { method: 'DELETE' }); fetchItems(); } catch (err: any) { alert(err.message); }
  }

  return (
    <div>
      <h2>Quản lý lớp học</h2>
      <div className="form-inline">
        <input placeholder="Tên lớp" value={name} onChange={e => setName(e.target.value)} />
        <input type="number" placeholder="Khối" value={gradeLevel} onChange={e => setGradeLevel(+e.target.value)} />
        <input placeholder="Năm học" value={academicYear} onChange={e => setAcademicYear(e.target.value)} />
        <button onClick={createItem}>Tạo lớp</button>
      </div>
      <table>
        <thead><tr><th>ID</th><th>Tên</th><th>Khối</th><th>Năm học</th><th></th></tr></thead>
        <tbody>
          {items.map(c => (
            <tr key={c.id}>
              <td>{c.id}</td><td>{c.name}</td><td>{c.gradeLevel}</td><td>{c.academicYear}</td>
              <td><button onClick={() => deleteItem(c.id)}>Xóa</button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
