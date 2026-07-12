import { useEffect, useMemo, useState } from 'react';
import { getAcademicYears } from './api/academicYear';
import { isAdminLoggedIn, logout } from './api/auth';
import { getSemesters } from './api/semester';
import AssignmentsPage from './pages/AssignmentsPage';
import ClassesPage from './pages/ClassesPage';
import StudentEnrollmentPage from './pages/StudentEnrollmentPage';
import LoginPage from './pages/LoginPage';
import MasterDataPage from './pages/MasterDataPage';
import UsersPage from './pages/UsersPage';
import ValidationPage from './pages/ValidationPage';
import ActivationPage from './pages/ActivationPage';
import TimetablesPage from './pages/TimetablesPage';
import StudentsAttendancePage from './pages/StudentsAttendancePage';
import GradesManagementPage from './pages/GradesManagementPage';
import GradeConfigurationPage from './pages/GradeConfigurationPage';
import AnnouncementsPage from './pages/AnnouncementsPage';
import { getAnnouncements as getAdminAnnouncements } from './api/announcement';

export interface AcademicYearItem {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  status: 'DRAFT' | 'ACTIVE' | 'COMPLETED';
}

export interface SemesterItem {
  id: number;
  name: string;
  academicYearId: number;
  academicYearName: string;
  order: number;
  startDate: string;
  endDate: string;
  isCurrent: boolean;
  status: 'NOT_STARTED' | 'ACTIVE' | 'COMPLETED';
}

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'NHÁP',
  NOT_STARTED: 'CHƯA BẮT ĐẦU',
  ACTIVE: 'ĐANG HOẠT ĐỘNG',
  COMPLETED: 'ĐÃ HOÀN THÀNH',
};

const CONFIG_TABS = [
  { key: 'years', icon: 'calendar', label: 'Năm học' },
  { key: 'grade-config', icon: 'grade', label: 'Cấu hình đầu điểm' },
  { key: 'master-data', icon: 'catalog', label: 'Danh mục' },
  { key: 'classes', icon: 'school', label: 'Lớp học' },
  { key: 'students', icon: 'family', label: 'Học sinh & phụ huynh' },
  { key: 'assignments', icon: 'assignment', label: 'Phân công giảng dạy' },
  { key: 'validation', icon: 'checklist', label: 'Kiểm tra dữ liệu' },
  { key: 'activation', icon: 'power', label: 'Kích hoạt năm học' },
] as const;

const MODULES = [
  { key: 'configuration', icon: 'settings', label: 'Cấu hình năm học' },
  { key: 'teachers', icon: 'teacher', label: 'Quản lý giáo viên' },
  { key: 'students-attendance', icon: 'family', label: 'Quản lý điểm danh' },
  { key: 'grades', icon: 'grade', label: 'Quản lý điểm' },
  { key: 'timetables', icon: 'timetable', label: 'Thời khóa biểu' },
  { key: 'announcements', icon: 'bell', label: 'Thông báo' },
] as const;

type ConfigTabKey = typeof CONFIG_TABS[number]['key'];
type ModuleKey = typeof MODULES[number]['key'];
type IconName = typeof CONFIG_TABS[number]['icon'] | typeof MODULES[number]['icon'];

function NavIcon({ name }: { name: IconName }) {
  const paths: Record<IconName, React.ReactNode> = {
    settings: <><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.12 2.12-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1 1.55V20h-3v-.09a1.7 1.7 0 0 0-1-1.55 1.7 1.7 0 0 0-1.88.34l-.06.06-2.12-2.12.06-.06A1.7 1.7 0 0 0 7.08 15a1.7 1.7 0 0 0-1.55-1H5.4v-3h.13a1.7 1.7 0 0 0 1.55-1 1.7 1.7 0 0 0-.34-1.88l-.06-.06 2.12-2.12.06.06a1.7 1.7 0 0 0 1.88.34 1.7 1.7 0 0 0 1-1.55V4.7h3v.09a1.7 1.7 0 0 0 1 1.55 1.7 1.7 0 0 0 1.88-.34l.06-.06 2.12 2.12-.06.06A1.7 1.7 0 0 0 19.4 10a1.7 1.7 0 0 0 1.55 1h.09v3h-.09a1.7 1.7 0 0 0-1.55 1Z"/></>,
    teacher: <><circle cx="9" cy="8" r="3"/><path d="M3.5 20v-1.5A4.5 4.5 0 0 1 8 14h2a4.5 4.5 0 0 1 4.5 4.5V20M15 5h6v8h-5M17 8h2M17 10.5h2"/></>,
    timetable: <><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 3v4M17 3v4M3 10h18M7 14h2M11 14h2M15 14h2M7 17.5h2M11 17.5h2"/></>,
    bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/></>,
    calendar: <><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 3v4M17 3v4M3 10h18M8 14h3M8 17h6"/></>,
    catalog: <><rect x="4" y="4" width="16" height="16" rx="2"/><path d="M8 8h8M8 12h8M8 16h5"/></>,
    school: <><path d="m3 10 9-5 9 5-9 5-9-5Z"/><path d="M6 12.5V18h12v-5.5M9 20v-4h6v4"/></>,
    family: <><circle cx="9" cy="8" r="3"/><circle cx="17" cy="10" r="2"/><path d="M3 20v-1.5A4.5 4.5 0 0 1 7.5 14h3A4.5 4.5 0 0 1 15 18.5V20M15 15h1.5a3.5 3.5 0 0 1 3.5 3.5V20"/></>,
    assignment: <><path d="M4 19.5V6.8A2.8 2.8 0 0 1 6.8 4H20v15.5H6.5A2.5 2.5 0 0 0 4 22"/><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20M9 9h7M9 12h5"/></>,
    checklist: <><rect x="5" y="3" width="14" height="18" rx="2"/><path d="m8 9 1.5 1.5L12 8M14 9h2M8 15l1.5 1.5L12 14M14 15h2"/></>,
    power: <><path d="M12 3v9"/><path d="M7.1 6.2a8 8 0 1 0 9.8 0"/></>,
    grade: <><path d="M4 5h16v14H4z"/><path d="M8 9h8M8 13h5"/></>,
  };
  return <svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>;
}

export default function App() {
  const [loggedIn, setLoggedIn] = useState(isAdminLoggedIn());
  const [module, setModule] = useState<ModuleKey>('configuration');
  const [configTab, setConfigTab] = useState<ConfigTabKey>('years');
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [years, setYears] = useState<AcademicYearItem[]>([]);
  const [semesters, setSemesters] = useState<SemesterItem[]>([]);
  const [yearId, setYearId] = useState('');
  const [semesterId, setSemesterId] = useState('');
  const [loadingContext, setLoadingContext] = useState(false);
  const [pendingAnnouncements, setPendingAnnouncements] = useState(0);

  async function refreshYears(preferredYearId?: string) {
    setLoadingContext(true);
    try {
      const data = (await getAcademicYears()) as AcademicYearItem[];
      const sorted = [...(data || [])].sort((a, b) => b.id - a.id);
      setYears(sorted);
      const selected = sorted.find(y => String(y.id) === (preferredYearId || yearId))
        || sorted.find(y => y.status === 'ACTIVE')
        || sorted.find(y => y.status === 'DRAFT')
        || sorted[0];
      setYearId(selected ? String(selected.id) : '');
    } finally {
      setLoadingContext(false);
    }
  }

  async function refreshSemesters(targetYearId = yearId) {
    if (!targetYearId) {
      setSemesters([]);
      setSemesterId('');
      return;
    }
    const data = await getSemesters(targetYearId) as SemesterItem[];
    const list = data || [];
    setSemesters(list);
    const selected = list.find(s => String(s.id) === semesterId)
      || list.find(s => s.status === 'ACTIVE')
      || list[0];
    setSemesterId(selected ? String(selected.id) : '');
  }

  useEffect(() => {
    if (loggedIn) refreshYears();
  }, [loggedIn]);

  useEffect(() => {
    refreshSemesters(yearId);
  }, [yearId]);

  useEffect(() => {
    let active = true;
    const refresh = async () => {
      if (!yearId) { setPendingAnnouncements(0); return; }
      try { const rows = await getAdminAnnouncements(yearId, 'PENDING'); if (active) setPendingAnnouncements(rows.length); } catch { /* retry on next poll */ }
    };
    void refresh(); const timer = window.setInterval(refresh, 15000);
    return () => { active = false; window.clearInterval(timer); };
  }, [yearId]);

  useEffect(() => {
    document.body.classList.toggle('sidebar-lock', sidebarOpen);
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setSidebarOpen(false);
    };
    if (sidebarOpen) window.addEventListener('keydown', closeOnEscape);
    return () => {
      document.body.classList.remove('sidebar-lock');
      window.removeEventListener('keydown', closeOnEscape);
    };
  }, [sidebarOpen]);

  const selectedYear = useMemo(
    () => years.find(year => String(year.id) === yearId),
    [years, yearId],
  );
  if (!loggedIn) return <LoginPage onLogin={() => setLoggedIn(true)} />;

  const goToConfigTab = (key: string) => {
    setModule('configuration');
    setConfigTab(key as ConfigTabKey);
    setSidebarOpen(false);
  };

  const selectModule = (key: ModuleKey) => {
    setModule(key);
    setSidebarOpen(false);
  };
  const configPages: Record<ConfigTabKey, React.ReactNode> = {
    years: (
      <MasterDataPage
        initialTab="academic-years"
        onYearCreated={() => refreshYears(yearId)}
      />
    ),
    'grade-config': <GradeConfigurationPage />,
    'master-data': (
      <MasterDataPage
        initialTab="catalogs"
        selectedYearId={yearId}
        selectedYearStatus={selectedYear?.status}
        onYearCreated={() => refreshYears(yearId)}
      />
    ),
    classes: <ClassesPage selectedYearId={yearId} selectedSemesterId={semesterId} classEditable={selectedYear?.status === 'DRAFT'} homeroomEditable={selectedYear?.status !== 'COMPLETED'} />,
    students: <StudentEnrollmentPage selectedYearId={yearId} editable={selectedYear?.status !== 'COMPLETED'} />,
    assignments: <AssignmentsPage selectedYearId={yearId} />,
    validation: <ValidationPage academicYearId={yearId} onNavigate={goToConfigTab} />,
    activation: (
      <ActivationPage
        academicYearId={yearId}
        academicYearStatus={selectedYear?.status}

        onChanged={() => Promise.all([refreshYears(yearId), refreshSemesters(yearId)]).then(() => undefined)}
        onNavigate={goToConfigTab}
      />
    ),
  };

  const content = module === 'teachers'
    ? <UsersPage />
    : module === 'students-attendance'
      ? <StudentsAttendancePage selectedYearId={yearId} selectedSemesterId={semesterId} />
      : module === 'grades'
        ? <GradesManagementPage selectedYearId={yearId} selectedSemesterId={semesterId} />
      : module === 'timetables'
        ? <TimetablesPage selectedYearId={yearId} selectedSemesterId={semesterId} />
        : module === 'announcements'
          ? <AnnouncementsPage selectedYearId={yearId} />
        : configPages[configTab];

  return (
    <div className={`app-shell ${sidebarOpen ? 'sidebar-is-open' : ''}`}>
      <button className="sidebar-backdrop" aria-label="Đóng menu" onClick={() => setSidebarOpen(false)} />
      <aside className="workflow-sidebar" aria-label="Menu quản trị">
        <button className="brand" onClick={() => selectModule('configuration')}>
          <span className="brand-mark">MF</span>
          <span className="brand-copy"><strong>MyFschool</strong><small>Quản trị nhà trường</small></span>
        </button>
        <div className="workflow-nav-label">Phân hệ quản trị</div>
        <nav className="workflow-nav">
          {MODULES.map(item => (
            <button
              key={item.key}
              className={module === item.key ? 'active' : ''}
              onClick={() => selectModule(item.key)}
              title={item.label}
            >
              <span className="module-icon"><NavIcon name={item.icon} /></span>
              <span className="module-label">{item.label}{item.key === 'announcements' && pendingAnnouncements > 0 ? ` (${pendingAnnouncements})` : ''}</span>
            </button>
          ))}
        </nav>
        <button className="logout-button" title="Đăng xuất" onClick={() => { logout(); setLoggedIn(false); }}>
          <span aria-hidden="true">→</span><span className="logout-label">Đăng xuất</span>
        </button>
      </aside>

      <section className="workspace">
        <header className="context-bar">
          <button className="mobile-menu-button" aria-label="Mở menu" aria-expanded={sidebarOpen} onClick={() => setSidebarOpen(true)}>
            <span></span><span></span><span></span>
          </button>
          <div>
            <p>Năm học</p>
            <select value={yearId} onChange={event => setYearId(event.target.value)} disabled={loadingContext}>
              <option value="">Chưa có năm học</option>
              {years.map(year => (
                <option key={year.id} value={year.id}>{year.name} · {STATUS_LABELS[year.status] || year.status}</option>
              ))}
            </select>
          </div>
          <div>
            <p>Học kỳ</p>
            <select value={semesterId} onChange={event => setSemesterId(event.target.value)} disabled={!yearId}>
              <option value="">Chưa có học kỳ</option>
              {semesters.map(semester => (
                <option key={semester.id} value={semester.id}>{semester.name} · {STATUS_LABELS[semester.status] || semester.status}</option>
              ))}
            </select>
          </div>
        </header>
        {module === 'configuration' && (
          <nav className="configuration-tabs" aria-label="Các bước cấu hình năm học">
            {CONFIG_TABS.map(tab => (
              <button key={tab.key} className={configTab === tab.key ? 'active' : ''} onClick={() => setConfigTab(tab.key)}>
                <span><NavIcon name={tab.icon} /></span>{tab.label}
              </button>
            ))}
          </nav>
        )}
        <main className="page-content">{content}</main>
      </section>
    </div>
  );
}
