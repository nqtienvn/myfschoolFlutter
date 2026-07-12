import { FormEvent, useEffect, useState } from 'react';
import { AnnouncementItem, broadcastAnnouncement, deleteAnnouncement, getAnnouncements, reviewAnnouncement } from '../api/announcement';
import { getClasses } from '../api/class';
import { getSubjects } from '../api/subject';
import { getAcademicYearMasterData } from '../api/academicYearConfig';

interface ClassItem { id: number; name: string; }
interface SubjectItem { id: number; name: string; code: string; }
type Scope = 'SCHOOL' | 'CLASSES' | 'TEACHERS';

export default function AnnouncementsPage({ selectedYearId }: { selectedYearId: string }) {
  const [items, setItems] = useState<AnnouncementItem[]>([]);
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [subjects, setSubjects] = useState<SubjectItem[]>([]);
  const [status, setStatus] = useState('');
  const [title, setTitle] = useState(''); const [body, setBody] = useState('');
  const [scope, setScope] = useState<Scope>('SCHOOL');
  const [targetRole, setTargetRole] = useState<'ALL' | 'PARENT' | 'STUDENT'>('ALL');
  const [classIds, setClassIds] = useState<number[]>([]);
  const [teacherAudience, setTeacherAudience] = useState<'ALL' | 'SUBJECT' | 'HOMEROOM'>('ALL');
  const [subjectId, setSubjectId] = useState('');
  const [busy, setBusy] = useState(false); const [error, setError] = useState(''); const [message, setMessage] = useState('');

  async function load() { if (!selectedYearId) { setItems([]); return; } setItems(await getAnnouncements(selectedYearId, status)); }
  async function loadOptions() {
    if (!selectedYearId) { setClasses([]); setSubjects([]); return; }
    const [classPage, allSubjects, config] = await Promise.all([getClasses({ academicYearId: selectedYearId, page: 0, size: 500 }), getSubjects(), getAcademicYearMasterData(selectedYearId)]);
    setClasses(classPage.content || []);
    const allowed = new Set(config.subjectIds); setSubjects((allSubjects as SubjectItem[]).filter(subject => allowed.has(subject.id)));
  }
  useEffect(() => {
    setItems([]); setClasses([]); setSubjects([]);
    setTitle(''); setBody(''); setScope('SCHOOL'); setTargetRole('ALL');
    setClassIds([]); setTeacherAudience('ALL'); setSubjectId('');
    setStatus(''); setError(''); setMessage('');
  }, [selectedYearId]);

  useEffect(() => {
    void Promise.all([load(), loadOptions()]).catch(cause => setError(cause.message));
  }, [selectedYearId, status]);

  async function review(item: AnnouncementItem, approve: boolean) { const reason = approve ? undefined : window.prompt('Nhập lý do từ chối:')?.trim(); if (!approve && !reason) return; await reviewAnnouncement(item.id, approve, reason); await load(); }
  async function submit(event: FormEvent) {
    event.preventDefault(); setError(''); setMessage('');
    if (scope === 'CLASSES' && classIds.length === 0) { setError('Hãy chọn ít nhất một lớp.'); return; }
    if (scope === 'TEACHERS' && teacherAudience === 'SUBJECT' && !subjectId) { setError('Hãy chọn môn phụ trách.'); return; }
    setBusy(true);
    try {
      await broadcastAnnouncement({ academicYearId: Number(selectedYearId), title: title.trim(), body: body.trim(), recipientScope: scope,
        targetRole: scope === 'CLASSES' ? targetRole : undefined, classIds: scope === 'CLASSES' ? classIds : undefined,
        teacherAudience: scope === 'TEACHERS' ? teacherAudience : undefined, subjectId: scope === 'TEACHERS' && teacherAudience === 'SUBJECT' ? Number(subjectId) : undefined });
      setTitle(''); setBody(''); setMessage('Đã gửi thông báo thành công.'); await load();
    } catch (cause: any) { setError(cause.message || 'Không thể gửi thông báo.'); } finally { setBusy(false); }
  }
  const recipientLabel = (item: AnnouncementItem) => item.recipientScope === 'SCHOOL' ? 'Toàn trường' : item.recipientScope === 'TEACHERS'
    ? item.teacherAudience === 'SUBJECT' ? `GV môn ${item.subjectName || ''}` : item.teacherAudience === 'HOMEROOM' ? 'Giáo viên chủ nhiệm' : 'Toàn bộ giáo viên'
    : `${item.classNames.join(', ')} · ${item.targetRole === 'PARENT' ? 'Phụ huynh' : item.targetRole === 'STUDENT' ? 'Học sinh' : 'PH & HS'}`;

  return <div className="page-stack announcement-admin">
    <div className="page-heading"><div><span className="eyebrow">Trung tâm thông báo</span><h1>Thông báo & phê duyệt</h1></div></div>
    {error && <div className="notice error">{error}</div>}{message && <div className="notice success">{message}</div>}
    <form className="announcement-compose" onSubmit={submit}><h2>Gửi thông báo từ nhà trường</h2>
      <div className="form-grid announcement-fields"><div className="form-group"><label>Phạm vi người nhận</label><select value={scope} onChange={e => { setScope(e.target.value as Scope); setClassIds([]); }}><option value="SCHOOL">Cả trường</option><option value="CLASSES">Theo lớp</option><option value="TEACHERS">Đội ngũ giáo viên</option></select></div>
        {scope === 'CLASSES' && <div className="form-group"><label>Đối tượng trong lớp</label><select value={targetRole} onChange={e => setTargetRole(e.target.value as typeof targetRole)}><option value="ALL">Phụ huynh & học sinh</option><option value="PARENT">Chỉ phụ huynh</option><option value="STUDENT">Chỉ học sinh</option></select></div>}
        {scope === 'TEACHERS' && <><div className="form-group"><label>Nhóm giáo viên</label><select value={teacherAudience} onChange={e => setTeacherAudience(e.target.value as typeof teacherAudience)}><option value="ALL">Toàn bộ giáo viên</option><option value="SUBJECT">Giáo viên theo môn</option><option value="HOMEROOM">Giáo viên chủ nhiệm</option></select></div>{teacherAudience === 'SUBJECT' && <div className="form-group"><label>Môn phụ trách</label><select required value={subjectId} onChange={e => setSubjectId(e.target.value)}><option value="">Chọn môn</option>{subjects.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</select></div>}</>}
      </div>
      {scope === 'CLASSES' && <fieldset className="announcement-class-picker"><legend>Chọn lớp nhận thông báo</legend><div>{classes.map(item => <label key={item.id} className={classIds.includes(item.id) ? 'selected' : ''}><input type="checkbox" checked={classIds.includes(item.id)} onChange={() => setClassIds(current => current.includes(item.id) ? current.filter(id => id !== item.id) : [...current, item.id])}/><span>{item.name}</span></label>)}</div></fieldset>}
      <div className="form-group"><label>Tiêu đề</label><input value={title} onChange={e => setTitle(e.target.value)} required /></div><div className="form-group"><label>Nội dung</label><textarea value={body} onChange={e => setBody(e.target.value)} rows={4} required /></div>
      <div className="form-actions"><button disabled={busy || !selectedYearId}>{busy ? 'Đang gửi...' : 'Gửi thông báo'}</button></div>
    </form>
    <div className="filters"><div className="form-group"><label>Trạng thái</label><select value={status} onChange={e => setStatus(e.target.value)}><option value="">Tất cả</option><option value="PENDING">Chờ duyệt</option><option value="APPROVED">Đã duyệt</option><option value="REJECTED">Từ chối</option></select></div></div>
    <div className="table-responsive"><table><thead><tr><th>Người gửi</th><th>Tiêu đề & nội dung</th><th>Người nhận</th><th>Thời gian</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>
      {items.map(item => <tr key={item.id}><td><strong>{item.teacherName}</strong><br/><small>{item.senderType === 'ADMIN' ? 'Quản trị viên' : item.senderType === 'HOMEROOM_TEACHER' ? 'GVCN' : 'GV bộ môn'}</small></td><td><strong>{item.title}</strong><br/><small>{item.body}</small>{item.rejectionReason && <div className="reject-reason">Lý do: {item.rejectionReason}</div>}</td><td>{recipientLabel(item)}</td><td>{new Date(item.createdAt).toLocaleString('vi-VN')}</td><td><span className={`badge-status ${item.approvalStatus === 'APPROVED' ? 'completed' : item.approvalStatus === 'PENDING' ? 'scheduled' : ''}`}>{item.approvalStatus}</span></td><td><div className="table-actions">{item.approvalStatus === 'PENDING' && <><button onClick={() => review(item, true)}>Duyệt</button><button className="danger" onClick={() => review(item, false)}>Từ chối</button></>}<button className="secondary-button" onClick={async () => { if (confirm('Xóa thông báo này?')) { await deleteAnnouncement(item.id); await load(); } }}>Xóa</button></div></td></tr>)}
      {!items.length && <tr><td colSpan={6}>Chưa có thông báo trong bộ lọc này.</td></tr>}
    </tbody></table></div>
  </div>;
}
