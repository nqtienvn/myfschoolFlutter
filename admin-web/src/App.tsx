import { useState, useEffect } from 'react';
import { isAdminLoggedIn, logout } from './api/auth';
import LoginPage from './pages/LoginPage';
import UsersPage from './pages/UsersPage';
import ClassesPage from './pages/ClassesPage';
import AssignmentsPage from './pages/AssignmentsPage';
import SchedulesPage from './pages/SchedulesPage';
import TuitionPage from './pages/TuitionPage';
import FeeCategoriesPage from './pages/FeeCategoriesPage';
import FeeTemplatesPage from './pages/FeeTemplatesPage';
import AnnouncementsPage from './pages/AnnouncementsPage';
import AttendanceSessionPage from './pages/AttendanceSessionPage';
import GradeBookPage from './pages/GradeBookPage';
import AcademicYearInitPage from './pages/AcademicYearInitPage';
import EnrollmentImportPage from './pages/EnrollmentImportPage';
import ScheduleImportPage from './pages/ScheduleImportPage';
import MasterDataPage from './pages/MasterDataPage';
import SetupWizardPage from './pages/SetupWizardPage';
import DashboardPage from './pages/DashboardPage';
import { getAcademicYears } from './api/academicYear';
import { getSemesters } from './api/semester';

interface AcademicYearItem {
  id: number;
  name: string;
  status: string;
}

interface SemesterItem {
  id: number;
  name: string;
  academicYearId: number;
  academicYearName: string;
  order: number;
  startDate: string;
  endDate: string;
  isCurrent: boolean;
  status: string;
}

export default function App() {
  const [loggedIn, setLoggedIn] = useState(isAdminLoggedIn());
  const [page, setPage] = useState('dashboard');
  const [academicYears, setAcademicYears] = useState<AcademicYearItem[]>([]);
  const [semesters, setSemesters] = useState<SemesterItem[]>([]);
  const [globalYearId, setGlobalYearId] = useState('');
  const [globalSemesterId, setGlobalSemesterId] = useState('');

  useEffect(() => {
    setLoggedIn(isAdminLoggedIn());
    if (isAdminLoggedIn()) {
      fetchYears();
    }
  }, [loggedIn]);

  // Load active semester for active academic year
  useEffect(() => {
    if (!globalYearId) {
      setSemesters([]);
      setGlobalSemesterId('');
      return;
    }
    getSemesters(globalYearId)
      .then((sems: any) => {
        const list = sems || [];
        setSemesters(list);
        const activeSem = list.find((s: any) => s.status === 'ACTIVE');
        setGlobalSemesterId(activeSem ? String(activeSem.id) : '');
      })
      .catch(err => {
        console.error('Error fetching semesters for global year:', err);
      });
  }, [globalYearId]);

  async function fetchYears() {
    try {
      const data = await getAcademicYears() as AcademicYearItem[];
      const years = data || [];
      setAcademicYears(years);
      const active = years.find(y => y.status === 'ACTIVE');
      if (!active) {
        setGlobalYearId('');
        setSemesters([]);
        setGlobalSemesterId('');
        return;
      }

      setGlobalYearId(String(active.id));
      const sems = await getSemesters(String(active.id)) as SemesterItem[];
      const semesterList = sems || [];
      setSemesters(semesterList);
      const activeSem = semesterList.find(s => s.status === 'ACTIVE');
      setGlobalSemesterId(activeSem ? String(activeSem.id) : '');
    } catch (err) {
      console.error('Error fetching academic years:', err);
    }
  }

  if (!loggedIn) {
    return <LoginPage onLogin={() => setLoggedIn(true)} />;
  }

  function handleLogout() {
    logout();
    setLoggedIn(false);
  }

  const pages: Record<string, React.ReactNode> = {
    dashboard: <DashboardPage selectedYearId={globalYearId} selectedSemesterId={globalSemesterId} />,
    wizard: <SetupWizardPage onNavigate={(key) => setPage(key)} />,
    semesters: <MasterDataPage initialTab="academic-years" onYearCreated={fetchYears} />,
    'master-data': <MasterDataPage onYearCreated={fetchYears} />,
    users: <UsersPage />,
    classes: <ClassesPage selectedYearId={globalYearId} selectedSemesterId={globalSemesterId} />,
    'academicyear-init': <AcademicYearInitPage />,
    'enrollment-import': <EnrollmentImportPage selectedYearId={globalYearId} />,
    schedules: <SchedulesPage selectedYearId={globalYearId} selectedSemesterId={globalSemesterId} />,
    'schedule-import': <ScheduleImportPage selectedYearId={globalYearId} selectedSemesterId={globalSemesterId} />,
    'attendance-sessions': <AttendanceSessionPage selectedYearId={globalYearId} selectedSemesterId={globalSemesterId} />,
    'grade-books': <GradeBookPage selectedYearId={globalYearId} selectedSemesterId={globalSemesterId} />,
    'fee-categories': <FeeCategoriesPage />,
    'fee-templates': <FeeTemplatesPage selectedYearId={globalYearId} selectedSemesterId={globalSemesterId} />,
    tuition: <TuitionPage selectedYearId={globalYearId} selectedSemesterId={globalSemesterId} />,
    announcements: <AnnouncementsPage />,
    assignments: <AssignmentsPage selectedYearId={globalYearId} selectedSemesterId={globalSemesterId} />,
  };
  const activeYear = academicYears.find(y => String(y.id) === globalYearId);
  const activeSemester = semesters.find(s => String(s.id) === globalSemesterId);

  return (
    <div className="app">
      <nav className="sidebar" style={{ overflowY: 'auto' }}>
        <h2 style={{ padding: '0 8px', marginBottom: '24px' }}>MyFschool Admin</h2>

        <div className="sidebar-section-title">Tổng quan</div>
        <button className={page === 'dashboard' ? 'active' : ''} onClick={() => setPage('dashboard')}>Trang tổng quan</button>
        <button className={page === 'wizard' ? 'active' : ''} onClick={() => setPage('wizard')}>Hướng dẫn cấu hình</button>

        <div className="sidebar-section-title">Danh mục tĩnh</div>
        <button className={page === 'master-data' ? 'active' : ''} onClick={() => setPage('master-data')}>Danh Mục Chung</button>

        <div className="sidebar-section-title">Nhân sự</div>
        <button className={page === 'users' ? 'active' : ''} onClick={() => setPage('users')}>Quản lý Giáo viên</button>

        <div className="sidebar-section-title">Cấu hình lớp</div>
        <button className={page === 'classes' ? 'active' : ''} onClick={() => setPage('classes')}>Thiết lập Lớp học</button>

        <div className="sidebar-section-title">Học sinh</div>
        <button className={page === 'enrollment-import' ? 'active' : ''} onClick={() => setPage('enrollment-import')}>Nhập Học sinh & PH</button>

        <div className="sidebar-section-title">Vận hành học tập</div>
        <button className={page === 'assignments' ? 'active' : ''} onClick={() => setPage('assignments')}>Phân công giảng dạy</button>
        <button className={page === 'schedules' ? 'active' : ''} onClick={() => setPage('schedules')}>Thời khóa biểu</button>
        <button className={page === 'schedule-import' ? 'active' : ''} onClick={() => setPage('schedule-import')}>Import Excel TKB</button>
        <button className={page === 'attendance-sessions' ? 'active' : ''} onClick={() => setPage('attendance-sessions')}>Điểm danh buổi học</button>
        <button className={page === 'grade-books' ? 'active' : ''} onClick={() => setPage('grade-books')}>Sổ điểm & Bảng điểm</button>

        <div className="sidebar-section-title">Tài chính & Thông báo</div>
        <button className={page === 'fee-categories' ? 'active' : ''} onClick={() => setPage('fee-categories')}>Danh mục Học phí</button>
        <button className={page === 'fee-templates' ? 'active' : ''} onClick={() => setPage('fee-templates')}>Tạo Mẫu học phí</button>
        <button className={page === 'tuition' ? 'active' : ''} onClick={() => setPage('tuition')}>Hóa đơn Học phí</button>
        <button className={page === 'announcements' ? 'active' : ''} onClick={() => setPage('announcements')}>Gửi Thông báo</button>
        <button className={page === 'academicyear-init' ? 'active' : ''} onClick={() => setPage('academicyear-init')}>Khởi tạo Năm mới</button>

        <button className="logout" onClick={handleLogout} style={{ marginTop: 24 }}>Đăng xuất</button>
      </nav>

      <div className="app-content">
        <header className="top-header">
          <div className="top-header-controls active-term-strip">
            <div className="term-chip">
              <span>Năm học đang hoạt động</span>
              <strong>{activeYear ? activeYear.name : 'Chưa có'}</strong>
            </div>

            <div className="term-chip">
              <span>Học kỳ đang hoạt động</span>
              <strong>{activeSemester ? activeSemester.name : 'Chưa có'}</strong>
            </div>
          </div>
        </header>

        <main>
          {pages[page] || <DashboardPage selectedYearId={globalYearId} selectedSemesterId={globalSemesterId} />}
        </main>
      </div>
    </div>
  );
}
