import { useEffect, useMemo, useState } from 'react';
import { getClasses } from '../api/class';
import {
  getPeriodicReports,
  reopenPeriodicReport,
  type PeriodicReportItem,
  type PeriodicReportStatus,
} from '../api/periodicReview';

interface ClassItem { id: number; name: string }

export default function PeriodicReviewsPage({
  selectedYearId,
  selectedSemesterId,
}: {
  selectedYearId?: string;
  selectedSemesterId?: string;
}) {
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [classId, setClassId] = useState('');
  const [status, setStatus] = useState<'' | PeriodicReportStatus>('');
  const [rows, setRows] = useState<PeriodicReportItem[]>([]);
  const [selected, setSelected] = useState<PeriodicReportItem | null>(null);
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setClasses([]);
    setClassId('');
    setStatus('');
    setRows([]);
    setSelected(null);
    setReason('');
    setError('');
    if (!selectedYearId) return;
    getClasses({ academicYearId: selectedYearId, size: 200 })
      .then((data) => setClasses((data || []) as ClassItem[]))
      .catch((err) => setError(err.message || 'Không thể tải danh sách lớp.'));
  }, [selectedYearId]);

  useEffect(() => {
    setRows([]);
    setSelected(null);
    setReason('');
    if (!selectedYearId || !selectedSemesterId) return;
    setLoading(true);
    setError('');
    getPeriodicReports({
      academicYearId: selectedYearId,
      semesterId: selectedSemesterId,
      classId: classId || undefined,
      status: status || undefined,
    })
      .then((data) => setRows(data || []))
      .catch((err) => setError(err.message || 'Không thể tải tiến độ nhận xét.'))
      .finally(() => setLoading(false));
  }, [selectedYearId, selectedSemesterId, classId, status]);

  const stats = useMemo(() => ({
    total: rows.length,
    published: rows.filter((item) => item.status === 'PUBLISHED').length,
    complete: rows.filter((item) => item.totalSubjects > 0 && item.submittedSubjects === item.totalSubjects).length,
  }), [rows]);

  async function reopen() {
    if (!selectedYearId || !selected?.id || !reason.trim()) return;
    setLoading(true);
    setError('');
    try {
      const updated = await reopenPeriodicReport(selected.id, selectedYearId, reason.trim());
      setRows((current) => current.map((item) => item.studentId === updated.studentId ? updated : item));
      setSelected(updated);
      setReason('');
    } catch (err: any) {
      setError(err.message || 'Không thể mở lại báo cáo.');
    } finally {
      setLoading(false);
    }
  }

  if (!selectedYearId || !selectedSemesterId) {
    return <div className="empty-state">Chọn năm học và học kỳ để theo dõi nhận xét định kỳ.</div>;
  }

  return (
    <main className="page-stack" aria-label="Theo dõi nhận xét định kỳ">
      <div className="page-heading">
        <div><h1>Nhận xét định kỳ</h1><p>Theo dõi luồng GVBM → GVCN → công bố cho phụ huynh và học sinh.</p></div>
      </div>

      {error && <div className="alert error" role="alert">{error}</div>}

      <div className="summary-cards review-summary">
        <div className="summary-card"><span>Học sinh</span><strong>{stats.total}</strong></div>
        <div className="summary-card"><span>Đủ nhận xét môn</span><strong>{stats.complete}</strong></div>
        <div className="summary-card"><span>Đã công bố</span><strong>{stats.published}</strong></div>
      </div>

      <section className="panel">
        <div className="filter-row">
          <label>Lớp<select value={classId} onChange={(event) => setClassId(event.target.value)}>
            <option value="">Tất cả lớp</option>
            {classes.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
          </select></label>
          <label>Trạng thái<select value={status} onChange={(event) => setStatus(event.target.value as '' | PeriodicReportStatus)}>
            <option value="">Tất cả</option><option value="DRAFT">Bản nháp</option><option value="PUBLISHED">Đã công bố</option>
          </select></label>
        </div>
        {loading && <div className="loading-state">Đang tải dữ liệu…</div>}
        {!loading && <div className="table-responsive"><table>
          <thead><tr><th>Lớp</th><th>Học sinh</th><th>Tiến độ môn</th><th>Hạnh kiểm</th><th>Trạng thái</th><th /></tr></thead>
          <tbody>{rows.map((item) => <tr key={`${item.classId}-${item.studentId}`}>
            <td>{item.className}</td><td><strong>{item.studentName}</strong><small className="table-subtext">{item.studentCode}</small></td>
            <td>{item.submittedSubjects}/{item.totalSubjects}{item.missingSubjects.length > 0 && <small className="table-subtext">Thiếu: {item.missingSubjects.join(', ')}</small>}</td>
            <td>{item.conduct || item.suggestedConduct || '—'}</td>
            <td><span className={`badge-status ${item.status === 'PUBLISHED' ? 'active' : 'draft'}`}>{item.status === 'PUBLISHED' ? 'Đã công bố' : 'Bản nháp'}</span></td>
            <td><button className="btn btn-secondary" onClick={() => { setSelected(item); setReason(''); }}>Xem</button></td>
          </tr>)}</tbody>
        </table>{rows.length === 0 && <div className="empty-state">Chưa có học sinh phù hợp bộ lọc.</div>}</div>}
      </section>

      {selected && <div className="modal-backdrop" role="presentation" onMouseDown={() => setSelected(null)}>
        <section className="modal-card review-detail" role="dialog" aria-modal="true" aria-label={`Báo cáo ${selected.studentName}`} onMouseDown={(event) => event.stopPropagation()}>
          <div className="modal-header"><div><h2>{selected.studentName}</h2><p>{selected.studentCode} · {selected.className}</p></div><button className="icon-button" onClick={() => setSelected(null)} aria-label="Đóng">×</button></div>
          <div className="review-general"><strong>Nhận xét GVCN</strong><p>{selected.generalComment || 'Chưa nhập'}</p><span>Hạnh kiểm: <b>{selected.conduct || 'Chưa nhập'}</b> · Gợi ý: {selected.suggestedConduct || 'Chưa có'}</span></div>
          <div className="review-subject-list">{selected.subjectReviews.map((review) => <article key={review.subjectName}>
            <header><strong>{review.subjectName}</strong><span className={`badge-status ${review.status === 'SUBMITTED' ? 'active' : 'draft'}`}>{review.status}</span></header>
            <p>{review.comment || 'Chưa gửi nhận xét'}</p><small>{review.subjectTeacherName}</small>
          </article>)}</div>
          {selected.status === 'PUBLISHED' && <div className="review-reopen"><label>Lý do mở lại<textarea value={reason} maxLength={500} onChange={(event) => setReason(event.target.value)} /></label><button className="btn btn-danger" disabled={!reason.trim() || loading} onClick={reopen}>Mở lại báo cáo</button></div>}
        </section>
      </div>}
    </main>
  );
}
