import { useState, useEffect, useRef } from 'react';
import { getAcademicYears } from '../api/academicYear';
import { getSemesters } from '../api/semester';
import { getClasses } from '../api/class';
import { getSubjects } from '../api/subject';
import { getTeachers } from '../api/user';
import type { TeacherItem } from '../api/user';
import { 
  getTeachingAssignments, 
  createTeachingAssignment, 
  updateTeachingAssignment, 
  deleteTeachingAssignment 
} from '../api/teachingAssignment';

interface AcademicYearItem { id: number; name: string; status: string; }
interface ClassItem { id: number; name: string; gradeLevel: number; academicYearId: number; academicYearName: string; }
interface SubjectItem { id: number; name: string; code: string; }
interface SemesterItem { id: number; name: string; academicYearId: number; academicYearName: string; isCurrent: boolean; order: number; }
interface TeachingAssignmentItem {
  id: number;
  classId: number;
  className: string;
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  teacherId: number;
  teacherName: string;
  teacherCode: string;
  semesterId: number;
  semesterName: string;
  status: string;
  effectiveFrom: string;
}

const today = () => new Date().toISOString().slice(0, 10);

export default function AssignmentsPage({ selectedYearId, selectedSemesterId }: { selectedYearId?: string; selectedSemesterId?: string }) {
  const [academicYears, setAcademicYears] = useState<AcademicYearItem[]>([]);
  const [academicYearId, setAcademicYearId] = useState(selectedYearId || '');
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [subjects, setSubjects] = useState<SubjectItem[]>([]);
  const [semesters, setSemesters] = useState<SemesterItem[]>([]);
  const [teachers, setTeachers] = useState<TeacherItem[]>([]);
  
  // Matrix specific states
  const [gradeFilter, setGradeFilter] = useState('10');
  const [subjectId, setSubjectId] = useState('');
  const [semesterId, setSemesterId] = useState(selectedSemesterId || '');
  
  // Assignments map: classId -> list of assignments
  const [classAssignmentsMap, setClassAssignmentsMap] = useState<Record<number, TeachingAssignmentItem[]>>({});
  
  const [loadingList, setLoadingList] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [progress, setProgress] = useState({ active: false, current: 0, total: 0, label: '' });

  const fileInputRef = useRef<HTMLInputElement>(null);

  // Initial load
  useEffect(() => {
    fetchAcademicYears();
    getSubjects().then((d: any) => setSubjects(d || [])).catch(() => {});
    fetchTeachers();
  }, []);

  useEffect(() => {
    if (selectedYearId) {
      setAcademicYearId(selectedYearId);
    }
  }, [selectedYearId]);

  useEffect(() => {
    if (selectedSemesterId) {
      setSemesterId(selectedSemesterId);
    }
  }, [selectedSemesterId]);

  function fetchTeachers(subjectIdParam?: string) {
    getTeachers({
      status: 'ACTIVE',
      subjectId: subjectIdParam ? Number(subjectIdParam) : undefined,
      page: 0,
      size: 100
    }).then((d: any) => setTeachers(d.content || d || []))
      .catch(() => {});
  }

  useEffect(() => {
    if (subjectId) {
      fetchTeachers(subjectId);
    }
  }, [subjectId]);

  // Academic year load
  useEffect(() => {
    if (!academicYearId) return;
    setSubjectId('');
    setClassAssignmentsMap({});
    
    getClasses({ academicYearId, page: 0, size: 100 })
      .then((d: any) => setClasses(d.content || []))
      .catch(() => {});
      
    getSemesters(academicYearId)
      .then((d: any) => {
        const list = d || [];
        setSemesters(list);
        if (!selectedSemesterId) {
          const current = list.find((s: SemesterItem) => s.isCurrent) || list[0];
          if (current) setSemesterId(String(current.id));
        }
      })
      .catch(() => {});
  }, [academicYearId, selectedSemesterId]);

  // Load assignments when filter class lists or semester changes
  const filteredClasses = classes.filter(c => String(c.gradeLevel) === gradeFilter);

  useEffect(() => {
    if (filteredClasses.length > 0 && semesterId) {
      reloadAssignmentsForGrade(filteredClasses, semesterId);
    } else {
      setClassAssignmentsMap({});
    }
  }, [gradeFilter, semesterId, classes]);

  async function fetchAcademicYears() {
    const data = await getAcademicYears() as AcademicYearItem[];
    setAcademicYears(data);
    if (!selectedYearId) {
      const active = data.find(y => y.status === 'ACTIVE') || data[0];
      if (active) setAcademicYearId(String(active.id));
    }
  }

  async function reloadAssignmentsForGrade(classesList: ClassItem[], semId: string) {
    setLoadingList(true);
    try {
      const promises = classesList.map(c => 
        getTeachingAssignments({ classId: c.id, semesterId: semId })
          .then((arr: any) => ({ classId: c.id, assignments: arr || [] }))
          .catch(() => ({ classId: c.id, assignments: [] }))
      );
      const results = await Promise.all(promises);
      const newMap: Record<number, TeachingAssignmentItem[]> = {};
      results.forEach(res => {
        newMap[res.classId] = res.assignments;
      });
      setClassAssignmentsMap(newMap);
    } catch (err: any) {
      setError(err.message || 'Không thể tải chi tiết phân công cho khối');
    } finally {
      setLoadingList(false);
    }
  }

  // Calculate teacher workloads dynamically based on current semester assignments loaded
  const getTeacherLoadMap = () => {
    const loadMap: Record<number, number> = {};
    teachers.forEach(t => {
      loadMap[t.id] = 0;
    });
    Object.values(classAssignmentsMap).forEach(list => {
      list.forEach(a => {
        if (a.status === 'ACTIVE' && a.teacherId) {
          loadMap[a.teacherId] = (loadMap[a.teacherId] || 0) + 1;
        }
      });
    });
    return loadMap;
  };

  const loadMap = getTeacherLoadMap();
  const isOverloaded = (tId: number) => (loadMap[tId] || 0) >= 4;

  // Save / Update / Delete assignment
  async function handleAssignTeacher(classItem: ClassItem, targetTeacherId: number | null) {
    setError('');
    setSuccessMsg('');
    if (!subjectId) {
      setError('Vui lòng chọn Môn học trước khi phân công.');
      return;
    }

    const existing = classAssignmentsMap[classItem.id]?.find(a => a.subjectId === Number(subjectId) && a.status === 'ACTIVE');

    try {
      if (targetTeacherId === null) {
        if (existing) {
          await deleteTeachingAssignment(existing.id);
          setSuccessMsg(`Đã hủy phân công giáo viên lớp ${classItem.name}`);
        }
      } else {
        if (existing) {
          await updateTeachingAssignment(existing.id, {
            classId: classItem.id,
            subjectId: Number(subjectId),
            teacherId: targetTeacherId,
            semesterId: Number(semesterId),
            effectiveFrom: today()
          });
          setSuccessMsg(`Đã cập nhật giáo viên giảng dạy lớp ${classItem.name}`);
        } else {
          await createTeachingAssignment({
            classId: classItem.id,
            subjectId: Number(subjectId),
            teacherId: targetTeacherId,
            semesterId: Number(semesterId),
            effectiveFrom: today()
          });
          setSuccessMsg(`Đã phân công giáo viên giảng dạy lớp ${classItem.name}`);
        }
      }
      await reloadAssignmentsForGrade(filteredClasses, semesterId);
    } catch (err: any) {
      setError(err.message || 'Lỗi khi lưu phân công');
    }
  }

  // Bulk apply teacher of the first class to all classes of this grade
  async function handleBulkApply() {
    setError('');
    setSuccessMsg('');
    if (filteredClasses.length === 0 || !subjectId) {
      setError('Vui lòng chọn Khối và Môn học.');
      return;
    }

    const firstClass = filteredClasses[0];
    const firstAssignment = classAssignmentsMap[firstClass.id]?.find(a => a.subjectId === Number(subjectId) && a.status === 'ACTIVE');
    if (!firstAssignment) {
      setError('Vui lòng phân công giáo viên cho lớp học đầu tiên trước khi áp dụng cho tất cả.');
      return;
    }

    const targetTeacherId = firstAssignment.teacherId;
    setLoadingList(true);
    try {
      const promises = filteredClasses.slice(1).map(async (classItem) => {
        const existing = classAssignmentsMap[classItem.id]?.find(a => a.subjectId === Number(subjectId) && a.status === 'ACTIVE');
        if (existing) {
          if (existing.teacherId !== targetTeacherId) {
            await updateTeachingAssignment(existing.id, {
              classId: classItem.id,
              subjectId: Number(subjectId),
              teacherId: targetTeacherId,
              semesterId: Number(semesterId),
              effectiveFrom: today()
            });
          }
        } else {
          await createTeachingAssignment({
            classId: classItem.id,
            subjectId: Number(subjectId),
            teacherId: targetTeacherId,
            semesterId: Number(semesterId),
            effectiveFrom: today()
          });
        }
      });

      await Promise.all(promises);
      setSuccessMsg(`Đã áp dụng giáo viên cho toàn bộ lớp thuộc khối`);
      await reloadAssignmentsForGrade(filteredClasses, semesterId);
    } catch (err: any) {
      setError(err.message || 'Lỗi áp dụng phân công hàng loạt');
    } finally {
      setLoadingList(false);
    }
  }

  // Auto assign teachers
  async function handleAutoAssign() {
    setError('');
    setSuccessMsg('');
    if (filteredClasses.length === 0 || !subjectId) {
      setError('Vui lòng chọn Môn học để tự động phân công.');
      return;
    }

    const availableTeachers = teachers;
    if (availableTeachers.length === 0) {
      setError('Không tìm thấy giáo viên khả dụng để tự động phân công.');
      return;
    }

    setLoadingList(true);
    try {
      const promises = filteredClasses.map(async (classItem, idx) => {
        const existing = classAssignmentsMap[classItem.id]?.find(a => a.subjectId === Number(subjectId) && a.status === 'ACTIVE');
        if (!existing) {
          const teacher = availableTeachers[idx % availableTeachers.length];
          await createTeachingAssignment({
            classId: classItem.id,
            subjectId: Number(subjectId),
            teacherId: teacher.id,
            semesterId: Number(semesterId),
            effectiveFrom: today()
          });
        }
      });

      await Promise.all(promises);
      setSuccessMsg('Tự động phân công giáo viên thành công!');
      await reloadAssignmentsForGrade(filteredClasses, semesterId);
    } catch (err: any) {
      setError(err.message || 'Lỗi phân công tự động');
    } finally {
      setLoadingList(false);
    }
  }

  // Import from Excel/CSV file select
  function handleImportCsvSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) processImportCsv(file);
  }

  async function processImportCsv(file: File) {
    setError('');
    setSuccessMsg('');
    if (!academicYearId || !semesterId) {
      setError('Vui lòng chọn đầy đủ Năm học và Học kỳ.');
      return;
    }
    if (!file.name.endsWith('.csv')) {
      setError('Chỉ chấp nhận tệp định dạng CSV (.csv)');
      return;
    }

    const reader = new FileReader();
    reader.onload = async (event) => {
      const text = event.target?.result as string;
      const lines = (text || '').split('\n').map(l => l.trim()).filter(Boolean);
      if (lines.length === 0) {
        setError('Tệp không chứa dữ liệu.');
        return;
      }

      let startIndex = 0;
      const firstLineParts = lines[0].split(',');
      const isHeader = isNaN(Number(firstLineParts[0])) && firstLineParts[0].toLowerCase() !== 'id';
      if (isHeader) startIndex = 1;

      const parsedAssignments: { classId: number; className: string; subjectId: number; subjectCode: string; teacherId: number; teacherCode: string }[] = [];
      const importErrors: string[] = [];

      for (let i = startIndex; i < lines.length; i++) {
        const parts = lines[i].split(',').map(p => p.trim());
        if (parts.length < 3) {
          importErrors.push(`Dòng ${i + 1}: Thiếu cột thông tin (cần Lớp học, Mã môn, Mã giáo viên).`);
          continue;
        }

        const classNameInput = parts[0];
        const subjectCodeInput = parts[1].toUpperCase();
        const teacherCodeInput = parts[2].toUpperCase();

        const matchedClass = classes.find(c => c.name.toLowerCase() === classNameInput.toLowerCase());
        if (!matchedClass) {
          importErrors.push(`Dòng ${i + 1}: Không tìm thấy lớp học '${classNameInput}' trong năm học hiện tại.`);
          continue;
        }

        const matchedSubject = subjects.find(s => s.code.toUpperCase() === subjectCodeInput);
        if (!matchedSubject) {
          importErrors.push(`Dòng ${i + 1}: Không tìm thấy môn học '${subjectCodeInput}'.`);
          continue;
        }

        const matchedTeacher = teachers.find(t => t.employeeCode?.toUpperCase() === teacherCodeInput);
        if (!matchedTeacher) {
          importErrors.push(`Dòng ${i + 1}: Không tìm thấy giáo viên mã '${teacherCodeInput}'.`);
          continue;
        }

        parsedAssignments.push({
          classId: matchedClass.id,
          className: matchedClass.name,
          subjectId: matchedSubject.id,
          subjectCode: matchedSubject.code,
          teacherId: matchedTeacher.id,
          teacherCode: matchedTeacher.employeeCode
        });
      }

      if (importErrors.length > 0) {
        setError(`Lỗi phân tích file CSV:\n${importErrors.slice(0, 5).join('\n')}${importErrors.length > 5 ? '\n...và các lỗi khác' : ''}`);
        return;
      }

      if (parsedAssignments.length === 0) {
        setError('Không tìm thấy bản ghi phân công nào hợp lệ.');
        return;
      }

      setProgress({ active: true, current: 0, total: parsedAssignments.length, label: 'Đang chuẩn bị nhập phân công...' });
      try {
        for (let idx = 0; idx < parsedAssignments.length; idx++) {
          const item = parsedAssignments[idx];
          setProgress(p => ({ ...p, current: idx, label: `Đang phân công: Lớp ${item.className} - Môn ${item.subjectCode} (${idx + 1}/${parsedAssignments.length})...` }));
          
          const classAssignments = await getTeachingAssignments({ classId: item.classId, semesterId: semesterId }).catch(() => []) as any[];
          const existing = classAssignments.find(a => a.subjectId === item.subjectId && a.status === 'ACTIVE');

          if (existing) {
            if (existing.teacherId !== item.teacherId) {
              await updateTeachingAssignment(existing.id, {
                classId: item.classId,
                subjectId: item.subjectId,
                teacherId: item.teacherId,
                semesterId: Number(semesterId),
                effectiveFrom: today()
              });
            }
          } else {
            await createTeachingAssignment({
              classId: item.classId,
              subjectId: item.subjectId,
              teacherId: item.teacherId,
              semesterId: Number(semesterId),
              effectiveFrom: today()
            });
          }
        }

        setProgress(p => ({ ...p, current: parsedAssignments.length, label: 'Nhập phân công thành công!' }));
        setTimeout(() => setProgress(p => ({ ...p, active: false })), 2000);
        setSuccessMsg(`Nhập thành công ${parsedAssignments.length} phân công giảng dạy từ file CSV!`);
        if (subjectId) {
          reloadAssignmentsForGrade(filteredClasses, semesterId);
        }
      } catch (err: any) {
        setError(`Lỗi import: ${err.message || 'Lỗi không xác định'}`);
        setProgress(p => ({ ...p, active: false }));
      }
    };
    reader.onerror = () => setError('Đọc tệp tin thất bại.');
    reader.readAsText(file);
  }

  function downloadCsvTemplate() {
    const csvContent = 'Lớp học,Mã môn,Mã giáo viên\n10A1,MATH,GV001\n10A2,LIT,GV002\n10A3,ENG,GV003\n';
    const blob = new Blob([new Uint8Array([0xEF, 0xBB, 0xBF]), csvContent], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'mau_phan_cong_giang_day.csv';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  // Filter teachers having profiles
  const teachersWithProfiles = teachers;

  return (
    <div>
      {/* Top Bar Navigation */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', borderBottom: '2px solid #000000', paddingBottom: '12px', flexWrap: 'wrap', gap: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <h2 style={{ margin: 0, border: 'none', padding: 0 }}>Phân công giảng dạy</h2>
          <select 
            value={academicYearId} 
            onChange={e => setAcademicYearId(e.target.value)}
            style={{ height: '32px', padding: '0 8px', border: '1px solid #000', fontSize: '12px', fontWeight: 'bold' }}
          >
            <option value="">Chọn năm học</option>
            {academicYears.map(y => <option key={y.id} value={y.id}>{y.name} {y.status === 'ACTIVE' ? '(Đang mở)' : ''}</option>)}
          </select>
          <select 
            value={semesterId} 
            onChange={e => setSemesterId(e.target.value)} 
            disabled={!academicYearId}
            style={{ height: '32px', padding: '0 8px', border: '1px solid #000', fontSize: '12px', fontWeight: 'bold' }}
          >
            <option value="">Chọn học kỳ</option>
            {semesters.map(s => <option key={s.id} value={s.id}>{s.name} {s.isCurrent ? '(Hiện tại)' : ''}</option>)}
          </select>
        </div>
        
        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <button 
            onClick={handleAutoAssign}
            disabled={loadingList || !subjectId}
            style={{ height: '32px', padding: '0 12px', background: '#000000', color: '#ffffff', border: 'none', fontSize: '12px', fontWeight: 'bold', cursor: 'pointer' }}
          >
            Tự động phân công
          </button>
          <button 
            onClick={() => fileInputRef.current?.click()}
            style={{ height: '32px', padding: '0 12px', background: '#ffffff', color: '#000000', border: '1px solid #000000', fontSize: '12px', fontWeight: 'bold', cursor: 'pointer' }}
          >
            Import CSV
          </button>
          <button 
            onClick={downloadCsvTemplate}
            style={{ height: '32px', padding: '0 12px', background: '#f3f4f6', color: '#374151', border: '1px solid #d1d5db', fontSize: '12px', cursor: 'pointer' }}
          >
            Tải mẫu CSV
          </button>
        </div>
      </div>

      <input 
        type="file" 
        ref={fileInputRef} 
        style={{ display: 'none' }} 
        accept=".csv" 
        onChange={handleImportCsvSelect} 
      />

      {error && <div className="error" style={{ marginBottom: '16px' }}>{error}</div>}
      {successMsg && <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: '16px' }}>[SUCCESS] {successMsg}</div>}
      
      {progress.active && (
        <div className="progress-container" style={{ marginBottom: '20px' }}>
          <div>{progress.label}</div>
          <div className="progress-bar-bg"><div className="progress-bar-fill" style={{ width: `${progress.total ? (progress.current / progress.total) * 100 : 0}%` }} /></div>
          <div className="progress-text">Đã hoàn thành: {progress.current} / {progress.total} phân công</div>
        </div>
      )}

      {/* Main Container Layout */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '24px', alignItems: 'start' }}>
        
        {/* LEFT COLUMN: Filters & Teachers */}
        <div style={{ background: '#ffffff', border: '1px solid #000000', padding: '16px' }}>
          <h3 style={{ margin: '0 0 16px 0', fontSize: '14px', fontWeight: 800, textTransform: 'uppercase', borderBottom: '1px solid #000000', paddingBottom: '6px' }}>
            Bộ lọc & Tài nguyên
          </h3>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '20px' }}>
            <div className="form-group" style={{ margin: 0 }}>
              <label style={{ fontSize: '11px', fontWeight: 'bold' }}>Chọn Khối</label>
              <select value={gradeFilter} onChange={e => setGradeFilter(e.target.value)} style={{ height: '34px', border: '1px solid #000' }}>
                {Array.from({ length: 12 }, (_, i) => i + 1).map(g => (
                  <option key={g} value={g}>Khối {g}</option>
                ))}
              </select>
            </div>

            <div className="form-group" style={{ margin: 0 }}>
              <label style={{ fontSize: '11px', fontWeight: 'bold' }}>Chọn Môn học</label>
              <select value={subjectId} onChange={e => setSubjectId(e.target.value)} style={{ height: '34px', border: '1px solid #000' }}>
                <option value="">Chọn môn học</option>
                {subjects.map(s => <option key={s.id} value={s.id}>{s.name} ({s.code})</option>)}
              </select>
            </div>
          </div>

          <h4 style={{ margin: '0 0 12px 0', fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', color: '#666', borderBottom: '1px dashed #e5e5e5', paddingBottom: '6px' }}>
            Danh sách Giáo viên bộ môn
          </h4>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '400px', overflowY: 'auto', paddingRight: '4px' }}>
            {teachersWithProfiles.map(t => {
              const count = loadMap[t.id] || 0;
              const overloaded = isOverloaded(t.id);
              
              return (
                <div 
                  key={t.id}
                  draggable
                  onDragStart={e => {
                    e.dataTransfer.setData("text/plain", String(t.id));
                  }}
                  style={{ 
                    padding: '8px 12px', 
                    background: overloaded ? '#fef08a' : '#fafafa', 
                    border: overloaded ? '1px solid #eab308' : '1px solid #d4d4d4',
                    fontSize: '12px',
                    fontWeight: 500,
                    cursor: 'grab',
                    userSelect: 'none',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                  }}
                  title={overloaded ? "Cảnh báo: Giáo viên này đang dạy >= 4 lớp học!" : ""}
                >
                  <span>{t.name}</span>
                  <span style={{ fontSize: '10px', color: overloaded ? '#a16207' : '#737373', fontWeight: 'bold' }}>
                    Đang dạy: {count}
                  </span>
                </div>
              );
            })}
            {teachersWithProfiles.length === 0 && (
              <p style={{ fontSize: '11px', color: '#737373', fontStyle: 'italic' }}>Không có giáo viên bộ môn nào.</p>
            )}
          </div>
        </div>

        {/* RIGHT COLUMN: Grid Matrix */}
        <div style={{ background: '#ffffff', border: '1px solid #000000', padding: '16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', borderBottom: '1px solid #000000', paddingBottom: '6px' }}>
            <h3 style={{ margin: 0, fontSize: '14px', fontWeight: 800, textTransform: 'uppercase' }}>
              Bảng phân công khối {gradeFilter}
            </h3>
            
            {subjectId && filteredClasses.length > 0 && (
              <button 
                onClick={handleBulkApply}
                style={{ height: '26px', padding: '0 8px', background: '#000000', color: '#ffffff', border: 'none', fontSize: '11px', fontWeight: 'bold', cursor: 'pointer' }}
              >
                Áp dụng cho toàn bộ khối
              </button>
            )}
          </div>

          {!subjectId ? (
            <div style={{ padding: '40px 20px', textAlign: 'center', background: '#fafafa', border: '1px dashed #d4d4d4', color: '#737373' }}>
              <p style={{ margin: 0, fontSize: '13px', fontWeight: 'bold' }}>VUI LÒNG CHỌN MÔN HỌC Ở CỘT TRÁI</p>
              <p style={{ margin: '4px 0 0 0', fontSize: '12px' }}>Hệ thống sẽ hiển thị lưới phân công chi tiết cho môn học đó.</p>
            </div>
          ) : filteredClasses.length === 0 ? (
            <p style={{ fontSize: '13px', color: '#737373', fontStyle: 'italic', textAlign: 'center', padding: '20px' }}>
              Chưa có dữ liệu lớp học nào thuộc Khối {gradeFilter} trong năm học hiện tại.
            </p>
          ) : (
            <div>
              <table style={{ margin: 0 }}>
                <thead>
                  <tr>
                    <th style={{ width: '150px' }}>Lớp học</th>
                    <th style={{ width: '180px' }}>Môn học</th>
                    <th>Giáo viên phụ trách</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredClasses.map(c => {
                    const assignment = classAssignmentsMap[c.id]?.find(
                      a => a.subjectId === Number(subjectId) && a.status === 'ACTIVE'
                    );
                    const selectedTeacherId = assignment ? assignment.teacherId : '';
                    const overloadedSelected = assignment ? isOverloaded(assignment.teacherId) : false;

                    return (
                      <tr key={c.id}>
                        <td style={{ fontWeight: 'bold' }}>{c.name}</td>
                        <td style={{ color: '#666' }}>
                          {subjects.find(s => String(s.id) === subjectId)?.name}
                        </td>
                        <td 
                          onDragOver={e => e.preventDefault()}
                          onDrop={e => {
                            const tId = e.dataTransfer.getData("text/plain");
                            if (tId) {
                              handleAssignTeacher(c, Number(tId));
                            }
                          }}
                          style={{ 
                            background: overloadedSelected ? '#fef9c3' : 'transparent',
                            transition: 'background 0.2s ease',
                            padding: '4px 8px'
                          }}
                        >
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', width: '100%' }}>
                            <select 
                              value={selectedTeacherId}
                              onChange={e => {
                                const val = e.target.value;
                                handleAssignTeacher(c, val ? Number(val) : null);
                              }}
                              style={{ 
                                flex: 1, 
                                height: '30px', 
                                border: '1px solid #d4d4d4', 
                                fontSize: '12px',
                                background: overloadedSelected ? '#fef08a' : '#ffffff' 
                              }}
                            >
                              <option value="">[Chọn giáo viên]</option>
                              {teachersWithProfiles.map(t => {
                                const count = loadMap[t.id] || 0;
                                return (
                                  <option key={t.id} value={t.id}>
                                    {t.name} ({t.employeeCode}) - Dạy: {count} lớp
                                  </option>
                                );
                              })}
                            </select>

                            {assignment && (
                              <button 
                                onClick={() => handleAssignTeacher(c, null)}
                                className="danger" 
                                style={{ 
                                  height: '30px', 
                                  width: '30px', 
                                  padding: 0, 
                                  display: 'flex', 
                                  alignItems: 'center', 
                                  justifyContent: 'center',
                                  fontSize: '12px',
                                  fontWeight: 'bold'
                                }}
                                title="Hủy phân công"
                              >
                                X
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
