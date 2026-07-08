import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

interface ClassItem { id: number; name: string; academicYear: string; }
interface SubjectItem { id: number; name: string; code: string; }
interface SemesterItem { id: number; name: string; academicYear: string; isCurrent: boolean; }
interface TeacherItem { id: number; name: string; phone: string; teacherProfile?: { id: number; employeeCode: string; } }

export default function AssignmentsPage() {
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [subjects, setSubjects] = useState<SubjectItem[]>([]);
  const [semesters, setSemesters] = useState<SemesterItem[]>([]);
  const [teachers, setTeachers] = useState<TeacherItem[]>([]);

  // State quản lý việc hiển thị danh sách giáo viên đã phân công của lớp đang chọn
  const [assignedSubjects, setAssignedSubjects] = useState<any[]>([]);
  const [loadingList, setLoadingList] = useState(false);

  // Form State
  const [classId, setClassId] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [teacherId, setTeacherId] = useState(''); // Lưu teacherProfile.id
  const [semesterId, setSemesterId] = useState('');
  const [isHomeroom, setIsHomeroom] = useState(false);

  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Tải dữ liệu cấu trúc ban đầu
  useEffect(() => {
    // 1. Tải danh sách lớp
    apiFetch('/classes?page=0&size=100')
      .then((d: any) => setClasses(d.content || []))
      .catch(() => {});

    // 2. Tải danh sách môn
    apiFetch('/subjects')
      .then((d: any) => setSubjects(d || []))
      .catch(() => {});

    // 3. Tải danh sách học kỳ
    apiFetch('/semesters')
      .then((d: any) => {
        const list = d || [];
        setSemesters(list);
        // Tự động tìm và set học kỳ hiện tại làm mặc định
        const current = list.find((s: SemesterItem) => s.isCurrent);
        if (current) setSemesterId(current.id.toString());
      })
      .catch(() => {});

    // 4. Tải danh sách giáo viên
    apiFetch('/admin/users?role=TEACHER')
      .then((d: any) => setTeachers(d || []))
      .catch(() => {});
  }, []);

  // Tải lại danh sách phân công môn học khi chọn lớp học hoặc học kỳ thay đổi
  useEffect(() => {
    if (!classId) {
      setAssignedSubjects([]);
      return;
    }
    loadAssignedSubjects();
  }, [classId]);

  async function loadAssignedSubjects() {
    setLoadingList(true);
    try {
      const data = await apiFetch(`/classes/${classId}`) as any;
      setAssignedSubjects(data.subjects || []);
    } catch (err: any) {
      console.error('Không tải được danh sách phân công: ', err.message);
    } finally {
      setLoadingList(false);
    }
  }

  async function assign() {
    setError('');
    setSuccessMsg('');

    if (!classId) {
      setError('Vui lòng chọn Lớp học.');
      return;
    }
    if (!subjectId) {
      setError('Vui lòng chọn Môn học.');
      return;
    }
    if (!teacherId) {
      setError('Vui lòng chọn Giáo viên giảng dạy.');
      return;
    }
    if (!semesterId) {
      setError('Vui lòng chọn Học kỳ.');
      return;
    }

    const selectedClass = classes.find(c => c.id === +classId);
    const selectedSemester = semesters.find(s => s.id === +semesterId);

    if (!selectedClass || !selectedSemester) {
      setError('Dữ liệu lớp học hoặc học kỳ không đồng bộ.');
      return;
    }

    try {
      await apiFetch(`/classes/${classId}/subjects`, {
        method: 'POST',
        body: JSON.stringify({
          classId: +classId,
          subjectId: +subjectId,
          teacherId: +teacherId, // Gửi ID của bảng Teacher
          isHomeroom,
          academicYear: selectedClass.academicYear,
          semesterId: selectedSemester.id,
        }),
      });
      setSuccessMsg('Đã hoàn tất phân công giáo viên thành công!');
      // Reset form
      setSubjectId('');
      setTeacherId('');
      setIsHomeroom(false);
      // Tải lại danh sách phân công
      loadAssignedSubjects();
    } catch (err: any) {
      setError(err.message || 'Lỗi phân công giảng dạy');
    }
  }

  async function handleRemove(classSubjectId: number) {
    if (!confirm('Bạn có chắc chắn muốn gỡ phân công môn học này của giáo viên?')) return;
    setError('');
    setSuccessMsg('');
    try {
      await apiFetch(`/classes/subjects/${classSubjectId}`, {
        method: 'DELETE',
      });
      setSuccessMsg('Đã gỡ phân công thành công.');
      loadAssignedSubjects();
    } catch (err: any) {
      setError(err.message || 'Lỗi khi gỡ phân công');
    }
  }

  return (
    <div>
      <h2>Phân công giáo viên</h2>

      {error && <div className="error">{error}</div>}
      {successMsg && (
        <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: '16px' }}>
          [SUCCESS] {successMsg}
        </div>
      )}

      {/* Form Phân công (Grid layout) */}
      <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))' }}>
        <div className="form-group">
          <label>Lớp học</label>
          <select value={classId} onChange={e => setClassId(e.target.value)}>
            <option value="">Chọn lớp học</option>
            {classes.map(c => <option key={c.id} value={c.id}>{c.name} ({c.academicYear})</option>)}
          </select>
          <span className="input-desc">Chọn lớp để phân công và xem danh sách</span>
        </div>

        <div className="form-group">
          <label>Học kỳ</label>
          <select value={semesterId} onChange={e => setSemesterId(e.target.value)}>
            <option value="">Chọn học kỳ</option>
            {semesters.map(s => <option key={s.id} value={s.id}>{s.name} - {s.academicYear} {s.isCurrent ? '(Hiện tại)' : ''}</option>)}
          </select>
          <span className="input-desc">Áp dụng cho học kỳ nào</span>
        </div>

        <div className="form-group">
          <label>Môn học</label>
          <select value={subjectId} onChange={e => setSubjectId(e.target.value)}>
            <option value="">Chọn môn học</option>
            {subjects.map(s => <option key={s.id} value={s.id}>{s.name} ({s.code})</option>)}
          </select>
          <span className="input-desc">Môn học giáo viên sẽ dạy</span>
        </div>

        <div className="form-group">
          <label>Giáo viên giảng dạy</label>
          <select value={teacherId} onChange={e => setTeacherId(e.target.value)}>
            <option value="">Chọn giáo viên</option>
            {teachers.map(t => {
              const profileId = t.teacherProfile?.id;
              if (!profileId) return null; // Bỏ qua nếu user không có teacherProfile
              return (
                <option key={t.id} value={profileId.toString()}>
                  {t.name} ({t.teacherProfile?.employeeCode || 'GV'})
                </option>
              );
            })}
          </select>
          <span className="input-desc">Chọn giáo viên trong danh sách trường</span>
        </div>

        <div className="form-group" style={{ justifyContent: 'center' }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '12px' }}>
            <input 
              type="checkbox" 
              checked={isHomeroom} 
              onChange={e => setIsHomeroom(e.target.checked)} 
              style={{ width: '16px', height: '16px', cursor: 'pointer' }}
            />
            Là Giáo viên chủ nhiệm (GVCN)
          </label>
          <span className="input-desc">Đặt quyền quản lý lớp học</span>
        </div>

        <div className="form-group">
          <label style={{ visibility: 'hidden' }}>Thao tác</label>
          <button onClick={assign} style={{ height: '38px', width: '100%' }}>Phân công dạy</button>
          <span className="input-desc" style={{ visibility: 'hidden' }}>&nbsp;</span>
        </div>
      </div>

      {/* Danh sách giáo viên đang dạy của lớp đã chọn */}
      {classId && (
        <div style={{ marginTop: '32px' }}>
          <h3 style={{ fontSize: '15px', fontWeight: 700, textTransform: 'uppercase', marginBottom: '16px', borderBottom: '1px dashed #000000', paddingBottom: '8px' }}>
            Danh sách Phân công của Lớp: {classes.find(c => c.id === +classId)?.name}
          </h3>

          {loadingList ? (
            <p style={{ fontSize: '13px', fontFamily: 'ui-monospace, monospace' }}>Đang tải danh sách phân công...</p>
          ) : assignedSubjects.length === 0 ? (
            <p style={{ fontSize: '13px', color: '#a3a3a3', fontStyle: 'italic' }}>Chưa có giáo viên nào được phân công giảng dạy cho lớp này.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Môn học</th>
                  <th>Mã môn</th>
                  <th>Giáo viên dạy</th>
                  <th>Vai trò giảng dạy</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {assignedSubjects.map((item: any) => (
                  <tr key={item.id}>
                    <td>{item.id}</td>
                    <td style={{ fontWeight: 600 }}>{item.subject?.name}</td>
                    <td>{item.subject?.code}</td>
                    <td>{item.teacher?.name} ({item.teacher?.employeeCode})</td>
                    <td>
                      {item.isHomeroom ? (
                        <span style={{ background: '#000000', color: '#ffffff', padding: '2px 8px', fontSize: '11px', fontWeight: 700 }}>
                          GV CHỦ NHIỆM (GVCN)
                        </span>
                      ) : (
                        <span style={{ color: '#737373' }}>Giáo viên Bộ môn</span>
                      )}
                    </td>
                    <td>
                      <button className="danger" onClick={() => handleRemove(item.id)}>
                        Gỡ dạy
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
