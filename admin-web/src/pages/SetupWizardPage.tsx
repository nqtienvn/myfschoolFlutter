interface SetupWizardPageProps {
  onNavigate: (pageKey: string) => void;
}

export default function SetupWizardPage({ onNavigate }: SetupWizardPageProps) {
  const steps = [
    {
      stepNum: 1,
      key: 'master-data',
      title: 'Bước 1: Thiết lập Danh mục nền tảng',
      badgeText: 'Khởi đầu',
      badgeClass: 'ready'
    },
    {
      stepNum: 2,
      key: 'semesters',
      title: 'Bước 2: Khởi tạo Dòng thời gian gốc',
      badgeText: 'Bắt buộc trước',
      badgeClass: 'ready'
    },
    {
      stepNum: 3,
      key: 'users',
      title: 'Bước 3: Nhập dữ liệu Giáo viên',
      badgeText: 'Nhập thô',
      badgeClass: ''
    },
    {
      stepNum: 4,
      key: 'classes',
      title: 'Bước 4: Khởi tạo Lớp học cụ thể',
      badgeText: 'Phụ thuộc năm học',
      badgeClass: 'ready'
    },
    {
      stepNum: 5,
      key: 'enrollment-import',
      title: 'Bước 5: Nhập Học sinh & Phụ huynh',
      badgeText: 'Liên kết cuối',
      badgeClass: 'ready'
    }
  ];

  return (
    <div style={{ paddingBottom: 40 }}>
      <div style={{ marginBottom: 32 }}>
        <h2 style={{ borderBottom: 'none', paddingBottom: 0, marginBottom: 8 }}>Hệ thống Cấu hình Dữ liệu Gốc (Year-Centric Flow)</h2>
      </div>

      <div className="wizard-container">
        {steps.map(step => (
          <div 
            key={step.stepNum} 
            className="wizard-step-card" 
            onClick={() => onNavigate(step.key)}
            style={{ padding: '20px 24px', minHeight: 'auto', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
              <div className="wizard-step-num" style={{ margin: 0 }}>{step.stepNum}</div>
              <div className="wizard-step-title" style={{ margin: 0, fontSize: '15px', fontWeight: 'bold' }}>
                {step.title}
              </div>
            </div>
            {step.badgeText && (
              <div className={`wizard-step-badge ${step.badgeClass}`} style={{ margin: 0, position: 'static' }}>
                {step.badgeText}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
