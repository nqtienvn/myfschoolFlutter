import { FormEvent, useEffect, useState } from 'react';
import {
  AnnouncementItem,
  broadcastAnnouncement,
  deleteAnnouncement,
  getAnnouncements,
  reviewAnnouncement,
} from '../api/announcement';

export default function AnnouncementsPage({
  selectedYearId,
  onPendingCountChange,
}: {
  selectedYearId: string;
  onPendingCountChange?: (count: number) => void;
}) {
  const [items, setItems] = useState<AnnouncementItem[]>([]);
  const [status, setStatus] = useState('PENDING');
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [busy, setBusy] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  async function load() {
    if (!selectedYearId) {
      setItems([]);
      onPendingCountChange?.(0);
      return;
    }
    const loaded = await getAnnouncements(selectedYearId, status);
    setItems(loaded);
    if (status === 'PENDING') onPendingCountChange?.(loaded.length);
  }

  useEffect(() => {
    setItems([]);
    setTitle('');
    setBody('');
    setStatus('PENDING');
    setError('');
    setMessage('');
    setShowForm(false);
  }, [selectedYearId]);

  useEffect(() => {
    void load().catch(cause => setError(cause.message));
  }, [selectedYearId, status]);

  async function review(item: AnnouncementItem, approve: boolean) {
    const reason = approve ? undefined : window.prompt('Nhập lý do từ chối:')?.trim();
    if (!approve && !reason) return;
    await reviewAnnouncement(item.id, approve, reason);
    await load();
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError('');
    setMessage('');
    setBusy(true);
    try {
      await broadcastAnnouncement({
        academicYearId: Number(selectedYearId),
        title: title.trim(),
        body: body.trim(),
      });
      setTitle('');
      setBody('');
      setShowForm(false);
      setMessage('Đã gửi thông báo đến toàn bộ tài khoản không phải quản trị viên.');
      await load();
    } catch (cause: any) {
      setError(cause.message || 'Không thể gửi thông báo.');
    } finally {
      setBusy(false);
    }
  }

  const recipientLabel = (item: AnnouncementItem) => item.recipientScope === 'SCHOOL'
    ? 'Toàn bộ tài khoản không phải quản trị viên'
    : `${item.classNames.join(', ')} · ${item.targetRole === 'PARENT'
      ? 'Phụ huynh'
      : item.targetRole === 'STUDENT'
        ? 'Học sinh'
        : 'Phụ huynh & học sinh'}`;

  return <div className="page-stack announcement-admin">
    <div className="page-heading">
      <div><span className="eyebrow">Trung tâm thông báo</span><h1>Thông báo & phê duyệt</h1></div>
      <div className="page-heading-actions">
        <button type="button" onClick={() => setShowForm(value => !value)}>
          {showForm ? '✕ Đóng' : '＋ Gửi thông báo'}
        </button>
      </div>
    </div>

    {error && <div className="notice error">{error}</div>}
    {message && <div className="notice success">{message}</div>}

    {showForm && <form className="announcement-compose" onSubmit={submit}>
      <h2>Gửi thông báo từ nhà trường</h2>
      <div className="notice info">
        Thông báo sẽ được gửi ngay đến toàn bộ tài khoản phụ huynh, học sinh và giáo viên.
      </div>
      <div className="form-group">
        <label>Tiêu đề</label>
        <input value={title} onChange={event => setTitle(event.target.value)} required />
      </div>
      <div className="form-group">
        <label>Nội dung</label>
        <textarea value={body} onChange={event => setBody(event.target.value)} rows={4} required />
      </div>
      <div className="form-actions" style={{ display: 'flex', gap: 8 }}>
        <button disabled={busy || !selectedYearId}>{busy ? 'Đang gửi...' : 'Gửi thông báo'}</button>
        <button type="button" className="secondary-button" onClick={() => setShowForm(false)}>Đóng</button>
      </div>
    </form>}

    <div className="filters">
      <div className="form-group">
        <label>Trạng thái</label>
        <select value={status} onChange={event => setStatus(event.target.value)}>
          <option value="PENDING">Chờ duyệt</option>
          <option value="">Tất cả</option>
          <option value="APPROVED">Đã duyệt</option>
          <option value="REJECTED">Từ chối</option>
        </select>
      </div>
    </div>

    <div className="table-responsive">
      <table>
        <thead><tr><th>Người gửi</th><th>Tiêu đề & nội dung</th><th>Phạm vi</th><th>Thời gian</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
        <tbody>
          {items.map(item => <tr key={item.id}>
            <td>
              <strong>{item.teacherName}</strong><br />
              <small>{item.senderType === 'ADMIN' ? 'Quản trị viên' : item.senderType === 'HOMEROOM_TEACHER' ? 'GVCN' : 'GV bộ môn'}</small>
            </td>
            <td>
              <strong>{item.title}</strong><br />
              <small>{item.body}</small>
              {item.rejectionReason && <div className="reject-reason">Lý do: {item.rejectionReason}</div>}
            </td>
            <td>{recipientLabel(item)}</td>
            <td>{new Date(item.createdAt).toLocaleString('vi-VN')}</td>
            <td>
              <span className={`badge-status ${item.approvalStatus === 'APPROVED' ? 'completed' : item.approvalStatus === 'PENDING' ? 'scheduled' : ''}`}>
                {item.approvalStatus === 'APPROVED' ? 'Đã duyệt' : item.approvalStatus === 'PENDING' ? 'Chờ duyệt' : 'Từ chối'}
              </span>
            </td>
            <td>
              <div className="table-actions">
                {item.approvalStatus === 'PENDING' && <>
                  <button onClick={() => review(item, true)}>Duyệt</button>
                  <button className="danger" onClick={() => review(item, false)}>Từ chối</button>
                </>}
                <button className="secondary-button" onClick={async () => {
                  if (confirm('Xóa thông báo này?')) {
                    await deleteAnnouncement(item.id);
                    await load();
                  }
                }}>Xóa</button>
              </div>
            </td>
          </tr>)}
          {!items.length && <tr><td colSpan={6}>Chưa có thông báo trong bộ lọc này.</td></tr>}
        </tbody>
      </table>
    </div>
  </div>;
}
