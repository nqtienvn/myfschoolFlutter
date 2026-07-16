import { useEffect, useState } from 'react';
import { approveLeaveRequest, getPendingLeaveRequests, getReviewedLeaveRequests, rejectLeaveRequest } from '../../api/teacher';
import { useTeacherAcademic } from '../../teacher/TeacherAcademicContext';

export default function TeacherLeaveRequestsPage() {
  const { selectedYearId, selectedSemesterId } = useTeacherAcademic();
  const [pending, setPending] = useState<any[]>([]);
  const [reviewed, setReviewed] = useState<any[]>([]);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState<number | null>(null);

  async function load() {
    if (!selectedYearId || !selectedSemesterId) return;
    try {
      const [pendingRows, reviewedRows] = await Promise.all([
        getPendingLeaveRequests(selectedYearId, selectedSemesterId), getReviewedLeaveRequests(selectedYearId, selectedSemesterId),
      ]);
      setPending(pendingRows || []); setReviewed(reviewedRows || []);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Không tải được đơn xin nghỉ.'); }
  }
  useEffect(() => { setPending([]); setReviewed([]); setError(''); void load(); }, [selectedYearId, selectedSemesterId]);

  async function approve(id: number) { setBusyId(id); setError(''); try { await approveLeaveRequest(id); await load(); } catch (cause) { setError(cause instanceof Error ? cause.message : 'Không duyệt được đơn.'); } finally { setBusyId(null); } }
  async function reject(id: number) { const reason = prompt('Lý do từ chối:')?.trim(); if (!reason) return; setBusyId(id); setError(''); try { await rejectLeaveRequest(id, reason); await load(); } catch (cause) { setError(cause instanceof Error ? cause.message : 'Không từ chối được đơn.'); } finally { setBusyId(null); } }

  const leaveTable = (rows: any[], actions: boolean) => <div className="table-responsive"><table><thead><tr><th>Học sinh</th><th>Thời gian</th><th>Buổi</th><th>Lý do</th><th>Trạng thái</th>{actions && <th />}</tr></thead><tbody>{rows.map(item => <tr key={item.id}><td><strong>{item.studentName}</strong><small className="table-subtext">{item.studentCode}</small></td><td>{item.dateFrom}{item.dateTo !== item.dateFrom ? ` → ${item.dateTo}` : ''}</td><td>{item.shift}</td><td>{item.reason}{item.response && <small className="table-subtext">Phản hồi: {item.response}</small>}</td><td><span className={`badge-status ${item.status === 'APPROVED' ? 'active' : 'draft'}`}>{item.status}</span></td>{actions && <td><div className="table-actions"><button disabled={busyId === item.id} onClick={() => approve(item.id)}>Duyệt</button><button className="danger" disabled={busyId === item.id} onClick={() => reject(item.id)}>Từ chối</button></div></td>}</tr>)}</tbody></table></div>;

  return <main className="teacher-page page-stack"><div className="page-heading"><div><p className="teacher-eyebrow">GVCN</p><h1>Duyệt đơn xin nghỉ</h1><p>Duyệt/từ chối đơn trong đúng năm học và học kỳ đang chọn.</p></div></div>{error && <div className="notice error">{error}</div>}<section className="panel"><h2>Chờ xử lý ({pending.length})</h2>{leaveTable(pending, true)}{!pending.length && <div className="empty-state">Không có đơn đang chờ.</div>}</section><section className="panel"><h2>Đã xử lý</h2>{leaveTable(reviewed, false)}</section></main>;
}
