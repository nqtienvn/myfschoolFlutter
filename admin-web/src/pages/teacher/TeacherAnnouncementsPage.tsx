import { useEffect, useState, type FormEvent } from 'react';
import {
  deleteTeacherAnnouncement,
  getEligibleAnnouncementClasses,
  getReceivedAnnouncements,
  getSentAnnouncements,
  markAnnouncementRead,
  saveTeacherAnnouncement,
  type TeacherAnnouncementSubmissionResult,
} from '../../api/teacher';
import { useTeacherAcademic } from '../../teacher/TeacherAcademicContext';

interface AnnouncementViolation {
  ruleId?: number;
  field: 'TITLE' | 'BODY';
  phrase: string;
}

interface Announcement {
  id: number;
  title: string;
  body: string;
  targetRole: string;
  classIds?: number[];
  classNames?: string[];
  deliveryStatus: 'PUBLISHED' | 'SYSTEM_REJECTED';
  systemRejectionMessage?: string;
  teacherName?: string;
  senderName?: string;
  createdAt: string;
  isRead?: boolean;
  violations?: AnnouncementViolation[];
}

const EMPTY_DRAFT = { title: '', body: '', targetRole: 'ALL', classIds: [] as number[] };

export default function TeacherAnnouncementsPage() {
  const { selectedYearId } = useTeacherAcademic();
  const [classes, setClasses] = useState<any[]>([]);
  const [received, setReceived] = useState<Announcement[]>([]);
  const [sent, setSent] = useState<Announcement[]>([]);
  const [selected, setSelected] = useState<Announcement | null>(null);
  const [retryOfAnnouncementId, setRetryOfAnnouncementId] = useState<number | null>(null);
  const [draft, setDraft] = useState(EMPTY_DRAFT);
  const [submissionResult, setSubmissionResult] = useState<TeacherAnnouncementSubmissionResult | null>(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  async function load() {
    if (!selectedYearId) return;
    setError('');
    try {
      const [classRows, inboxRows, sentRows] = await Promise.all([
        getEligibleAnnouncementClasses(selectedYearId),
        getReceivedAnnouncements(selectedYearId),
        getSentAnnouncements(selectedYearId),
      ]);
      setClasses((classRows || []) as any[]);
      setReceived((inboxRows || []) as Announcement[]);
      setSent((sentRows || []) as Announcement[]);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không tải được thông báo.');
    }
  }

  useEffect(() => {
    setClasses([]);
    setReceived([]);
    setSent([]);
    setSelected(null);
    setRetryOfAnnouncementId(null);
    setDraft(EMPTY_DRAFT);
    setSubmissionResult(null);
    setError('');
    setMessage('');
    void load();
  }, [selectedYearId]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!selectedYearId || !draft.classIds.length) return;
    setBusy(true);
    setError('');
    setMessage('');
    setSubmissionResult(null);
    try {
      const result = await saveTeacherAnnouncement({
        ...draft,
        academicYearId: selectedYearId,
        retryOfAnnouncementId,
      });
      setSubmissionResult(result);
      if (result.outcome === 'PUBLISHED') {
        setRetryOfAnnouncementId(null);
        setDraft(EMPTY_DRAFT);
        setMessage(result.message);
      } else {
        setRetryOfAnnouncementId(result.announcement.id);
      }
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không gửi được thông báo.');
    } finally {
      setBusy(false);
    }
  }

  async function openReceived(item: Announcement) {
    try {
      if (!item.isRead) await markAnnouncementRead(item.id);
      setSelected(item);
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không mở được thông báo.');
    }
  }

  function editAndRetry(item: Announcement) {
    setRetryOfAnnouncementId(item.id);
    setDraft({
      title: item.title,
      body: item.body,
      targetRole: item.targetRole,
      classIds: item.classIds || [],
    });
    setSubmissionResult(null);
    setMessage('Đã nạp nội dung bị từ chối. Hãy chỉnh sửa rồi gửi lại.');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  return <main className="teacher-page page-stack">
    <div className="page-heading"><div><span className="eyebrow">Kết nối nhà trường</span>
      <h1>Thông báo</h1><p>Thông báo lớp được kiểm tra chính sách và gửi ngay khi nội dung hợp lệ.</p></div></div>
    {error && <div className="notice error">{error}</div>}
    {message && <div className="notice success">{message}</div>}
    {busy && <div className="notice info teacher-announcement-checking" role="status">
      <span className="announcement-spinner" aria-hidden="true" />
      <strong>Đang kiểm tra nội dung thông báo…</strong>
    </div>}

    <div className="teacher-announcement-grid">
      <section className="panel"><h2>Thông báo đã nhận</h2>
        <div className="teacher-inbox-list">{received.map(item => <button key={item.id}
          className={item.isRead ? '' : 'unread'} onClick={() => openReceived(item)}>
          <span><strong>{item.title}</strong><small>{item.teacherName || item.senderName || 'Nhà trường'} · {new Date(item.createdAt).toLocaleString('vi-VN')}</small></span>
          {!item.isRead && <b>Mới</b>}
        </button>)}</div>
        {!received.length && <div className="empty-state">Chưa có thông báo từ Nhà trường/Admin.</div>}
      </section>

      <section className="panel"><h2>{retryOfAnnouncementId ? 'Sửa và gửi lại thông báo' : 'Gửi thông báo lớp'}</h2>
        {retryOfAnnouncementId && <div className="notice warning">Bản bị từ chối vẫn được giữ trong lịch sử.</div>}
        <form className="teacher-announcement-form" onSubmit={submit}>
          <label>Tiêu đề<input required maxLength={500} value={draft.title}
            onChange={event => setDraft(current => ({ ...current, title: event.target.value }))} /></label>
          <label>Nội dung<textarea required rows={6} value={draft.body}
            onChange={event => setDraft(current => ({ ...current, body: event.target.value }))} /></label>
          <label>Người nhận<select value={draft.targetRole}
            onChange={event => setDraft(current => ({ ...current, targetRole: event.target.value }))}>
            <option value="ALL">Phụ huynh & học sinh</option><option value="PARENT">Phụ huynh</option><option value="STUDENT">Học sinh</option>
          </select></label>
          <fieldset><legend>Lớp được phân công</legend>{classes.map(item => <label className="check-row" key={item.id}>
            <input type="checkbox" checked={draft.classIds.includes(item.id)}
              onChange={event => setDraft(current => ({ ...current, classIds: event.target.checked
                ? [...current.classIds, item.id] : current.classIds.filter(id => id !== item.id) }))} />
            <span>{item.name}{item.isHomeroom ? ' · GVCN' : ''}</span>
          </label>)}</fieldset>
          <div className="monitoring-actions"><button disabled={busy || !draft.classIds.length}>
            {busy ? 'Đang kiểm tra nội dung…' : retryOfAnnouncementId ? 'Gửi lại thông báo' : 'Gửi thông báo'}
          </button>{retryOfAnnouncementId && <button type="button" className="secondary-button" onClick={() => {
            setRetryOfAnnouncementId(null); setDraft(EMPTY_DRAFT); setSubmissionResult(null);
          }}>Hủy gửi lại</button>}</div>
        </form>
      </section>
    </div>

    <section className="panel"><h2>Lịch sử đã gửi</h2><div className="table-responsive"><table>
      <thead><tr><th>Thông báo</th><th>Lớp</th><th>Kết quả</th><th>Ngày gửi</th><th /></tr></thead>
      <tbody>{sent.map(item => <tr key={item.id}>
        <td><strong>{item.title}</strong><small className="table-subtext">{item.body}</small>
          {item.systemRejectionMessage && <small className="text-danger">{item.systemRejectionMessage}</small>}
          {!!item.violations?.length && <div className="violation-list">{item.violations.map((violation, index) =>
            <span key={`${violation.field}-${violation.ruleId}-${index}`}>{violation.field === 'TITLE' ? 'Tiêu đề' : 'Nội dung'}: “{violation.phrase}”</span>)}</div>}
        </td>
        <td>{item.classNames?.join(', ') || '—'}</td>
        <td><span className={`badge-status ${item.deliveryStatus === 'PUBLISHED' ? 'completed' : 'system-rejected'}`}>
          {item.deliveryStatus === 'PUBLISHED' ? 'Gửi thành công' : 'Hệ thống từ chối'}
        </span></td>
        <td>{new Date(item.createdAt).toLocaleString('vi-VN')}</td>
        <td><div className="table-actions">{item.deliveryStatus === 'SYSTEM_REJECTED' &&
          <button className="secondary-button" onClick={() => editAndRetry(item)}>Sửa và gửi lại</button>}
          <button className="danger" onClick={async () => { if (confirm('Xóa thông báo này?')) {
            await deleteTeacherAnnouncement(item.id); await load();
          } }}>Xóa</button></div></td>
      </tr>)}
      {!sent.length && <tr><td colSpan={5}>Chưa có thông báo đã gửi.</td></tr>}
      </tbody></table></div></section>

    {submissionResult && <div className="modal-backdrop" onMouseDown={() => setSubmissionResult(null)}>
      <section className={`modal-card announcement-result-modal ${submissionResult.outcome === 'PUBLISHED' ? 'published' : 'rejected'}`}
        onMouseDown={event => event.stopPropagation()}>
        <div className="announcement-result-icon">{submissionResult.outcome === 'PUBLISHED' ? '✓' : '!'}</div>
        <h2>{submissionResult.outcome === 'PUBLISHED' ? 'Gửi thông báo thành công' : 'Thông báo bị hệ thống từ chối'}</h2>
        <p>{submissionResult.message}</p>
        {!!submissionResult.violations?.length && <div className="violation-list">{submissionResult.violations.map((violation, index) =>
          <span key={`${violation.field}-${violation.ruleId}-${index}`}>{violation.field === 'TITLE' ? 'Tiêu đề' : 'Nội dung'}: “{violation.phrase}”</span>)}</div>}
        <button onClick={() => setSubmissionResult(null)}>{submissionResult.outcome === 'PUBLISHED' ? 'Đóng' : 'Quay lại chỉnh sửa'}</button>
      </section>
    </div>}

    {selected && <div className="modal-backdrop" onMouseDown={() => setSelected(null)}>
      <section className="modal-card review-detail" onMouseDown={event => event.stopPropagation()}>
        <div className="modal-header"><div><h2>{selected.title}</h2><p>{selected.teacherName || selected.senderName || 'Nhà trường'}</p></div>
          <button className="icon-button" onClick={() => setSelected(null)}>×</button></div>
        <p className="announcement-detail-body">{selected.body}</p>
      </section>
    </div>}
  </main>;
}
