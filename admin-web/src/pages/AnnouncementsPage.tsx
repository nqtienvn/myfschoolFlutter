import { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import {
  AnnouncementDeliveryStatus,
  AnnouncementItem,
  AnnouncementPolicyMatchType,
  AnnouncementPolicyScope,
  AnnouncementSummary,
  broadcastAnnouncement,
  deleteAnnouncement,
  getAnnouncementPolicy,
  getAnnouncements,
  getAnnouncementSummary,
  updateAnnouncementPolicy,
} from '../api/announcement';
import AttendanceCorrectionInbox from '../components/AttendanceCorrectionInbox';

type TabKey = 'list' | 'attendance-corrections' | 'policy';

interface AnnouncementsPageProps {
  selectedYearId: string;
  pendingAttendanceCorrectionCount: number;
  onPendingAttendanceCorrectionCountChange: (count: number) => void;
}

interface EditableRule {
  key: string;
  id?: number;
  phrase: string;
  scope: AnnouncementPolicyScope;
  matchType: AnnouncementPolicyMatchType;
}

const EMPTY_SUMMARY: AnnouncementSummary = { total: 0, published: 0, systemRejected: 0 };

function newRule(rule?: Partial<EditableRule>): EditableRule {
  return {
    key: `${Date.now()}-${Math.random()}`,
    phrase: '',
    scope: 'ALL',
    matchType: 'CONTAINS',
    ...rule,
  };
}

export default function AnnouncementsPage({
  selectedYearId,
  pendingAttendanceCorrectionCount,
  onPendingAttendanceCorrectionCountChange,
}: AnnouncementsPageProps) {
  const [activeTab, setActiveTab] = useState<TabKey>('list');
  const [items, setItems] = useState<AnnouncementItem[]>([]);
  const [summary, setSummary] = useState<AnnouncementSummary>(EMPTY_SUMMARY);
  const [status, setStatus] = useState<AnnouncementDeliveryStatus | ''>('');
  const [keywordDraft, setKeywordDraft] = useState('');
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [busy, setBusy] = useState(false);
  const [policyEnabled, setPolicyEnabled] = useState(true);
  const [rejectionMessage, setRejectionMessage] = useState('');
  const [rules, setRules] = useState<EditableRule[]>([newRule()]);
  const [policyLoading, setPolicyLoading] = useState(false);
  const [policySaving, setPolicySaving] = useState(false);
  const [policyLoaded, setPolicyLoaded] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const listRequestId = useRef(0);

  useEffect(() => {
    setActiveTab('list');
    setItems([]);
    setSummary(EMPTY_SUMMARY);
    setStatus('');
    setKeywordDraft('');
    setKeyword('');
    setPage(0);
    setPageSize(20);
    setTotalElements(0);
    setTotalPages(0);
    setShowForm(false);
    setTitle('');
    setBody('');
    setPolicyEnabled(true);
    setRejectionMessage('');
    setRules([newRule()]);
    setPolicyLoaded(false);
    setError('');
    setMessage('');
  }, [selectedYearId]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setKeyword(keywordDraft.trim());
      setPage(0);
    }, 400);
    return () => window.clearTimeout(timer);
  }, [keywordDraft]);

  async function loadList() {
    const requestId = ++listRequestId.current;
    if (!selectedYearId) {
      setItems([]);
      return;
    }
    setLoading(true);
    setError('');
    try {
      const result = await getAnnouncements({
        academicYearId: selectedYearId,
        status,
        keyword,
        page,
        size: pageSize,
      });
      if (requestId !== listRequestId.current) return;
      setItems(result.content || []);
      setTotalElements(result.totalElements || 0);
      setTotalPages(result.totalPages || 0);
      if (page > 0 && result.totalPages > 0 && page >= result.totalPages) {
        setPage(result.totalPages - 1);
      }
    } catch (cause: any) {
      if (requestId !== listRequestId.current) return;
      setError(cause.message || 'Không thể tải danh sách thông báo.');
    } finally {
      if (requestId === listRequestId.current) setLoading(false);
    }
  }

  async function loadSummary() {
    if (!selectedYearId) {
      setSummary(EMPTY_SUMMARY);
      return;
    }
    try {
      setSummary(await getAnnouncementSummary(selectedYearId));
    } catch (cause: any) {
      setError(cause.message || 'Không thể tải thống kê thông báo.');
    }
  }

  useEffect(() => {
    if (activeTab === 'list') void loadList();
  }, [selectedYearId, activeTab, status, keyword, page, pageSize]);

  useEffect(() => {
    if (activeTab === 'list') void loadSummary();
  }, [selectedYearId, activeTab]);

  useEffect(() => {
    if (activeTab !== 'policy' || !selectedYearId || policyLoaded) return;
    let alive = true;
    setPolicyLoading(true);
    setError('');
    getAnnouncementPolicy(selectedYearId)
      .then(policy => {
        if (!alive) return;
        setPolicyEnabled(policy.enabled);
        setRejectionMessage(policy.rejectionMessage);
        setRules(policy.rules.length
          ? policy.rules.map(rule => newRule(rule))
          : [newRule()]);
        setPolicyLoaded(true);
      })
      .catch((cause: any) => alive && setError(cause.message || 'Không thể tải cấu hình.'))
      .finally(() => alive && setPolicyLoading(false));
    return () => { alive = false; };
  }, [activeTab, selectedYearId, policyLoaded]);

  async function submitBroadcast(event: FormEvent) {
    event.preventDefault();
    if (!selectedYearId) return;
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
      setStatus('');
      setKeywordDraft('');
      setKeyword('');
      setPage(0);
      setMessage('Đã gửi thông báo đến toàn bộ tài khoản không phải quản trị viên.');
      await Promise.all([loadList(), loadSummary()]);
    } catch (cause: any) {
      setError(cause.message || 'Không thể gửi thông báo.');
    } finally {
      setBusy(false);
    }
  }

  async function savePolicy(event: FormEvent) {
    event.preventDefault();
    if (!selectedYearId) return;
    const normalizedRules = rules.map(rule => ({ ...rule, phrase: rule.phrase.trim() }));
    if (normalizedRules.some(rule => !rule.phrase)) {
      setError('Vui lòng nhập đầy đủ từng câu từ hoặc xóa dòng không sử dụng.');
      return;
    }
    setPolicySaving(true);
    setError('');
    setMessage('');
    try {
      const saved = await updateAnnouncementPolicy({
        academicYearId: Number(selectedYearId),
        enabled: policyEnabled,
        rejectionMessage: rejectionMessage.trim(),
        rules: normalizedRules.map(({ phrase, scope, matchType }) => ({ phrase, scope, matchType })),
      });
      setPolicyEnabled(saved.enabled);
      setRejectionMessage(saved.rejectionMessage);
      setRules(saved.rules.length ? saved.rules.map(rule => newRule(rule)) : [newRule()]);
      setMessage('Đã lưu chính sách kiểm tra nội dung cho năm học đang chọn.');
    } catch (cause: any) {
      setError(cause.message || 'Không thể lưu chính sách.');
    } finally {
      setPolicySaving(false);
    }
  }

  function updateRule(key: string, patch: Partial<EditableRule>) {
    setRules(current => current.map(rule => rule.key === key ? { ...rule, ...patch } : rule));
  }

  function removeRule(key: string) {
    setRules(current => current.length === 1 ? [newRule()] : current.filter(rule => rule.key !== key));
  }

  const firstItem = totalElements === 0 ? 0 : page * pageSize + 1;
  const lastItem = Math.min((page + 1) * pageSize, totalElements);
  const pageNumbers = useMemo(() => {
    const start = Math.max(0, Math.min(page - 2, totalPages - 5));
    return Array.from({ length: Math.min(5, totalPages) }, (_, index) => start + index);
  }, [page, totalPages]);

  const recipientLabel = (item: AnnouncementItem) => item.recipientScope === 'SCHOOL'
    ? 'Toàn bộ tài khoản không phải quản trị viên'
    : `${item.classNames.join(', ')} · ${item.targetRole === 'PARENT'
      ? 'Phụ huynh'
      : item.targetRole === 'STUDENT' ? 'Học sinh' : 'Phụ huynh & học sinh'}`;

  return <div className="page-stack announcement-admin">
    <div className="page-heading">
      <div>
        <span className="eyebrow">Trung tâm thông báo</span>
        <h1>Quản lý thông báo</h1>
        <p>Quản lý thông báo và các yêu cầu nghiệp vụ cần Admin xử lý trong năm học.</p>
      </div>
      {activeTab === 'list' && <div className="page-heading-actions">
        <button type="button" disabled={!selectedYearId} onClick={() => setShowForm(value => !value)}>
          {showForm ? '× Đóng' : '+ Gửi thông báo toàn trường'}
        </button>
      </div>}
    </div>

    <div className="announcement-tabs" role="tablist" aria-label="Quản lý thông báo">
      <button type="button" role="tab" aria-selected={activeTab === 'list'}
        className={activeTab === 'list' ? 'active' : ''} onClick={() => { setActiveTab('list'); setError(''); setMessage(''); }}>
        Danh sách thông báo
      </button>
      <button type="button" role="tab" aria-selected={activeTab === 'attendance-corrections'}
        className={activeTab === 'attendance-corrections' ? 'active' : ''}
        onClick={() => { setActiveTab('attendance-corrections'); setError(''); setMessage(''); }}>
        Yêu cầu xử lý
        {pendingAttendanceCorrectionCount > 0 && (
          <span className="notification-count-badge">{pendingAttendanceCorrectionCount}</span>
        )}
      </button>
      <button type="button" role="tab" aria-selected={activeTab === 'policy'}
        className={activeTab === 'policy' ? 'active' : ''} onClick={() => { setActiveTab('policy'); setError(''); setMessage(''); }}>
        Cấu hình chính sách
      </button>
    </div>

    {!selectedYearId && <div className="notice info">Hãy chọn năm học để quản lý thông báo.</div>}
    {error && <div className="notice error" role="alert">{error}</div>}
    {message && <div className="notice success" role="status">{message}</div>}

    {activeTab === 'attendance-corrections' && selectedYearId && (
      <AttendanceCorrectionInbox
        selectedYearId={selectedYearId}
        onPendingCountChange={onPendingAttendanceCorrectionCountChange}
      />
    )}

    {activeTab === 'list' && <>
      {showForm && <form className="announcement-compose" onSubmit={submitBroadcast}>
        <h2>Gửi thông báo từ nhà trường</h2>
        <div className="notice info">
          Thông báo được phát hành ngay đến mọi tài khoản phụ huynh, học sinh và giáo viên, đồng thời xuất hiện trong danh sách quản lý.
        </div>
        <div className="form-group"><label>Tiêu đề</label>
          <input value={title} onChange={event => setTitle(event.target.value)} maxLength={500} required />
        </div>
        <div className="form-group"><label>Nội dung</label>
          <textarea value={body} onChange={event => setBody(event.target.value)} rows={4} required />
        </div>
        <div className="form-actions">
          <button disabled={busy || !selectedYearId}>{busy ? 'Đang gửi...' : 'Gửi thông báo'}</button>
          <button type="button" className="secondary-button" onClick={() => setShowForm(false)}>Đóng</button>
        </div>
      </form>}

      <div className="announcement-summary-grid">
        <article><span>Tất cả</span><strong>{summary.total}</strong></article>
        <article className="published"><span>Gửi thành công</span><strong>{summary.published}</strong></article>
        <article className="rejected"><span>Hệ thống từ chối</span><strong>{summary.systemRejected}</strong></article>
      </div>

      <div className="filters announcement-filters">
        <div className="form-group"><label>Tìm kiếm</label>
          <input value={keywordDraft} onChange={event => setKeywordDraft(event.target.value)}
            placeholder="Tiêu đề, nội dung hoặc người gửi" />
        </div>
        <div className="form-group"><label>Trạng thái</label>
          <select value={status} onChange={event => { setStatus(event.target.value as AnnouncementDeliveryStatus | ''); setPage(0); }}>
            <option value="">Tất cả</option>
            <option value="PUBLISHED">Gửi thành công</option>
            <option value="SYSTEM_REJECTED">Hệ thống từ chối</option>
          </select>
        </div>
      </div>

      <div className="teacher-table-toolbar">
        <span>Hiển thị {firstItem}–{lastItem} trong {totalElements} thông báo</span>
        <label>Số dòng <select value={pageSize} onChange={event => { setPageSize(Number(event.target.value)); setPage(0); }}>
          <option value={20}>20</option><option value={50}>50</option><option value={100}>100</option>
        </select></label>
      </div>

      <div className="table-responsive"><table className="announcement-table">
        <thead><tr><th>Người gửi</th><th>Tiêu đề & nội dung</th><th>Phạm vi</th><th>Thời gian</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
        <tbody>
          {items.map(item => <tr key={item.id}>
            <td><strong>{item.teacherName}</strong><br /><small>{item.senderType === 'ADMIN'
              ? 'Quản trị viên' : item.senderType === 'HOMEROOM_TEACHER' ? 'GVCN' : 'GV bộ môn'}</small></td>
            <td><strong>{item.title}</strong><br /><small className="announcement-body-preview">{item.body}</small>
              {item.systemRejectionMessage && <div className="reject-reason">{item.systemRejectionMessage}</div>}
              {!!item.violations?.length && <div className="violation-list">
                {item.violations.map((violation, index) => <span key={`${violation.ruleId}-${violation.field}-${index}`}>
                  {violation.field === 'TITLE' ? 'Tiêu đề' : 'Nội dung'}: “{violation.phrase}”
                </span>)}
              </div>}
            </td>
            <td>{recipientLabel(item)}</td>
            <td>{new Date(item.createdAt).toLocaleString('vi-VN')}</td>
            <td><span className={`badge-status ${item.deliveryStatus === 'PUBLISHED' ? 'completed' : 'system-rejected'}`}>
              {item.deliveryStatus === 'PUBLISHED' ? 'Gửi thành công' : 'Hệ thống từ chối'}
            </span></td>
            <td><button type="button" className="secondary-button" onClick={async () => {
              if (!confirm('Xóa thông báo này?')) return;
              try { await deleteAnnouncement(item.id); await Promise.all([loadList(), loadSummary()]); }
              catch (cause: any) { setError(cause.message || 'Không thể xóa thông báo.'); }
            }}>Xóa</button></td>
          </tr>)}
          {!items.length && !loading && <tr><td colSpan={6}>Chưa có thông báo phù hợp.</td></tr>}
          {loading && <tr><td colSpan={6}>Đang tải danh sách thông báo…</td></tr>}
        </tbody>
      </table></div>

      <div className="teacher-pagination announcement-pagination" aria-label="Phân trang thông báo">
        <button type="button" className="secondary-button" disabled={page === 0 || loading} onClick={() => setPage(value => value - 1)}>Trang trước</button>
        <div className="announcement-page-numbers">
          {pageNumbers.map(pageNumber => <button type="button" key={pageNumber}
            className={pageNumber === page ? 'active' : 'secondary-button'}
            onClick={() => setPage(pageNumber)} disabled={loading}>{pageNumber + 1}</button>)}
          {totalPages === 0 && <span>0</span>}
        </div>
        <button type="button" className="secondary-button" disabled={page + 1 >= totalPages || loading} onClick={() => setPage(value => value + 1)}>Trang sau</button>
      </div>
    </>}

    {activeTab === 'policy' && <form className="announcement-policy-card" onSubmit={savePolicy}>
      <div className="policy-heading">
        <div><h2>Chính sách câu từ không hợp lệ</h2>
          <p>Áp dụng riêng cho năm học đang chọn. Mỗi ô là một từ hoặc một câu cần kiểm tra độc lập.</p></div>
        <label className="policy-switch"><input type="checkbox" checked={policyEnabled}
          onChange={event => setPolicyEnabled(event.target.checked)} /> Bật kiểm tra tự động</label>
      </div>

      {policyLoading ? <div className="notice info">Đang tải cấu hình…</div> : <>
        <div className="form-group"><label>Thông báo trả về cho giáo viên</label>
          <textarea value={rejectionMessage} onChange={event => setRejectionMessage(event.target.value)}
            maxLength={500} rows={3} required />
        </div>

        <div className="policy-rules-heading">
          <div><strong>Danh sách câu từ</strong><small>{rules.length} quy tắc</small></div>
          <button type="button" onClick={() => setRules(current => [...current, newRule()])}>+ Thêm câu từ</button>
        </div>

        <div className="policy-rule-list">
          {rules.map((rule, index) => <div className="policy-rule-row" key={rule.key}>
            <span className="policy-rule-index">{index + 1}</span>
            <div className="form-group"><label>Câu từ không hợp lệ</label>
              <input value={rule.phrase} onChange={event => updateRule(rule.key, { phrase: event.target.value })}
                placeholder="Ví dụ: mua ngay, nội dung quảng cáo…" maxLength={250} required />
            </div>
            <div className="form-group"><label>Kiểm tra tại</label>
              <select value={rule.scope} onChange={event => updateRule(rule.key, { scope: event.target.value as AnnouncementPolicyScope })}>
                <option value="ALL">Tiêu đề & nội dung</option><option value="TITLE">Chỉ tiêu đề</option><option value="BODY">Chỉ nội dung</option>
              </select>
            </div>
            <div className="form-group"><label>Cách khớp</label>
              <select value={rule.matchType} onChange={event => updateRule(rule.key, { matchType: event.target.value as AnnouncementPolicyMatchType })}>
                <option value="CONTAINS">Có chứa câu từ</option><option value="EXACT">Khớp toàn bộ</option>
              </select>
            </div>
            <button type="button" className="policy-remove-rule" aria-label={`Xóa quy tắc ${index + 1}`} onClick={() => removeRule(rule.key)}>×</button>
          </div>)}
        </div>

        <div className="notice info policy-normalization-note">
          Hệ thống tự chuẩn hóa chữ hoa/thường, ký tự ẩn, dấu câu và khoảng trắng; vẫn giữ nguyên dấu tiếng Việt và chỉ khớp theo ranh giới từ.
        </div>
        <div className="form-actions"><button disabled={policySaving || !selectedYearId}>
          {policySaving ? 'Đang lưu...' : 'Lưu cấu hình'}
        </button></div>
      </>}
    </form>}
  </div>;
}
