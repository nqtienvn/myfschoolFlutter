import { useEffect, useState } from 'react';
import { getTeacherAssignments, getTeacherDashboard } from '../../api/teacher';
import { useTeacherAcademic } from '../../teacher/TeacherAcademicContext';

function TeachingIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M4 5h16v14H4z"/><path d="M8 9h8M8 13h5"/></svg>;
}

function HomeroomIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><circle cx="9" cy="8" r="3"/><path d="M3.5 20v-1.5A4.5 4.5 0 0 1 8 14h2a4.5 4.5 0 0 1 4.5 4.5V20M15 5h6v8h-5"/></svg>;
}

function AttendanceIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>;
}

function ChartIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/></svg>;
}

function BellIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/></svg>;
}

function ChatIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4z"/><path d="M8 9h8M8 13h5"/></svg>;
}

function ArrowRightIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>;
}

const ACTIONS = [
  { path: '/teacher/grades', title: 'Nhập điểm', description: 'Nhập trực tiếp hoặc tải dữ liệu từ Excel/CSV.', Icon: TeachingIcon },
  { path: '/teacher/reviews', title: 'Nhận xét định kỳ', description: 'Hoàn thành độc lập phần nhận xét GVBM hoặc GVCN.', Icon: ChartIcon },
  { path: '/teacher/announcements', title: 'Thông báo lớp', description: 'Soạn mới, xem lịch sử gửi và thông báo đã nhận.', Icon: BellIcon },
  { path: '/teacher/leave-requests', title: 'Đơn xin nghỉ', description: 'Duyệt hoặc từ chối đơn của lớp chủ nhiệm.', Icon: AttendanceIcon },
  { path: '/teacher/chat', title: 'Tin nhắn', description: 'Trao đổi với phụ huynh và học sinh trong phạm vi phụ trách.', Icon: ChatIcon },
];

export default function TeacherDashboardPage({ navigate }: { navigate: (path: string) => void }) {
  const { years, semesters, selectedYearId, selectedSemesterId } = useTeacherAcademic();
  const [dashboard, setDashboard] = useState<any>(null);
  const [assignments, setAssignments] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const selectedYear = years.find(item => item.id === selectedYearId);
  const selectedSemester = semesters.find(item => item.id === selectedSemesterId);

  useEffect(() => {
    setDashboard(null);
    setAssignments([]);
    setError('');
    if (!selectedYearId || !selectedSemesterId) return;

    setLoading(true);
    Promise.all([
      getTeacherAssignments(selectedYearId),
      getTeacherDashboard(selectedYearId, selectedSemesterId).catch(() => null),
    ]).then(([rows, stats]) => {
      setAssignments(rows || []);
      setDashboard(stats);
    }).catch(cause => {
      setError(cause instanceof Error ? cause.message : 'Không thể tải tổng quan.');
    }).finally(() => setLoading(false));
  }, [selectedYearId, selectedSemesterId]);

  const stats = [
    { label: 'Lớp/môn đang dạy', value: assignments.length, detail: selectedYear?.name || 'Năm học đã chọn', color: 'blue', Icon: TeachingIcon },
    { label: 'Lớp chủ nhiệm', value: dashboard?.className || '—', detail: dashboard ? 'Đang phụ trách' : 'Không có phân công GVCN', color: 'purple', Icon: HomeroomIcon },
    { label: 'Chuyên cần lớp CN', value: dashboard?.attendanceRate != null ? `${dashboard.attendanceRate}%` : '—', detail: selectedSemester?.name || 'Học kỳ đã chọn', color: 'green', Icon: AttendanceIcon },
    { label: 'GPA lớp chủ nhiệm', value: dashboard?.averageGpa != null ? Number(dashboard.averageGpa).toFixed(1) : '—', detail: selectedSemester?.name || 'Học kỳ đã chọn', color: 'orange', Icon: ChartIcon },
  ];

  return (
    <main className="teacher-page page-stack">
      <div className="page-heading">
        <div>
          <h1>Tổng quan</h1>
          <p>
            {selectedYear
              ? <>Năm học <strong>{selectedYear.name}</strong>{selectedSemester ? <> · {selectedSemester.name}</> : ''}</>
              : 'Chọn năm học và học kỳ để xem dữ liệu nghiệp vụ'}
          </p>
        </div>
      </div>

      {error && <div className="notice error">{error}</div>}
      {loading && <div className="loading-state">Đang tải dữ liệu…</div>}

      <section aria-label="Chỉ số tổng quan giáo viên">
        <div className="dashboard-grid">
          {stats.map(({ label, value, detail, color, Icon }) => (
            <article className="stat-card" key={label}>
              <span className={`stat-icon ${color}`} aria-hidden="true"><Icon /></span>
              <span>
                <strong className="stat-value">{value}</strong>
                <span className="stat-label">{label}</span>
                <span className="teacher-stat-detail">{detail}</span>
              </span>
            </article>
          ))}
        </div>
      </section>

      <section aria-label="Nghiệp vụ thường dùng">
        <div className="ops-section-title">Nghiệp vụ thường dùng</div>
        <nav className="ops-module-grid" aria-label="Menu nghiệp vụ giáo viên">
          {ACTIONS.map(({ path, title, description, Icon }) => (
            <button className="ops-module-card" key={path} onClick={() => navigate(path)}>
              <span className="ops-card-icon" aria-hidden="true"><Icon /></span>
              <span className="ops-card-body">
                <span className="ops-card-header"><strong>{title}</strong></span>
                <span className="teacher-action-description">{description}</span>
              </span>
              <span className="ops-card-arrow" aria-hidden="true"><ArrowRightIcon /></span>
            </button>
          ))}
        </nav>
      </section>
    </main>
  );
}
