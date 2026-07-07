import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

interface Subject { id: number; name: string; code: string; }

export default function SubjectsPage() {
  const [items, setItems] = useState<Subject[]>([]);
  const [name, setName] = useState('');
  const [code, setCode] = useState('');

  async function fetchItems() {
    try { setItems(await apiFetch('/subjects')); } catch (err: any) { alert(err.message); }
  }
  useEffect(() => { fetchItems(); }, []);

  async function createItem() {
    try {
      await apiFetch('/subjects', { method: 'POST', body: JSON.stringify({ name, code }) });
      setName(''); setCode(''); fetchItems();
    } catch (err: any) { alert(err.message); }
  }

  async function deleteItem(id: number) {
    if (!confirm('Xóa môn học?')) return;
    try { await apiFetch(`/subjects/${id}`, { method: 'DELETE' }); fetchItems(); } catch (err: any) { alert(err.message); }
  }

  return (
    <div>
      <h2>Quản lý môn học</h2>
      <div className="form-inline">
        <input placeholder="Tên môn" value={name} onChange={e => setName(e.target.value)} />
        <input placeholder="Mã môn" value={code} onChange={e => setCode(e.target.value)} />
        <button onClick={createItem}>Tạo môn</button>
      </div>
      <table>
        <thead><tr><th>ID</th><th>Tên</th><th>Mã</th><th></th></tr></thead>
        <tbody>
          {items.map(s => (
            <tr key={s.id}><td>{s.id}</td><td>{s.name}</td><td>{s.code}</td>
              <td><button onClick={() => deleteItem(s.id)}>Xóa</button></td></tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
