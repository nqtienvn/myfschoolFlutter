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
  { key: 'years', label: 'Năm học' },
  { key: 'master-data', label: 'Danh mục' },
  { key: 'classes', label: 'Lớp học' },
  { key: 'students', label: 'Học sinh & phụ huynh' },
  { key: 'assignments', label: 'Phân công giảng dạy' },
  { key: 'validation', label: 'Kiểm tra dữ liệu' },
  { key: 'activation', label: 'Kích hoạt năm học' },
] as const;

const MODULES = [
  { key: 'configuration', number: '01', label: 'Cấu hình năm học' },
  { key: 'teachers', number: '02', label: 'Quản lý giáo viên' },
  { key: 'timetables', number: '03', label: 'Thời khóa biểu' },
] as const;

type ConfigTabKey = typeof CONFIG_TABS[number]['key'];
type ModuleKey = typeof MODULES[number]['key'];

export default function App() {
  const [loggedIn, setLoggedIn] = useState(isAdminLoggedIn());
  const [module, setModule] = useState<ModuleKey>('configuration');
  const [configTab, setConfigTab] = useState<ConfigTabKey>('years');
  const [years, setYears] = useState<AcademicYearItem[]>([]);
  const [semesters, setSemesters] = useState<SemesterItem[]>([]);
  const [yearId, setYearId] = useState('');
  const [semesterId, setSemesterId] = useState('');
  const [loadingContext, setLoadingContext] = useState(false);

  async function refreshYears(preferredYearId?: string) {
    setLoadingContext(true);
    try {
      const data = (await getAcademicYears()) as AcademicYearItem[];
      const sorted = [...(data || [])].sort((a, b) => b.id - a.id);
      setYears(sorted);
      const selected = sorted.find(y => String(y.id) === (preferredYearId || yearId))
        || sorted.find(y => y.status === 'DRAFT')
        || sorted.find(y => y.status === 'ACTIVE')
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

  const selectedYear = useMemo(
    () => years.find(year => String(year.id) === yearId),
    [years, yearId],
  );
  if (!loggedIn) return <LoginPage onLogin={() => setLoggedIn(true)} />;

  const goToConfigTab = (key: string) => {
    setModule('configuration');
    setConfigTab(key as ConfigTabKey);
  };
  const configPages: Record<ConfigTabKey, React.ReactNode> = {
    years: (
      <MasterDataPage
        initialTab="academic-years"
        onYearCreated={() => refreshYears(yearId)}
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
    : module === 'timetables'
      ? <TimetablesPage selectedYearId={yearId} selectedSemesterId={semesterId} />
      : configPages[configTab];

  return (
    <div className="app-shell">
      <aside className="workflow-sidebar">
        <button className="brand" onClick={() => setModule('configuration')}>
          <span className="brand-mark">MF</span>
          <span><strong>MyFschool</strong><small>Thiết lập năm học</small></span>
        </button>
        <div className="workflow-nav-label">Phân hệ quản trị</div>
        <nav className="workflow-nav">
          {MODULES.map(item => (
            <button
              key={item.key}
              className={module === item.key ? 'active' : ''}
              onClick={() => setModule(item.key)}
            >
              <span>{item.number}</span>
              {item.label}
            </button>
          ))}
        </nav>
        <button className="logout-button" onClick={() => { logout(); setLoggedIn(false); }}>
          Đăng xuất
        </button>
      </aside>

      <section className="workspace">
        <header className="context-bar">
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
            {CONFIG_TABS.map((tab, index) => (
              <button key={tab.key} className={configTab === tab.key ? 'active' : ''} onClick={() => setConfigTab(tab.key)}>
                <span>{index + 1}</span>{tab.label}
              </button>
            ))}
          </nav>
        )}
        <main className="page-content">{content}</main>
      </section>
    </div>
  );
}
