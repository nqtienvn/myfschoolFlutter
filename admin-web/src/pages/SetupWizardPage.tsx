interface SetupWizardPageProps {
  onNavigate: (pageKey: string) => void;
}

export default function SetupWizardPage({ onNavigate }: SetupWizardPageProps) {
  const steps = [
    {
      stepNum: 1,
      key: 'master-data',
      title: 'Bước 1: Thiết lập Danh mục nền tảng',
      desc: 'Cấu hình các danh mục tĩnh dùng chung cho hệ thống bao gồm Khối lớp từ 1 đến 12, Ca học (Sáng/Chiều) và các Tiết học từ 1 đến 10.',
      badgeText: 'Khởi đầu',
      badgeClass: 'ready'
    },
    {
      stepNum: 2,
      key: 'semesters',
      title: 'Bước 2: Khởi tạo Dòng thời gian gốc',
      desc: 'Thêm Năm học mới (VD: 2026-2027) và tự động tính toán thời gian các Học kỳ trực thuộc (Học kỳ I, Học kỳ II) để làm gốc thời gian cho hệ thống.',
      badgeText: 'Bắt buộc trước',
      badgeClass: 'ready'
    },
    {
      stepNum: 3,
      key: 'users',
      title: 'Bước 3: Nhập dữ liệu Giáo viên',
      desc: 'Nhập thô danh sách giáo viên của trường qua file Excel để sinh mã định danh duy nhất (employeeCode), chuẩn bị cho việc phân công chủ nhiệm.',
      badgeText: 'Nhập thô',
      badgeClass: ''
    },
    {
      stepNum: 4,
      key: 'classes',
      title: 'Bước 4: Khởi tạo Lớp học cụ thể',
      desc: 'Tạo lớp học (VD: 10A1) bằng cách gắn chặt tên lớp với Năm học đang chọn, gán khối tương ứng và gán mã Giáo viên chủ nhiệm.',
      badgeText: 'Phụ thuộc năm học',
      badgeClass: 'ready'
    },
    {
      stepNum: 5,
      key: 'enrollment-import',
      title: 'Bước 5: Nhập Học sinh & Phụ huynh (Bước cuối)',
      desc: 'Tải file Excel chứa thông tin học sinh và thông tin phụ huynh (Tên, SĐT). Backend sẽ tự tạo học sinh, xếp vào lớp của năm học hiện tại, đồng thời tự tạo phụ huynh và tạo mối quan hệ liên kết gia đình.',
      badgeText: 'Liên kết cuối',
      badgeClass: 'ready'
    }
  ];

  return (
    <div style={{ paddingBottom: 40 }}>
      <div style={{ marginBottom: 32 }}>
        <h2 style={{ borderBottom: 'none', paddingBottom: 0, marginBottom: 8 }}>Hệ thống Cấu hình Dữ liệu Gốc (Year-Centric Flow)</h2>
        <p style={{ fontSize: 13, color: '#666666', lineHeight: 1.5 }}>
          Để hệ thống vận hành đúng nghiệp vụ và tránh xung đột ràng buộc, vui lòng thực hiện cấu hình dữ liệu theo đúng trình tự từ Bước 1 đến Bước 5 dưới đây. 
          Chọn <strong>Năm học hiện tại</strong> ở thanh tiêu đề trên cùng để tự động áp dụng bộ lọc dữ liệu cho các bước sau.
        </p>
      </div>

      <div className="wizard-container">
        {steps.map(step => (
          <div 
            key={step.stepNum} 
            className="wizard-step-card" 
            onClick={() => onNavigate(step.key)}
          >
            <div className="wizard-step-num">{step.stepNum}</div>
            <div className="wizard-step-content">
              <div className="wizard-step-title">
                {step.title}
              </div>
              <div className="wizard-step-desc">{step.desc}</div>
            </div>
            {step.badgeText && (
              <div className={`wizard-step-badge ${step.badgeClass}`}>
                {step.badgeText}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
