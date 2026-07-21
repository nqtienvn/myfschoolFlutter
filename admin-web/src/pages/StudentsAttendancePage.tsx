import { useEffect, useMemo, useState } from 'react';
import { getClasses } from '../api/class';
import {
  getClassAttendanceSummary,
  type ClassAttendanceSummary,
} from '../api/attendance';

interface ClassItem {
  id: number;
  name: string;
  gradeLevel: number;
}

const gradeOptions = Array.from({ length: 12 }, (_, index) => String(index + 1));

export default function StudentsAttendancePage({
  selectedYearId,
  selectedSemesterId,
}: {
  selectedYearId?: string;
  selectedSemesterId?: string;
}) {
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [selectedGrade, setSelectedGrade] = useState('12');
  const [selectedClassId, setSelectedClassId] = useState('');
  const [summaryList, setSummaryList] = useState<ClassAttendanceSummary[]>([]);
  const [loadingClasses, setLoadingClasses] = useState(false);
  const [loadingSummary, setLoadingSummary] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let alive = true;
    setClasses([]);
    setSelectedClassId('');
    setSummaryList([]);
    setError('');
    if (!selectedYearId) return () => { alive = false; };

    setLoadingClasses(true);
    getClasses({ academicYearId: selectedYearId, page: 0, size: 500 })
      .then((data: any) => {
        if (!alive) return;
        setClasses(data.content || []);
      })
      .catch((cause: any) => {
        if (alive) setError(cause.message || 'Không thể tải danh sách lớp.');
      })
      .finally(() => {
        if (alive) setLoadingClasses(false);
      });
    return () => { alive = false; };
  }, [selectedYearId]);

  const filteredClasses = useMemo(
    () => classes.filter(item => String(item.gradeLevel) === selectedGrade),
    [classes, selectedGrade],
  );

  useEffect(() => {
    setSelectedClassId(current => (
      filteredClasses.some(item => String(item.id) === current)
        ? current
        : filteredClasses.length > 0 ? String(filteredClasses[0].id) : ''
    ));
  }, [filteredClasses]);

  useEffect(() => {
    let alive = true;
    setSummaryList([]);
    if (!selectedClassId || !selectedSemesterId || !selectedYearId) {
      return () => { alive = false; };
    }

    setLoadingSummary(true);
    setError('');
    getClassAttendanceSummary(selectedClassId, selectedSemesterId, selectedYearId)
      .then(data => {
        if (alive) setSummaryList(data || []);
      })
      .catch((cause: any) => {
        if (alive) setError(cause.message || 'Không thể tải thống kê chuyên cần của lớp.');
      })
      .finally(() => {
        if (alive) setLoadingSummary(false);
      });
    return () => { alive = false; };
  }, [selectedClassId, selectedSemesterId, selectedYearId]);

  const averageAttendanceRate = useMemo(() => {
    if (!summaryList.length) return 0;
    const total = summaryList.reduce((sum, item) => sum + item.attendanceRate, 0);
    return Math.round((total / summaryList.length) * 10) / 10;
  }, [summaryList]);

  const warningConductCount = useMemo(
    () => summaryList.filter(item => item.suggestedConduct && item.suggestedConduct !== 'TỐT').length,
    [summaryList],
  );

  const selectedClass = classes.find(item => String(item.id) === selectedClassId);

  const conductBadge = (conduct?: string) => {
    if (!conduct) return <span className="badge muted">Chưa xếp loại</span>;
    const normalized = conduct.toUpperCase();
    const className = normalized === 'TỐT'
      ? 'success'
      : normalized === 'KHÁ' ? 'info' : normalized === 'TRUNG BÌNH' ? 'warning' : 'danger';
    return <span className={`badge ${className}`}>{normalized}</span>;
  };

  return (
    <div className="page-stack students-attendance-page">
      <header className="page-heading">
        <div>
          <span className="eyebrow">Phân hệ chuyên cần</span>
          <h1>Báo cáo chuyên cần & xếp loại hạnh kiểm</h1>
          <p>
            Theo dõi dữ liệu điểm danh do giáo viên ghi nhận. Admin chỉ xem báo cáo;
            yêu cầu sửa điểm danh được xử lý tại Thông báo → Yêu cầu xử lý.
          </p>
        </div>
      </header>

      {!selectedYearId && (
        <div className="notice info">Hãy chọn năm học để xem báo cáo chuyên cần.</div>
      )}
      {error && <div className="notice error" role="alert">{error}</div>}

      <section className="panel attendance-report-filters">
        <div className="filters">
          <div className="form-group">
            <label>Khối</label>
            <select value={selectedGrade} onChange={event => setSelectedGrade(event.target.value)}>
              {gradeOptions.map(grade => <option key={grade} value={grade}>Khối {grade}</option>)}
            </select>
          </div>
          <div className="form-group">
            <label>Lớp</label>
            <select
              value={selectedClassId}
              onChange={event => setSelectedClassId(event.target.value)}
              disabled={loadingClasses || !filteredClasses.length}
            >
              {!filteredClasses.length && <option value="">Không có lớp</option>}
              {filteredClasses.map(item => (
                <option key={item.id} value={item.id}>{item.name}</option>
              ))}
            </select>
          </div>
        </div>
      </section>

      <section className="announcement-summary-grid attendance-summary-grid" aria-label="Tổng quan chuyên cần">
        <article><span>Sĩ số lớp</span><strong>{summaryList.length}</strong></article>
        <article className="published"><span>Tỷ lệ chuyên cần TB</span><strong>{averageAttendanceRate}%</strong></article>
        <article className="rejected"><span>Cần lưu ý hạnh kiểm</span><strong>{warningConductCount}</strong></article>
      </section>

      <section className="panel">
        <div className="teacher-table-toolbar">
          <span>{selectedClass ? `Báo cáo lớp ${selectedClass.name}` : 'Chưa chọn lớp'}</span>
          <span>{summaryList.length} học sinh</span>
        </div>
        <div className="table-responsive">
          <table className="account-table attendance-report-table">
            <thead>
              <tr>
                <th>Mã học sinh</th>
                <th>Họ và tên</th>
                <th>Có mặt</th>
                <th>Vắng có phép</th>
                <th>Vắng không phép</th>
                <th>Tỷ lệ chuyên cần</th>
                <th>Hạnh kiểm đề xuất</th>
              </tr>
            </thead>
            <tbody>
              {summaryList.map(item => (
                <tr key={item.studentId}>
                  <td>{item.studentCode}</td>
                  <td><strong>{item.studentName}</strong></td>
                  <td>{item.presentCount}</td>
                  <td>{item.absentWithLeaveCount}</td>
                  <td>{item.absentWithoutLeaveCount}</td>
                  <td><strong>{item.attendanceRate}%</strong></td>
                  <td>{conductBadge(item.suggestedConduct)}</td>
                </tr>
              ))}
              {!summaryList.length && !loadingSummary && (
                <tr><td colSpan={7}>Chưa có dữ liệu chuyên cần phù hợp.</td></tr>
              )}
              {loadingSummary && (
                <tr><td colSpan={7}>Đang tải báo cáo chuyên cần…</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
