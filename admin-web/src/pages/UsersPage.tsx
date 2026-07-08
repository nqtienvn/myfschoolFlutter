import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';
import { AdminUser } from '../api/auth';

interface UsersPageData {
  content: AdminUser[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

const PAGE_SIZE = 20;

export default function UsersPage() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('');
  const [keyword, setKeyword] = useState('');
  const [appliedKeyword, setAppliedKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);

  async function fetchUsers() {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
      if (role) params.set('role', role);
      if (status) params.set('status', status);
      if (appliedKeyword) params.set('keyword', appliedKeyword);
      const data: UsersPageData = await apiFetch(`/admin/users?${params.toString()}`);
      setUsers(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err: any) {
      alert(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchUsers();
  }, [role, status, appliedKeyword, page]);

  function changeRole(value: string) {
    setRole(value);
    setPage(0);
  }

  function changeStatus(value: string) {
    setStatus(value);
    setPage(0);
  }

  function searchUsers() {
    setAppliedKeyword(keyword.trim());
    setPage(0);
  }

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
        <select value={role} onChange={e => changeRole(e.target.value)}>
          <option value="">Tất cả vai trò</option>
          <option value="PARENT">Phụ huynh</option>
          <option value="STUDENT">Học sinh</option>
          <option value="TEACHER">Giáo viên</option>
        </select>
        <select value={status} onChange={e => changeStatus(e.target.value)}>
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
        <button onClick={searchUsers} disabled={loading}>Tìm</button>
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
          {users.length === 0 && !loading && (
            <tr>
              <td colSpan={6}>Không có tài khoản phù hợp</td>
            </tr>
          )}
        </tbody>
      </table>
      <div className="filters">
        <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={loading || page === 0}>
          Trước
        </button>
        <span>
          Trang {totalPages === 0 ? 0 : page + 1}/{totalPages} - {totalElements} tài khoản
        </span>
        <button onClick={() => setPage(p => p + 1)} disabled={loading || page + 1 >= totalPages}>
          Sau
        </button>
      </div>
      {loading && <p>Đang tải...</p>}
    </div>
  );
}
