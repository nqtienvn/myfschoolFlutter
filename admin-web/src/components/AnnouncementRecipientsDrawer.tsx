import { FormEvent, useEffect, useState } from 'react';
import {
  AnnouncementItem,
  AnnouncementRecipient,
  getAnnouncementRecipients,
  RecipientPage,
} from '../api/announcement';

interface ClassItem { id: number; name: string; }

interface Props {
  announcement: AnnouncementItem;
  selectedYearId: string;
  classes: ClassItem[];
  onClose: () => void;
}

const emptyPage: RecipientPage = { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 };

export default function AnnouncementRecipientsDrawer({ announcement, selectedYearId, classes, onClose }: Props) {
  const [data, setData] = useState<RecipientPage>(emptyPage);
  const [classId, setClassId] = useState('');
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('');
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function load(page = 0) {
    setLoading(true); setError('');
    try {
      setData(await getAnnouncementRecipients(announcement.id, {
        academicYearId: selectedYearId, classId, role, status, keyword, page, size: 20,
      }));
    } catch (cause: any) {
      setError(cause.message || 'Không thể tải danh sách người nhận.');
      setData(emptyPage);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    setData(emptyPage); setClassId(''); setRole(''); setStatus(''); setKeyword(''); setError('');
    void getAnnouncementRecipients(announcement.id, { academicYearId: selectedYearId, page: 0, size: 20 })
      .then(setData)
      .catch((cause: any) => setError(cause.message || 'Không thể tải danh sách người nhận.'));
  }, [announcement.id, selectedYearId]);

  function submit(event: FormEvent) { event.preventDefault(); void load(0); }

  return <div className="drawer-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) onClose(); }}>
    <aside className="recipient-drawer" role="dialog" aria-modal="true" aria-label="Chi tiết người nhận thông báo">
      <header className="recipient-drawer-header">
        <div><span className="eyebrow">Theo dõi thông báo</span><h2>{announcement.title}</h2></div>
        <button type="button" className="secondary-button" onClick={onClose}>✕ Đóng</button>
      </header>
      <div className="recipient-stat-grid">
        <Stat label="Người nhận" value={announcement.totalRecipients} />
        <Stat label="Đã đọc" value={announcement.readCount} />
        <Stat label="Đã xác nhận" value={announcement.acknowledgedCount} />
        <Stat label="Đã phản hồi" value={announcement.repliedCount} />
      </div>
      <form className="recipient-filters" onSubmit={submit}>
        <div className="form-group"><label>Lớp</label><select value={classId} onChange={event => setClassId(event.target.value)}><option value="">Tất cả</option>{classes.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div>
        <div className="form-group"><label>Vai trò</label><select value={role} onChange={event => setRole(event.target.value)}><option value="">Tất cả</option><option value="PARENT">Phụ huynh</option><option value="STUDENT">Học sinh</option><option value="TEACHER">Giáo viên</option></select></div>
        <div className="form-group"><label>Trạng thái</label><select value={status} onChange={event => setStatus(event.target.value)}><option value="">Tất cả</option><option value="PENDING">Chờ hành động</option><option value="UNREAD">Chưa đọc</option><option value="READ">Đã đọc</option><option value="ACKNOWLEDGED">Đã xác nhận</option><option value="REPLIED">Đã phản hồi</option></select></div>
        <div className="form-group"><label>Tìm kiếm</label><input value={keyword} onChange={event => setKeyword(event.target.value)} placeholder="Tên người nhận hoặc học sinh" /></div>
        <button type="submit" disabled={loading}>Lọc</button>
      </form>
      {error && <div className="notice error">{error}</div>}
      <div className="table-responsive recipient-table"><table><thead><tr><th>Người nhận</th><th>Học sinh / lớp</th><th>Trạng thái</th><th>Phản hồi</th></tr></thead><tbody>
        {data.content.map(item => <RecipientRow key={item.userId} item={item} />)}
        {!loading && !data.content.length && <tr><td colSpan={4}>Không có người nhận phù hợp bộ lọc.</td></tr>}
        {loading && <tr><td colSpan={4}>Đang tải...</td></tr>}
      </tbody></table></div>
      <footer className="recipient-pagination">
        <span>{data.totalElements} người nhận · Trang {data.totalPages ? data.number + 1 : 0}/{data.totalPages}</span>
        <div><button type="button" className="secondary-button" disabled={loading || data.number <= 0} onClick={() => void load(data.number - 1)}>Trước</button><button type="button" className="secondary-button" disabled={loading || data.number + 1 >= data.totalPages} onClick={() => void load(data.number + 1)}>Sau</button></div>
      </footer>
    </aside>
  </div>;
}

function Stat({ label, value }: { label: string; value: number }) {
  return <div><strong>{value}</strong><span>{label}</span></div>;
}

function RecipientRow({ item }: { item: AnnouncementRecipient }) {
  const labels: Record<AnnouncementRecipient['status'], string> = { UNREAD: 'Chưa đọc', READ: 'Đã đọc', ACKNOWLEDGED: 'Đã xác nhận', REPLIED: 'Đã phản hồi' };
  return <tr><td><strong>{item.userName}</strong><br/><small>{item.role === 'PARENT' ? 'Phụ huynh' : item.role === 'STUDENT' ? 'Học sinh' : 'Giáo viên'}</small></td><td>{item.studentNames.join(', ') || '—'}<br/><small>{item.classNames.join(', ') || '—'}</small></td><td><span className={`badge-status recipient-${item.status.toLowerCase()}`}>{labels[item.status]}</span>{item.readAt && <><br/><small>{new Date(item.readAt).toLocaleString('vi-VN')}</small></>}</td><td>{item.replyText || '—'}{item.repliedAt && <><br/><small>{new Date(item.repliedAt).toLocaleString('vi-VN')}</small></>}</td></tr>;
}
