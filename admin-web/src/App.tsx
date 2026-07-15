import { useEffect, useMemo, useState } from 'react';
import { getAcademicYears } from './api/academicYear';
import { isAdminLoggedIn, logout } from './api/auth';
import { getSemesters } from './api/semester';
import AssignmentsPage from './pages/AssignmentsPage';
import ClassesPage from './pages/ClassesPage';
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
import PaymentSettingsPage from './pages/PaymentSettingsPage';
import PeriodicReviewsPage from './pages/PeriodicReviewsPage';
import HomeroomMonitoringPage from './pages/HomeroomMonitoringPage';
import { getAnnouncements as getAdminAnnouncements } from './api/announcement';
import SetupWizardShell, { type WizardStepKey } from './components/SetupWizardShell';

/* ── Types ───────────────────────────────────────────────────── */
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
  DRAFT: 'Đang cấu hình',
  NOT_STARTED: 'Chưa bắt đầu',
  ACTIVE: 'Đang hoạt động',
  COMPLETED: 'Đã hoàn thành',
};
const STATUS_CLASS: Record<string, string> = {
  DRAFT: 'draft',
  NOT_STARTED: 'draft',
  ACTIVE: 'active',
  COMPLETED: 'completed',
};

type ModuleKey =
  | 'dashboard'
  | 'configuration'
  | 'teachers'
  | 'students-attendance'
  | 'grades'
  | 'timetables'
  | 'payments'
  | 'reviews'
  | 'monitoring'
  | 'announcements';

/* ─── SVG Icons ──────────────────────────────────────────────── */
function DashboardIcon()  { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/></svg>; }
function SettingsIcon()   { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.12 2.12-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1 1.55V20h-3v-.09a1.7 1.7 0 0 0-1-1.55 1.7 1.7 0 0 0-1.88.34l-.06.06-2.12-2.12.06-.06A1.7 1.7 0 0 0 7.08 15a1.7 1.7 0 0 0-1.55-1H5.4v-3h.13a1.7 1.7 0 0 0 1.55-1 1.7 1.7 0 0 0-.34-1.88l-.06-.06 2.12-2.12.06.06a1.7 1.7 0 0 0 1.88.34 1.7 1.7 0 0 0 1-1.55V4.7h3v.09a1.7 1.7 0 0 0 1 1.55 1.7 1.7 0 0 0 1.88-.34l.06-.06 2.12 2.12-.06.06A1.7 1.7 0 0 0 19.4 10a1.7 1.7 0 0 0 1.55 1h.09v3h-.09a1.7 1.7 0 0 0-1.55 1Z"/></svg>; }
function TeacherIcon()    { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><circle cx="9" cy="8" r="3"/><path d="M3.5 20v-1.5A4.5 4.5 0 0 1 8 14h2a4.5 4.5 0 0 1 4.5 4.5V20M15 5h6v8h-5M17 8h2M17 10.5h2"/></svg>; }
function AttendanceIcon() { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>; }
function GradeIcon()      { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M4 5h16v14H4z"/><path d="M8 9h8M8 13h5"/></svg>; }
function TimetableIcon()  { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 3v4M17 3v4M3 10h18M7 14h2M11 14h2M15 14h2M7 17.5h2M11 17.5h2"/></svg>; }
function BellIcon()       { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/></svg>; }
function PaymentIcon()    { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 9h18M7 15h4"/></svg>; }
function LogoutIcon()     { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>; }
function SchoolIcon()     { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="m3 10 9-5 9 5-9 5-9-5Z"/><path d="M6 12.5V18h12v-5.5M9 20v-4h6v4"/></svg>; }
function ArrowRightIcon() { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" width="18" height="18"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>; }

/* ─── Operations Dashboard Module Cards ──────────────────────── */
const OPS_MODULES = [
  {
    key: 'teachers' as ModuleKey,
    label: 'Quản lý giáo viên',
    description: 'Quản lý giáo viên, phụ huynh và học sinh trên hai tab tài khoản riêng.',
    Icon: TeacherIcon,
  },
  {
    key: 'students-attendance' as ModuleKey,
    label: 'Điểm danh học sinh',
    description: 'Theo dõi điểm danh theo lớp và từng ngày. Duyệt đơn xin phép nghỉ của học sinh.',
    Icon: AttendanceIcon,
  },
  {
    key: 'grades' as ModuleKey,
    label: 'Quản lý điểm số',
    description: 'Xem và theo dõi kết quả học tập của học sinh theo lớp và học kỳ.',
    Icon: GradeIcon,
  },
  {
    key: 'timetables' as ModuleKey,
    label: 'Thời khóa biểu',
    description: 'Xem lịch học theo từng lớp. Quản lý phân tiết và thời gian học trong tuần.',
    Icon: TimetableIcon,
  },
  {
    key: 'payments' as ModuleKey,
    label: 'Thanh toán & học phí',
    description: 'Cấu hình tài khoản ngân hàng nhận chuyển khoản theo từng năm học.',
    Icon: PaymentIcon,
  },
  {
    key: 'reviews' as ModuleKey,
    label: 'Nhận xét định kỳ',
    description: 'Theo dõi tiến độ nhận xét môn, báo cáo GVCN và hạnh kiểm đã công bố.',
    Icon: GradeIcon,
  },
  {
    key: 'monitoring' as ModuleKey,
    label: 'Theo dõi học sinh',
    description: 'Theo dõi cảnh báo, chỉ số lớp và cấu hình ngưỡng rủi ro theo năm học, học kỳ.',
    Icon: AttendanceIcon,
  },
  {
    key: 'announcements' as ModuleKey,
    label: 'Thông báo',
    description: 'Gửi thông báo đến phụ huynh và học sinh. Duyệt các thông báo đang chờ phê duyệt.',
    Icon: BellIcon,
  },
] as const;

/* ─── Operations Dashboard ───────────────────────────────────── */
function OperationsDashboard({
  years,
  semesters,
  yearId,
  semesterId,
  pendingAnnouncements,
  onNavigate,
}: {
  years: AcademicYearItem[];
  semesters: SemesterItem[];
  yearId: string;
  semesterId: string;
  pendingAnnouncements: number;
  onNavigate: (module: ModuleKey) => void;
}) {
  const selectedYear     = years.find(y => String(y.id) === yearId);
  const selectedSemester = semesters.find(s => String(s.id) === semesterId);
  const isOperational    = selectedYear?.status === 'ACTIVE';
  const hasDraftYear     = years.some(y => y.status === 'DRAFT');

  return (
    <main className="page-stack ops-dashboard" id="main-content" role="main" aria-label="Tổng quan vận hành">

      {/* Page heading */}
      <div className="page-heading">
        <div>
          <h1>Tổng quan</h1>
          <p>
            {selectedYear
              ? <>Năm học <strong>{selectedYear.name}</strong>{selectedSemester ? <> · {selectedSemester.name}</> : ''}</>
              : 'Chọn năm học để xem tổng quan'}
          </p>
        </div>
      </div>

      {/* Warning: no active year */}
      {!isOperational && selectedYear && (
        <div className="ops-no-year-banner" role="alert" aria-live="polite">
          <div className="ops-banner-icon" aria-hidden="true">⚡</div>
          <div>
            <strong>Năm học chưa được kích hoạt</strong>
            <p>
              Năm học <strong>{selectedYear.name}</strong> đang ở trạng thái{' '}
              <em>{STATUS_LABELS[selectedYear.status]}</em>.
              {selectedYear.status === 'DRAFT'
                ? ' Hoàn thành cấu hình và kích hoạt trước khi sử dụng các phân hệ.'
                : ' Hãy chọn năm học đang hoạt động để quản lý.'}
            </p>
          </div>
          {selectedYear.status === 'DRAFT' && (
            <button onClick={() => onNavigate('configuration')} aria-label="Đến trang cấu hình năm học">
              Đến trang cấu hình →
            </button>
          )}
        </div>
      )}

      {/* No year selected */}
      {!selectedYear && years.length > 0 && (
        <div className="ops-no-year-banner" role="alert">
          <div className="ops-banner-icon" aria-hidden="true">📅</div>
          <div>
            <strong>Chưa chọn năm học</strong>
            <p>Hãy chọn năm học ở thanh tiêu đề để bắt đầu quản lý.</p>
          </div>
        </div>
      )}

      {/* No years at all */}
      {years.length === 0 && (
        <div className="ops-no-year-banner" role="alert">
          <div className="ops-banner-icon" aria-hidden="true">🏫</div>
          <div>
            <strong>Chưa có năm học nào</strong>
            <p>Hệ thống cần ít nhất một năm học. Bắt đầu bằng cách tạo năm học mới.</p>
          </div>
          {hasDraftYear && (
            <button onClick={() => onNavigate('configuration')} aria-label="Cấu hình năm học">
              Cấu hình năm học →
            </button>
          )}
        </div>
      )}

      {/* Active year stats */}
      {selectedYear && (
        <section aria-label="Thông tin năm học đang chọn">
          <div className="year-status-card">
            <div className="year-status-row">
              <div>
                <div className="dashboard-section-title">Năm học đang xem</div>
                <div className="year-status-name">{selectedYear.name}</div>
                <div className="year-status-dates">{selectedYear.startDate} — {selectedYear.endDate}</div>
              </div>
              <span
                className={`ctx-status-badge ${STATUS_CLASS[selectedYear.status] || ''}`}
                aria-label={`Trạng thái: ${STATUS_LABELS[selectedYear.status]}`}
              >
                {STATUS_LABELS[selectedYear.status] || selectedYear.status}
              </span>
            </div>
            {semesters.length > 0 && (
              <div className="year-status-semesters" role="list" aria-label="Danh sách học kỳ">
                {semesters.map(s => (
                  <span
                    key={s.id}
                    className={`semester-pill ${s.status === 'ACTIVE' ? 'active' : ''}`}
                    role="listitem"
                  >
                    {s.name} · {STATUS_LABELS[s.status] || s.status}
                  </span>
                ))}
              </div>
            )}
          </div>
        </section>
      )}

      {/* Module cards */}
      <section aria-label="Các phân hệ quản lý">
        <div className="ops-section-title">Phân hệ quản lý</div>
        <nav className="ops-module-grid" aria-label="Menu phân hệ">
          {OPS_MODULES.map(item => {
            const Icon = item.Icon;
            const isPending = item.key === 'announcements' && pendingAnnouncements > 0;
            return (
              <button
                key={item.key}
                className="ops-module-card"
                onClick={() => onNavigate(item.key)}
                aria-label={`Mở phân hệ ${item.label}`}
              >
                <div className="ops-card-icon" aria-hidden="true"><Icon /></div>
                <div className="ops-card-body">
                  <div className="ops-card-header">
                    <strong>{item.label}</strong>
                    {isPending && (
                      <span className="nav-badge" aria-label={`${pendingAnnouncements} thông báo chờ duyệt`}>
                        {pendingAnnouncements}
                      </span>
                    )}
                  </div>
                  <p>{item.description}</p>
                </div>
                <div className="ops-card-arrow" aria-hidden="true">
                  <ArrowRightIcon />
                </div>
              </button>
            );
          })}
        </nav>
      </section>

      {/* All years list */}
      {years.length > 0 && (
        <section aria-label="Danh sách toàn bộ năm học">
          <div className="ops-section-title">Tất cả năm học</div>
          <div className="table-responsive" role="region" aria-label="Bảng năm học">
            <table aria-label="Danh sách các năm học trong hệ thống">
              <thead>
                <tr>
                  <th scope="col">Năm học</th>
                  <th scope="col">Thời gian</th>
                  <th scope="col">Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                {years.slice(0, 8).map(year => (
                  <tr key={year.id}>
                    <td><strong>{year.name}</strong></td>
                    <td style={{ color: 'var(--text-secondary)', fontSize: 12 }}>
                      {year.startDate} → {year.endDate}
                    </td>
                    <td>
                      <span className={`badge-status ${STATUS_CLASS[year.status] || ''}`}>
                        {STATUS_LABELS[year.status] || year.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </main>
  );
}

/* ─── App root ───────────────────────────────────────────────── */
export default function App() {
  const [loggedIn, setLoggedIn]         = useState(isAdminLoggedIn());
  const [module, setModule]             = useState<ModuleKey>('dashboard');
  const [configTab, setConfigTab]       = useState<WizardStepKey>('years');
  const [sidebarOpen, setSidebarOpen]   = useState(false);
  const [years, setYears]               = useState<AcademicYearItem[]>([]);
  const [semesters, setSemesters]       = useState<SemesterItem[]>([]);
  const [yearId, setYearId]             = useState('');
  const [semesterId, setSemesterId]     = useState('');
  const [loadingCtx, setLoadingCtx]     = useState(false);
  const [pendingAnn, setPendingAnn]     = useState(0);

  /* Data refresh helpers */
  async function refreshYears(preferredYearId?: string) {
    setLoadingCtx(true);
    try {
      const data = (await getAcademicYears()) as AcademicYearItem[];
      const sorted = [...(data || [])].sort((a, b) => b.id - a.id);
      setYears(sorted);
      const preferredYear = preferredYearId
        ? sorted.find(y => String(y.id) === preferredYearId)
        : undefined;
      const selected =
        preferredYear ||
        sorted.find(y => y.status === 'ACTIVE') ||
        sorted.find(y => String(y.id) === yearId) ||
        sorted.find(y => y.status === 'DRAFT') ||
        sorted[0];
      setYearId(selected ? String(selected.id) : '');
    } finally {
      setLoadingCtx(false);
    }
  }

  async function refreshSemesters(targetYearId = yearId) {
    if (!targetYearId) { setSemesters([]); setSemesterId(''); return; }
    const data = await getSemesters(targetYearId) as SemesterItem[];
    const list = data || [];
    setSemesters(list);
    const sel =
      list.find(s => String(s.id) === semesterId) ||
      list.find(s => s.status === 'ACTIVE') ||
      list[0];
    setSemesterId(sel ? String(sel.id) : '');
  }

  useEffect(() => { if (loggedIn) refreshYears(); }, [loggedIn]);
  useEffect(() => { refreshSemesters(yearId); }, [yearId]);

  /* Poll pending announcements */
  useEffect(() => {
    let alive = true;
    const poll = async () => {
      if (!yearId) { setPendingAnn(0); return; }
      try {
        const rows = await getAdminAnnouncements(yearId, 'PENDING');
        if (alive) setPendingAnn(rows.length);
      } catch { /* silent */ }
    };
    void poll();
    const t = window.setInterval(poll, 15_000);
    return () => { alive = false; clearInterval(t); };
  }, [yearId]);

  /* Sidebar lock on mobile */
  useEffect(() => {
    document.body.classList.toggle('sidebar-lock', sidebarOpen);
    const onEsc = (e: KeyboardEvent) => { if (e.key === 'Escape') setSidebarOpen(false); };
    if (sidebarOpen) window.addEventListener('keydown', onEsc);
    return () => {
      document.body.classList.remove('sidebar-lock');
      window.removeEventListener('keydown', onEsc);
    };
  }, [sidebarOpen]);

  const selectedYear = useMemo(
    () => years.find(y => String(y.id) === yearId),
    [years, yearId],
  );

  /* Q1: Show "Cấu hình năm học" when any DRAFT year exists (not just selected year).
     This prevents confusion where user has to switch year to see the config entry. */
  const draftYear = years.find(y => y.status === 'DRAFT');
  const showConfigInSidebar = !!draftYear;

  /* Navigation helpers */
  const navigate = (mod: ModuleKey, tab?: WizardStepKey) => {
    setModule(mod);
    if (tab) setConfigTab(tab);
    setSidebarOpen(false);
  };

  if (!loggedIn) {
    return <LoginPage onLogin={() => { setLoggedIn(true); setModule('dashboard'); }} />;
  }

  /* ── Config pages (rendered inside wizard shell) ─────────────── */
  const configPageContent: Record<WizardStepKey, React.ReactNode> = {
    years: (
      <MasterDataPage
        initialTab="academic-years"
        selectedYearId={yearId}
        selectedYearStatus={selectedYear?.status}
        onYearCreated={() => refreshYears(yearId)}
      />
    ),
    'grade-config': (
      <GradeConfigurationPage
        selectedYearId={yearId}
        selectedYearStatus={selectedYear?.status}
      />
    ),
    'master-data': (
      <MasterDataPage
        initialTab="catalogs"
        selectedYearId={yearId}
        selectedYearStatus={selectedYear?.status}
        onYearCreated={() => refreshYears(yearId)}
      />
    ),
    classes: (
      <ClassesPage
        selectedYearId={yearId}
        selectedSemesterId={semesterId}
        classEditable={selectedYear?.status === 'DRAFT'}
        homeroomEditable={selectedYear?.status !== 'COMPLETED'}
      />
    ),
    assignments: <AssignmentsPage selectedYearId={yearId} />,
    validation: (
      <ValidationPage
        academicYearId={yearId}
        onNavigate={(key) => setConfigTab(key as WizardStepKey)}
      />
    ),
    activation: (
      <ActivationPage
        academicYearId={yearId}
        academicYearStatus={selectedYear?.status}
        onChanged={() => Promise.all([refreshYears(yearId), refreshSemesters(yearId)]).then(() => undefined)}
        onNavigate={(key) => setConfigTab(key as WizardStepKey)}
      />
    ),
  };

  /* ── Main content area ───────────────────────────────────────── */
  const mainContent =
    module === 'configuration' ? (
      <SetupWizardShell
        currentStep={configTab}
        onStepChange={setConfigTab}
        yearName={selectedYear?.name}
        yearStatus={selectedYear?.status}
        onExit={() => setModule('dashboard')}
      >
        {configPageContent[configTab]}
      </SetupWizardShell>
    ) : module === 'dashboard' ? (
      <div className="page-content">
        <OperationsDashboard
          years={years}
          semesters={semesters}
          yearId={yearId}
          semesterId={semesterId}
          pendingAnnouncements={pendingAnn}
          onNavigate={navigate}
        />
      </div>
    ) : module === 'teachers' ? (
      <div className="page-content">
        <UsersPage
          selectedYearId={yearId}
          studentAccountsEditable={selectedYear?.status !== 'COMPLETED'}
        />
      </div>
    ) : module === 'students-attendance' ? (
      <div className="page-content">
        <StudentsAttendancePage selectedYearId={yearId} selectedSemesterId={semesterId} />
      </div>
    ) : module === 'grades' ? (
      <div className="page-content">
        <GradesManagementPage selectedYearId={yearId} selectedSemesterId={semesterId} />
      </div>
    ) : module === 'timetables' ? (
      <div className="page-content">
        <TimetablesPage selectedYearId={yearId} selectedSemesterId={semesterId} />
      </div>
    ) : module === 'payments' ? (
      <div className="page-content">
        <PaymentSettingsPage
          selectedYearId={yearId}
          selectedYearStatus={selectedYear?.status}
        />
      </div>
    ) : module === 'reviews' ? (
      <div className="page-content">
        <PeriodicReviewsPage selectedYearId={yearId} selectedSemesterId={semesterId} />
      </div>
    ) : module === 'monitoring' ? (
      <div className="page-content">
        <HomeroomMonitoringPage selectedYearId={yearId} selectedSemesterId={semesterId} />
      </div>
    ) : module === 'announcements' ? (
      <div className="page-content">
        <AnnouncementsPage selectedYearId={yearId} />
      </div>
    ) : null;

  return (
    <>
      {/* SEO: Skip link */}
      <a href="#main-content" className="skip-link">Bỏ qua điều hướng</a>

      <div className={`app-shell ${sidebarOpen ? 'sidebar-is-open' : ''}`}>
        {/* Mobile backdrop */}
        <button
          className="sidebar-backdrop"
          aria-label="Đóng menu"
          onClick={() => setSidebarOpen(false)}
        />

        {/* ══ Sidebar ══════════════════════════════════════════════ */}
        <aside
          className="workflow-sidebar"
          id="sidebar"
          aria-label="Menu điều hướng quản trị"
          role="navigation"
        >
          {/* Brand */}
          <button
            className="brand"
            onClick={() => navigate('dashboard')}
            aria-label="Về trang tổng quan"
          >
            <span className="brand-mark" aria-hidden="true">MF</span>
            <span className="brand-copy">
              <strong>MyFschool</strong>
              <small>Quản trị nhà trường</small>
            </span>
          </button>

          {/* ─── THIẾT LẬP section ─── */}
          {showConfigInSidebar && (
            <>
              <div className="sidebar-section-label" aria-hidden="true">Thiết lập</div>
              <nav className="workflow-nav" aria-label="Thiết lập hệ thống">
                <button
                  className={module === 'configuration' ? 'active' : ''}
                  onClick={() => navigate('configuration', 'years')}
                  title="Cấu hình năm học"
                  aria-current={module === 'configuration' ? 'page' : undefined}
                  aria-label="Cấu hình năm học"
                >
                  <span className="module-icon" aria-hidden="true"><SettingsIcon /></span>
                  <span className="module-label">
                    Cấu hình năm học
                    {selectedYear?.status === 'DRAFT' && (
                      <span
                        style={{
                          display: 'inline-flex', alignItems: 'center', gap: 3,
                          marginLeft: 7, padding: '1px 6px',
                          borderRadius: 4, background: 'rgba(255,107,53,.2)',
                          color: '#FF6B35', fontSize: 9, fontWeight: 800, letterSpacing: '.06em',
                        }}
                        aria-label="Đang cấu hình"
                      >
                        NHÁP
                      </span>
                    )}
                  </span>
                </button>
              </nav>
            </>
          )}

          {/* ─── VẬN HÀNH section ─── */}
          <div className="sidebar-section-label" aria-hidden="true">Vận hành</div>
          <nav className="workflow-nav" aria-label="Quản lý vận hành">
            {/* Dashboard */}
            <button
              className={module === 'dashboard' ? 'active' : ''}
              onClick={() => navigate('dashboard')}
              title="Tổng quan"
              aria-current={module === 'dashboard' ? 'page' : undefined}
              aria-label="Tổng quan"
            >
              <span className="module-icon" aria-hidden="true"><DashboardIcon /></span>
              <span className="module-label">Tổng quan</span>
            </button>

            {/* Teachers */}
            <button
              className={module === 'teachers' ? 'active' : ''}
              onClick={() => navigate('teachers')}
              title="Quản lý giáo viên"
              aria-current={module === 'teachers' ? 'page' : undefined}
              aria-label="Quản lý giáo viên"
            >
              <span className="module-icon" aria-hidden="true"><TeacherIcon /></span>
              <span className="module-label">Quản lý giáo viên</span>
            </button>

            {/* Attendance */}
            <button
              className={module === 'students-attendance' ? 'active' : ''}
              onClick={() => navigate('students-attendance')}
              title="Điểm danh học sinh"
              aria-current={module === 'students-attendance' ? 'page' : undefined}
              aria-label="Điểm danh học sinh"
            >
              <span className="module-icon" aria-hidden="true"><AttendanceIcon /></span>
              <span className="module-label">Điểm danh học sinh</span>
            </button>

            {/* Grades */}
            <button
              className={module === 'grades' ? 'active' : ''}
              onClick={() => navigate('grades')}
              title="Quản lý điểm số"
              aria-current={module === 'grades' ? 'page' : undefined}
              aria-label="Quản lý điểm số"
            >
              <span className="module-icon" aria-hidden="true"><GradeIcon /></span>
              <span className="module-label">Quản lý điểm số</span>
            </button>

            {/* Timetables */}
            <button
              className={module === 'timetables' ? 'active' : ''}
              onClick={() => navigate('timetables')}
              title="Thời khóa biểu"
              aria-current={module === 'timetables' ? 'page' : undefined}
              aria-label="Thời khóa biểu"
            >
              <span className="module-icon" aria-hidden="true"><TimetableIcon /></span>
              <span className="module-label">Thời khóa biểu</span>
            </button>

            {/* Payments */}
            <button
              className={module === 'payments' ? 'active' : ''}
              onClick={() => navigate('payments')}
              title="Thanh toán & học phí"
              aria-current={module === 'payments' ? 'page' : undefined}
              aria-label="Thanh toán và học phí"
            >
              <span className="module-icon" aria-hidden="true"><PaymentIcon /></span>
              <span className="module-label">Thanh toán & học phí</span>
            </button>

            {/* Announcements */}
            <button
              className={module === 'reviews' ? 'active' : ''}
              onClick={() => navigate('reviews')}
              title="Nhận xét định kỳ"
              aria-current={module === 'reviews' ? 'page' : undefined}
              aria-label="Nhận xét định kỳ"
            >
              <span className="module-icon" aria-hidden="true"><GradeIcon /></span>
              <span className="module-label">Nhận xét định kỳ</span>
            </button>

            {/* Homeroom monitoring */}
            <button
              className={module === 'monitoring' ? 'active' : ''}
              onClick={() => navigate('monitoring')}
              title="Theo dõi học sinh"
              aria-current={module === 'monitoring' ? 'page' : undefined}
              aria-label="Theo dõi học sinh"
            >
              <span className="module-icon" aria-hidden="true"><AttendanceIcon /></span>
              <span className="module-label">Theo dõi học sinh</span>
            </button>

            {/* Announcements */}
            <button
              className={module === 'announcements' ? 'active' : ''}
              onClick={() => navigate('announcements')}
              title="Thông báo"
              aria-current={module === 'announcements' ? 'page' : undefined}
              aria-label={pendingAnn > 0 ? `Thông báo — ${pendingAnn} chờ duyệt` : 'Thông báo'}
            >
              <span className="module-icon" aria-hidden="true"><BellIcon /></span>
              <span className="module-label">
                Thông báo
                {pendingAnn > 0 && (
                  <span className="nav-badge" aria-label={`${pendingAnn} chờ duyệt`}>
                    {pendingAnn}
                  </span>
                )}
              </span>
            </button>
          </nav>

          {/* Footer: user + logout */}
          <div className="sidebar-footer">
            <div className="sidebar-user" aria-label="Thông tin tài khoản">
              <span className="sidebar-user-info">
                <span className="sidebar-user-name">Quản trị viên</span>
                <span className="sidebar-user-role">Admin · FPT Schools</span>
              </span>
            </div>
            <button
              className="logout-button"
              title="Đăng xuất"
              aria-label="Đăng xuất khỏi hệ thống"
              onClick={() => { logout(); setLoggedIn(false); }}
            >
              <span className="logout-icon" aria-hidden="true"><LogoutIcon /></span>
              <span className="logout-label">Đăng xuất</span>
            </button>
          </div>
        </aside>

        {/* ══ Workspace ═════════════════════════════════════════════ */}
        <div className="workspace">

          {/* ─── Context Header ─── */}
          <header
            className="context-bar"
            role="banner"
            aria-label="Thanh ngữ cảnh — chọn năm học và học kỳ"
          >
            {/* Mobile hamburger */}
            <button
              className="mobile-menu-button"
              aria-label="Mở menu điều hướng"
              aria-expanded={sidebarOpen}
              aria-controls="sidebar"
              onClick={() => setSidebarOpen(true)}
            >
              <span aria-hidden="true" />
              <span aria-hidden="true" />
              <span aria-hidden="true" />
            </button>

            {/* Page title on mobile */}
            <span className="sr-only" aria-live="polite">
              {module === 'configuration' ? 'Cấu hình năm học'
               : module === 'dashboard' ? 'Tổng quan'
               : module === 'teachers' ? 'Quản lý giáo viên'
               : module === 'students-attendance' ? 'Điểm danh học sinh'
               : module === 'grades' ? 'Quản lý điểm số'
               : module === 'timetables' ? 'Thời khóa biểu'
               : module === 'payments' ? 'Thanh toán & học phí'
               : module === 'reviews' ? 'Nhận xét định kỳ'
               : module === 'monitoring' ? 'Theo dõi học sinh'
               : 'Thông báo'}
            </span>

            <div style={{ flex: 1 }} />

            {/* Year selector */}
            <div>
              <label htmlFor="year-select" className="ctx-label">Năm học</label>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <div className="ctx-pill-select">
                  <select
                    id="year-select"
                    value={yearId}
                    onChange={e => setYearId(e.target.value)}
                    disabled={loadingCtx}
                    aria-label="Chọn năm học hiện tại"
                  >
                    <option value="">— Chưa có năm học —</option>
                    {years.map(year => (
                      <option key={year.id} value={year.id}>{year.name}</option>
                    ))}
                  </select>
                </div>
                {selectedYear && (
                  <span
                    className={`ctx-status-badge ${STATUS_CLASS[selectedYear.status] || ''}`}
                    aria-label={`Trạng thái năm học: ${STATUS_LABELS[selectedYear.status]}`}
                  >
                    {STATUS_LABELS[selectedYear.status] || selectedYear.status}
                  </span>
                )}
              </div>
            </div>

            {/* Semester selector */}
            <div>
              <label htmlFor="semester-select" className="ctx-label">Học kỳ</label>
              <div className="ctx-pill-select">
                <select
                  id="semester-select"
                  value={semesterId}
                  onChange={e => setSemesterId(e.target.value)}
                  disabled={!yearId || semesters.length === 0}
                  aria-label="Chọn học kỳ"
                >
                  <option value="">— Chọn học kỳ —</option>
                  {semesters.map(s => (
                    <option key={s.id} value={s.id}>
                      {s.name} · {STATUS_LABELS[s.status] || s.status}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </header>

          {/* ─── Main content (wizard has its own layout) ─── */}
          <div id="main-content" tabIndex={-1}>
            {mainContent}
          </div>
        </div>
      </div>
    </>
  );
}
