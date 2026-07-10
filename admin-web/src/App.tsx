import { useEffect, useMemo, useState } from 'react';
import { getAcademicYears } from './api/academicYear';
import { isAdminLoggedIn, logout } from './api/auth';
import { getSemesters } from './api/semester';
import AssignmentsPage from './pages/AssignmentsPage';
import ClassesPage from './pages/ClassesPage';
import StudentEnrollmentPage from './pages/StudentEnrollmentPage';
import LoginPage from './pages/LoginPage';
import MasterDataPage from './pages/MasterDataPage';
import SetupWizardPage from './pages/SetupWizardPage';
import UsersPage from './pages/UsersPage';
import ValidationPage from './pages/ValidationPage';
import ActivationPage from './pages/ActivationPage';

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

const STEPS = [
  { key: 'years', number: 1, label: 'Khởi tạo năm học' },
  { key: 'master-data', number: 2, label: 'Cấu hình danh mục' },
  { key: 'teachers', number: 3, label: 'Quản lý giáo viên' },
  { key: 'classes', number: 4, label: 'Sinh lớp học' },
  { key: 'students', number: 5, label: 'Thêm học sinh & PH' },
  { key: 'assignments', number: 6, label: 'Phân công giảng dạy' },
  { key: 'validation', number: 7, label: 'Kiểm tra dữ liệu' },
  { key: 'activation', number: 8, label: 'Kích hoạt năm học' },
] as const;

type PageKey = 'workflow' | typeof STEPS[number]['key'];

export default function App() {
  const [loggedIn, setLoggedIn] = useState(isAdminLoggedIn());
  const [page, setPage] = useState<PageKey>('workflow');
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

  useEffect(() => {
    if (loggedIn) refreshYears();
  }, [loggedIn]);

  useEffect(() => {
    if (!yearId) {
      setSemesters([]);
      setSemesterId('');
      return;
    }
    getSemesters(yearId).then((data: any) => {
      const list = (data || []) as SemesterItem[];
      setSemesters(list);
      const selected = list.find(s => String(s.id) === semesterId)
        || list.find(s => s.status === 'ACTIVE')
        || list[0];
      setSemesterId(selected ? String(selected.id) : '');
    });
  }, [yearId]);

  const selectedYear = useMemo(
    () => years.find(year => String(year.id) === yearId),
    [years, yearId],
  );
  const selectedSemester = useMemo(
    () => semesters.find(semester => String(semester.id) === semesterId),
    [semesters, semesterId],
  );

  if (!loggedIn) return <LoginPage onLogin={() => setLoggedIn(true)} />;

  const goTo = (key: string) => setPage(key as PageKey);
  const pages: Record<PageKey, React.ReactNode> = {
    workflow: <SetupWizardPage onNavigate={goTo} selectedYear={selectedYear} />,
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
    teachers: <UsersPage />,
    classes: <ClassesPage selectedYearId={yearId} selectedSemesterId={semesterId} editable={selectedYear?.status === 'DRAFT'} />,
    students: <StudentEnrollmentPage selectedYearId={yearId} editable={selectedYear?.status === 'DRAFT'} />,
    assignments: <AssignmentsPage selectedYearId={yearId} />,
    validation: <ValidationPage academicYearId={yearId} onNavigate={goTo} />,
    activation: (
      <ActivationPage
        academicYearId={yearId}
        academicYearStatus={selectedYear?.status}
        onChanged={() => refreshYears(yearId)}
        onNavigate={goTo}
      />
    ),
  };

  return (
    <div className="app-shell">
      <aside className="workflow-sidebar">
        <button className="brand" onClick={() => setPage('workflow')}>
          <span className="brand-mark">MF</span>
          <span><strong>MyFschool</strong><small>Thiết lập năm học</small></span>
        </button>
        <div className="workflow-nav-label">Quy trình Admin</div>
        <nav className="workflow-nav">
          {STEPS.map(step => (
            <button
              key={step.key}
              className={page === step.key ? 'active' : ''}
              onClick={() => setPage(step.key)}
            >
              <span>{step.number}</span>
              {step.label}
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
            <p>Năm học đang cấu hình</p>
            <select value={yearId} onChange={event => setYearId(event.target.value)} disabled={loadingContext}>
              <option value="">Chưa có năm học</option>
              {years.map(year => (
                <option key={year.id} value={year.id}>{year.name} · {STATUS_LABELS[year.status] || year.status}</option>
              ))}
            </select>
          </div>
          {page !== 'assignments' && <div>
            <p>Học kỳ</p>
            <select value={semesterId} onChange={event => setSemesterId(event.target.value)} disabled={!yearId}>
              <option value="">Chưa có học kỳ</option>
              {semesters.map(semester => (
                <option key={semester.id} value={semester.id}>{semester.name} · {STATUS_LABELS[semester.status] || semester.status}</option>
              ))}
            </select>
          </div>}
          <div className={`year-state state-${selectedYear?.status?.toLowerCase() || 'none'}`}>
            <span>Trạng thái</span>
            <strong>{selectedYear?.status ? STATUS_LABELS[selectedYear.status] : 'CHƯA KHỞI TẠO'}</strong>
            <small>{page === 'assignments' ? 'Phân công áp dụng toàn năm' : selectedSemester?.name || 'Chọn năm học để bắt đầu'}</small>
          </div>
        </header>
        <main className="page-content">{pages[page]}</main>
      </section>
    </div>
  );
}
