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

const navItems = [
  ['/teacher/dashboard', 'Tổng quan', '⌂'],
  ['/teacher/grades', 'Nhập điểm', '▦'],
  ['/teacher/reviews', 'Nhận xét định kỳ', '✎'],
  ['/teacher/announcements', 'Thông báo', '◉'],
  ['/teacher/homeroom', 'Hồ sơ lớp chủ nhiệm', '♙'],
  ['/teacher/leave-requests', 'Đơn xin nghỉ', '✓'],
  ['/teacher/chat', 'Tin nhắn', '◇'],
] as const;

function go(path: string) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

export default function TeacherPortal() {
  const [path, setPath] = useState(window.location.pathname);
  const [loggedIn, setLoggedIn] = useState(isTeacherLoggedIn());
  const invalidPath = path === '/teacher' || !navItems.some(([itemPath]) => itemPath === path);
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
  return <TeacherAcademicProvider><TeacherPortalLayout path={path} setLoggedIn={setLoggedIn} /></TeacherAcademicProvider>;
}

function TeacherPortalLayout({ path, setLoggedIn }: { path: string; setLoggedIn: (value: boolean) => void }) {
  const academic = useTeacherAcademic();
  const teacher = getStoredTeacher();
  const page = path === '/teacher/grades' ? <TeacherGradesPage />
    : path === '/teacher/reviews' ? <TeacherPeriodicReviewsPage />
    : path === '/teacher/announcements' ? <TeacherAnnouncementsPage />
    : path === '/teacher/homeroom' ? <TeacherHomeroomProfilePage />
    : path === '/teacher/leave-requests' ? <TeacherLeaveRequestsPage />
    : path === '/teacher/chat' ? <TeacherChatPage />
    : <TeacherDashboardPage navigate={go} />;

  return <div className="teacher-portal-shell">
    <aside className="teacher-sidebar"><a className="teacher-sidebar-brand" href="/teacher/dashboard" onClick={event => { event.preventDefault(); go('/teacher/dashboard'); }}><span>F</span><div><strong>FPT Schools</strong><small>Teacher Portal</small></div></a><nav>{navItems.map(([itemPath, label, icon]) => <button key={itemPath} className={path === itemPath ? 'active' : ''} onClick={() => go(itemPath)}><i>{icon}</i><span>{label}</span></button>)}</nav><div className="teacher-sidebar-user"><strong>{teacher?.name || 'Giáo viên'}</strong><small>{teacher?.accountCode || teacher?.phone}</small><button onClick={() => { logoutTeacher(); setLoggedIn(false); go('/teacher/login'); }}>Đăng xuất</button></div></aside>
    <div className="teacher-workspace"><header className="teacher-topbar"><div><strong>Cổng nghiệp vụ Giáo viên</strong><small>Mobile và Web đồng bộ cùng dữ liệu</small></div><div className="teacher-period-selectors"><label>Năm học<select disabled={academic.loading} value={academic.selectedYearId || ''} onChange={event => academic.selectYear(Number(event.target.value))}>{academic.years.map(year => <option key={year.id} value={year.id}>{year.name}</option>)}</select></label><label>Học kỳ<select disabled={academic.loading} value={academic.selectedSemesterId || ''} onChange={event => academic.selectSemester(Number(event.target.value))}>{academic.semesters.map(semester => <option key={semester.id} value={semester.id}>{semester.name}</option>)}</select></label></div></header>{academic.error && <div className="notice error teacher-context-error">{academic.error}</div>}<div className="teacher-page-content">{page}</div></div>
  </div>;
}
