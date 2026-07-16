import { useEffect, useState } from 'react';
import { getTeacherAssignments, getTeacherDashboard } from '../../api/teacher';
import { useTeacherAcademic } from '../../teacher/TeacherAcademicContext';

export default function TeacherDashboardPage({ navigate }: { navigate: (path: string) => void }) {
  const { selectedYearId, selectedSemesterId } = useTeacherAcademic();
  const [dashboard, setDashboard] = useState<any>(null);
  const [assignments, setAssignments] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setDashboard(null); setAssignments([]); setError('');
    if (!selectedYearId || !selectedSemesterId) return;
    setLoading(true);
    Promise.all([
      getTeacherAssignments(selectedYearId),
      getTeacherDashboard(selectedYearId, selectedSemesterId).catch(() => null),
    ]).then(([rows, stats]) => { setAssignments(rows || []); setDashboard(stats); })
      .catch(cause => setError(cause instanceof Error ? cause.message : 'Không thể tải tổng quan.'))
      .finally(() => setLoading(false));
  }, [selectedYearId, selectedSemesterId]);

  return <main className="teacher-page page-stack">
    <div className="page-heading"><div><p className="teacher-eyebrow">Tổng quan nghiệp vụ</p><h1>Dashboard Giáo viên</h1><p>Dữ liệu được đồng bộ với ứng dụng Mobile qua cùng hệ thống API.</p></div></div>
    {error && <div className="notice error">{error}</div>}
    {loading && <div className="loading-state">Đang tải dữ liệu…</div>}
    <div className="teacher-kpi-grid">
      <article><span>Lớp/môn đang dạy</span><strong>{assignments.length}</strong><small>Trong năm học đã chọn</small></article>
      <article><span>Lớp chủ nhiệm</span><strong>{dashboard?.className || '—'}</strong><small>{dashboard ? 'Đang phụ trách' : 'Không có phân công GVCN'}</small></article>
      <article><span>Chuyên cần lớp CN</span><strong>{dashboard?.attendanceRate != null ? `${dashboard.attendanceRate}%` : '—'}</strong><small>Trung bình học kỳ</small></article>
      <article><span>GPA lớp CN</span><strong>{dashboard?.averageGpa != null ? Number(dashboard.averageGpa).toFixed(1) : '—'}</strong><small>Không gồm tỷ lệ đọc thông báo</small></article>
    </div>
    <section className="teacher-action-grid">
      {[
        ['/teacher/grades', 'Nhập điểm', 'Nhập trực tiếp hoặc upload Excel/CSV.'],
        ['/teacher/reviews', 'Nhận xét định kỳ', 'Submit độc lập phần GVBM hoặc GVCN.'],
        ['/teacher/announcements', 'Thông báo lớp', 'Soạn mới, xem đã gửi và thông báo đã nhận.'],
        ['/teacher/leave-requests', 'Đơn xin nghỉ', 'Duyệt hoặc từ chối đơn của lớp chủ nhiệm.'],
        ['/teacher/chat', 'Tin nhắn', 'Trao đổi với phụ huynh và học sinh.'],
      ].map(([path, title, description]) => <button key={path} onClick={() => navigate(path)}><strong>{title}</strong><span>{description}</span><b>→</b></button>)}
    </section>
  </main>;
}
