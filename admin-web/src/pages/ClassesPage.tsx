import { useEffect, useState } from 'react';
import { deleteClass, generateClasses, getClasses } from '../api/class';
import { createHomeroomAssignment, getHomeroomAssignment, updateHomeroomAssignment } from '../api/homeroomAssignment';
import type { HomeroomAssignmentItem } from '../api/homeroomAssignment';
import { getTeachers } from '../api/user';
import type { TeacherItem } from '../api/user';

interface ClassItem { id: number; name: string; gradeLevel: number; academicYearId: number; academicYearName: string; }
const today = () => new Date().toISOString().slice(0, 10);

export default function ClassesPage({ selectedYearId, editable = true }: { selectedYearId?: string; selectedSemesterId?: string; editable?: boolean }) {
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [teachers, setTeachers] = useState<TeacherItem[]>([]);
  const [homerooms, setHomerooms] = useState<Record<number, HomeroomAssignmentItem | null>>({});
  const [gradeLevel, setGradeLevel] = useState(10);
  const [namingPrefix, setNamingPrefix] = useState('A');
  const [count, setCount] = useState<number | ''>(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [pageIndex, setPageIndex] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  async function load() {
    if (!selectedYearId) return;
    setLoading(true);
    try {
      const [classPage, teacherPage] = await Promise.all([
        getClasses({ academicYearId: selectedYearId, page: pageIndex, size: 10 }),
        teachers.length === 0 ? getTeachers({ status: 'ACTIVE', page: 0, size: 500 }) : Promise.resolve(null),
      ]);
      const list = (classPage.content || []) as ClassItem[];
      setClasses(list);
      setTotalPages(classPage.totalPages || 0);
      setTotalElements(classPage.totalElements || 0);
      if (teacherPage) {
        setTeachers(teacherPage.content || []);
      }
      const values = await Promise.all(list.map(async cls => [cls.id, await getHomeroomAssignment(cls.id, selectedYearId)] as const));
      setHomerooms(Object.fromEntries(values));
    } catch (cause: any) { setError(cause.message || 'Không thể tải lớp học.'); }
    finally { setLoading(false); }
  }

  useEffect(() => {
    setPageIndex(0);
  }, [selectedYearId]);

  useEffect(() => {
    load();
  }, [selectedYearId, pageIndex]);

  async function bulkGenerate() {
    if (!selectedYearId) return setError('Chọn năm học cần cấu hình.');
    if (!namingPrefix.trim() || count === '' || count < 1 || count > 50) return setError('Ký hiệu lớp và số lượng từ 1 đến 50 là bắt buộc.');
    setLoading(true); setError(''); setMessage('');
    try {
      await generateClasses({ academicYearId: Number(selectedYearId), gradeLevel, namingPrefix: namingPrefix.trim().toUpperCase(), count: Number(count) });
      setMessage(`Đã sinh ${count} lớp theo mẫu ${gradeLevel}${namingPrefix.toUpperCase()}1…`);
      if (pageIndex === 0) {
        await load();
      } else {
        setPageIndex(0);
      }
    } catch (cause: any) { setError(cause.message || 'Không thể sinh lớp.'); }
    finally { setLoading(false); }
  }

  async function assignHomeroom(cls: ClassItem, teacherId: number) {
    if (!selectedYearId) return;
    setError('');
    try {
      const existing = homerooms[cls.id];
      const data = { classId: cls.id, teacherId, academicYearId: Number(selectedYearId), effectiveFrom: today() };
      const saved = existing ? await updateHomeroomAssignment(existing.id, data) : await createHomeroomAssignment(data);
      setHomerooms(current => ({ ...current, [cls.id]: saved }));
      setMessage(`Đã gán GVCN cho lớp ${cls.name}.`);
    } catch (cause: any) { setError(cause.message || 'Không thể gán giáo viên chủ nhiệm.'); }
  }

  return (
    <div className="page-stack">
      <section className="page-heading"><div><span className="eyebrow">Bước 4</span><h1>Sinh lớp & gán GVCN</h1></div></section>
      {!selectedYearId && <div className="notice warning">Chọn năm học DRAFT ở thanh trên.</div>}
      {!editable && selectedYearId && <div className="notice warning">Danh sách lớp và GVCN đã bị khóa vì năm học không còn ở trạng thái DRAFT.</div>}
      {error && <div className="notice error">{error}</div>}
      {message && <div className="notice success">{message}</div>}
      <section className="class-generation-form">
        <div className="form-group"><label>Khối lớp</label><select value={gradeLevel} onChange={e => setGradeLevel(Number(e.target.value))}>{Array.from({ length: 12 }, (_, i) => i + 1).map(value => <option key={value} value={value}>Khối {value}</option>)}</select><small className="input-desc">Khối áp dụng.</small></div>
        <div className="form-group"><label>Ký hiệu</label><input value={namingPrefix} onChange={e => setNamingPrefix(e.target.value)} maxLength={6} placeholder="A" /><small className="input-desc">Ví dụ: A, SE.</small></div>
        <div className="form-group"><label>Số lượng lớp</label><input type="number" min={1} max={50} value={count} onChange={e => setCount(e.target.value === '' ? '' : Number(e.target.value))} /><small className="input-desc">Tối đa 50 lớp.</small></div>
        <div className="class-generate-action"><span aria-hidden="true">Thao tác</span><button type="button" onClick={bulkGenerate} disabled={!selectedYearId || !editable || loading}>{loading ? 'Đang sinh lớp…' : 'Sinh lớp hàng loạt'}</button><small aria-hidden="true">Các lớp được tạo theo thứ tự tăng dần.</small></div>
      </section>
      <div className="class-list-heading"><div><h2>Danh sách lớp & giáo viên chủ nhiệm</h2><p>Chọn giáo viên chủ nhiệm ngay trên từng dòng lớp.</p></div><span>{totalElements} lớp</span></div>
      <div className="table-responsive"><table className="classes-table"><thead><tr><th>Lớp</th><th>Khối</th><th>Giáo viên chủ nhiệm</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>
        {classes.map(cls => <tr key={cls.id}><td><strong>{cls.name}</strong></td><td>{cls.gradeLevel}</td><td><select className="homeroom-select" disabled={!editable} value={homerooms[cls.id]?.teacherId || ''} onChange={e => assignHomeroom(cls, Number(e.target.value))}><option value="">Chọn GVCN</option>{teachers.map(teacher => <option key={teacher.id} value={teacher.id}>{teacher.name} · {teacher.employeeCode}</option>)}</select></td><td><span className={`badge-status ${homerooms[cls.id] ? 'active' : 'preparing'}`}>{homerooms[cls.id] ? 'ĐÃ CÓ GVCN' : 'THIẾU GVCN'}</span></td><td><button type="button" disabled={!editable} className="danger class-delete-button" onClick={async () => { if (confirm(`Xóa lớp ${cls.name}?`)) { try { await deleteClass(cls.id); if (classes.length === 1 && pageIndex > 0) { setPageIndex(prev => prev - 1); } else { await load(); } } catch (cause: any) { setError(cause.message); } } }}>Xóa</button></td></tr>)}
        {!loading && classes.length === 0 && <tr><td colSpan={5}>Chưa có lớp trong năm học này.</td></tr>}
      </tbody></table></div>
      {totalPages > 1 && (
        <div className="pagination-bar" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 8, marginTop: 16 }}>
          <button
            type="button"
            className="secondary-button"
            disabled={pageIndex === 0 || loading}
            onClick={() => setPageIndex(prev => prev - 1)}
          >
            Trước
          </button>
          <span style={{ fontSize: 13, color: 'var(--muted)', fontWeight: 600 }}>
            Trang {pageIndex + 1} / {totalPages} (Tổng số {totalElements} lớp)
          </span>
          <button
            type="button"
            className="secondary-button"
            disabled={pageIndex >= totalPages - 1 || loading}
            onClick={() => setPageIndex(prev => prev + 1)}
          >
            Sau
          </button>
        </div>
      )}
    </div>
  );
}
