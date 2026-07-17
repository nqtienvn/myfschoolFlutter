import { useEffect, useState } from 'react';
import { getStoredTeacher, isTeacherLoggedIn, logoutTeacher } from '../api/teacher';
import TeacherAnnouncementsPage from '../pages/teacher/TeacherAnnouncementsPage';
import TeacherChatPage from '../pages/teacher/TeacherChatPage';
import TeacherDashboardPage from '../pages/teacher/TeacherDashboardPage';
import TeacherGradesPage from '../pages/teacher/TeacherGradesPage';
import TeacherHomeroomProfilePage from '../pages/teacher/TeacherHomeroomProfilePage';
import TeacherLeaveRequestsPage from '../pages/teacher/TeacherLeaveRequestsPage';
import TeacherLoginPage from '../pages/teacher/TeacherLoginPage';
import TeacherPeriodicReviewsPage from '../pages/teacher/TeacherPeriodicReviewsPage';
import { TeacherAcademicProvider, useTeacherAcademic } from './TeacherAcademicContext';

function DashboardIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/></svg>;
}

function GradeIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M4 5h16v14H4z"/><path d="M8 9h8M8 13h5"/></svg>;
}

function ReviewIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M9 5h10a2 2 0 0 1 2 2v12H5V9"/><path d="M3 5h10v10H3zM6 9h4M6 12h3"/></svg>;
}

function BellIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/></svg>;
}

function HomeroomIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><circle cx="9" cy="8" r="3"/><path d="M3.5 20v-1.5A4.5 4.5 0 0 1 8 14h2a4.5 4.5 0 0 1 4.5 4.5V20M15 5h6v8h-5M17 8h2M17 10.5h2"/></svg>;
}

function LeaveIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>;
}

function ChatIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4z"/><path d="M8 9h8M8 13h5"/></svg>;
}

function LogoutIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>;
}

const navItems = [
  { path: '/teacher/dashboard', label: 'Tổng quan', group: 'Giảng dạy', Icon: DashboardIcon },
  { path: '/teacher/grades', label: 'Nhập điểm', group: 'Giảng dạy', Icon: GradeIcon },
  { path: '/teacher/reviews', label: 'Nhận xét định kỳ', group: 'Giảng dạy', Icon: ReviewIcon },
  { path: '/teacher/announcements', label: 'Thông báo', group: 'Công tác lớp', Icon: BellIcon },
  { path: '/teacher/homeroom', label: 'Hồ sơ lớp chủ nhiệm', group: 'Công tác lớp', Icon: HomeroomIcon },
  { path: '/teacher/leave-requests', label: 'Đơn xin nghỉ', group: 'Công tác lớp', Icon: LeaveIcon },
  { path: '/teacher/chat', label: 'Tin nhắn', group: 'Công tác lớp', Icon: ChatIcon },
] as const;

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Nháp',
  ACTIVE: 'Đang hoạt động',
  COMPLETED: 'Đã kết thúc',
};

const STATUS_CLASS: Record<string, string> = {
  DRAFT: 'draft',
  ACTIVE: 'active',
  COMPLETED: 'completed',
};

function go(path: string) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

export default function TeacherPortal() {
  const [path, setPath] = useState(window.location.pathname);
  const [loggedIn, setLoggedIn] = useState(isTeacherLoggedIn());
  const invalidPath = path === '/teacher' || !navItems.some(item => item.path === path);

  useEffect(() => {
    const update = () => setPath(window.location.pathname);
    window.addEventListener('popstate', update);
    return () => window.removeEventListener('popstate', update);
  }, []);

  useEffect(() => {
    if (loggedIn && path !== '/teacher/login' && invalidPath) go('/teacher/dashboard');
  }, [invalidPath, loggedIn, path]);

  if (!loggedIn || path === '/teacher/login') {
    return <TeacherLoginPage onLoggedIn={() => { setLoggedIn(true); go('/teacher/dashboard'); }} />;
  }
  if (invalidPath) return null;

  return (
    <TeacherAcademicProvider>
      <TeacherPortalLayout path={path} setLoggedIn={setLoggedIn} />
    </TeacherAcademicProvider>
  );
}

function TeacherPortalLayout({ path, setLoggedIn }: { path: string; setLoggedIn: (value: boolean) => void }) {
  const academic = useTeacherAcademic();
  const teacher = getStoredTeacher();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const selectedYear = academic.years.find(item => item.id === academic.selectedYearId);
  const selectedSemester = academic.semesters.find(item => item.id === academic.selectedSemesterId);
  const activeItem = navItems.find(item => item.path === path) || navItems[0];
  const page = path === '/teacher/grades' ? <TeacherGradesPage />
    : path === '/teacher/reviews' ? <TeacherPeriodicReviewsPage />
    : path === '/teacher/announcements' ? <TeacherAnnouncementsPage />
    : path === '/teacher/homeroom' ? <TeacherHomeroomProfilePage />
    : path === '/teacher/leave-requests' ? <TeacherLeaveRequestsPage />
    : path === '/teacher/chat' ? <TeacherChatPage />
    : <TeacherDashboardPage navigate={go} />;

  useEffect(() => setSidebarOpen(false), [path]);

  useEffect(() => {
    document.body.classList.toggle('sidebar-lock', sidebarOpen);
    return () => document.body.classList.remove('sidebar-lock');
  }, [sidebarOpen]);

  function signOut() {
    logoutTeacher();
    setLoggedIn(false);
    go('/teacher/login');
  }

  return (
    <>
      <a href="#teacher-main-content" className="skip-link">Bỏ qua điều hướng</a>
      <div className={`app-shell teacher-app-shell ${sidebarOpen ? 'sidebar-is-open' : ''}`}>
        <button
          className="sidebar-backdrop"
          aria-label="Đóng menu"
          onClick={() => setSidebarOpen(false)}
        />

        <aside className="workflow-sidebar" id="teacher-sidebar" aria-label="Menu nghiệp vụ giáo viên">
          <button className="brand" onClick={() => go('/teacher/dashboard')} aria-label="Về trang tổng quan">
            <span className="brand-mark" aria-hidden="true">MF</span>
            <span className="brand-copy">
              <strong>MyFschool</strong>
              <small>Cổng nghiệp vụ giáo viên</small>
            </span>
          </button>

          {(['Giảng dạy', 'Công tác lớp'] as const).map(group => (
            <div className="teacher-nav-group" key={group}>
              <div className="sidebar-section-label" aria-hidden="true">{group}</div>
              <nav className="workflow-nav" aria-label={group}>
                {navItems.filter(item => item.group === group).map(item => {
                  const Icon = item.Icon;
                  const active = path === item.path;
                  return (
                    <button
                      key={item.path}
                      className={active ? 'active' : ''}
                      onClick={() => go(item.path)}
                      title={item.label}
                      aria-current={active ? 'page' : undefined}
                    >
                      <span className="module-icon" aria-hidden="true"><Icon /></span>
                      <span className="module-label">{item.label}</span>
                    </button>
                  );
                })}
              </nav>
            </div>
          ))}

          <div className="sidebar-footer">
            <div className="sidebar-user" aria-label="Thông tin tài khoản giáo viên">
              <span className="sidebar-user-info">
                <span className="sidebar-user-name">{teacher?.name || 'Giáo viên'}</span>
                <span className="sidebar-user-role">Giáo viên · {teacher?.accountCode || teacher?.phone || 'FPT Schools'}</span>
              </span>
            </div>
            <button className="logout-button" title="Đăng xuất" aria-label="Đăng xuất khỏi hệ thống" onClick={signOut}>
              <span className="logout-icon" aria-hidden="true"><LogoutIcon /></span>
              <span className="logout-label">Đăng xuất</span>
            </button>
          </div>
        </aside>

        <div className="workspace">
          <header className="context-bar" role="banner" aria-label="Chọn năm học và học kỳ">
            <button
              className="mobile-menu-button"
              aria-label="Mở menu điều hướng"
              aria-expanded={sidebarOpen}
              aria-controls="teacher-sidebar"
              onClick={() => setSidebarOpen(true)}
            >
              <span aria-hidden="true" />
              <span aria-hidden="true" />
              <span aria-hidden="true" />
            </button>

            <span className="sr-only" aria-live="polite">{activeItem.label}</span>
            <div className="context-spacer" />

            <div>
              <label htmlFor="teacher-year-select" className="ctx-label">Năm học</label>
              <div className="teacher-context-control">
                <div className="ctx-pill-select">
                  <select
                    id="teacher-year-select"
                    disabled={academic.loading}
                    value={academic.selectedYearId || ''}
                    onChange={event => academic.selectYear(Number(event.target.value))}
                    aria-label="Chọn năm học"
                  >
                    <option value="">— Chưa có năm học —</option>
                    {academic.years.map(year => <option key={year.id} value={year.id}>{year.name}</option>)}
                  </select>
                </div>
                {selectedYear && (
                  <span className={`ctx-status-badge ${STATUS_CLASS[selectedYear.status] || ''}`}>
                    {STATUS_LABELS[selectedYear.status] || selectedYear.status}
                  </span>
                )}
              </div>
            </div>

            <div>
              <label htmlFor="teacher-semester-select" className="ctx-label">Học kỳ</label>
              <div className="ctx-pill-select">
                <select
                  id="teacher-semester-select"
                  disabled={academic.loading || !academic.selectedYearId || !academic.semesters.length}
                  value={academic.selectedSemesterId || ''}
                  onChange={event => academic.selectSemester(Number(event.target.value))}
                  aria-label="Chọn học kỳ"
                >
                  <option value="">— Chọn học kỳ —</option>
                  {academic.semesters.map(semester => (
                    <option key={semester.id} value={semester.id}>
                      {semester.name} · {STATUS_LABELS[semester.status] || semester.status}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </header>

          <div id="teacher-main-content" className="page-content" tabIndex={-1}>
            {academic.error && <div className="notice error teacher-context-error">{academic.error}</div>}
            {!academic.error && !academic.loading && selectedYear && !selectedSemester && (
              <div className="notice warning teacher-context-error">Năm học này chưa có học kỳ khả dụng.</div>
            )}
            {page}
          </div>
        </div>
      </div>
    </>
  );
}
