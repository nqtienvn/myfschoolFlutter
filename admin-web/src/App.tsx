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

  // Load semesters when globalYearId changes
  useEffect(() => {
    if (!globalYearId) return;
    getSemesters(globalYearId)
      .then((sems: any) => {
        const list = sems || [];
        setSemesters(list);
        
        // Find active semester
        const activeSem = list.find((s: any) => s.status === 'ACTIVE');
        if (activeSem) {
          setGlobalSemesterId(String(activeSem.id));
        } else if (list.length > 0) {
          setGlobalSemesterId(String(list[0].id));
        } else {
          setGlobalSemesterId('');
        }
      })
      .catch(err => {
        console.error('Error fetching semesters for global year:', err);
      });
  }, [globalYearId]);

  async function fetchYears() {
    try {
      const data = await getAcademicYears() as AcademicYearItem[];
      setAcademicYears(data || []);
      const active = data.find(y => y.status === 'ACTIVE');
      if (active) {
        setGlobalYearId(String(active.id));
      } else if (data.length > 0) {
        setGlobalYearId(String(data[0].id));
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

  return (
    <div className="app">
      <nav className="sidebar" style={{ overflowY: 'auto' }}>
        <h2 style={{ padding: '0 8px', marginBottom: '24px' }}>MyFschool Admin</h2>
        
        <div className="sidebar-section-title">Tổng quan</div>
        <button className={page === 'dashboard' ? 'active' : ''} onClick={() => setPage('dashboard')}>Trang tổng quan</button>
        <button className={page === 'wizard' ? 'active' : ''} onClick={() => setPage('wizard')}>Hướng dẫn cấu hình</button>

        <div className="sidebar-section-title">Danh mục tĩnh</div>
        <button className={page === 'master-data' ? 'active' : ''} onClick={() => setPage('master-data')}>Khối lớp, Ca học & Môn học</button>

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

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        <header className="top-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '14px 30px', background: '#ffffff', borderBottom: '1px solid #e2e8f0' }}>
          {/* Left: Search input */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', width: '200px', position: 'relative' }}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#64748b" strokeWidth="2.5" style={{ position: 'absolute', left: '12px', top: '12px' }}>
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input 
              type="text" 
              placeholder="Tìm kiếm hoặc nhập lệnh... ⌘K" 
              style={{ width: '100%', padding: '9px 12px 9px 36px', borderRadius: '6px', border: '1px solid #e2e8f0', background: '#f8fafc', fontSize: '13px', outline: 'none' }}
            />
          </div>

          {/* Center/Right: Academic Context */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
            {/* Year Selector */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ fontSize: '13px', fontWeight: 600, color: '#64748b' }}>Năm học:</span>
              <select 
                value={globalYearId} 
                onChange={e => setGlobalYearId(e.target.value)}
                style={{ padding: '6px 12px', borderRadius: '6px', border: '1px solid #e2e8f0', outline: 'none', fontSize: '13px', fontWeight: 600, color: '#1c2434', background: '#ffffff' }}
              >
                {academicYears.map(y => (
                  <option key={y.id} value={y.id}>
                    Năm học {y.name} {y.status === 'ACTIVE' ? ' [Đang hoạt động]' : ''}
                  </option>
                ))}
              </select>
            </div>

            {/* Semester Selector */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ fontSize: '13px', fontWeight: 600, color: '#64748b' }}>Học kỳ:</span>
              <select 
                value={globalSemesterId} 
                onChange={e => setGlobalSemesterId(e.target.value)}
                style={{ padding: '6px 12px', borderRadius: '6px', border: '1px solid #e2e8f0', outline: 'none', fontSize: '13px', fontWeight: 600, color: '#1c2434', background: '#ffffff' }}
              >
                {semesters.map(s => (
                  <option key={s.id} value={s.id}>
                    {s.name} {s.status === 'ACTIVE' ? ' [Đang hoạt động]' : ''}
                  </option>
                ))}
              </select>
            </div>

            {/* Active badge */}
            {(() => {
              const activeYear = academicYears.find(y => String(y.id) === globalYearId);
              const activeSem = semesters.find(s => String(s.id) === globalSemesterId);
              const isActive = (activeYear?.status === 'ACTIVE') && (activeSem?.status === 'ACTIVE');
              return (
                <span style={{ 
                  fontSize: '11px', 
                  fontWeight: 700, 
                  textTransform: 'uppercase', 
                  padding: '4px 8px', 
                  borderRadius: '4px',
                  background: isActive ? '#e1fbf2' : '#f1f5f9',
                  color: isActive ? '#10b981' : '#64748b',
                  border: `1px solid ${isActive ? '#a7f3d0' : '#cbd5e1'}`
                }}>
                  {isActive ? 'Đang hoạt động' : 'Không hoạt động'}
                </span>
              );
            })()}

            {/* Dark mode mock toggle */}
            <button style={{ border: 'none', background: 'transparent', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', width: '36px', height: '36px', borderRadius: '50%' }}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#1c2434" strokeWidth="2">
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
              </svg>
            </button>

            {/* Notification bell */}
            <div style={{ position: 'relative' }}>
              <button style={{ border: 'none', background: 'transparent', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', width: '36px', height: '36px', borderRadius: '50%' }}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#1c2434" strokeWidth="2">
                  <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                  <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                </svg>
              </button>
              <span style={{ position: 'absolute', top: '4px', right: '4px', width: '8px', height: '8px', background: '#ef4444', borderRadius: '50%', border: '1px solid #ffffff' }}></span>
            </div>

            {/* Avatar profile */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', borderLeft: '1px solid #e2e8f0', paddingLeft: '16px' }}>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: '13px', fontWeight: 700, color: '#1c2434', lineHeight: '1.2' }}>Musharof</div>
                <div style={{ fontSize: '11px', color: '#64748b', fontWeight: 500 }}>Quản trị viên</div>
              </div>
              <div style={{ width: '36px', height: '36px', borderRadius: '50%', background: '#3c50e0', color: '#ffffff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold', fontSize: '14px' }}>
                M
              </div>
            </div>
          </div>
        </header>

        <main style={{ flex: 1, overflow: 'auto', padding: '30px', background: '#f1f5f9' }}>
          {pages[page] || <DashboardPage selectedYearId={globalYearId} selectedSemesterId={globalSemesterId} />}
        </main>
      </div>
    </div>
  );
}
