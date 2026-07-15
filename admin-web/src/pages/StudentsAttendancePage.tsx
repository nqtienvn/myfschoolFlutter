import { useEffect, useMemo, useState } from 'react';
import { getClasses } from '../api/class';
import {
  adjustAdminDailyAttendance,
  getAdminDailyAttendance,
  getAttendanceCorrectionHistory,
  getClassAttendanceSummary,
  getPendingAttendanceCorrections,
  reviewAttendanceCorrection,
  type AdminAttendanceDay,
  type AttendanceCorrectionRequest,
  type ClassAttendanceSummary,
} from '../api/attendance';

interface ClassItem {
  id: number;
  name: string;
  gradeLevel: number;
}

const gradeOptions = Array.from({ length: 12 }, (_, index) => String(index + 1));

const attendanceStatusLabel = (status?: string | null) => ({
  PRESENT: 'Có mặt',
  ABSENT_WITH_LEAVE: 'Vắng có phép',
  ABSENT_WITHOUT_LEAVE: 'Vắng không phép',
}[status || ''] || 'Chưa điểm danh');

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
  const [dailyDate, setDailyDate] = useState(() => new Date().toLocaleDateString('en-CA'));
  const [dailyRows, setDailyRows] = useState<AdminAttendanceDay[]>([]);
  const [corrections, setCorrections] = useState<AttendanceCorrectionRequest[]>([]);
  const [correctionHistory, setCorrectionHistory] = useState<AttendanceCorrectionRequest[]>([]);
  const [dailyLoading, setDailyLoading] = useState(false);
  const [showDailyForm, setShowDailyForm] = useState(false);
  const [savingKey, setSavingKey] = useState('');
  const [draftCounts, setDraftCounts] = useState<Record<string, {
    presentCount: number;
    absentWithLeaveCount: number;
    absentWithoutLeaveCount: number;
  }>>({});

  const dailyKey = (row: Pick<AdminAttendanceDay, 'classId' | 'shift'>) => `${row.classId}-${row.shift}`;

  const loadDailyManagement = async () => {
    if (!selectedYearId || !dailyDate) return;
    setDailyLoading(true);
    try {
      const [rows, pending, history] = await Promise.all([
        getAdminDailyAttendance(selectedYearId, dailyDate),
        getPendingAttendanceCorrections(selectedYearId, dailyDate),
        getAttendanceCorrectionHistory(selectedYearId, dailyDate),
      ]);
      setDailyRows(rows || []);
      setCorrections(pending || []);
      setCorrectionHistory((history || []).filter(item => item.status !== 'PENDING'));
      setDraftCounts(Object.fromEntries((rows || []).map((row) => [dailyKey(row), {
        presentCount: row.presentCount,
        absentWithLeaveCount: row.absentWithLeaveCount,
        absentWithoutLeaveCount: row.absentWithoutLeaveCount,
      }])));
    } catch (err: any) {
      setError(err.message || 'Không thể tải quản lý điểm danh theo ngày.');
    } finally {
      setDailyLoading(false);
    }
  };

  useEffect(() => {
    setDailyRows([]);
    setCorrections([]);
    setCorrectionHistory([]);
    setDraftCounts({});
    loadDailyManagement();
  }, [selectedYearId, dailyDate]);

  const saveDailyRow = async (row: AdminAttendanceDay) => {
    if (!selectedYearId) return;
    const key = dailyKey(row);
    const values = draftCounts[key];
    if (!values) return;
    setSavingKey(key);
    setError('');
    try {
      await adjustAdminDailyAttendance({
        academicYearId: Number(selectedYearId),
        classId: row.classId,
        date: dailyDate,
        shift: row.shift,
        ...values,
      });
      await loadDailyManagement();
    } catch (err: any) {
      setError(err.message || 'Không thể cập nhật điểm danh.');
    } finally {
      setSavingKey('');
    }
  };

  const reviewCorrection = async (id: number, approve: boolean) => {
    setSavingKey(`correction-${id}`);
    setError('');
    try {
      await reviewAttendanceCorrection(id, approve);
      await loadDailyManagement();
    } catch (err: any) {
      setError(err.message || 'Không thể xử lý yêu cầu sửa điểm danh.');
    } finally {
      setSavingKey('');
    }
  };

  // 1. Tải danh sách lớp khi chọn năm học
  useEffect(() => {
    setClasses([]);
    setSelectedClassId('');
    setSummaryList([]);
    setError('');

    if (!selectedYearId) return;

    setLoadingClasses(true);
    getClasses({ academicYearId: selectedYearId, page: 0, size: 500 })
      .then((data: any) => {
        const list = data.content || [];
        setClasses(list);
        // Chọn lớp đầu tiên của Khối đang chọn nếu có
        const filtered = list.filter((item: ClassItem) => String(item.gradeLevel) === selectedGrade);
        if (filtered.length > 0) {
          setSelectedClassId(String(filtered[0].id));
        }
      })
      .catch((err: any) => {
        setError(err.message || 'Không thể tải danh sách lớp.');
      })
      .finally(() => {
        setLoadingClasses(false);
      });
  }, [selectedYearId, selectedGrade]);

  // Lọc lớp theo khối
  const filteredClasses = useMemo(() => {
    return classes.filter((item) => String(item.gradeLevel) === selectedGrade);
  }, [classes, selectedGrade]);

  // 2. Tải thống kê chuyên cần khi chọn Lớp & Học kỳ
  useEffect(() => {
    setSummaryList([]);
    if (!selectedClassId || !selectedSemesterId || !selectedYearId) return;

    setLoadingSummary(true);
    setError('');
    getClassAttendanceSummary(selectedClassId, selectedSemesterId, selectedYearId)
      .then((data) => {
        setSummaryList(data || []);
      })
      .catch((err: any) => {
        setError(err.message || 'Không thể tải thống kê chuyên cần của lớp.');
      })
      .finally(() => {
        setLoadingSummary(false);
      });
  }, [selectedClassId, selectedSemesterId, selectedYearId]);


  // Tính toán dữ liệu tổng quan (Stats cards)
  const totalStudents = summaryList.length;
  const averageAttendanceRate = useMemo(() => {
    if (summaryList.length === 0) return 0;
    const total = summaryList.reduce((acc, curr) => acc + curr.attendanceRate, 0);
    return Math.round((total / summaryList.length) * 10) / 10;
  }, [summaryList]);

  const warningConductCount = useMemo(() => {
    // Đếm số học sinh có hạnh kiểm đề xuất khác 'TỐT'
    return summaryList.filter(
      (item) => item.suggestedConduct && item.suggestedConduct !== 'TỐT'
    ).length;
  }, [summaryList]);

  const selectedClassObj = classes.find((c) => String(c.id) === selectedClassId);

  // Nhãn màu sắc cho Hạnh kiểm
  const getConductBadge = (conduct?: string) => {
    if (!conduct) return <span className="badge muted">Chưa xếp loại</span>;
    switch (conduct.toUpperCase()) {
      case 'TỐT':
        return <span className="badge success" style={{ backgroundColor: '#e1f7ec', color: '#10b981', padding: '4px 8px', borderRadius: '4px', fontWeight: 'bold' }}>TỐT</span>;
      case 'KHÁ':
        return <span className="badge info" style={{ backgroundColor: '#e0f2fe', color: '#0284c7', padding: '4px 8px', borderRadius: '4px', fontWeight: 'bold' }}>KHÁ</span>;
      case 'TRUNG BÌNH':
        return <span className="badge warning" style={{ backgroundColor: '#fff7ed', color: '#ea580c', padding: '4px 8px', borderRadius: '4px', fontWeight: 'bold' }}>TRUNG BÌNH</span>;
      case 'YẾU':
        return <span className="badge danger" style={{ backgroundColor: '#fef2f2', color: '#dc2626', padding: '4px 8px', borderRadius: '4px', fontWeight: 'bold' }}>YẾU</span>;
      default:
        return <span className="badge muted">{conduct}</span>;
    }
  };

  return (
    <div className="page-stack students-attendance-page">
      <header className="page-heading">
        <div>
          <span className="eyebrow">Phân hệ chuyên cần</span>
          <h1>Báo cáo Chuyên cần & Xếp loại hạnh kiểm</h1>
          <p>
            Theo dõi số buổi nghỉ của học sinh theo từng học kỳ. Mỗi buổi sáng và chiều được thống kê riêng theo thời khóa biểu.
          </p>
        </div>
        <div className="page-heading-actions">
          <button type="button" onClick={() => setShowDailyForm(v => !v)}>
            {showDailyForm ? '✕ Đóng' : '＋ Cập nhật điểm danh'}
          </button>
        </div>
      </header>

      {showDailyForm && <section className="panel" style={{ padding: '20px', border: '1px solid #e5e7eb', borderRadius: '12px', background: '#fff' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: '16px', alignItems: 'center', marginBottom: '16px' }}>
          <div>
            <h2 style={{ margin: 0 }}>Cập nhật điểm danh</h2>
            <p style={{ margin: '6px 0 0', color: '#6b7280' }}>
              Chỉ hiển thị lớp và buổi có tiết học trong thời khóa biểu. Mỗi buổi sáng/chiều được tính riêng.
            </p>
          </div>
          <input
            type="date"
            value={dailyDate}
            onChange={(event) => setDailyDate(event.target.value)}
            style={{ padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: '8px' }}
          />
        </div>

        {corrections.length > 0 && (
          <div style={{ marginBottom: '20px', padding: '16px', background: '#fffbeb', border: '1px solid #fcd34d', borderRadius: '10px' }}>
            <h3 style={{ margin: '0 0 12px', color: '#92400e' }}>Yêu cầu sửa điểm danh chờ duyệt</h3>
            {corrections.map((item) => (
              <div key={item.id} style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '10px 0', borderTop: '1px solid #fde68a' }}>
                <div style={{ flex: 1 }}>
                  <strong>{item.className} · {item.shift === 'MORNING' ? 'Buổi sáng' : 'Buổi chiều'}</strong>
                  <div style={{ fontSize: '13px', color: '#78350f', marginTop: '4px' }}>
                    GV {item.teacherName} · Lý do: {item.reason}
                  </div>
                  <div style={{ fontSize: '13px', color: '#78350f', marginTop: '4px' }}>
                    Có mặt {item.originalPresentCount} → {item.presentCount} · Vắng phép {item.originalAbsentWithLeaveCount} → {item.absentWithLeaveCount} · Vắng không phép {item.originalAbsentWithoutLeaveCount} → {item.absentWithoutLeaveCount}
                  </div>
                  {item.changes.length > 0 && <div style={{ fontSize: '12px', color: '#78350f', marginTop: '6px' }}>
                    {item.changes.map(change => <div key={change.studentId}>• {change.studentName}: {attendanceStatusLabel(change.oldStatus)} → {attendanceStatusLabel(change.newStatus)}</div>)}
                  </div>
                  }
                </div>
                <button disabled={savingKey === `correction-${item.id}`} onClick={() => reviewCorrection(item.id, false)} style={{ padding: '7px 12px' }}>Từ chối</button>
                <button disabled={savingKey === `correction-${item.id}`} onClick={() => reviewCorrection(item.id, true)} style={{ padding: '7px 12px', background: '#16a34a', color: '#fff', border: 0, borderRadius: '6px' }}>Duyệt</button>
              </div>
            ))}
          </div>
        )}

        {correctionHistory.length > 0 && (
          <div style={{ marginBottom: '20px', padding: '16px', background: '#f8fafc', border: '1px solid #cbd5e1', borderRadius: '10px' }}>
            <h3 style={{ margin: '0 0 12px', color: '#334155' }}>Lịch sử duyệt sửa điểm danh</h3>
            {correctionHistory.map(item => (
              <div key={item.id} style={{ padding: '12px 0', borderTop: '1px solid #e2e8f0' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: '12px', flexWrap: 'wrap' }}>
                  <strong>{item.className} · {item.shift === 'MORNING' ? 'Buổi sáng' : 'Buổi chiều'} · GV {item.teacherName}</strong>
                  <span className={`badge-status ${item.status === 'APPROVED' ? 'active' : ''}`}>
                    {item.status === 'APPROVED' ? 'ĐÃ DUYỆT' : item.status === 'REJECTED' ? 'ĐÃ TỪ CHỐI' : 'CHỜ DUYỆT'}
                  </span>
                </div>
                <div style={{ marginTop: '6px', fontSize: '13px' }}>Lý do: {item.reason}</div>
                <div style={{ marginTop: '4px', fontSize: '13px', color: '#475569' }}>
                  Có mặt {item.originalPresentCount} → {item.presentCount} · Vắng phép {item.originalAbsentWithLeaveCount} → {item.absentWithLeaveCount} · Vắng không phép {item.originalAbsentWithoutLeaveCount} → {item.absentWithoutLeaveCount}
                </div>
                {item.changes.map(change => <div key={change.studentId} style={{ marginTop: '3px', fontSize: '12px', color: '#475569' }}>
                  • {change.studentName} ({change.studentCode}): {attendanceStatusLabel(change.oldStatus)} → {attendanceStatusLabel(change.newStatus)}
                </div>)}
                {item.reviewedByName && <div style={{ marginTop: '6px', fontSize: '12px', color: '#64748b' }}>
                  Người duyệt: {item.reviewedByName}{item.reviewedAt ? ` · ${new Date(item.reviewedAt).toLocaleString('vi-VN')}` : ''}
                </div>}
              </div>
            ))}
          </div>
        )}

        {dailyLoading ? (
          <div className="account-empty"><strong>Đang tải dữ liệu điểm danh...</strong></div>
        ) : dailyRows.length === 0 ? (
          <div className="account-empty"><strong>Không có lớp nào có lịch học trong ngày đã chọn.</strong></div>
        ) : (
          <div className="table-responsive account-table-wrap">
            <table className="account-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead><tr>
                <th>Lớp</th><th>Buổi</th><th>Số tiết</th><th>Sĩ số</th><th>Có mặt</th><th>Vắng phép</th><th>Vắng không phép</th><th>Trạng thái</th><th></th>
              </tr></thead>
              <tbody>
                {dailyRows.map((row) => {
                  const key = dailyKey(row);
                  const values = draftCounts[key] || row;
                  const setCount = (field: 'presentCount' | 'absentWithLeaveCount' | 'absentWithoutLeaveCount', value: string) => {
                    setDraftCounts((current) => ({ ...current, [key]: { ...values, [field]: Math.max(0, Number(value) || 0) } }));
                  };
                  return (
                    <tr key={key}>
                      <td><strong>{row.className}</strong></td>
                      <td>{row.shift === 'MORNING' ? 'Sáng' : 'Chiều'}</td>
                      <td style={{ textAlign: 'center' }}>{row.scheduledPeriods}</td>
                      <td style={{ textAlign: 'center' }}>{row.totalStudents}</td>
                      <td><input type="number" min="0" value={values.presentCount} onChange={(e) => setCount('presentCount', e.target.value)} style={{ width: 64 }} /></td>
                      <td><input type="number" min="0" value={values.absentWithLeaveCount} onChange={(e) => setCount('absentWithLeaveCount', e.target.value)} style={{ width: 64 }} /></td>
                      <td><input type="number" min="0" value={values.absentWithoutLeaveCount} onChange={(e) => setCount('absentWithoutLeaveCount', e.target.value)} style={{ width: 64 }} /></td>
                      <td><span className={`badge-status ${row.submitted ? 'active' : ''}`}>{row.submitted ? 'ĐÃ ĐIỂM DANH' : 'CHƯA ĐIỂM DANH'}</span></td>
                      <td><button disabled={savingKey === key} onClick={() => saveDailyRow(row)}>{savingKey === key ? 'Đang lưu...' : 'Cập nhật'}</button></td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '16px' }}>
          <button type="button" className="secondary-button" onClick={() => setShowDailyForm(false)}>Đóng</button>
        </div>
      </section>}

      {/* Điều kiện cảnh báo nếu chưa chọn năm học/học kỳ */}
      {!selectedYearId && (
        <div className="notice warning">Vui lòng chọn Năm học ở thanh menu phía trên để xem báo cáo.</div>
      )}
      {selectedYearId && !selectedSemesterId && (
        <div className="notice warning">Vui lòng chọn Học kỳ ở thanh menu phía trên để xem báo cáo.</div>
      )}
      {error && <div className="notice error">{error}</div>}

      {selectedYearId && selectedSemesterId && (
        <>
          {/* Bộ lọc Khối & Lớp */}
          <div className="action-bar" style={{ display: 'flex', gap: '16px', alignItems: 'center', marginBottom: '24px', backgroundColor: '#f9fafb', padding: '16px', borderRadius: '8px', border: '1px solid #e5e7eb' }}>
            <div className="form-group" style={{ margin: 0 }}>
              <label style={{ marginRight: '8px', fontWeight: '600' }}>Khối:</label>
              <select
                value={selectedGrade}
                onChange={(e) => setSelectedGrade(e.target.value)}
                style={{ padding: '6px 12px', borderRadius: '6px', border: '1px solid #d1d5db' }}
              >
                {gradeOptions.map((v) => (
                  <option key={v} value={v}>
                    Khối {v}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group" style={{ margin: 0 }}>
              <label style={{ marginRight: '8px', fontWeight: '600' }}>Lớp học:</label>
              <select
                value={selectedClassId}
                onChange={(e) => setSelectedClassId(e.target.value)}
                style={{ padding: '6px 12px', borderRadius: '6px', border: '1px solid #d1d5db' }}
                disabled={loadingClasses || filteredClasses.length === 0}
              >
                <option value="">Chọn lớp học</option>
                {filteredClasses.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.name}
                  </option>
                ))}
              </select>
            </div>

          </div>

          {/* Thống kê Tổng quan (Stats cards) */}
          {selectedClassId && !loadingSummary && (
            <div className="stats-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', marginBottom: '24px' }}>
              <div className="stat-card" style={{ padding: '16px', borderRadius: '8px', border: '1px solid #e5e7eb', backgroundColor: '#fff', boxShadow: '0 1px 2px 0 rgba(0,0,0,0.05)' }}>
                <span className="stat-label" style={{ fontSize: '12px', color: '#6b7280', fontWeight: 'bold', display: 'block', marginBottom: '8px' }}>SĨ SỐ LỚP {selectedClassObj?.name}</span>
                <span className="stat-value" style={{ fontSize: '28px', fontWeight: '900', color: '#f97316' }}>{totalStudents} học sinh</span>
              </div>
              <div className="stat-card" style={{ padding: '16px', borderRadius: '8px', border: '1px solid #e5e7eb', backgroundColor: '#fff', boxShadow: '0 1px 2px 0 rgba(0,0,0,0.05)' }}>
                <span className="stat-label" style={{ fontSize: '12px', color: '#6b7280', fontWeight: 'bold', display: 'block', marginBottom: '8px' }}>TỶ LỆ CHUYÊN CẦN TRUNG BÌNH</span>
                <span className="stat-value" style={{ fontSize: '28px', fontWeight: '900', color: '#10b981' }}>{averageAttendanceRate}%</span>
              </div>
              <div className="stat-card" style={{ padding: '16px', borderRadius: '8px', border: '1px solid #e5e7eb', backgroundColor: '#fff', boxShadow: '0 1px 2px 0 rgba(0,0,0,0.05)' }}>
                <span className="stat-label" style={{ fontSize: '12px', color: '#6b7280', fontWeight: 'bold', display: 'block', marginBottom: '8px' }}>CẢNH BÁO HẠNH KIỂM (KHÁ/TB/YẾU)</span>
                <span className="stat-value" style={{ fontSize: '28px', fontWeight: '900', color: '#ef4444' }}>{warningConductCount} học sinh</span>
              </div>
            </div>
          )}

          {/* Bảng Dữ liệu */}
          {!selectedClassId && (
            <div className="account-empty">
              <span>⌕</span>
              <strong>Chọn lớp để hiển thị báo cáo chuyên cần</strong>
            </div>
          )}

          {selectedClassId && loadingSummary && (
            <div className="account-empty">
              <span className="loading-dot">•••</span>
              <strong>Đang tính toán dữ liệu chuyên cần lớp học...</strong>
            </div>
          )}

          {selectedClassId && !loadingSummary && summaryList.length === 0 && (
            <div className="account-empty">
              <span>0</span>
              <strong>Lớp chưa có dữ liệu học sinh hoặc chuyên cần</strong>
            </div>
          )}

          {selectedClassId && !loadingSummary && summaryList.length > 0 && (
            <div className="table-responsive account-table-wrap">
              <table className="account-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ borderBottom: '2px solid #e5e7eb', textAlign: 'left' }}>
                    <th style={{ padding: '12px 8px' }}>STT</th>
                    <th style={{ padding: '12px 8px' }}>Mã Học sinh</th>
                    <th style={{ padding: '12px 8px' }}>Họ và Tên</th>
                    <th style={{ padding: '12px 8px', textAlign: 'center' }}>Có mặt</th>
                    <th style={{ padding: '12px 8px', textAlign: 'center' }}>Vắng phép</th>
                    <th style={{ padding: '12px 8px', textAlign: 'center' }}>Vắng không phép</th>
                    <th style={{ padding: '12px 8px', textAlign: 'center' }}>Tỷ lệ chuyên cần</th>
                    <th style={{ padding: '12px 8px', textAlign: 'center' }}>Đề xuất hạnh kiểm</th>
                  </tr>
                </thead>
                <tbody>
                  {summaryList.map((item, index) => (
                    <tr
                      key={item.studentId}
                      style={{
                        borderBottom: '1px solid #f3f4f6',
                        backgroundColor: item.suggestedConduct !== 'TỐT' ? '#fffbeb' : 'transparent',
                      }}
                    >
                      <td style={{ padding: '12px 8px' }}>
                        <span className="row-number">{index + 1}</span>
                      </td>
                      <td style={{ padding: '12px 8px' }}>
                        <span className="login-chip" style={{ fontFamily: 'monospace', fontWeight: 'bold' }}>
                          {item.studentCode}
                        </span>
                      </td>
                      <td style={{ padding: '12px 8px' }}>
                        <strong>{item.studentName}</strong>
                      </td>
                      <td style={{ padding: '12px 8px', textAlign: 'center', color: '#10b981', fontWeight: '600' }}>
                        {item.presentCount}
                      </td>
                      <td style={{ padding: '12px 8px', textAlign: 'center', color: '#0284c7', fontWeight: '600' }}>
                        {item.absentWithLeaveCount}
                      </td>
                      <td style={{ padding: '12px 8px', textAlign: 'center', color: '#ef4444', fontWeight: '600' }}>
                        {item.absentWithoutLeaveCount}
                      </td>
                      <td style={{ padding: '12px 8px', textAlign: 'center', fontWeight: '700' }}>
                        <span
                          style={{
                            color: item.attendanceRate < 90 ? '#ef4444' : '#10b981',
                          }}
                        >
                          {item.attendanceRate}%
                        </span>
                      </td>
                      <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                        {getConductBadge(item.suggestedConduct)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  );
}
