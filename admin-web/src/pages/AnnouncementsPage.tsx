import { FormEvent, useEffect, useState } from 'react';
import { AnnouncementItem, broadcastAnnouncement, deleteAnnouncement, getAnnouncements, reviewAnnouncement } from '../api/announcement';

export default function AnnouncementsPage({ selectedYearId }: { selectedYearId: string }) {
  const [items, setItems] = useState<AnnouncementItem[]>([]);
  const [status, setStatus] = useState('');
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [busy, setBusy] = useState(false);

  async function load() {
    if (!selectedYearId) { setItems([]); return; }
    setItems(await getAnnouncements(selectedYearId, status));
  }
  useEffect(() => { setItems([]); void load(); }, [selectedYearId, status]);

  async function review(item: AnnouncementItem, approve: boolean) {
    const reason = approve ? undefined : window.prompt('Nhập lý do từ chối:')?.trim();
    if (!approve && !reason) return;
    await reviewAnnouncement(item.id, approve, reason); await load();
  }
  async function submit(event: FormEvent) {
    event.preventDefault(); if (!selectedYearId || !title.trim() || !body.trim()) return;
    setBusy(true); try { await broadcastAnnouncement(selectedYearId, title.trim(), body.trim()); setTitle(''); setBody(''); await load(); }
    finally { setBusy(false); }
  }
  return <section className="announcement-admin">
    <div className="page-heading"><div><p>TRUNG TÂM THÔNG BÁO</p><h1>Thông báo & phê duyệt</h1></div>
      <select value={status} onChange={e => setStatus(e.target.value)}><option value="">Tất cả trạng thái</option><option value="PENDING">Chờ duyệt</option><option value="APPROVED">Đã duyệt</option><option value="REJECTED">Từ chối</option></select></div>
    <form className="announcement-compose" onSubmit={submit}><h2>Gửi thông báo toàn trường</h2>
      <input value={title} onChange={e => setTitle(e.target.value)} placeholder="Tiêu đề" required />
      <textarea value={body} onChange={e => setBody(e.target.value)} placeholder="Nội dung gửi tới toàn bộ tài khoản" rows={4} required />
      <button disabled={busy || !selectedYearId}>{busy ? 'Đang gửi...' : 'Gửi toàn trường'}</button></form>
    <div className="approval-list">{items.map(item => <article key={item.id} className={`approval-card ${item.approvalStatus.toLowerCase()}`}>
      <div className="approval-meta"><strong>{item.teacherName}</strong><span>{new Date(item.createdAt).toLocaleString('vi-VN')}</span></div>
      <h3>{item.title}</h3><p>{item.body}</p>
      <div className="approval-tags"><span>Lớp: {item.classNames.join(', ') || 'Toàn trường'}</span><span>{item.targetRole === 'ALL' ? 'Phụ huynh & học sinh' : item.targetRole === 'PARENT' ? 'Phụ huynh' : 'Học sinh'}</span><span>{item.senderType === 'HOMEROOM_TEACHER' ? 'GVCN' : item.senderType === 'ADMIN' ? 'Admin' : 'GV bộ môn'}</span></div>
      {item.rejectionReason && <p className="reject-reason">Lý do: {item.rejectionReason}</p>}
      <div className="approval-actions">{item.approvalStatus === 'PENDING' && <><button onClick={() => review(item, true)}>Phê duyệt</button><button className="danger" onClick={() => review(item, false)}>Từ chối</button></>}<button className="ghost" onClick={async () => { if (confirm('Xóa thông báo này?')) { await deleteAnnouncement(item.id); await load(); } }}>Xóa</button></div>
    </article>)}{!items.length && <div className="empty-state">Không có thông báo trong bộ lọc này.</div>}</div>
  </section>;
}
