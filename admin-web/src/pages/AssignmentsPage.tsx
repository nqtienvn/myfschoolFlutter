import { useState, useEffect } from 'react';
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

  const [semesterId, setSemesterId] = useState(selectedSemesterId || '');
  const [selectedClassId, setSelectedClassId] = useState('');
  const [gradeFilter, setGradeFilter] = useState('10');

  const [classAssignments, setClassAssignments] = useState<TeachingAssignmentItem[]>([]);
  const [loadingList, setLoadingList] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  useEffect(() => {
    getAcademicYears().then((d: any) => {
      setAcademicYears(d || []);
      if (!selectedYearId) {
        const active = d?.find((y: AcademicYearItem) => y.status === 'ACTIVE') || d?.[0];
        if (active) setAcademicYearId(String(active.id));
      }
    }).catch(() => {});
    getSubjects().then((d: any) => setSubjects(d || [])).catch(() => {});
    getTeachers({ status: 'ACTIVE', page: 0, size: 500 }).then((d: any) => setTeachers(d.content || [])).catch(() => {});
  }, []);

  useEffect(() => { if (selectedYearId) setAcademicYearId(selectedYearId); }, [selectedYearId]);
  useEffect(() => { if (selectedSemesterId) setSemesterId(selectedSemesterId); }, [selectedSemesterId]);

  useEffect(() => {
    if (!academicYearId) return;
    setSelectedClassId('');
    setClassAssignments([]);
    getClasses({ academicYearId, page: 0, size: 200 }).then((d: any) => setClasses(d.content || [])).catch(() => {});
    getSemesters(academicYearId).then((d: any) => {
      const list = d || [];
      setSemesters(list);
      if (!selectedSemesterId) {
        const current = list.find((s: SemesterItem) => s.isCurrent) || list[0];
        if (current) setSemesterId(String(current.id));
      }
    }).catch(() => {});
  }, [academicYearId, selectedSemesterId]);

  const filteredClasses = classes.filter(c => String(c.gradeLevel) === gradeFilter);

  useEffect(() => {
    if (selectedClassId && semesterId) {
      loadClassAssignments(Number(selectedClassId), semesterId);
    } else {
      setClassAssignments([]);
    }
  }, [selectedClassId, semesterId]);

  async function loadClassAssignments(classId: number, semId: string) {
    setLoadingList(true);
    try {
      const data = await getTeachingAssignments({ classId, semesterId: semId }) as any[];
      setClassAssignments(data || []);
    } catch (err: any) {
      setError(err.message || 'Không thể tải phân công');
    } finally {
      setLoadingList(false);
    }
  }

  function getTeachersForSubject(subjectId: number): TeacherItem[] {
    return teachers.filter(t => t.subjects.some(s => s.id === subjectId));
  }

  async function handleAssignSubject(subjectId: number, teacherId: number | null) {
    setError('');
    setSuccessMsg('');
    if (!selectedClassId || !semesterId) return;

    const existing = classAssignments.find(a => a.subjectId === subjectId && a.status === 'ACTIVE');
    const classItem = classes.find(c => c.id === Number(selectedClassId));
    const subjectItem = subjects.find(s => s.id === subjectId);

    try {
      if (teacherId === null) {
        if (existing) {
          await deleteTeachingAssignment(existing.id);
          setSuccessMsg(`Đã hủy phân công ${subjectItem?.name} lớp ${classItem?.name}`);
        }
      } else {
        const teacher = teachers.find(t => t.id === teacherId);
        if (existing) {
          await updateTeachingAssignment(existing.id, {
            classId: Number(selectedClassId),
            subjectId,
            teacherId,
            semesterId: Number(semesterId),
            effectiveFrom: today()
          });
          setSuccessMsg(`Đã cập nhật ${subjectItem?.name} → ${teacher?.name} (${classItem?.name})`);
        } else {
          await createTeachingAssignment({
            classId: Number(selectedClassId),
            subjectId,
            teacherId,
            semesterId: Number(semesterId),
            effectiveFrom: today()
          });
          setSuccessMsg(`Đã phân công ${teacher?.name} dạy ${subjectItem?.name} (${classItem?.name})`);
        }
      }
      await loadClassAssignments(Number(selectedClassId), semesterId);
    } catch (err: any) {
      setError(err.message || 'Lỗi khi lưu phân công');
    }
  }

  const selectedClass = classes.find(c => c.id === Number(selectedClassId));

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', borderBottom: '2px solid #000000', paddingBottom: '12px', flexWrap: 'wrap', gap: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', flexWrap: 'wrap' }}>
          <h2 style={{ margin: 0, border: 'none', padding: 0 }}>Phân công giảng dạy</h2>
          <select value={academicYearId} onChange={e => setAcademicYearId(e.target.value)} style={{ height: '32px', padding: '0 8px', border: '1px solid #000', fontSize: '12px', fontWeight: 'bold' }}>
            <option value="">Năm học</option>
            {academicYears.map(y => <option key={y.id} value={y.id}>{y.name} {y.status === 'ACTIVE' ? '(Đang mở)' : ''}</option>)}
          </select>
          <select value={semesterId} onChange={e => setSemesterId(e.target.value)} disabled={!academicYearId} style={{ height: '32px', padding: '0 8px', border: '1px solid #000', fontSize: '12px', fontWeight: 'bold' }}>
            <option value="">Học kỳ</option>
            {semesters.map(s => <option key={s.id} value={s.id}>{s.name} {s.isCurrent ? '(Hiện tại)' : ''}</option>)}
          </select>
          <select value={gradeFilter} onChange={e => { setGradeFilter(e.target.value); setSelectedClassId(''); }} style={{ height: '32px', padding: '0 8px', border: '1px solid #000', fontSize: '12px', fontWeight: 'bold' }}>
            {Array.from({ length: 12 }, (_, i) => i + 1).map(g => (
              <option key={g} value={g}>Khối {g}</option>
            ))}
          </select>
        </div>
      </div>

      {error && <div className="error" style={{ marginBottom: 16 }}>{error}</div>}
      {successMsg && <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: 16 }}>[SUCCESS] {successMsg}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: '260px 1fr', gap: '24px', alignItems: 'start' }}>
        {/* LEFT: Class list */}
        <div style={{ background: '#fff', border: '1px solid #000', padding: '16px' }}>
          <h3 style={{ margin: '0 0 12px 0', fontSize: '14px', fontWeight: 800, textTransform: 'uppercase', borderBottom: '1px solid #000', paddingBottom: '6px' }}>
            Lớp học khối {gradeFilter}
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', maxHeight: '500px', overflowY: 'auto' }}>
            {filteredClasses.map(c => {
              const isActive = String(c.id) === selectedClassId;
              return (
                <button
                  key={c.id}
                  onClick={() => setSelectedClassId(String(c.id))}
                  style={{
                    width: '100%', textAlign: 'left', padding: '10px 12px', fontSize: '13px', fontWeight: 600, cursor: 'pointer', border: '1px solid',
                    borderColor: isActive ? '#000' : '#e5e5e5',
                    background: isActive ? '#000' : '#fafafa',
                    color: isActive ? '#fff' : '#000',
                  }}
                >
                  {c.name}
                </button>
              );
            })}
            {filteredClasses.length === 0 && (
              <p style={{ fontSize: 12, color: '#999', fontStyle: 'italic' }}>Chưa có lớp thuộc khối {gradeFilter}</p>
            )}
          </div>
        </div>

        {/* RIGHT: Subject assignments for selected class */}
        <div style={{ background: '#fff', border: '1px solid #000', padding: '16px' }}>
          {!selectedClassId ? (
            <div style={{ padding: '60px 20px', textAlign: 'center', background: '#fafafa', border: '1px dashed #d4d4d4', color: '#737373' }}>
              <p style={{ margin: 0, fontSize: '13px', fontWeight: 'bold' }}>CHỌN LỚP HỌC Ở CỘT TRÁI</p>
              <p style={{ margin: '4px 0 0', fontSize: '12px' }}>Chọn lớp để phân công giáo viên bộ môn.</p>
            </div>
          ) : (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', borderBottom: '1px solid #000', paddingBottom: '6px' }}>
                <h3 style={{ margin: 0, fontSize: '14px', fontWeight: 800, textTransform: 'uppercase' }}>
                  Phân công giáo viên — {selectedClass?.name}
                </h3>
                <span style={{ fontSize: 12, color: '#666' }}>
                  Học kỳ: {semesters.find(s => String(s.id) === semesterId)?.name || '—'}
                </span>
              </div>

              {loadingList ? (
                <p style={{ fontSize: 13, color: '#666' }}>Đang tải...</p>
              ) : (
                <table style={{ margin: 0 }}>
                  <thead>
                    <tr>
                      <th style={{ width: 40 }}>#</th>
                      <th>Môn học</th>
                      <th>Mã môn</th>
                      <th>Giáo viên phụ trách</th>
                      <th style={{ width: 60 }}></th>
                    </tr>
                  </thead>
                  <tbody>
                    {subjects.map((subj, idx) => {
                      const assignment = classAssignments.find(a => a.subjectId === subj.id && a.status === 'ACTIVE');
                      const teachersForSubject = getTeachersForSubject(subj.id);
                      const assignedTeacher = assignment ? teachers.find(t => t.id === assignment.teacherId) : null;

                      return (
                        <tr key={subj.id}>
                          <td style={{ color: '#999' }}>{idx + 1}</td>
                          <td style={{ fontWeight: 600 }}>{subj.name}</td>
                          <td style={{ fontFamily: 'ui-monospace, monospace', fontSize: 12 }}>{subj.code}</td>
                          <td>
                            <select
                              value={assignment?.teacherId || ''}
                              onChange={e => handleAssignSubject(subj.id, e.target.value ? Number(e.target.value) : null)}
                              style={{ width: '100%', height: 32, border: '1px solid #d4d4d4', fontSize: 12, background: assignment ? '#fff' : '#fafafa' }}
                            >
                              <option value="">— Chọn giáo viên —</option>
                              {teachersForSubject.map(t => (
                                <option key={t.id} value={t.id}>{t.name} ({t.employeeCode})</option>
                              ))}
                            </select>
                          </td>
                          <td>
                            {assignment && (
                              <button
                                onClick={() => handleAssignSubject(subj.id, null)}
                                className="danger"
                                style={{ height: 28, width: 28, padding: 0, fontSize: 11, fontWeight: 'bold', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                                title="Hủy phân công"
                              >
                                X
                              </button>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}

              {!loadingList && classAssignments.length === 0 && (
                <p style={{ fontSize: 12, color: '#999', marginTop: 12, fontStyle: 'italic' }}>
                  Chưa có phân công nào cho lớp này trong học kỳ hiện tại.
                </p>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
