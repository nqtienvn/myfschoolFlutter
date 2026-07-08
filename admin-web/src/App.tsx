import { useState, useEffect } from 'react';
import { isAdminLoggedIn, logout } from './api/auth';
import LoginPage from './pages/LoginPage';
import UsersPage from './pages/UsersPage';
import ClassesPage from './pages/ClassesPage';
import SubjectsPage from './pages/SubjectsPage';
import SemestersPage from './pages/SemestersPage';
import AssignmentsPage from './pages/AssignmentsPage';
import SchedulesPage from './pages/SchedulesPage';
import TuitionPage from './pages/TuitionPage';
import FeeCategoriesPage from './pages/FeeCategoriesPage';
import FeeTemplatesPage from './pages/FeeTemplatesPage';
import AnnouncementsPage from './pages/AnnouncementsPage';
import AttendanceSessionPage from './pages/AttendanceSessionPage';
import GradeBookPage from './pages/GradeBookPage';

const MENU = [
  { key: 'users', label: 'Tài khoản' },
  { key: 'classes', label: 'Lớp học' },
  { key: 'subjects', label: 'Môn học' },
  { key: 'semesters', label: 'Học kỳ' },
  { key: 'assignments', label: 'Phân công GV' },
  { key: 'schedules', label: 'Thời khóa biểu' },
  { key: 'fee-categories', label: 'Danh mục phí' },
  { key: 'fee-templates', label: 'Mẫu phí' },
  { key: 'tuition', label: 'Học phí' },
  { key: 'announcements', label: 'Thông báo' },
  { key: 'attendance-sessions', label: 'Điểm danh' },
  { key: 'grade-books', label: 'Bảng điểm' },
];

export default function App() {
  const [loggedIn, setLoggedIn] = useState(isAdminLoggedIn());
  const [page, setPage] = useState('users');

  useEffect(() => { setLoggedIn(isAdminLoggedIn()); }, []);

  if (!loggedIn) {
    return <LoginPage onLogin={() => setLoggedIn(true)} />;
  }

  function handleLogout() {
    logout();
    setLoggedIn(false);
  }

  const pages: Record<string, React.ReactNode> = {
    users: <UsersPage />,
    classes: <ClassesPage />,
    subjects: <SubjectsPage />,
    semesters: <SemestersPage />,
    assignments: <AssignmentsPage />,
    schedules: <SchedulesPage />,
    'fee-categories': <FeeCategoriesPage />,
    'fee-templates': <FeeTemplatesPage />,
    tuition: <TuitionPage />,
    announcements: <AnnouncementsPage />,
    'attendance-sessions': <AttendanceSessionPage />,
    'grade-books': <GradeBookPage />,
  };

  return (
    <div className="app">
      <nav className="sidebar">
        <h2>MyFschool Admin</h2>
        {MENU.map(m => (
          <button
            key={m.key}
            className={page === m.key ? 'active' : ''}
            onClick={() => setPage(m.key)}
          >
            {m.label}
          </button>
        ))}
        <button className="logout" onClick={handleLogout}>Đăng xuất</button>
      </nav>
      <main>{pages[page] || <UsersPage />}</main>
    </div>
  );
}
