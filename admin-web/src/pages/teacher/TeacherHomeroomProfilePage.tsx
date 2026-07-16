import { useEffect, useState } from 'react';
import { getHomeroomClass, getHomeroomRanking, getTeacherDashboard } from '../../api/teacher';
import { useTeacherAcademic } from '../../teacher/TeacherAcademicContext';

export default function TeacherHomeroomProfilePage() {
  const { selectedYearId, selectedSemesterId } = useTeacherAcademic();
  const [stats, setStats] = useState<any>(null);
  const [classDetail, setClassDetail] = useState<any>(null);
  const [ranking, setRanking] = useState<any[]>([]);
  const [query, setQuery] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    setStats(null); setClassDetail(null); setRanking([]); setQuery(''); setError('');
    if (!selectedYearId || !selectedSemesterId) return;
    getTeacherDashboard(selectedYearId, selectedSemesterId).then(async dashboard => {
      setStats(dashboard);
      const [detail, rank] = await Promise.all([getHomeroomClass(dashboard.classId), getHomeroomRanking(dashboard.classId, selectedSemesterId)]);
      setClassDetail(detail); setRanking(rank?.rankings || []);
    }).catch(cause => setError(cause instanceof Error ? cause.message : 'Không có hồ sơ lớp chủ nhiệm.'));
  }, [selectedYearId, selectedSemesterId]);

  const students = (classDetail?.students || []).filter((student: any) => `${student.name} ${student.studentCode}`.toLowerCase().includes(query.toLowerCase()));
  const rankByStudent = new Map(ranking.map(row => [row.studentId, row]));

  return <main className="teacher-page page-stack">
    <div className="page-heading"><div><p className="teacher-eyebrow">GVCN</p><h1>Hồ sơ lớp chủ nhiệm</h1><p>Thông tin học tập và xếp hạng; không tính hoặc hiển thị tỷ lệ phụ huynh đọc thông báo.</p></div></div>
    {error && <div className="notice error">{error}</div>}
    {stats && <div className="teacher-kpi-grid homeroom-kpis"><article><span>Lớp</span><strong>{stats.className}</strong><small>{stats.academicYearName}</small></article><article><span>Sĩ số</span><strong>{classDetail?.students?.length ?? '—'}</strong><small>Học sinh đang học</small></article><article><span>Chuyên cần TB</span><strong>{stats.attendanceRate != null ? `${stats.attendanceRate}%` : '—'}</strong><small>Trong học kỳ</small></article><article><span>GPA trung bình</span><strong>{stats.averageGpa != null ? Number(stats.averageGpa).toFixed(1) : '—'}</strong><small>Trong học kỳ</small></article></div>}
    {classDetail && <section className="panel"><div className="monitoring-actions"><div><h2>Danh sách học sinh</h2><p>{classDetail.schoolName}</p></div><input className="table-search" placeholder="Tìm tên hoặc mã học sinh" value={query} onChange={event => setQuery(event.target.value)}/></div><div className="table-responsive"><table><thead><tr><th>Học sinh</th><th>Mã HS</th><th>GPA</th><th>Xếp hạng</th><th>Học lực</th><th>Hạnh kiểm</th></tr></thead><tbody>{students.map((student: any) => { const rank = rankByStudent.get(student.id) as any; return <tr key={student.id}><td><strong>{student.name}</strong></td><td>{student.studentCode}</td><td>{rank?.gpa ?? '—'}</td><td>{rank?.rank ?? '—'}</td><td>{rank?.academicAbility ?? '—'}</td><td>{rank?.conduct ?? '—'}</td></tr>; })}</tbody></table></div></section>}
  </main>;
}
