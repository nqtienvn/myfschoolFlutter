import { useEffect, useState } from 'react';
import { apiFetch } from '../api/client';

interface FeeCategory { id: number; name: string; description: string; }

export default function FeeCategoriesPage() {
  const [items, setItems] = useState<FeeCategory[]>([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  useEffect(() => { fetchItems(); }, []);

  async function fetchItems() {
    try {
      setItems(await apiFetch('/fee-categories'));
    } catch (err: any) {
      setError(err.message || 'Không thể tải danh mục phí');
    }
  }

  async function createItem() {
    setError('');
    setSuccessMsg('');
    if (!name.trim()) return setError('Tên danh mục không được để trống.');
    if (!description.trim()) return setError('Mô tả không được để trống.');
    try {
      await apiFetch('/fee-categories', { method: 'POST', body: JSON.stringify({ name: name.trim(), description: description.trim() }) });
      setName('');
      setDescription('');
      setSuccessMsg('Đã tạo danh mục phí.');
      fetchItems();
    } catch (err: any) {
      setError(err.message || 'Lỗi tạo danh mục phí');
    }
  }

  async function deleteItem(id: number) {
    if (!confirm('Xóa danh mục phí này?')) return;
    setError('');
    setSuccessMsg('');
    try {
      await apiFetch(`/fee-categories/${id}`, { method: 'DELETE' });
      setSuccessMsg('Đã xóa danh mục phí.');
      fetchItems();
    } catch (err: any) {
      setError(err.message || 'Lỗi xóa danh mục phí');
    }
  }

  return (
    <div>
      <h2>Danh mục phí</h2>
      {error && <div className="error">{error}</div>}
      {successMsg && <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: '16px' }}>[SUCCESS] {successMsg}</div>}

      <div className="form-grid">
        <div className="form-group"><label>Tên danh mục</label><input placeholder="VD: Học phí, Bảo hiểm..." value={name} onChange={e => setName(e.target.value)} /><span className="input-desc">Tên là duy nhất</span></div>
        <div className="form-group"><label>Mô tả</label><input placeholder="Mô tả ngắn" value={description} onChange={e => setDescription(e.target.value)} /><span className="input-desc">Tối đa 200 ký tự</span></div>
        <div className="form-group"><label style={{ visibility: 'hidden' }}>Thao tác</label><button onClick={createItem}>Tạo danh mục</button><span className="input-desc" style={{ visibility: 'hidden' }}>&nbsp;</span></div>
      </div>

      <table>
        <thead><tr><th>ID</th><th>Tên</th><th>Mô tả</th><th>Thao tác</th></tr></thead>
        <tbody>{items.map(item => <tr key={item.id}><td>{item.id}</td><td style={{ fontWeight: 600 }}>{item.name}</td><td>{item.description}</td><td><button className="danger" onClick={() => deleteItem(item.id)}>Xóa</button></td></tr>)}</tbody>
      </table>
    </div>
  );
}
