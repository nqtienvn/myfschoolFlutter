import { useEffect, useState } from 'react';
import { getHomeroomClass, getHomeroomRanking, getTeacherDashboard } from '../../api/teacher';
import { useTeacherAcademic } from '../../teacher/TeacherAcademicContext';

function SchoolIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="m3 10 9-5 9 5-9 5-9-5Z"/><path d="M6 12.5V18h12v-5.5M9 20v-4h6v4"/></svg>;
}

function UsersIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><circle cx="9" cy="8" r="3"/><path d="M3 20v-1.5A4.5 4.5 0 0 1 7.5 14h3A4.5 4.5 0 0 1 15 18.5V20M16 5.5a3 3 0 0 1 0 5.5M17.5 14a4 4 0 0 1 3.5 4v2"/></svg>;
}

function AttendanceIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>;
}

function ChartIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/></svg>;
}

export default function TeacherHomeroomProfilePage() {
  const { selectedYearId, selectedSemesterId } = useTeacherAcademic();
  const [stats, setStats] = useState<any>(null);
  const [classDetail, setClassDetail] = useState<any>(null);
  const [ranking, setRanking] = useState<any[]>([]);
  const [query, setQuery] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    setStats(null);
    setClassDetail(null);
    setRanking([]);
    setQuery('');
    setError('');
    if (!selectedYearId || !selectedSemesterId) return;

    getTeacherDashboard(selectedYearId, selectedSemesterId).then(async dashboard => {
      setStats(dashboard);
      const [detail, rank] = await Promise.all([
        getHomeroomClass(dashboard.classId),
        getHomeroomRanking(dashboard.classId, selectedSemesterId),
      ]);
      setClassDetail(detail);
      setRanking(rank?.rankings || []);
    }).catch(cause => {
      setError(cause instanceof Error ? cause.message : 'Không có hồ sơ lớp chủ nhiệm.');
    });
  }, [selectedYearId, selectedSemesterId]);

  const students = (classDetail?.students || []).filter((student: any) => (
    `${student.name} ${student.studentCode}`.toLowerCase().includes(query.toLowerCase())
  ));
  const rankByStudent = new Map(ranking.map(row => [row.studentId, row]));
  const metrics = stats ? [
    { label: 'Lớp chủ nhiệm', value: stats.className, detail: stats.academicYearName, color: 'blue', Icon: SchoolIcon },
    { label: 'Sĩ số', value: classDetail?.students?.length ?? '—', detail: 'Học sinh đang học', color: 'purple', Icon: UsersIcon },
    { label: 'Chuyên cần TB', value: stats.attendanceRate != null ? `${stats.attendanceRate}%` : '—', detail: 'Trong học kỳ', color: 'green', Icon: AttendanceIcon },
    { label: 'GPA trung bình', value: stats.averageGpa != null ? Number(stats.averageGpa).toFixed(1) : '—', detail: 'Trong học kỳ', color: 'orange', Icon: ChartIcon },
  ] : [];

  return (
    <main className="teacher-page page-stack">
      <div className="page-heading">
        <div>
          <h1>Hồ sơ lớp chủ nhiệm</h1>
          <p>Thông tin học tập và xếp hạng của lớp trong đúng năm học, học kỳ đang chọn.</p>
        </div>
      </div>

      {error && <div className="notice error">{error}</div>}

      {stats && (
        <section aria-label="Tổng quan lớp chủ nhiệm">
          <div className="dashboard-grid">
            {metrics.map(({ label, value, detail, color, Icon }) => (
              <article className="stat-card" key={label}>
                <span className={`stat-icon ${color}`} aria-hidden="true"><Icon /></span>
                <span>
                  <strong className="stat-value">{value}</strong>
                  <span className="stat-label">{label}</span>
                  <span className="teacher-stat-detail">{detail}</span>
                </span>
              </article>
            ))}
          </div>
        </section>
      )}

      {classDetail && (
        <section className="panel">
          <div className="class-list-heading teacher-student-list-heading">
            <div>
              <h2>Danh sách học sinh</h2>
              <p>{classDetail.schoolName} · {students.length} học sinh</p>
            </div>
            <input
              className="table-search"
              placeholder="Tìm tên hoặc mã học sinh"
              value={query}
              onChange={event => setQuery(event.target.value)}
            />
          </div>
          <div className="table-responsive">
            <table className="teacher-student-table">
              <thead><tr><th>Học sinh</th><th>Mã HS</th><th>GPA</th><th>Xếp hạng</th><th>Học lực</th><th>Hạnh kiểm</th></tr></thead>
              <tbody>
                {students.map((student: any) => {
                  const rank = rankByStudent.get(student.id) as any;
                  return (
                    <tr key={student.id}>
                      <td><strong>{student.name}</strong></td>
                      <td>{student.studentCode}</td>
                      <td>{rank?.gpa ?? '—'}</td>
                      <td>{rank?.rank ?? '—'}</td>
                      <td>{rank?.academicAbility ?? '—'}</td>
                      <td>{rank?.conduct ?? '—'}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          {!students.length && <div className="empty-state">Không tìm thấy học sinh phù hợp.</div>}
        </section>
      )}
    </main>
  );
}
