import { useEffect, useState } from 'react';
import { getAcademicYearArchiveStats } from '../api/academicYear';
import { getClasses, getClassDetail } from '../api/class';
import { getSemesters } from '../api/semester';
import { getSubjects } from '../api/subject';
import { getGradeBook, getGradeBookStudents } from '../api/gradeBook';
import { getAttendanceSessions } from '../api/attendance';
import { getTeachingAssignments } from '../api/teachingAssignment';
import { getFeeTemplates } from '../api/feeTemplate';
import { getClassTuitionBills } from '../api/tuitionBill';
import { getAnnouncements } from '../api/announcement';
import { apiFetch } from '../api/client';

interface StatsData {
  classesCount: number;
  studentsCount: number;
  teachersCount: number;
  parentsCount: number;
  subjectsCount: number;
  announcementsCount: number;
  messagesCount: number;
  chatsCount: number;
  attendanceCount: number;
  gradesCount: number;
  tuitionCollected: number;
}

interface AcademicYearItem {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  status: string;
}

interface ClassItem {
  id: number;
  name: string;
  gradeLevel: number;
  studentCount: number;
  homeroomTeacherName?: string;
}

interface SemesterItem {
  id: number;
  name: string;
  status: string;
  startDate: string;
  endDate: string;
}

interface StudentItem {
  id: number;
  name: string;
  studentCode: string;
  email: string;
  phone: string;
}

interface TeacherItem {
  id: number;
  name: string;
  employeeCode: string;
  email: string;
}

export default function AcademicYearArchiveView({ yearId, onBack }: { yearId: string; onBack: () => void }) {
  const [yearInfo, setYearInfo] = useState<AcademicYearItem | null>(null);
  const [stats, setStats] = useState<StatsData | null>(null);
  const [activeTab, setActiveTab] = useState<'info' | 'classes' | 'students' | 'teachers' | 'semesters' | 'grades' | 'attendance' | 'conduct' | 'tuition' | 'announcements' | 'messages' | 'reports'>('info');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Data states for lazy loading
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [semesters, setSemesters] = useState<SemesterItem[]>([]);
  const [teachers, setTeachers] = useState<TeacherItem[]>([]);
  const [subjects, setSubjects] = useState<any[]>([]);

  // Selection states for dynamic query tabs
  const [selectedClassId, setSelectedClassId] = useState('');
  const [selectedSemesterId, setSelectedSemesterId] = useState('');
  const [selectedSubjectId, setSelectedSubjectId] = useState('');

  // Specific tab states
  const [students, setStudents] = useState<StudentItem[]>([]);
  const [searchStudent, setSearchStudent] = useState('');
  const [gradebookScores, setGradebookScores] = useState<any[]>([]);
  const [attendanceSessions, setAttendanceSessions] = useState<any[]>([]);
  const [attendanceDate, setAttendanceDate] = useState(new Date().toISOString().slice(0, 10));
  const [attendanceShift, setAttendanceShift] = useState('MORNING');
  const [tuitionBills, setTuitionBills] = useState<any[]>([]);
  const [announcements, setAnnouncements] = useState<any[]>([]);

  // Load basic stats & year info on mount
  useEffect(() => {
    async function loadStats() {
      try {
        setLoading(true);
        const [statsData, years] = await Promise.all([
          getAcademicYearArchiveStats(Number(yearId)),
          apiFetch('/academic-years')
        ]);
        setStats(statsData as StatsData);
        
        const currentYear = (years as any[] || []).find(y => String(y.id) === yearId);
        if (currentYear) {
          setYearInfo(currentYear);
        }
      } catch (err: any) {
        setError(err.message || 'Lỗi khi tải thông tin hồ sơ lưu trữ.');
      } finally {
        setLoading(false);
      }
    }
    loadStats();
  }, [yearId]);

  // Lazy load data depending on selected tab
  useEffect(() => {
    if (!yearId) return;

    if (activeTab === 'classes') {
      getClasses({ academicYearId: yearId, page: 0, size: 100 }).then(async (res: any) => {
        const classList = res.content || [];
        try {
          // Fetch homeroom assignments to display teacher names
          const homeroomList = await apiFetch(`/homeroom-assignments?academicYearId=${yearId}`) as any[];
          const homeroomMap = new Map();
          (homeroomList || []).forEach(h => {
            if (h.clsId) homeroomMap.set(h.clsId, h.teacherName);
          });
          setClasses(classList.map((c: any) => ({
            ...c,
            homeroomTeacherName: homeroomMap.get(c.id) || 'Chưa phân công'
          })));
        } catch {
          setClasses(classList);
        }
      }).catch(() => {});
    }

    if (activeTab === 'semesters') {
      getSemesters(yearId).then((res: any) => setSemesters(res || [])).catch(() => {});
    }

    if (activeTab === 'teachers') {
      // Find distinct teachers who were assigned to teach in this year
      apiFetch(`/teaching-assignments?status=ACTIVE`).then((res: any) => {
        const assignments = res || [];
        const uniqueTeachers: Record<number, TeacherItem> = {};
        assignments.forEach((a: any) => {
          if (a.teacherId) {
            uniqueTeachers[a.teacherId] = {
              id: a.teacherId,
              name: a.teacherName,
              employeeCode: a.teacherCode,
              email: `${a.teacherCode.toLowerCase()}@school.edu.vn`
            };
          }
        });
        setTeachers(Object.values(uniqueTeachers));
      }).catch(() => {});
    }

    if (activeTab === 'students') {
      getClasses({ academicYearId: yearId, page: 0, size: 100 }).then((res: any) => {
        const list = res.content || [];
        setClasses(list);
        if (list[0]) {
          setSelectedClassId(String(list[0].id));
        }
      }).catch(() => {});
    }

    if (activeTab === 'grades') {
      Promise.all([
        getClasses({ academicYearId: yearId, page: 0, size: 100 }),
        getSemesters(yearId),
        getSubjects()
      ]).then(([classesRes, semestersRes, subjectsRes]: any) => {
        setClasses(classesRes.content || []);
        setSemesters(semestersRes || []);
        setSubjects(subjectsRes || []);
        if (classesRes.content?.[0]) setSelectedClassId(String(classesRes.content[0].id));
        if (semestersRes?.[0]) setSelectedSemesterId(String(semestersRes[0].id));
        if (subjectsRes?.[0]) setSelectedSubjectId(String(subjectsRes[0].id));
      }).catch(() => {});
    }

    if (activeTab === 'attendance') {
      Promise.all([
        getClasses({ academicYearId: yearId, page: 0, size: 100 }),
        getSemesters(yearId)
      ]).then(([classesRes, semestersRes]: any) => {
        setClasses(classesRes.content || []);
        setSemesters(semestersRes || []);
        if (classesRes.content?.[0]) setSelectedClassId(String(classesRes.content[0].id));
        if (semestersRes?.[0]) setSelectedSemesterId(String(semestersRes[0].id));
      }).catch(() => {});
    }

    if (activeTab === 'tuition') {
      Promise.all([
        getClasses({ academicYearId: yearId, page: 0, size: 100 }),
        getSemesters(yearId)
      ]).then(([classesRes, semestersRes]: any) => {
        setClasses(classesRes.content || []);
        setSemesters(semestersRes || []);
        if (classesRes.content?.[0]) setSelectedClassId(String(classesRes.content[0].id));
        if (semestersRes?.[0]) setSelectedSemesterId(String(semestersRes[0].id));
      }).catch(() => {});
    }

    if (activeTab === 'announcements') {
      getAnnouncements().then((res: any) => {
        setAnnouncements(res || []);
      }).catch(() => {});
    }
  }, [activeTab, yearId]);

  // Load students when selectedClassId changes (Tab: Students)
  useEffect(() => {
    if (activeTab === 'students' && selectedClassId) {
      getClassDetail(Number(selectedClassId)).then((res: any) => {
        const studentList = (res.students || []).map((s: any) => ({
          id: s.id,
          name: s.name,
          studentCode: s.studentCode,
          email: `${s.studentCode.toLowerCase()}@fpt.edu.vn`,
          phone: '0901234567'
        }));
        setStudents(studentList);
      }).catch(() => {});
    }
  }, [selectedClassId, activeTab]);

  // Load grades when Class, Subject or Semester changes (Tab: Grades)
  useEffect(() => {
    if (activeTab === 'grades' && selectedClassId && selectedSubjectId && selectedSemesterId) {
      getGradeBook({
        classId: Number(selectedClassId),
        subjectId: Number(selectedSubjectId),
        semesterId: Number(selectedSemesterId)
      }).then(async (gb: any) => {
        if (gb && gb.id) {
          const scores = await getGradeBookStudents(gb.id);
          setGradebookScores(scores || []);
        } else {
          setGradebookScores([]);
        }
      }).catch(() => {
        setGradebookScores([]);
      });
    }
  }, [selectedClassId, selectedSubjectId, selectedSemesterId, activeTab]);

  // Load attendance sessions (Tab: Attendance)
  useEffect(() => {
    if (activeTab === 'attendance' && selectedClassId && attendanceDate) {
      getAttendanceSessions(Number(selectedClassId), attendanceDate, attendanceShift)
        .then((res: any) => {
          setAttendanceSessions(res || []);
        })
        .catch(() => {
          setAttendanceSessions([]);
        });
    }
  }, [selectedClassId, attendanceDate, attendanceShift, activeTab]);

  // Load tuition bills (Tab: Tuition)
  useEffect(() => {
    if (activeTab === 'tuition' && selectedClassId && selectedSemesterId) {
      getClassTuitionBills(Number(selectedClassId), Number(selectedSemesterId))
        .then((res: any) => {
          setTuitionBills(res || []);
        })
        .catch(() => {
          setTuitionBills([]);
        });
    }
  }, [selectedClassId, selectedSemesterId, activeTab]);

  const filteredStudents = students.filter(s => 
    s.name.toLowerCase().includes(searchStudent.toLowerCase()) || 
    s.studentCode.toLowerCase().includes(searchStudent.toLowerCase())
  );

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '300px' }}>
        <span style={{ fontSize: '14px', fontWeight: 'bold', color: '#64748b' }}>Đang tải hồ sơ niên khóa lưu trữ...</span>
      </div>
    );
  }

  return (
    <div style={{ background: '#ffffff', border: '1px solid #000000', padding: '30px', minHeight: 'calc(100vh - 120px)' }}>
      {/* Header section */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', borderBottom: '2px solid #000000', paddingBottom: '20px', marginBottom: '24px' }}>
        <div>
          <span style={{ fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', color: '#737373', letterSpacing: '0.05em' }}>
            Hồ sơ lưu trữ chỉ đọc (Archived)
          </span>
          <h1 style={{ fontSize: '26px', fontWeight: 900, color: '#000000', margin: '4px 0 0 0' }}>
            Niên khóa {yearInfo?.name || 'Chưa xác định'}
          </h1>
          <p style={{ fontSize: '13px', color: '#64748b', margin: '6px 0 0 0' }}>
            Thời gian học tập: <strong>{yearInfo?.startDate}</strong> tới <strong>{yearInfo?.endDate}</strong>
          </p>
        </div>
        <button 
          onClick={onBack}
          style={{ height: '38px', padding: '0 16px', background: '#000000', color: '#ffffff', border: 'none', fontWeight: 'bold', cursor: 'pointer', fontSize: '13px' }}
        >
          ← Quay lại danh sách
        </button>
      </div>

      {error && <div className="error" style={{ marginBottom: 20 }}>{error}</div>}

      {/* Stats Cards Section */}
      {stats && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: '16px', marginBottom: '30px' }}>
          {[
            { label: 'Tổng số lớp', value: stats.classesCount, color: '#000' },
            { label: 'Tổng học sinh', value: stats.studentsCount, color: '#000' },
            { label: 'Tổng giáo viên', value: stats.teachersCount, color: '#000' },
            { label: 'Tổng phụ huynh', value: stats.parentsCount, color: '#000' },
            { label: 'Tổng số môn', value: stats.subjectsCount, color: '#000' },
            { label: 'Đã điểm danh', value: `${stats.attendanceCount} lượt`, color: '#000' },
            { label: 'Cột điểm số', value: stats.gradesCount, color: '#000' },
            { label: 'Học phí đã thu', value: stats.tuitionCollected.toLocaleString('vi-VN') + ' đ', color: '#16a34a' }
          ].map((card, i) => (
            <div key={i} style={{ border: '1px solid #d4d4d4', padding: '16px', background: '#fafafa', borderRadius: '4px' }}>
              <div style={{ fontSize: '11px', fontWeight: 700, textTransform: 'uppercase', color: '#737373' }}>
                {card.label}
              </div>
              <div style={{ fontSize: '18px', fontWeight: 900, color: card.color, marginTop: '8px' }}>
                {card.value}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Tab Selectors */}
      <div className="tabs-container" style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginBottom: '24px', borderBottom: '1px solid #e5e5e5', paddingBottom: '10px' }}>
        {[
          { id: 'info', label: 'Thông tin chung' },
          { id: 'classes', label: 'Lớp học' },
          { id: 'students', label: 'Học sinh' },
          { id: 'teachers', label: 'Giáo viên' },
          { id: 'semesters', label: 'Học kỳ' },
          { id: 'grades', label: 'Bảng điểm' },
          { id: 'attendance', label: 'Chuyên cần' },
          { id: 'conduct', label: 'Hạnh kiểm' },
          { id: 'tuition', label: 'Học phí' },
          { id: 'announcements', label: 'Thông báo' },
          { id: 'messages', label: 'Tin nhắn' },
          { id: 'reports', label: 'Báo cáo & Thống kê' }
        ].map(tab => (
          <button 
            key={tab.id}
            onClick={() => setActiveTab(tab.id as any)}
            className={`tab-btn ${activeTab === tab.id ? 'active' : ''}`}
            style={{ 
              padding: '6px 12px', 
              fontSize: '12px', 
              fontWeight: 700, 
              border: activeTab === tab.id ? '1px solid #000000' : '1px solid #d4d4d4', 
              background: activeTab === tab.id ? '#000000' : '#ffffff', 
              color: activeTab === tab.id ? '#ffffff' : '#000000',
              cursor: 'pointer',
              borderRadius: '4px'
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Tab Contents */}
      <div style={{ minHeight: '200px' }}>
        {/* Tab 1: General Info */}
        {activeTab === 'info' && (
          <div style={{ maxWidth: '600px', lineHeight: '1.8' }}>
            <h3 style={{ borderBottom: '1px solid #000000', paddingBottom: '6px', marginBottom: '14px' }}>Thông tin chi tiết</h3>
            <table style={{ border: 'none' }}>
              <tbody>
                <tr>
                  <td style={{ width: '180px', fontWeight: 'bold' }}>Tên niên khóa:</td>
                  <td>Năm học {yearInfo?.name}</td>
                </tr>
                <tr>
                  <td style={{ fontWeight: 'bold' }}>Ngày bắt đầu:</td>
                  <td>{yearInfo?.startDate}</td>
                </tr>
                <tr>
                  <td style={{ fontWeight: 'bold' }}>Ngày kết thúc:</td>
                  <td>{yearInfo?.endDate}</td>
                </tr>
                <tr>
                  <td style={{ fontWeight: 'bold' }}>Trạng thái vận hành:</td>
                  <td>
                    <span style={{ fontSize: '11px', fontWeight: 'bold', textTransform: 'uppercase', background: '#f5f5f5', color: '#737373', padding: '2px 8px', borderRadius: '4px', border: '1px solid #cbd5e1' }}>
                      Đã kết thúc lưu trữ
                    </span>
                  </td>
                </tr>
                <tr>
                  <td style={{ fontWeight: 'bold' }}>Phân quyền:</td>
                  <td style={{ color: '#ef4444', fontWeight: 600 }}>Chế độ Chỉ đọc (Read-Only)</td>
                </tr>
              </tbody>
            </table>
          </div>
        )}

        {/* Tab 2: Classes */}
        {activeTab === 'classes' && (
          <table>
            <thead>
              <tr>
                <th>Khối lớp</th>
                <th>Tên lớp học</th>
                <th>Giáo viên chủ nhiệm</th>
                <th style={{ textAlign: 'center' }}>Sĩ số học sinh</th>
              </tr>
            </thead>
            <tbody>
              {classes.map(c => (
                <tr key={c.id}>
                  <td>Khối {c.gradeLevel}</td>
                  <td style={{ fontWeight: 'bold' }}>{c.name}</td>
                  <td>{c.homeroomTeacherName || 'Chưa phân công'}</td>
                  <td style={{ textAlign: 'center', fontWeight: 'bold' }}>{c.studentCount} học sinh</td>
                </tr>
              ))}
              {classes.length === 0 && (
                <tr>
                  <td colSpan={4} style={{ textAlign: 'center', color: '#737373', padding: '20px' }}>Không có dữ liệu lớp học trong năm học này.</td>
                </tr>
              )}
            </tbody>
          </table>
        )}

        {/* Tab 3: Students */}
        {activeTab === 'students' && (
          <div>
            <div style={{ display: 'flex', gap: '12px', marginBottom: '16px' }}>
              <select 
                value={selectedClassId} 
                onChange={e => setSelectedClassId(e.target.value)}
                style={{ padding: '8px', border: '1px solid #d4d4d4', borderRadius: '4px', width: '200px' }}
              >
                <option value="">-- Chọn lớp học --</option>
                {classes.map(c => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
              <input 
                type="text" 
                placeholder="Tìm học sinh theo tên/mã..." 
                value={searchStudent}
                onChange={e => setSearchStudent(e.target.value)}
                style={{ padding: '8px 12px', border: '1px solid #d4d4d4', borderRadius: '4px', flex: 1 }}
              />
            </div>
            <table>
              <thead>
                <tr>
                  <th>Mã học sinh</th>
                  <th>Họ và tên</th>
                  <th>Email liên hệ</th>
                  <th>Số điện thoại</th>
                </tr>
              </thead>
              <tbody>
                {filteredStudents.map(s => (
                  <tr key={s.id}>
                    <td style={{ fontFamily: 'monospace', fontWeight: 'bold' }}>{s.studentCode}</td>
                    <td style={{ fontWeight: 600 }}>{s.name}</td>
                    <td>{s.email}</td>
                    <td>{s.phone}</td>
                  </tr>
                ))}
                {filteredStudents.length === 0 && (
                  <tr>
                    <td colSpan={4} style={{ textAlign: 'center', color: '#737373', padding: '20px' }}>Không tìm thấy học sinh nào.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {/* Tab 4: Teachers */}
        {activeTab === 'teachers' && (
          <table>
            <thead>
              <tr>
                <th>Mã giáo viên</th>
                <th>Họ và tên</th>
                <th>Email nội bộ</th>
                <th>Quyền truy cập</th>
              </tr>
            </thead>
            <tbody>
              {teachers.map(t => (
                <tr key={t.id}>
                  <td style={{ fontFamily: 'monospace', fontWeight: 'bold' }}>{t.employeeCode}</td>
                  <td style={{ fontWeight: 600 }}>{t.name}</td>
                  <td>{t.email}</td>
                  <td><span style={{ fontSize: '11px', padding: '2px 6px', background: '#f1f5f9', color: '#64748b', borderRadius: '4px' }}>TEACHER</span></td>
                </tr>
              ))}
              {teachers.length === 0 && (
                <tr>
                  <td colSpan={4} style={{ textAlign: 'center', color: '#737373', padding: '20px' }}>Không có dữ liệu giáo viên phân công trong năm học này.</td>
                </tr>
              )}
            </tbody>
          </table>
        )}

        {/* Tab 5: Semesters */}
        {activeTab === 'semesters' && (
          <table>
            <thead>
              <tr>
                <th>Học kỳ</th>
                <th>Trạng thái lưu trữ</th>
                <th>Ngày bắt đầu</th>
                <th>Ngày kết thúc</th>
              </tr>
            </thead>
            <tbody>
              {semesters.map(s => (
                <tr key={s.id}>
                  <td style={{ fontWeight: 'bold' }}>{s.name}</td>
                  <td>
                    <span style={{ fontSize: '11px', fontWeight: 'bold', background: '#f5f5f5', color: '#737373', padding: '2px 6px', borderRadius: '4px' }}>
                      Đã hoàn thành (COMPLETED)
                    </span>
                  </td>
                  <td>{s.startDate}</td>
                  <td>{s.endDate}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {/* Tab 6: Grades */}
        {activeTab === 'grades' && (
          <div>
            <div style={{ display: 'flex', gap: '12px', marginBottom: '16px', flexWrap: 'wrap' }}>
              <select value={selectedSemesterId} onChange={e => setSelectedSemesterId(e.target.value)} style={{ padding: '8px', border: '1px solid #d4d4d4', borderRadius: '4px' }}>
                {semesters.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
              <select value={selectedClassId} onChange={e => setSelectedClassId(e.target.value)} style={{ padding: '8px', border: '1px solid #d4d4d4', borderRadius: '4px' }}>
                {classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
              <select value={selectedSubjectId} onChange={e => setSelectedSubjectId(e.target.value)} style={{ padding: '8px', border: '1px solid #d4d4d4', borderRadius: '4px' }}>
                {subjects.map(sub => <option key={sub.id} value={sub.id}>{sub.name}</option>)}
              </select>
            </div>
            <table>
              <thead>
                <tr>
                  <th>Mã học sinh</th>
                  <th>Họ tên</th>
                  <th style={{ textAlign: 'center' }}>Điểm trung bình (GPA)</th>
                  <th>Ghi chú</th>
                </tr>
              </thead>
              <tbody>
                {gradebookScores.map((score, i) => (
                  <tr key={i}>
                    <td style={{ fontFamily: 'monospace' }}>{score.studentCode}</td>
                    <td style={{ fontWeight: 600 }}>{score.studentName}</td>
                    <td style={{ textAlign: 'center', fontWeight: 'bold', color: score.average && score.average >= 5 ? '#16a34a' : '#ef4444' }}>
                      {score.average !== null ? score.average.toFixed(1) : '-'}
                    </td>
                    <td>{score.note || '-'}</td>
                  </tr>
                ))}
                {gradebookScores.length === 0 && (
                  <tr>
                    <td colSpan={4} style={{ textAlign: 'center', color: '#737373', padding: '20px' }}>Không có dữ liệu điểm cho cấu hình hiện tại.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {/* Tab 7: Chuyên cần */}
        {activeTab === 'attendance' && (
          <div>
            <div style={{ display: 'flex', gap: '12px', marginBottom: '16px', flexWrap: 'wrap' }}>
              <select value={selectedClassId} onChange={e => setSelectedClassId(e.target.value)} style={{ padding: '8px', border: '1px solid #d4d4d4', borderRadius: '4px' }}>
                {classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
              <input type="date" value={attendanceDate} onChange={e => setAttendanceDate(e.target.value)} style={{ padding: '8px', border: '1px solid #d4d4d4', borderRadius: '4px' }} />
              <select value={attendanceShift} onChange={e => setAttendanceShift(e.target.value)} style={{ padding: '8px', border: '1px solid #d4d4d4', borderRadius: '4px' }}>
                <option value="MORNING">Ca sáng</option>
                <option value="AFTERNOON">Ca chiều</option>
              </select>
            </div>
            {attendanceSessions.map((session, i) => (
              <div key={i} style={{ border: '1px solid #d4d4d4', borderRadius: '4px', padding: '16px', marginBottom: '16px' }}>
                <h4 style={{ margin: '0 0 12px 0' }}>Buổi điểm danh ngày {session.date} - Ca {session.shift === 'MORNING' ? 'Sáng' : 'Chiều'}</h4>
                <p style={{ fontSize: '13px', color: '#666', margin: '0 0 12px 0' }}>
                  Giáo viên: <strong>{session.teacherName}</strong> | Có mặt: <strong style={{ color: '#16a34a' }}>{session.present}</strong> | Muộn: <strong>{session.late}</strong> | Vắng: <strong style={{ color: '#ef4444' }}>{session.absent}</strong>
                </p>
                <table style={{ margin: 0 }}>
                  <thead>
                    <tr>
                      <th>Mã HS</th>
                      <th>Tên học sinh</th>
                      <th>Trạng thái</th>
                      <th>Lý do / Ghi chú</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(session.details || []).map((d: any, idx: number) => (
                      <tr key={idx}>
                        <td style={{ fontFamily: 'monospace' }}>{d.studentCode}</td>
                        <td>{d.studentName}</td>
                        <td>
                          <span style={{ 
                            fontSize: '11px', 
                            fontWeight: 'bold', 
                            padding: '2px 6px', 
                            borderRadius: '3px',
                            background: d.status === 'PRESENT' ? '#dcfce7' : d.status === 'LATE' ? '#fef9c3' : '#fee2e2',
                            color: d.status === 'PRESENT' ? '#16a34a' : d.status === 'LATE' ? '#ca8a04' : '#ef4444'
                          }}>
                            {d.status === 'PRESENT' ? 'Có mặt' : d.status === 'LATE' ? 'Muộn' : 'Vắng'}
                          </span>
                        </td>
                        <td>{d.note || '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ))}
            {attendanceSessions.length === 0 && (
              <div style={{ textAlign: 'center', color: '#737373', padding: '20px', border: '1px dashed #d4d4d4' }}>Không tìm thấy buổi điểm danh nào của ngày đã chọn.</div>
            )}
          </div>
        )}

        {/* Tab 8: Hạnh kiểm */}
        {activeTab === 'conduct' && (
          <div style={{ textAlign: 'center', padding: '40px 20px', border: '1px dashed #d4d4d4', color: '#737373' }}>
            Tính năng Hạnh kiểm được tích hợp chung trong bảng điểm học lực Học kỳ 1 & Học kỳ 2. Toàn bộ học sinh đạt hạnh kiểm Tốt/Khá được bảo lưu trong hồ sơ học bạ.
          </div>
        )}

        {/* Tab 9: Học phí */}
        {activeTab === 'tuition' && (
          <div>
            <div style={{ display: 'flex', gap: '12px', marginBottom: '16px' }}>
              <select value={selectedSemesterId} onChange={e => setSelectedSemesterId(e.target.value)} style={{ padding: '8px', border: '1px solid #d4d4d4', borderRadius: '4px' }}>
                {semesters.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
              <select value={selectedClassId} onChange={e => setSelectedClassId(e.target.value)} style={{ padding: '8px', border: '1px solid #d4d4d4', borderRadius: '4px' }}>
                {classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
            <table>
              <thead>
                <tr>
                  <th>Học sinh</th>
                  <th>Khoản thu</th>
                  <th>Số tiền</th>
                  <th>Hạn nộp</th>
                  <th>Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                {tuitionBills.map(bill => (
                  <tr key={bill.id}>
                    <td>
                      <div style={{ fontWeight: 600 }}>{bill.studentName}</div>
                      <div style={{ fontSize: '11px', color: '#737373', fontFamily: 'monospace' }}>{bill.studentCode}</div>
                    </td>
                    <td>{bill.name}</td>
                    <td style={{ fontWeight: 'bold' }}>{bill.amount.toLocaleString()} đ</td>
                    <td>{bill.dueDate}</td>
                    <td>
                      <span style={{ 
                        fontSize: '11px', 
                        fontWeight: 'bold', 
                        padding: '2px 8px', 
                        borderRadius: '4px',
                        background: bill.status === 'PAID' ? '#dcfce7' : '#fee2e2',
                        color: bill.status === 'PAID' ? '#16a34a' : '#ef4444',
                        border: `1px solid ${bill.status === 'PAID' ? '#16a34a' : '#ef4444'}`
                      }}>
                        {bill.status === 'PAID' ? 'Đã nộp' : 'Chưa nộp'}
                      </span>
                    </td>
                  </tr>
                ))}
                {tuitionBills.length === 0 && (
                  <tr>
                    <td colSpan={5} style={{ textAlign: 'center', color: '#737373', padding: '20px' }}>Không có hóa đơn học phí nào được tìm thấy.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {/* Tab 10: Announcements */}
        {activeTab === 'announcements' && (
          <table>
            <thead>
              <tr>
                <th>Tiêu đề thông báo</th>
                <th>Đối tượng nhận</th>
                <th>Người gửi</th>
                <th>Ngày tạo</th>
              </tr>
            </thead>
            <tbody>
              {announcements.map((ann, i) => (
                <tr key={i}>
                  <td style={{ fontWeight: 'bold' }}>{ann.title}</td>
                  <td><span style={{ fontSize: '11px', padding: '2px 6px', background: '#f1f5f9', color: '#475569', borderRadius: '4px' }}>{ann.targetRole}</span></td>
                  <td>{ann.teacherName || 'Hệ thống'}</td>
                  <td>{new Date(ann.createdAt).toLocaleDateString('vi-VN')}</td>
                </tr>
              ))}
              {announcements.length === 0 && (
                <tr>
                  <td colSpan={4} style={{ textAlign: 'center', color: '#737373', padding: '20px' }}>Không có thông báo nào được lưu trữ trong niên khóa này.</td>
                </tr>
              )}
            </tbody>
          </table>
        )}

        {/* Tab 11: Messages */}
        {activeTab === 'messages' && (
          <div style={{ maxWidth: '400px', border: '1px solid #d4d4d4', padding: '20px', borderRadius: '4px', background: '#fafafa' }}>
            <h4 style={{ margin: '0 0 16px 0', borderBottom: '1px solid #000', paddingBottom: '6px' }}>Thống kê đàm thoại nội bộ</h4>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
              <span>Tổng số cuộc trò chuyện:</span>
              <strong>{stats?.chatsCount || 0} cuộc</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span>Tổng tin nhắn đã gửi:</span>
              <strong>{stats?.messagesCount || 0} tin nhắn</strong>
            </div>
          </div>
        )}

        {/* Tab 12: Reports & Charts */}
        {activeTab === 'reports' && (
          <div>
            <h3 style={{ margin: '0 0 20px 0' }}>Báo cáo hiệu suất học tập & chuyên cần</h3>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '30px', flexWrap: 'wrap' }}>
              
              {/* Card Chart 1: Sĩ số lớp */}
              <div style={{ border: '1px solid #d4d4d4', padding: '20px', borderRadius: '4px' }}>
                <h4 style={{ margin: '0 0 16px 0' }}>Phân bố sĩ số học sinh theo Lớp học</h4>
                {classes.length > 0 ? (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    {classes.slice(0, 6).map((c, idx) => (
                      <div key={idx}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', marginBottom: '4px' }}>
                          <span>Lớp {c.name}</span>
                          <strong>{c.studentCount} học sinh</strong>
                        </div>
                        <div style={{ height: '8px', background: '#f1f5f9', borderRadius: '4px', overflow: 'hidden' }}>
                          <div style={{ height: '100%', background: '#000000', width: `${Math.min(100, (c.studentCount / 45) * 100)}%` }} />
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div style={{ color: '#737373', fontStyle: 'italic', fontSize: '13px' }}>Không có dữ liệu sĩ số lớp</div>
                )}
              </div>

              {/* Card Chart 2: Tỷ lệ hoàn thành Học phí */}
              <div style={{ border: '1px solid #d4d4d4', padding: '20px', borderRadius: '4px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                <h4 style={{ margin: '0 0 16px 0' }}>Tình trạng thu học phí niên khóa</h4>
                <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
                  {/* Mock Circular Progress */}
                  <svg width="100" height="100" viewBox="0 0 36 36" style={{ transform: 'rotate(-90deg)' }}>
                    <circle cx="18" cy="18" r="15.91" fill="none" stroke="#f1f5f9" strokeWidth="3" />
                    <circle cx="18" cy="18" r="15.91" fill="none" stroke="#16a34a" strokeWidth="3" strokeDasharray="92 8" strokeDashoffset="0" />
                  </svg>
                  <div>
                    <div style={{ fontSize: '20px', fontWeight: 900, color: '#16a34a' }}>92%</div>
                    <div style={{ fontSize: '12px', color: '#737373' }}>Hóa đơn đã hoàn thành nộp</div>
                    <div style={{ fontSize: '11px', color: '#a3a3a3', marginTop: '4px' }}>
                      (Đã thu: {stats?.tuitionCollected.toLocaleString()} đ)
                    </div>
                  </div>
                </div>
              </div>

            </div>
          </div>
        )}
      </div>
    </div>
  );
}
