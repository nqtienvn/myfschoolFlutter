import { useEffect, useState, type FormEvent } from 'react';
import {
  deleteTeacherAnnouncement,
  getEligibleAnnouncementClasses,
  getReceivedAnnouncements,
  getSentAnnouncements,
  markAnnouncementRead,
  saveTeacherAnnouncement,
} from '../../api/teacher';
import { useTeacherAcademic } from '../../teacher/TeacherAcademicContext';

interface Announcement { id: number; title: string; body: string; targetRole: string; classIds?: number[]; classNames?: string[]; approvalStatus: string; rejectionReason?: string; senderName?: string; createdAt: string; isRead?: boolean }

export default function TeacherAnnouncementsPage() {
  const { selectedYearId } = useTeacherAcademic();
  const [classes, setClasses] = useState<any[]>([]);
  const [received, setReceived] = useState<Announcement[]>([]);
  const [sent, setSent] = useState<Announcement[]>([]);
  const [selected, setSelected] = useState<Announcement | null>(null);
  const [editing, setEditing] = useState<Announcement | null>(null);
  const [draft, setDraft] = useState({ title: '', body: '', targetRole: 'ALL', classIds: [] as number[] });
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  async function load() {
    if (!selectedYearId) return;
    setError('');
    try {
      const [classRows, inboxRows, sentRows] = await Promise.all([
        getEligibleAnnouncementClasses(selectedYearId), getReceivedAnnouncements(selectedYearId), getSentAnnouncements(selectedYearId),
      ]);
      setClasses(classRows || []); setReceived(inboxRows || []); setSent(sentRows || []);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Không tải được thông báo.'); }
  }

  useEffect(() => { setClasses([]); setReceived([]); setSent([]); setSelected(null); setEditing(null); setDraft({ title: '', body: '', targetRole: 'ALL', classIds: [] }); void load(); }, [selectedYearId]);

  async function submit(event: FormEvent) {
    event.preventDefault(); if (!selectedYearId || !draft.classIds.length) return;
    setBusy(true); setError(''); setMessage('');
    try {
      await saveTeacherAnnouncement({ ...draft, academicYearId: selectedYearId, requiresReply: false }, editing?.id);
      setEditing(null); setDraft({ title: '', body: '', targetRole: 'ALL', classIds: [] }); await load();
      setMessage('Đã gửi thông báo tới Admin để phê duyệt.');
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Không gửi được thông báo.'); }
    finally { setBusy(false); }
  }

  async function openReceived(item: Announcement) {
    try { if (!item.isRead) await markAnnouncementRead(item.id); setSelected(item); await load(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không mở được thông báo.'); }
  }

  return <main className="teacher-page page-stack">
    <div className="page-heading"><div><p className="teacher-eyebrow">Kết nối nhà trường</p><h1>Thông báo</h1><p>Hòm thư đã nhận và lịch sử thông báo lớp được quản lý cùng một nơi.</p></div></div>
    {error && <div className="notice error">{error}</div>}{message && <div className="notice success">{message}</div>}
    <div className="teacher-announcement-grid">
      <section className="panel"><h2>Thông báo đã nhận</h2><div className="teacher-inbox-list">{received.map(item => <button key={item.id} className={item.isRead ? '' : 'unread'} onClick={() => openReceived(item)}><span><strong>{item.title}</strong><small>{item.senderName || 'Nhà trường'} · {new Date(item.createdAt).toLocaleString('vi-VN')}</small></span>{!item.isRead && <b>Mới</b>}</button>)}</div>{!received.length && <div className="empty-state">Chưa có thông báo từ Nhà trường/Admin.</div>}</section>
      <section className="panel"><h2>{editing ? 'Sửa thông báo lớp' : 'Gửi thông báo lớp'}</h2><form className="teacher-announcement-form" onSubmit={submit}><label>Tiêu đề<input required maxLength={500} value={draft.title} onChange={event => setDraft(d => ({ ...d, title: event.target.value }))}/></label><label>Nội dung<textarea required rows={6} value={draft.body} onChange={event => setDraft(d => ({ ...d, body: event.target.value }))}/></label><label>Người nhận<select value={draft.targetRole} onChange={event => setDraft(d => ({ ...d, targetRole: event.target.value }))}><option value="ALL">Phụ huynh & học sinh</option><option value="PARENT">Phụ huynh</option><option value="STUDENT">Học sinh</option></select></label><fieldset><legend>Lớp được phân công</legend>{classes.map(item => <label className="check-row" key={item.id}><input type="checkbox" checked={draft.classIds.includes(item.id)} onChange={event => setDraft(d => ({ ...d, classIds: event.target.checked ? [...d.classIds, item.id] : d.classIds.filter(id => id !== item.id) }))}/><span>{item.name}{item.isHomeroom ? ' · GVCN' : ''}</span></label>)}</fieldset><p className="table-subtext">Thông báo lớp không yêu cầu xác nhận/phản hồi; tùy chọn này luôn được gửi là false.</p><div className="monitoring-actions"><button disabled={busy || !draft.classIds.length}>{busy ? 'Đang gửi…' : editing ? 'Lưu thay đổi' : 'Gửi duyệt'}</button>{editing && <button type="button" className="secondary-button" onClick={() => { setEditing(null); setDraft({ title: '', body: '', targetRole: 'ALL', classIds: [] }); }}>Hủy sửa</button>}</div></form></section>
    </div>
    <section className="panel"><h2>Lịch sử đã gửi</h2><div className="table-responsive"><table><thead><tr><th>Thông báo</th><th>Lớp</th><th>Trạng thái duyệt</th><th>Ngày gửi</th><th /></tr></thead><tbody>{sent.map(item => <tr key={item.id}><td><strong>{item.title}</strong><small className="table-subtext">{item.body}</small>{item.rejectionReason && <small className="text-danger">Lý do: {item.rejectionReason}</small>}</td><td>{item.classNames?.join(', ') || '—'}</td><td><span className={`badge-status ${item.approvalStatus === 'APPROVED' ? 'active' : 'draft'}`}>{item.approvalStatus}</span></td><td>{new Date(item.createdAt).toLocaleString('vi-VN')}</td><td><div className="table-actions">{item.approvalStatus !== 'APPROVED' && <button className="secondary-button" onClick={() => { setEditing(item); setDraft({ title: item.title, body: item.body, targetRole: item.targetRole, classIds: item.classIds || [] }); }}>Sửa</button>}<button className="danger" onClick={async () => { if (confirm('Xóa thông báo này?')) { await deleteTeacherAnnouncement(item.id); await load(); } }}>Xóa</button></div></td></tr>)}</tbody></table></div></section>
    {selected && <div className="modal-backdrop" onMouseDown={() => setSelected(null)}><section className="modal-card review-detail" onMouseDown={event => event.stopPropagation()}><div className="modal-header"><div><h2>{selected.title}</h2><p>{selected.senderName || 'Nhà trường'}</p></div><button className="icon-button" onClick={() => setSelected(null)}>×</button></div><p className="announcement-detail-body">{selected.body}</p></section></div>}
  </main>;
}
