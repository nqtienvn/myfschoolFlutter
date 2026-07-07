import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

export default function TuitionPage() {
  const [items, setItems] = useState<any[]>([]);
  const [classId, setClassId] = useState('');
  const [semesterId, setSemesterId] = useState('');
  const [classes, setClasses] = useState<any[]>([]);

  useEffect(() => {
    apiFetch('/classes?page=0&size=100').then(d => setClasses(d.content || [])).catch(() => {});
  }, []);

  async function fetchItems() {
    if (!classId || !semesterId) return;
    try { setItems(await apiFetch(`/tuition/bills/class?classId=${classId}&semesterId=${semesterId}`)); } catch (err: any) { alert(err.message); }
  }

  async function deleteItem(id: number) {
    if (!confirm('Xóa khoản học phí?')) return;
    try { await apiFetch(`/tuition/bills/${id}`, { method: 'DELETE' }); fetchItems(); } catch (err: any) { alert(err.message); }
  }

  return (
    <div>
      <h2>Học phí</h2>
      <div className="form-inline">
        <select value={classId} onChange={e => setClassId(e.target.value)}>
          <option value="">Chọn lớp</option>
          {classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <input placeholder="Semester ID" type="number" value={semesterId} onChange={e => setSemesterId(e.target.value)} />
        <button onClick={fetchItems}>Xem</button>
      </div>
      <table>
        <thead><tr><th>ID</th><th>Học sinh</th><th>Lớp</th><th>Số tiền</th><th>Trạng thái</th><th></th></tr></thead>
        <tbody>
          {items.map((b: any) => (
            <tr key={b.id}><td>{b.id}</td><td>{b.studentName}</td><td>{b.className}</td><td>{b.amount?.toLocaleString()}</td><td>{b.status}</td>
              <td><button onClick={() => deleteItem(b.id)}>Xóa</button></td></tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
