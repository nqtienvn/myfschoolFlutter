import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';
import { AdminUser } from '../api/auth';

export default function UsersPage() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('');
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(false);

  async function fetchUsers() {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (role) params.set('role', role);
      if (status) params.set('status', status);
      if (keyword) params.set('keyword', keyword);
      const data = await apiFetch(`/admin/users?${params.toString()}`);
      setUsers(data);
    } catch (err: any) {
      alert(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { fetchUsers(); }, []);

  async function toggleStatus(user: AdminUser) {
    const newStatus = user.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE';
    if (!confirm(`${newStatus === 'LOCKED' ? 'Khóa' : 'Mở'} tài khoản ${user.name}?`)) return;
    try {
      await apiFetch(`/admin/users/${user.id}/status`, {
        method: 'PUT',
        body: JSON.stringify({ status: newStatus }),
      });
      fetchUsers();
    } catch (err: any) {
      alert(err.message);
    }
  }

  return (
    <div>
      <h2>Quản lý tài khoản</h2>
      <div className="filters">
        <select value={role} onChange={e => setRole(e.target.value)}>
          <option value="">Tất cả vai trò</option>
          <option value="PARENT">Phụ huynh</option>
          <option value="STUDENT">Học sinh</option>
          <option value="TEACHER">Giáo viên</option>
        </select>
        <select value={status} onChange={e => setStatus(e.target.value)}>
          <option value="">Tất cả trạng thái</option>
          <option value="ACTIVE">Hoạt động</option>
          <option value="LOCKED">Khóa</option>
        </select>
        <input
          type="text"
          placeholder="Tìm theo tên hoặc SĐT..."
          value={keyword}
          onChange={e => setKeyword(e.target.value)}
        />
        <button onClick={fetchUsers} disabled={loading}>Tìm</button>
      </div>
      <table>
        <thead>
          <tr>
            <th>ID</th><th>Tên</th><th>SĐT</th><th>Vai trò</th><th>Trạng thái</th><th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {users.map(u => (
            <tr key={u.id}>
              <td>{u.id}</td>
              <td>{u.name}</td>
              <td>{u.phone}</td>
              <td>{u.role}</td>
              <td>{u.status}</td>
              <td>
                <button onClick={() => toggleStatus(u)}>
                  {u.status === 'ACTIVE' ? 'Khóa' : 'Mở'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {loading && <p>Đang tải...</p>}
    </div>
  );
}
