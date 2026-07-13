import type { ReactNode } from 'react';

/* ─── Step definitions ───────────────────────────────────────── */
function CalendarIcon() { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 3v4M17 3v4M3 10h18M8 14h3M8 17h6"/></svg>; }
function GradeIcon()    { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M4 5h16v14H4z"/><path d="M8 9h8M8 13h5"/></svg>; }
function CatalogIcon()  { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><rect x="4" y="4" width="16" height="16" rx="2"/><path d="M8 8h8M8 12h8M8 16h5"/></svg>; }
function SchoolIcon()   { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="m3 10 9-5 9 5-9 5-9-5Z"/><path d="M6 12.5V18h12v-5.5M9 20v-4h6v4"/></svg>; }
function AssignIcon()   { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M4 19.5V6.8A2.8 2.8 0 0 1 6.8 4H20v15.5H6.5A2.5 2.5 0 0 0 4 22"/><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20M9 9h7M9 12h5"/></svg>; }
function ChecklistIcon(){ return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><rect x="5" y="3" width="14" height="18" rx="2"/><path d="m8 9 1.5 1.5L12 8M14 9h2M8 15l1.5 1.5L12 14M14 15h2"/></svg>; }
function PowerIcon()    { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M12 3v9"/><path d="M7.1 6.2a8 8 0 1 0 9.8 0"/></svg>; }
function ChevronLeftIcon()  { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" width="16" height="16"><polyline points="15 18 9 12 15 6"/></svg>; }
function ChevronRightIcon() { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" width="16" height="16"><polyline points="9 18 15 12 9 6"/></svg>; }

export const WIZARD_STEPS = [
  {
    key: 'years',
    label: 'Năm học & Học kỳ',
    desc: 'Tạo năm học mới, thiết lập thời gian và 2 học kỳ',
    Icon: CalendarIcon,
  },
  {
    key: 'grade-config',
    label: 'Cấu hình đầu điểm',
    desc: 'Mẫu điểm, hệ số và công thức tính GPA',
    Icon: GradeIcon,
  },
  {
    key: 'master-data',
    label: 'Danh mục dữ liệu',
    desc: 'Môn học, tiết học và ca học trong ngày',
    Icon: CatalogIcon,
  },
  {
    key: 'classes',
    label: 'Lớp học',
    desc: 'Tạo lớp học, phân công giáo viên chủ nhiệm',
    Icon: SchoolIcon,
  },
  {
    key: 'assignments',
    label: 'Phân công giảng dạy',
    desc: 'Gán giáo viên vào môn học và lớp học',
    Icon: AssignIcon,
  },
  {
    key: 'validation',
    label: 'Kiểm tra dữ liệu',
    desc: 'Xác nhận đủ điều kiện trước khi kích hoạt',
    Icon: ChecklistIcon,
  },
  {
    key: 'activation',
    label: 'Kích hoạt năm học',
    desc: 'Mở năm học cho toàn hệ thống sử dụng',
    Icon: PowerIcon,
  },
] as const;

export type WizardStepKey = typeof WIZARD_STEPS[number]['key'];

const STATUS_LABEL: Record<string, string> = {
  DRAFT: 'Đang cấu hình',
  ACTIVE: 'Đang hoạt động',
  COMPLETED: 'Đã hoàn thành',
};

interface Props {
  /** Key of the currently active step */
  currentStep: WizardStepKey;
  /** Called when user clicks a step or prev/next buttons */
  onStepChange: (key: WizardStepKey) => void;
  /** Display name of the year being configured */
  yearName?: string;
  /** DRAFT | ACTIVE | COMPLETED */
  yearStatus?: string;
  /** The page content for the current step */
  children: ReactNode;
  /** Called when user wants to go back to the main view (select year) */
  onExit?: () => void;
}

export default function SetupWizardShell({
  currentStep,
  onStepChange,
  yearName,
  yearStatus,
  children,
  onExit,
}: Props) {
  const stepIndex = WIZARD_STEPS.findIndex(s => s.key === currentStep);
  const safePIndex = Math.max(0, stepIndex);
  const totalSteps = WIZARD_STEPS.length;
  const hasPrev = safePIndex > 0;
  const hasNext = safePIndex < totalSteps - 1;
  // Progress = steps completed (0-indexed) out of total-1 steps
  const progressPct = totalSteps > 1
    ? Math.round((safePIndex / (totalSteps - 1)) * 100)
    : 0;

  const prevStep = hasPrev ? WIZARD_STEPS[safePIndex - 1] : null;
  const nextStep = hasNext ? WIZARD_STEPS[safePIndex + 1] : null;
  const currentStepData = WIZARD_STEPS[safePIndex];

  return (
    <div className="wizard-layout" role="main" aria-label={`Thiết lập năm học ${yearName || ''}`}>

      {/* ── Left Panel: Steps overview ───────────────────────────── */}
      <aside className="wizard-left" aria-label="Các bước thiết lập">

        {/* Year info box */}
        <div className="wizard-year-info">
          <div className="wizard-year-label">Đang cấu hình</div>
          <div className="wizard-year-name">{yearName || '—'}</div>
          {yearStatus && (
            <span
              className={`ctx-status-badge ${yearStatus === 'ACTIVE' ? 'active' : yearStatus === 'COMPLETED' ? 'completed' : 'draft'}`}
              aria-label={`Trạng thái: ${STATUS_LABEL[yearStatus] || yearStatus}`}
            >
              {STATUS_LABEL[yearStatus] || yearStatus}
            </span>
          )}
        </div>

        {/* Progress bar */}
        <div className="wizard-progress">
          <div className="wizard-progress-label">
            <span>Tiến độ thiết lập</span>
            <strong>Bước {safePIndex + 1}/{totalSteps}</strong>
          </div>
          <div
            className="wizard-progress-bar"
            role="progressbar"
            aria-valuenow={progressPct}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-label={`Hoàn thành ${progressPct}%`}
          >
            <div className="wizard-progress-fill" style={{ width: `${progressPct}%` }} />
          </div>
        </div>

        {/* Steps list */}
        <div className="wizard-section-title" aria-hidden="true">Các bước cấu hình</div>
        <nav className="wizard-step-list" aria-label="Danh sách bước thiết lập" role="tablist">
          {WIZARD_STEPS.map((step, i) => {
            const isDone   = i < safePIndex;
            const isActive = i === safePIndex;
            const Icon = step.Icon;
            return (
              <button
                key={step.key}
                className={`wizard-step-item${isActive ? ' active' : ''}${isDone ? ' done' : ''}`}
                onClick={() => onStepChange(step.key)}
                role="tab"
                aria-selected={isActive}
                aria-label={`Bước ${i + 1}: ${step.label}${isDone ? ' (đã hoàn thành)' : isActive ? ' (đang thực hiện)' : ''}`}
              >
                <span className="wizard-step-num" aria-hidden="true">
                  {isDone ? '✓' : i + 1}
                </span>
                <span className="wizard-step-copy">
                  <strong>{step.label}</strong>
                  <small>{step.desc}</small>
                </span>
              </button>
            );
          })}
        </nav>

        {/* Exit/select year button */}
        {onExit && (
          <div className="wizard-left-footer">
            <button
              className="secondary-button"
              onClick={onExit}
              style={{ width: '100%', justifyContent: 'center', fontSize: 12 }}
              aria-label="Chọn năm học khác"
            >
              ← Chọn năm học khác
            </button>
          </div>
        )}
      </aside>

      {/* ── Right Panel: Content + Footer nav ───────────────────── */}
      <div className="wizard-right">

        {/* Step content */}
        <div className="wizard-content" role="tabpanel" aria-label={`Nội dung bước ${safePIndex + 1}: ${currentStepData?.label}`}>
          {/* Eyebrow pill */}
          <div className="wizard-eyebrow" aria-hidden="true">
            Bước {safePIndex + 1} trong {totalSteps}
          </div>

          {/* Render the actual page component */}
          {children}
        </div>

        {/* Sticky footer navigation */}
        <footer className="wizard-footer" aria-label="Điều hướng bước">
          <button
            className="wizard-footer-prev"
            onClick={() => prevStep && onStepChange(prevStep.key)}
            disabled={!hasPrev}
            aria-label={prevStep ? `Quay lại bước ${safePIndex}: ${prevStep.label}` : 'Đây là bước đầu tiên'}
          >
            <ChevronLeftIcon />
            {prevStep ? prevStep.label : 'Bước trước'}
          </button>

          {/* Dot progress indicator */}
          <div className="wizard-footer-dots" aria-hidden="true" role="presentation">
            {WIZARD_STEPS.map((_, i) => (
              <span
                key={i}
                className={`wizard-dot${i === safePIndex ? ' active' : i < safePIndex ? ' done' : ''}`}
              />
            ))}
          </div>

          <button
            className="wizard-footer-next"
            onClick={() => nextStep && onStepChange(nextStep.key)}
            disabled={!hasNext}
            aria-label={nextStep ? `Tiếp tục sang bước ${safePIndex + 2}: ${nextStep.label}` : 'Đây là bước cuối cùng'}
          >
            {nextStep ? nextStep.label : 'Bước cuối cùng'}
            <ChevronRightIcon />
          </button>
        </footer>
      </div>
    </div>
  );
}
