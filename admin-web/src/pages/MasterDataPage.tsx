import { useState, useEffect } from 'react';
import { getGradeLevels, getShifts, getPeriods } from '../api/masterData';
import { getSubjects, createSubject, deleteSubject } from '../api/subject';
import { getAcademicYears, createAcademicYear, openAcademicYear, openSemester2, completeAcademicYear, updateAcademicYear } from '../api/academicYear';
import { getSemesters, createSemester, setCurrentSemester, deleteSemester } from '../api/semester';
import AcademicYearArchiveView from './AcademicYearArchiveView';

interface GradeLevel {
  id: number;
  name: string;
  code: string;
  order: number;
  description: string;
}

interface SchoolShift {
  id: number;
  name: string;
  code: string;
  order: number;
}

interface Period {
  id: number;
  name: string;
  order: number;
  shiftId: number;
  shiftName: string;
}

interface Subject {
  id: number;
  name: string;
  code: string;
}

interface AcademicYearItem {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  status: string;
}

interface SemesterItem {
  id: number;
  name: string;
  academicYearId: number;
  academicYearName: string;
  order: number;
  startDate: string;
  endDate: string;
  isCurrent: boolean;
  status: string;
}

type TabKey = 'grade-levels' | 'shifts' | 'periods' | 'subjects' | 'academic-years';

interface MasterDataPageProps {
  initialTab?: TabKey;
  onYearCreated?: () => void;
}

export default function MasterDataPage({ initialTab = 'academic-years', onYearCreated }: MasterDataPageProps) {
  const [activeTab, setActiveTab] = useState<TabKey>(initialTab);
  const [gradeLevels, setGradeLevels] = useState<GradeLevel[]>([]);
  const [shifts, setShifts] = useState<SchoolShift[]>([]);
  const [periods, setPeriods] = useState<Period[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  // State for Academic Years and Semesters
  const [academicYears, setAcademicYears] = useState<AcademicYearItem[]>([]);
  const [semesters, setSemesters] = useState<SemesterItem[]>([]);
  const [creating, setCreating] = useState(false);
  const [archiveYearId, setArchiveYearId] = useState<string | null>(null);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [editingYearId, setEditingYearId] = useState<number | null>(null);

  const [subjectName, setSubjectName] = useState('');
  const [subjectCode, setSubjectCode] = useState('');

  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  async function refreshData() {
    const yearsData = await getAcademicYears() as AcademicYearItem[];
    setAcademicYears(yearsData || []);
    const semsData = await getSemesters() as SemesterItem[];
    setSemesters(semsData || []);
    if (onYearCreated) {
      onYearCreated();
    }
  }

  async function handleOpenYear(yearId: number) {
    setError('');
    setSuccessMsg('');
    try {
      await openAcademicYear(yearId);
      setSuccessMsg('Mở năm học thành công!');
      await refreshData();
    } catch (err: any) {
      setError(err.message || 'Lỗi khi mở năm học');
    }
  }

  async function handleOpenSemester2(yearId: number) {
    setError('');
    setSuccessMsg('');
    try {
      await openSemester2(yearId);
      setSuccessMsg('Mở học kỳ 2 thành công!');
      await refreshData();
    } catch (err: any) {
      setError(err.message || 'Lỗi khi mở học kỳ 2');
    }
  }

  async function handleCompleteYear(yearId: number) {
    setError('');
    setSuccessMsg('');
    try {
      await completeAcademicYear(yearId);
      setSuccessMsg('Kết thúc năm học thành công!');
      await refreshData();
    } catch (err: any) {
      setError(err.message || 'Lỗi khi kết thúc năm học');
    }
  }

  async function handleCreateAcademicYear() {
    setError('');
    setSuccessMsg('');

    if (!startDate || !endDate) {
      setError('Vui lòng nhập đầy đủ ngày bắt đầu và ngày kết thúc');
      return;
    }

    setCreating(true);
    try {
      if (editingYearId) {
        await updateAcademicYear(editingYearId, { startDate, endDate });
        setSuccessMsg('Cập nhật năm học thành công!');
        setEditingYearId(null);
      } else {
        await createAcademicYear({
          startDate: startDate,
          endDate: endDate
        });
        setSuccessMsg('Tạo năm học mới thành công!');
      }
      setStartDate('');
      setEndDate('');
      await refreshData();
    } catch (err: any) {
      setError(err.message || 'Lỗi khi lưu thông tin năm học');
    } finally {
      setCreating(false);
    }
  }

  useEffect(() => {
    setActiveTab(initialTab);
  }, [initialTab]);

  useEffect(() => {
    fetchMasterData();
  }, [activeTab]);

  async function fetchMasterData() {
    setError('');
    try {
      if (activeTab === 'grade-levels') {
        const data = await getGradeLevels();
        setGradeLevels(data || []);
      } else if (activeTab === 'shifts') {
        const data = await getShifts();
        setShifts(data || []);
      } else if (activeTab === 'periods') {
        const data = await getPeriods();
        setPeriods(data || []);
      } else if (activeTab === 'subjects') {
        const data = await getSubjects();
        setSubjects(data || []);
      } else if (activeTab === 'academic-years') {
        const yearsData = await getAcademicYears() as AcademicYearItem[];
        setAcademicYears(yearsData || []);
        const semsData = await getSemesters() as SemesterItem[];
        setSemesters(semsData || []);
      }
    } catch (err: any) {
      setError(err.message || 'Không thể tải dữ liệu danh mục');
    }
  }

  async function createSubjectItem() {
    setError('');
    setSuccessMsg('');
    if (!subjectName.trim() || !subjectCode.trim()) {
      setError('Vui lòng nhập đầy đủ tên và mã môn học');
      return;
    }
    try {
      await createSubject({ name: subjectName, code: subjectCode });
      setSubjectName('');
      setSubjectCode('');
      setSuccessMsg('Tạo môn học thành công!');
      const data = await getSubjects();
      setSubjects(data || []);
    } catch (err: any) {
      setError(err.message || 'Lỗi khi tạo môn học');
    }
  }

  async function deleteSubjectItem(id: number) {
    setError('');
    setSuccessMsg('');
    if (!confirm('Bạn có chắc chắn muốn xóa môn học này?')) return;
    try {
      await deleteSubject(id);
      setSuccessMsg('Xóa môn học thành công!');
      const data = await getSubjects();
      setSubjects(data || []);
    } catch (err: any) {
      setError(err.message || 'Lỗi khi xóa môn học');
    }
  }

  const hasActiveYear = academicYears.some(y => y.status === 'ACTIVE');

  if (archiveYearId) {
    return <AcademicYearArchiveView yearId={archiveYearId} onBack={() => setArchiveYearId(null)} />;
  }

  return (
    <div>
      <div style={{ marginBottom: '24px', borderBottom: '2px solid #000000', paddingBottom: '12px' }}>
        <h2 style={{ margin: 0, border: 'none', padding: 0 }}>Quản lý danh mục chung (Master Data)</h2>
      </div>

      <div className="tabs-container">
        <button className={`tab-btn ${activeTab === 'academic-years' ? 'active' : ''}`} onClick={() => setActiveTab('academic-years')}>Năm học & Học kỳ</button>
        <button className={`tab-btn ${activeTab === 'grade-levels' ? 'active' : ''}`} onClick={() => setActiveTab('grade-levels')}>Khối lớp</button>
        <button className={`tab-btn ${activeTab === 'shifts' ? 'active' : ''}`} onClick={() => setActiveTab('shifts')}>Ca học</button>
        <button className={`tab-btn ${activeTab === 'periods' ? 'active' : ''}`} onClick={() => setActiveTab('periods')}>Tiết học</button>
        <button className={`tab-btn ${activeTab === 'subjects' ? 'active' : ''}`} onClick={() => setActiveTab('subjects')}>Môn học</button>
      </div>

      {error && <div className="error" style={{ marginBottom: 16 }}>{error}</div>}
      {successMsg && <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: '16px' }}>[SUCCESS] {successMsg}</div>}

      {activeTab === 'academic-years' && (
        <div className="master-data-layout">
          {/* Left: List of academic years and semesters (7 parts / 70% width) */}
          <div className="master-data-main">
            <div className="table-responsive">
              <table>
              <thead>
                <tr>
                  <th>Năm học</th>
                  <th>Học kỳ</th>
                  <th>Ngày bắt đầu</th>
                  <th>Ngày kết thúc</th>
                  <th>Trạng thái</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {[...academicYears].sort((a, b) => b.id - a.id).map(year => {
                  const yearSemesters = semesters.filter(sem => sem.academicYearId === year.id);
                  if (yearSemesters.length === 0) {
                    return (
                      <tr key={`year-${year.id}`}>
                        <td style={{ fontWeight: 600 }}>{year.name}</td>
                        <td colSpan={5} style={{ color: '#737373', fontStyle: 'italic', padding: '12px 16px' }}>
                          Chưa có học kỳ nào
                        </td>
                      </tr>
                    );
                  }
                  return yearSemesters.map((sem, index) => (
                    <tr key={sem.id}>
                      {index === 0 ? (
                        <td rowSpan={yearSemesters.length} style={{ fontWeight: 600, verticalAlign: 'middle', borderRight: '1px solid #e5e5e5' }}>
                          {year.name}
                          <div className="year-sub-info">
                            ({year.startDate} - {year.endDate})
                          </div>
                          <div className="year-status-container">
                            <span className={`badge-status ${year.status === 'ACTIVE' ? 'active' : year.status === 'COMPLETED' ? 'completed' : 'preparing'}`}>
                              {year.status === 'DRAFT' ? 'CHƯA BẮT ĐẦU' : year.status === 'ACTIVE' ? 'ĐANG HOẠT ĐỘNG' : 'HOÀN THÀNH'}
                            </span>
                          </div>
                        </td>
                      ) : null}
                      <td style={{ fontWeight: 600 }}>{sem.name}</td>
                      <td>{sem.startDate}</td>
                      <td>{sem.endDate}</td>
                      <td>
                        <span className={`badge-status ${sem.status === 'ACTIVE' ? 'active' : sem.status === 'COMPLETED' ? 'completed' : 'preparing'}`}>
                          {sem.status === 'NOT_STARTED' ? 'CHƯA BẮT ĐẦU' : sem.status === 'ACTIVE' ? 'ĐANG HOẠT ĐỘNG' : 'HOÀN THÀNH'}
                        </span>
                      </td>
                      {index === 0 ? (
                        <td rowSpan={yearSemesters.length} style={{ verticalAlign: 'middle', borderLeft: '1px solid #e5e5e5', textAlign: 'center' }}>
                          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', alignItems: 'center' }}>
                            {year.status === 'DRAFT' && (
                              <button
                                type="button"
                                onClick={() => {
                                  setEditingYearId(year.id);
                                  setStartDate(year.startDate);
                                  setEndDate(year.endDate);
                                  setError('');
                                  setSuccessMsg('');
                                }}
                                className="btn-edit-year"
                                style={{ margin: 0, width: '100px' }}
                              >
                                Sửa
                              </button>
                            )}

                            {year.status === 'DRAFT' && !hasActiveYear && (
                              <button
                                type="button"
                                onClick={() => handleOpenYear(year.id)}
                                style={{
                                  background: '#000000',
                                  color: '#ffffff',
                                  border: '1px solid #000000',
                                  padding: '2px 8px',
                                  fontSize: '10px',
                                  fontWeight: 'bold',
                                  cursor: 'pointer',
                                  textTransform: 'uppercase',
                                  letterSpacing: '0.05em',
                                  width: '100px'
                                }}
                              >
                                Mở năm học
                              </button>
                            )}

                            {year.status === 'ACTIVE' && (() => {
                              const sem2 = yearSemesters.find(sem => sem.order === 2);
                              if (sem2 && sem2.status === 'NOT_STARTED') {
                                return (
                                  <button
                                    type="button"
                                    onClick={() => handleOpenSemester2(year.id)}
                                    style={{
                                      background: '#000000',
                                      color: '#ffffff',
                                      border: '1px solid #000000',
                                      padding: '2px 8px',
                                      fontSize: '10px',
                                      fontWeight: 'bold',
                                      cursor: 'pointer',
                                      textTransform: 'uppercase',
                                      letterSpacing: '0.05em',
                                      width: '100px'
                                    }}
                                  >
                                    Mở HK2
                                  </button>
                                );
                              }
                              if (sem2 && sem2.status === 'ACTIVE') {
                                return (
                                  <button
                                    type="button"
                                    onClick={() => handleCompleteYear(year.id)}
                                    style={{
                                      background: '#ef4444',
                                      color: '#ffffff',
                                      border: '1px solid #ef4444',
                                      padding: '2px 8px',
                                      fontSize: '10px',
                                      fontWeight: 'bold',
                                      cursor: 'pointer',
                                      textTransform: 'uppercase',
                                      letterSpacing: '0.05em',
                                      width: '100px'
                                    }}
                                  >
                                    Kết thúc năm
                                  </button>
                                );
                              }
                              return null;
                            })()}

                            {year.status === 'COMPLETED' && (
                              <button
                                type="button"
                                onClick={() => setArchiveYearId(String(year.id))}
                                style={{
                                  background: '#ffffff',
                                  color: '#000000',
                                  border: '1px solid #000000',
                                  padding: '2px 8px',
                                  fontSize: '10px',
                                  fontWeight: 'bold',
                                  cursor: 'pointer',
                                  textTransform: 'uppercase',
                                  letterSpacing: '0.05em',
                                  width: '100px'
                                }}
                              >
                                Thống kê
                              </button>
                            )}
                          </div>
                        </td>
                      ) : null}
                    </tr>
                  ));
                })}
                {academicYears.length === 0 && (
                  <tr>
                    <td colSpan={6} style={{ textAlign: 'center', color: '#737373', padding: '20px' }}>
                      Chưa có dữ liệu năm học nào.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

          {/* Right: Form to add new academic year (3 parts / 30% width) */}
          <div className="master-data-side">
            <div className="master-data-form-card">
              <h3>
                {editingYearId ? 'Cập nhật năm học' : 'Thêm năm học mới'}
              </h3>

              <div className="form-group">
                <label>Nhập ngày bắt đầu, ngày kết thúc</label>
                <div className="date-input-row">
                  <input
                    type="date"
                    placeholder="Ngày bắt đầu"
                    value={startDate}
                    onChange={e => setStartDate(e.target.value)}
                    className="date-input-field"
                  />
                  <input
                    type="date"
                    placeholder="Ngày kết thúc"
                    value={endDate}
                    onChange={e => setEndDate(e.target.value)}
                    className="date-input-field"
                  />
                </div>
              </div>

              <button
                type="button"
                onClick={handleCreateAcademicYear}
                disabled={creating}
                className="btn-primary-block"
              >
                {creating ? 'Đang lưu...' : (editingYearId ? 'Cập nhật' : 'Tạo năm học')}
              </button>

              {editingYearId && (
                <button
                  type="button"
                  onClick={() => {
                    setEditingYearId(null);
                    setStartDate('');
                    setEndDate('');
                    setError('');
                    setSuccessMsg('');
                  }}
                  className="btn-edit-year"
                  style={{ margin: '8px 0 0 0', width: '100%', height: '38px' }}
                >
                  Hủy
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {activeTab === 'grade-levels' && (
        <div className="table-responsive">
          <table>
          <thead>
            <tr>
              <th>Thứ tự</th>
              <th>Mã khối</th>
              <th>Tên khối lớp</th>
              <th>Mô tả</th>
            </tr>
          </thead>
          <tbody>
            {gradeLevels.map(gl => (
              <tr key={gl.id}>
                <td>{gl.order}</td>
                <td>{gl.code}</td>
                <td style={{ fontWeight: 600 }}>{gl.name}</td>
                <td>{gl.description}</td>
              </tr>
            ))}
            {gradeLevels.length === 0 && (
              <tr>
                <td colSpan={4} style={{ textAlign: 'center', color: '#737373', padding: '20px' }}>
                  Chưa có dữ liệu khối lớp. Vui lòng nhấn nút "Khởi tạo nhanh danh mục mẫu" ở phía trên.
                </td>
              </tr>
            )}
          </tbody>
        </table>
        </div>
      )}

      {activeTab === 'shifts' && (
        <div className="table-responsive">
          <table>
          <thead>
            <tr>
              <th>Thứ tự</th>
              <th>Mã ca</th>
              <th>Tên ca học</th>
            </tr>
          </thead>
          <tbody>
            {shifts.map(s => (
              <tr key={s.id}>
                <td>{s.order}</td>
                <td>{s.code}</td>
                <td style={{ fontWeight: 600 }}>{s.name}</td>
              </tr>
            ))}
            {shifts.length === 0 && (
              <tr>
                <td colSpan={3} style={{ textAlign: 'center', color: '#737373', padding: '20px' }}>
                  Chưa có dữ liệu ca học. Vui lòng nhấn nút "Khởi tạo nhanh danh mục mẫu" ở phía trên.
                </td>
              </tr>
            )}
          </tbody>
        </table>
        </div>
      )}

      {activeTab === 'periods' && (
        <div className="table-responsive">
          <table>
          <thead>
            <tr>
              <th>Thứ tự tiết</th>
              <th>Tên tiết học</th>
              <th>Ca học thuộc</th>
            </tr>
          </thead>
          <tbody>
            {periods.map(p => (
              <tr key={p.id}>
                <td>{p.order}</td>
                <td style={{ fontWeight: 600 }}>{p.name}</td>
                <td>{p.shiftName}</td>
              </tr>
            ))}
            {periods.length === 0 && (
              <tr>
                <td colSpan={3} style={{ textAlign: 'center', color: '#737373', padding: '20px' }}>
                  Chưa có dữ liệu tiết học. Vui lòng nhấn nút "Khởi tạo nhanh danh mục mẫu" ở phía trên.
                </td>
              </tr>
            )}
          </tbody>
        </table>
        </div>
      )}

      {activeTab === 'subjects' && (
        <div>
          <div style={{ display: 'flex', gap: '8px', marginBottom: '20px', flexWrap: 'wrap' }}>
            <input
              type="text"
              placeholder="Tên môn học (Ví dụ: Toán học)"
              value={subjectName}
              onChange={e => setSubjectName(e.target.value)}
              style={{ flex: 1, minWidth: '180px', height: '38px', padding: '8px', border: '1px solid #d4d4d4' }}
            />
            <input
              type="text"
              placeholder="Mã môn học (Ví dụ: TOAN)"
              value={subjectCode}
              onChange={e => setSubjectCode(e.target.value)}
              style={{ width: '150px', height: '38px', padding: '8px', border: '1px solid #d4d4d4' }}
            />
            <button
              onClick={createSubjectItem}
              style={{ height: '38px', padding: '0 16px', background: '#000000', color: '#ffffff', border: 'none', fontWeight: 'bold', cursor: 'pointer' }}
            >
              Tạo môn
            </button>
          </div>

          <div className="table-responsive">
            <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Tên môn học</th>
                <th>Mã môn học</th>
                <th style={{ width: '100px', textAlign: 'center' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {subjects.map(s => (
                <tr key={s.id}>
                  <td>{s.id}</td>
                  <td style={{ fontWeight: 600 }}>{s.name}</td>
                  <td>{s.code}</td>
                  <td style={{ textAlign: 'center' }}>
                    <button
                      onClick={() => deleteSubjectItem(s.id)}
                      style={{ background: '#ef4444', color: '#ffffff', border: 'none', padding: '4px 10px', cursor: 'pointer', fontSize: '11px', fontWeight: 'bold' }}
                    >
                      Xóa
                    </button>
                  </td>
                </tr>
              ))}
              {subjects.length === 0 && (
                <tr>
                  <td colSpan={4} style={{ textAlign: 'center', color: '#737373', padding: '20px' }}>
                    Chưa có môn học nào được cấu hình.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    )}


    </div>
  );
}
