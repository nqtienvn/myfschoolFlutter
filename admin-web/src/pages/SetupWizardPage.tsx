import type { AcademicYearItem } from '../App';

interface Props {
  onNavigate: (pageKey: string) => void;
  selectedYear?: AcademicYearItem;
}

const steps = [
  ['years', 'Khởi tạo năm học', 'Tạo năm học ở trạng thái DRAFT; hệ thống tự sinh Học kỳ 1 và Học kỳ 2.'],
  ['master-data', 'Cấu hình danh mục', 'Chọn môn học, ca học và tiết học áp dụng cho năm học.'],
  ['teachers', 'Quản lý giáo viên', 'Tạo hồ sơ giáo viên dùng nhiều năm và khai báo môn phụ trách.'],
  ['classes', 'Sinh lớp hàng loạt', 'Sinh lớp theo khối, ký hiệu và số lượng; sau đó gán giáo viên chủ nhiệm.'],
  ['students', 'Thêm học sinh & phụ huynh', 'Tạo thủ công tài khoản, liên kết phụ huynh và xếp lớp.'],
  ['assignments', 'Phân công giảng dạy', 'Mỗi lớp, môn và học kỳ chỉ có một giáo viên phụ trách.'],
  ['validation', 'Kiểm tra dữ liệu', 'Xác nhận lớp có GVCN, học sinh và phân công môn trước khi mở năm học.'],
  ['activation', 'Kích hoạt năm học', 'Chỉ kích hoạt khi mọi kiểm tra đạt; cấu hình quan trọng sẽ được khóa.'],
] as const;

export default function SetupWizardPage({ onNavigate, selectedYear }: Props) {
  return (
    <div className="page-stack">
      <section className="page-hero">
        <div>
          <span className="eyebrow">Quy trình chuẩn BA</span>
          <h1>Thiết lập năm học theo 8 bước</h1>
          <p>Thực hiện lần lượt để dữ liệu nhất quán và đủ điều kiện kích hoạt.</p>
        </div>
        <div className="hero-context">
          <span>Năm học hiện tại</span>
          <strong>{selectedYear?.name || 'Chưa chọn'}</strong>
          <small>{selectedYear?.status || 'Tạo năm học để bắt đầu'}</small>
        </div>
      </section>

      <section className="step-grid">
        {steps.map(([key, title, description], index) => (
          <button key={key} className="step-card" onClick={() => onNavigate(key)}>
            <span className="step-index">{String(index + 1).padStart(2, '0')}</span>
            <span className="step-copy"><strong>{title}</strong><small>{description}</small></span>
            <span className="step-arrow">→</span>
          </button>
        ))}
      </section>
    </div>
  );
}
