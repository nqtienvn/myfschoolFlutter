import { useState, useEffect } from 'react';
import { getGradeLevels, getShifts, getPeriods } from '../api/masterData';
import { getSubjects, createSubject, deleteSubject } from '../api/subject';
import { getAcademicYears, createAcademicYear, generate10Years, openAcademicYear, openSemester2, completeAcademicYear } from '../api/academicYear';
import { getSemesters, createSemester, setCurrentSemester, deleteSemester } from '../api/semester';

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

  const [subjectName, setSubjectName] = useState('');
  const [subjectCode, setSubjectCode] = useState('');
  
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [initializing, setInitializing] = useState(false);

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

  // Academic Years logic
  function getCalculatedDates(year: number) {
    const name = `${year}-${year + 1}`;
    const yearStart = `${year}-09-05`;
    const yearEnd = `${year + 1}-05-30`;
    const endHK1 = `${year}-12-31`;
    const startHK2 = `${year + 1}-01-01`;
    const endHK2 = `${year + 1}-05-30`;
    return {
      name,
      yearStart,
      yearEnd,
      hk1: { start: yearStart, end: endHK1 },
      hk2: { start: startHK2, end: endHK2 }
    };
  }

  async function handleAdd10Years() {
    setError('');
    setSuccessMsg('');
    setCreating(true);

    try {
      await generate10Years();

      setSuccessMsg('Khởi tạo thành công 10 niên khóa tiếp theo kèm 2 học kỳ!');

      // Làm mới dữ liệu
      const updatedYearsData = await getAcademicYears() as AcademicYearItem[];
      setAcademicYears(updatedYearsData || []);
      const semsData = await getSemesters() as SemesterItem[];
      setSemesters(semsData || []);

      if (onYearCreated) {
        onYearCreated();
      }
    } catch (err: any) {
      setError(err.message || 'Lỗi trong quá trình tạo 10 năm học');
    } finally {
      setCreating(false);
    }
  }

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

  async function handleLoadData() {
    setError('');
    setSuccessMsg('');
    setInitializing(true);
    try {
      const timerPromise = new Promise(resolve => setTimeout(resolve, 2000));
      await Promise.all([fetchMasterData(), timerPromise]);
      setSuccessMsg('Tải dữ liệu danh mục thành công!');
    } catch (err: any) {
      setError(err.message || 'Lỗi khi tải dữ liệu');
    } finally {
      setInitializing(false);
    }
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '16px', borderBottom: '2px solid #000000', paddingBottom: '12px' }}>
        <h2 style={{ margin: 0, border: 'none', padding: 0 }}>Quản lý danh mục chung (Master Data)</h2>
        <button 
          onClick={handleLoadData} 
          disabled={initializing}
          style={{ height: '34px', padding: '0 16px', background: '#000000', color: '#ffffff', border: 'none', fontWeight: 'bold', cursor: 'pointer', fontSize: '12px' }}
        >
          Tải dữ liệu
        </button>
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

      {/* Tab: Grade Levels Read-only */}
      {activeTab === 'grade-levels' && (
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
      )}

      {/* Tab: Shifts Read-only */}
      {activeTab === 'shifts' && (
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
      )}

      {/* Tab: Periods Read-only */}
      {activeTab === 'periods' && (
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
      )}
      
      {/* Tab: Subjects CRUD */}
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
      )}

      {/* Tab: Academic Years & Semesters CRUD */}
      {activeTab === 'academic-years' && (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 320px', gap: '24px', alignItems: 'start' }}>
          <div style={{ overflowX: 'auto' }}>
            <table>
              <thead>
                <tr>
                  <th>Niên khóa</th>
                  <th>Ngày bắt đầu</th>
                  <th>Ngày kết thúc</th>
                  <th>Trạng thái</th>
                  <th style={{ minWidth: '240px' }}>Học kỳ trực thuộc</th>
                  <th style={{ width: '120px', textAlign: 'center' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {academicYears.map(year => {
                  const yearSemesters = semesters.filter(s => s.academicYearId === year.id);
                  return (
                    <tr key={year.id}>
                      <td style={{ fontWeight: 'bold', fontSize: '14px' }}>
                        Năm học {year.name}
                      </td>
                      <td>{year.startDate}</td>
                      <td>{year.endDate}</td>
                      <td>
                        <span style={{
                          display: 'inline-block',
                          padding: '2px 8px',
                          borderRadius: '4px',
                          fontSize: '11px',
                          fontWeight: 'bold',
                          background: year.status === 'ACTIVE' ? '#dcfce7' : year.status === 'COMPLETED' ? '#f5f5f5' : '#fef9c3',
                          color: year.status === 'ACTIVE' ? '#16a34a' : year.status === 'COMPLETED' ? '#737373' : '#ca8a04',
                          border: year.status === 'ACTIVE' ? '1px solid #16a34a' : year.status === 'COMPLETED' ? '1px solid #737373' : '1px solid #ca8a04'
                        }}>
                          {year.status === 'ACTIVE' ? 'Đang hoạt động' : year.status === 'COMPLETED' ? 'Đã hoàn thành' : 'Bản nháp'}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                          {yearSemesters.map(s => (
                            <div key={s.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '12px', background: '#fafafa', border: '1px solid #e5e5e5', borderRadius: '4px', padding: '6px 10px', borderLeft: s.status === 'ACTIVE' ? '4px solid #16a34a' : '1px solid #e5e5e5' }}>
                              <span style={{ display: 'flex', flexDirection: 'column' }}>
                                <span style={{ fontWeight: 600, color: s.status === 'ACTIVE' ? '#16a34a' : '#171717' }}>
                                  {s.name}
                                </span>
                                <span style={{ fontSize: '11px', color: '#737373', marginTop: '2px' }}>
                                  {s.startDate} → {s.endDate}
                                </span>
                              </span>
                              <div>
                                <span style={{
                                  display: 'inline-block',
                                  padding: '2px 6px',
                                  borderRadius: '3px',
                                  fontSize: '10px',
                                  fontWeight: 'bold',
                                  background: s.status === 'ACTIVE' ? '#dcfce7' : s.status === 'COMPLETED' ? '#f5f5f5' : '#f1f5f9',
                                  color: s.status === 'ACTIVE' ? '#16a34a' : s.status === 'COMPLETED' ? '#737373' : '#64748b'
                                }}>
                                  {s.status === 'ACTIVE' ? 'Đang hoạt động' : s.status === 'COMPLETED' ? 'Đã hoàn thành' : 'Chưa bắt đầu'}
                                </span>
                              </div>
                            </div>
                          ))}
                          {yearSemesters.length === 0 && <span style={{ color: '#a3a3a3', fontStyle: 'italic', fontSize: '12px' }}>Không có học kỳ</span>}
                        </div>
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', alignItems: 'stretch' }}>
                          {year.status === 'DRAFT' && (
                            <button
                              onClick={() => handleOpenYear(year.id)}
                              style={{ background: '#16a34a', color: '#ffffff', border: 'none', padding: '6px 8px', cursor: 'pointer', fontSize: '11px', fontWeight: 'bold' }}
                            >
                              Mở năm học
                            </button>
                          )}
                          {year.status === 'ACTIVE' && (() => {
                            const sem1 = yearSemesters.find(s => s.order === 1);
                            const sem2 = yearSemesters.find(s => s.order === 2);
                            if (sem1?.status === 'ACTIVE' && sem2?.status === 'NOT_STARTED') {
                              return (
                                <button
                                  onClick={() => handleOpenSemester2(year.id)}
                                  style={{ background: '#3c50e0', color: '#ffffff', border: 'none', padding: '6px 8px', cursor: 'pointer', fontSize: '11px', fontWeight: 'bold' }}
                                >
                                  Mở học kỳ 2
                                </button>
                              );
                            }
                            if (sem2?.status === 'ACTIVE') {
                              return (
                                <button
                                  onClick={() => handleCompleteYear(year.id)}
                                  style={{ background: '#ef4444', color: '#ffffff', border: 'none', padding: '6px 8px', cursor: 'pointer', fontSize: '11px', fontWeight: 'bold' }}
                                >
                                  Kết thúc năm học
                                </button>
                              );
                            }
                            return <span style={{ fontSize: '11px', color: '#737373' }}>-</span>;
                          })()}
                          {year.status === 'COMPLETED' && (
                            <span style={{ fontSize: '11px', color: '#737373', fontStyle: 'italic' }}>Đã kết thúc</span>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
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

          <div style={{ background: '#ffffff', border: '1px solid #000000', padding: '20px' }}>
            <h3 style={{ fontSize: '13px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '12px', paddingBottom: '6px', borderBottom: '1px solid #e5e5e5' }}>
              Quản lý niên khóa
            </h3>
            <p style={{ fontSize: '12px', color: '#666', marginBottom: '16px', lineHeight: '1.4' }}>
              Khởi tạo hàng loạt 10 niên khóa tiếp theo kèm 2 học kỳ (Học kỳ 1 & Học kỳ 2) tính từ niên khóa cuối cùng trong hệ thống.
            </p>
            <button
              onClick={handleAdd10Years}
              disabled={creating}
              style={{ width: '100%', height: '38px', background: '#000000', color: '#ffffff', border: 'none', fontWeight: 'bold', cursor: 'pointer', fontSize: '12px' }}
            >
              {creating ? 'Đang khởi tạo...' : 'Thêm 10 niên khóa tiếp theo'}
            </button>
          </div>
        </div>
      )}
      {initializing && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          width: '100vw',
          height: '100vh',
          background: 'rgba(255, 255, 255, 0.75)',
          backdropFilter: 'blur(5px)',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 9999,
          gap: '16px'
        }}>
          <div className="spinner" />
          <span style={{ fontSize: '14px', fontWeight: 700, color: '#000000', letterSpacing: '0.05em', textTransform: 'uppercase' }}>
            Đang tải dữ liệu...
          </span>
        </div>
      )}
    </div>
  );
}
