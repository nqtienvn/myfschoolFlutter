import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  getAttendanceCorrections,
  reviewAttendanceCorrection,
  type AttendanceCorrectionRequest,
  type AttendanceCorrectionStatus,
} from '../api/attendance';
import { filterAttendanceCorrections } from '../utils/attendanceCorrections';

interface AttendanceCorrectionInboxProps {
  selectedYearId: string;
  onPendingCountChange: (count: number) => void;
}

const statusLabels: Record<AttendanceCorrectionStatus, string> = {
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Đã từ chối',
};

const attendanceLabels: Record<string, string> = {
  PRESENT: 'Có mặt',
  ABSENT_WITH_LEAVE: 'Vắng có phép',
  ABSENT_WITHOUT_LEAVE: 'Vắng không phép',
};

export default function AttendanceCorrectionInbox({
  selectedYearId,
  onPendingCountChange,
}: AttendanceCorrectionInboxProps) {
  const [items, setItems] = useState<AttendanceCorrectionRequest[]>([]);
  const [status, setStatus] = useState<AttendanceCorrectionStatus | ''>('PENDING');
  const [date, setDate] = useState('');
  const [classId, setClassId] = useState('');
  const [teacherId, setTeacherId] = useState('');
  const [loading, setLoading] = useState(false);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const requestId = useRef(0);

  const loadCorrections = useCallback(async () => {
    const currentRequest = ++requestId.current;
    if (!selectedYearId) {
      setItems([]);
      onPendingCountChange(0);
      return;
    }
    setLoading(true);
    setError('');
    try {
      const result = await getAttendanceCorrections(selectedYearId);
      if (requestId.current !== currentRequest) return;
      const rows = result || [];
      setItems(rows);
      onPendingCountChange(rows.filter(item => item.status === 'PENDING').length);
    } catch (cause: any) {
      if (requestId.current === currentRequest) {
        setError(cause.message || 'Không thể tải yêu cầu sửa điểm danh.');
      }
    } finally {
      if (requestId.current === currentRequest) setLoading(false);
    }
  }, [onPendingCountChange, selectedYearId]);

  useEffect(() => {
    setItems([]);
    setStatus('PENDING');
    setDate('');
    setClassId('');
    setTeacherId('');
    setError('');
    setMessage('');
    void loadCorrections();
    return () => { requestId.current += 1; };
  }, [loadCorrections]);

  const classOptions = useMemo(() => Array.from(
    new Map(items.map(item => [item.classId, item.className])).entries(),
  ).sort((left, right) => left[1].localeCompare(right[1], 'vi')), [items]);

  const teacherOptions = useMemo(() => Array.from(
    new Map(items.map(item => [item.teacherId, item.teacherName])).entries(),
  ).sort((left, right) => left[1].localeCompare(right[1], 'vi')), [items]);

  const filteredItems = useMemo(() => filterAttendanceCorrections(items, {
    status, date, classId, teacherId,
  }), [items, status, date, classId, teacherId]);

  async function review(item: AttendanceCorrectionRequest, approve: boolean) {
    if (!selectedYearId) return;
    const action = approve ? 'duyệt' : 'từ chối';
    if (!window.confirm(`Xác nhận ${action} yêu cầu sửa điểm danh của lớp ${item.className}?`)) return;
    setBusyId(item.id);
    setError('');
    setMessage('');
    try {
      await reviewAttendanceCorrection(item.id, selectedYearId, approve);
      setMessage(approve
        ? 'Đã duyệt và áp dụng dữ liệu điểm danh mới.'
        : 'Đã từ chối yêu cầu; dữ liệu điểm danh được giữ nguyên.');
      await loadCorrections();
    } catch (cause: any) {
      setError(cause.message || 'Không thể xử lý yêu cầu sửa điểm danh.');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <section className="attendance-correction-inbox" aria-labelledby="attendance-correction-title">
      <div className="attendance-correction-heading">
        <div>
          <span className="eyebrow">Yêu cầu xử lý</span>
          <h2 id="attendance-correction-title">Sửa điểm danh</h2>
          <p>Tiếp nhận yêu cầu từ giáo viên và duyệt trong phạm vi năm học đang chọn.</p>
        </div>
        <span className="attendance-correction-pending-count">
          {items.filter(item => item.status === 'PENDING').length} chờ duyệt
        </span>
      </div>

      {error && <div className="notice error" role="alert">{error}</div>}
      {message && <div className="notice success" role="status">{message}</div>}

      <div className="filters attendance-correction-filters">
        <div className="form-group">
          <label>Trạng thái</label>
          <select value={status} onChange={event => setStatus(event.target.value as AttendanceCorrectionStatus | '')}>
            <option value="PENDING">Chờ duyệt</option>
            <option value="APPROVED">Đã duyệt</option>
            <option value="REJECTED">Đã từ chối</option>
            <option value="">Tất cả</option>
          </select>
        </div>
        <div className="form-group">
          <label>Ngày điểm danh</label>
          <input type="date" value={date} onChange={event => setDate(event.target.value)} />
        </div>
        <div className="form-group">
          <label>Lớp</label>
          <select value={classId} onChange={event => setClassId(event.target.value)}>
            <option value="">Tất cả lớp</option>
            {classOptions.map(([id, name]) => <option key={id} value={id}>{name}</option>)}
          </select>
        </div>
        <div className="form-group">
          <label>Giáo viên</label>
          <select value={teacherId} onChange={event => setTeacherId(event.target.value)}>
            <option value="">Tất cả giáo viên</option>
            {teacherOptions.map(([id, name]) => <option key={id} value={id}>{name}</option>)}
          </select>
        </div>
      </div>

      <div className="attendance-correction-list">
        {filteredItems.map(item => (
          <article className="attendance-correction-card" key={item.id}>
            <div className="attendance-correction-card-header">
              <div>
                <strong>{item.className} · {item.shift === 'MORNING' ? 'Buổi sáng' : 'Buổi chiều'}</strong>
                <span>{new Date(`${item.date}T00:00:00`).toLocaleDateString('vi-VN')} · GV {item.teacherName}</span>
              </div>
              <span className={`badge-status correction-${item.status.toLowerCase()}`}>
                {statusLabels[item.status]}
              </span>
            </div>

            <p className="attendance-correction-reason"><strong>Lý do:</strong> {item.reason}</p>
            <div className="attendance-correction-counts">
              <span>Có mặt <strong>{item.originalPresentCount} → {item.presentCount}</strong></span>
              <span>Vắng phép <strong>{item.originalAbsentWithLeaveCount} → {item.absentWithLeaveCount}</strong></span>
              <span>Vắng không phép <strong>{item.originalAbsentWithoutLeaveCount} → {item.absentWithoutLeaveCount}</strong></span>
            </div>

            {!!item.changes.length && (
              <div className="attendance-correction-changes">
                <strong>Học sinh thay đổi ({item.changes.length})</strong>
                {item.changes.map(change => (
                  <span key={change.studentId}>
                    {change.studentName} ({change.studentCode}): {' '}
                    {change.oldStatus ? attendanceLabels[change.oldStatus] : 'Chưa điểm danh'}
                    {' → '}{attendanceLabels[change.newStatus]}
                  </span>
                ))}
              </div>
            )}

            {item.reviewedByName && (
              <small className="attendance-correction-reviewer">
                Xử lý bởi {item.reviewedByName}
                {item.reviewedAt ? ` · ${new Date(item.reviewedAt).toLocaleString('vi-VN')}` : ''}
              </small>
            )}

            {item.status === 'PENDING' && (
              <div className="attendance-correction-actions">
                <button type="button" className="secondary-button" disabled={busyId === item.id}
                  onClick={() => void review(item, false)}>Từ chối</button>
                <button type="button" disabled={busyId === item.id}
                  onClick={() => void review(item, true)}>
                  {busyId === item.id ? 'Đang xử lý…' : 'Duyệt yêu cầu'}
                </button>
              </div>
            )}
          </article>
        ))}
        {!filteredItems.length && !loading && (
          <div className="account-empty"><strong>Không có yêu cầu sửa điểm danh phù hợp.</strong></div>
        )}
        {loading && <div className="account-empty"><strong>Đang tải yêu cầu xử lý…</strong></div>}
      </div>
    </section>
  );
}
