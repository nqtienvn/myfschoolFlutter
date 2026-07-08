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
import AcademicYearInitPage from './pages/AcademicYearInitPage';
import EnrollmentImportPage from './pages/EnrollmentImportPage';
import ScheduleImportPage from './pages/ScheduleImportPage';
import MasterDataPage from './pages/MasterDataPage';
import SetupWizardPage from './pages/SetupWizardPage';
import { apiFetch } from './api/client';

interface AcademicYearItem {
  id: number;
  name: string;
  status: string;
}

export default function App() {
  const [loggedIn, setLoggedIn] = useState(isAdminLoggedIn());
  const [page, setPage] = useState('wizard');
  const [academicYears, setAcademicYears] = useState<AcademicYearItem[]>([]);
  const [globalYearId, setGlobalYearId] = useState('');

  useEffect(() => {
    setLoggedIn(isAdminLoggedIn());
    if (isAdminLoggedIn()) {
      fetchYears();
    }
  }, [loggedIn]);

  async function fetchYears() {
    try {
      const data = await apiFetch('/academic-years') as AcademicYearItem[];
      setAcademicYears(data || []);
      const active = data.find(y => y.status === 'ACTIVE') || data[0];
      if (active) {
        setGlobalYearId(String(active.id));
      }
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
    wizard: <SetupWizardPage onNavigate={(key) => setPage(key)} />,
    semesters: <SemestersPage selectedYearId={globalYearId} />,
    'master-data': <MasterDataPage />,
    subjects: <SubjectsPage />,
    users: <UsersPage />,
    classes: <ClassesPage selectedYearId={globalYearId} />,
    'academicyear-init': <AcademicYearInitPage />,
    'enrollment-import': <EnrollmentImportPage selectedYearId={globalYearId} />,
    schedules: <SchedulesPage selectedYearId={globalYearId} />,
    'schedule-import': <ScheduleImportPage selectedYearId={globalYearId} />,
    'attendance-sessions': <AttendanceSessionPage selectedYearId={globalYearId} />,
    'grade-books': <GradeBookPage selectedYearId={globalYearId} />,
    'fee-categories': <FeeCategoriesPage />,
    'fee-templates': <FeeTemplatesPage selectedYearId={globalYearId} />,
    tuition: <TuitionPage selectedYearId={globalYearId} />,
    announcements: <AnnouncementsPage />,
    assignments: <AssignmentsPage selectedYearId={globalYearId} />,
  };

  const activeYearName = academicYears.find(y => String(y.id) === globalYearId)?.name || 'Chưa chọn';

  return (
    <div className="app">
      <nav className="sidebar" style={{ overflowY: 'auto' }}>
        <h2>MyFschool Admin</h2>
        
        <div className="sidebar-section-title">Khởi động</div>
        <button className={page === 'wizard' ? 'active' : ''} onClick={() => setPage('wizard')}>Hướng dẫn cấu hình</button>

        <div className="sidebar-section-title">Bước 1: Danh mục tĩnh</div>
        <button className={page === 'master-data' ? 'active' : ''} onClick={() => setPage('master-data')}>Khối lớp & Ca học</button>

        <div className="sidebar-section-title">Bước 2: Dòng thời gian</div>
        <button className={page === 'semesters' ? 'active' : ''} onClick={() => setPage('semesters')}>Năm học & Học kỳ</button>
        <button className={page === 'subjects' ? 'active' : ''} onClick={() => setPage('subjects')}>Danh mục Môn học</button>

        <div className="sidebar-section-title">Bước 3: Nhân sự</div>
        <button className={page === 'users' ? 'active' : ''} onClick={() => setPage('users')}>Quản lý Giáo viên</button>

        <div className="sidebar-section-title">Bước 4: Cấu hình lớp</div>
        <button className={page === 'classes' ? 'active' : ''} onClick={() => setPage('classes')}>Thiết lập Lớp học</button>

        <div className="sidebar-section-title">Bước 5: Học sinh</div>
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

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        <header className="top-header">
          <div className="top-header-title">
            Trang Quản Trị Hệ Thống Điện Tử
          </div>
          {page !== 'wizard' && page !== 'semesters' && (
            <div className="top-header-year-selector">
              <label>Năm học hoạt động:</label>
              <select value={globalYearId} onChange={e => setGlobalYearId(e.target.value)}>
                {academicYears.map(y => (
                  <option key={y.id} value={y.id}>
                    Năm học {y.name} {y.status === 'ACTIVE' ? '(Đang mở)' : ''}
                  </option>
                ))}
              </select>
            </div>
          )}
        </header>

        <main style={{ flex: 1, overflowY: 'auto' }}>
          {pages[page] || <SetupWizardPage onNavigate={(key) => setPage(key)} />}
        </main>
      </div>
    </div>
  );
}
