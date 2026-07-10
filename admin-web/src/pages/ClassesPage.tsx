import { useEffect, useState } from 'react';
import { createClass, deleteClass, generateClasses, getClasses } from '../api/class';
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
  const [count, setCount] = useState(1);
  const [manualName, setManualName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  async function load() {
    if (!selectedYearId) return;
    setLoading(true);
    try {
      const [classPage, teacherPage] = await Promise.all([
        getClasses({ academicYearId: selectedYearId, page: 0, size: 500 }),
        getTeachers({ status: 'ACTIVE', page: 0, size: 500 }),
      ]);
      const list = (classPage.content || []) as ClassItem[];
      setClasses(list);
      setTeachers(teacherPage.content || []);
      const values = await Promise.all(list.map(async cls => [cls.id, await getHomeroomAssignment(cls.id, selectedYearId)] as const));
      setHomerooms(Object.fromEntries(values));
    } catch (cause: any) { setError(cause.message || 'Không thể tải lớp học.'); }
    finally { setLoading(false); }
  }

  useEffect(() => { load(); }, [selectedYearId]);

  async function bulkGenerate() {
    if (!selectedYearId) return setError('Chọn năm học cần cấu hình.');
    if (!namingPrefix.trim() || count < 1 || count > 50) return setError('Ký hiệu lớp và số lượng từ 1 đến 50 là bắt buộc.');
    setLoading(true); setError(''); setMessage('');
    try {
      await generateClasses({ academicYearId: Number(selectedYearId), gradeLevel, namingPrefix: namingPrefix.trim().toUpperCase(), count });
      setMessage(`Đã sinh ${count} lớp theo mẫu ${gradeLevel}${namingPrefix.toUpperCase()}1…`);
      await load();
    } catch (cause: any) { setError(cause.message || 'Không thể sinh lớp.'); }
    finally { setLoading(false); }
  }

  async function addManual() {
    if (!selectedYearId || !manualName.trim()) return setError('Nhập tên lớp và chọn năm học.');
    try {
      await createClass({ name: manualName.trim(), gradeLevel, academicYearId: Number(selectedYearId), schoolName: 'FPT Schools' });
      setManualName(''); setMessage('Đã tạo lớp.'); await load();
    } catch (cause: any) { setError(cause.message || 'Không thể tạo lớp.'); }
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
      <section className="page-heading"><div><span className="eyebrow">Bước 4</span><h1>Sinh lớp & gán GVCN</h1><p>Tên lớp không được trùng trong cùng năm học; mỗi lớp chỉ có một GVCN.</p></div></section>
      {!selectedYearId && <div className="notice warning">Chọn năm học DRAFT ở thanh trên.</div>}
      {!editable && selectedYearId && <div className="notice warning">Danh sách lớp và GVCN đã bị khóa vì năm học không còn ở trạng thái DRAFT.</div>}
      {error && <div className="notice error">{error}</div>}
      {message && <div className="notice success">{message}</div>}
      <section className="form-grid">
        <div className="form-group"><label>Khối lớp</label><select value={gradeLevel} onChange={e=>setGradeLevel(Number(e.target.value))}>{Array.from({length:12},(_,i)=>i+1).map(value=><option key={value} value={value}>Khối {value}</option>)}</select></div>
        <div className="form-group"><label>Ký hiệu</label><input value={namingPrefix} onChange={e=>setNamingPrefix(e.target.value)} maxLength={3} placeholder="A"/><small className="input-desc">Ví dụ khối 10, ký hiệu A → 10A1</small></div>
        <div className="form-group"><label>Số lượng lớp</label><input type="number" min={1} max={50} value={count} onChange={e=>setCount(Number(e.target.value))}/></div>
        <div className="form-group" style={{alignSelf:'end'}}><button onClick={bulkGenerate} disabled={!selectedYearId||!editable||loading}>Sinh lớp hàng loạt</button></div>
      </section>
      <section className="form-inline"><div className="form-group"><label>Tạo riêng một lớp</label><input value={manualName} onChange={e=>setManualName(e.target.value)} placeholder="Ví dụ: 10A1"/></div><button onClick={addManual} disabled={!selectedYearId||!editable}>Tạo lớp</button></section>
      <div className="table-responsive"><table><thead><tr><th>Lớp</th><th>Khối</th><th>Giáo viên chủ nhiệm</th><th>Trạng thái</th><th></th></tr></thead><tbody>
        {classes.map(cls=><tr key={cls.id}><td><strong>{cls.name}</strong></td><td>{cls.gradeLevel}</td><td><select disabled={!editable} value={homerooms[cls.id]?.teacherId || ''} onChange={e=>assignHomeroom(cls,Number(e.target.value))}><option value="">Chọn GVCN</option>{teachers.map(teacher=><option key={teacher.id} value={teacher.id}>{teacher.name} · {teacher.employeeCode}</option>)}</select></td><td><span className={`badge-status ${homerooms[cls.id]?'active':'preparing'}`}>{homerooms[cls.id]?'ĐÃ CÓ GVCN':'THIẾU GVCN'}</span></td><td><button disabled={!editable} className="danger" onClick={async()=>{if(confirm(`Xóa lớp ${cls.name}?`)){try{await deleteClass(cls.id);await load();}catch(cause:any){setError(cause.message);}}}}>Xóa</button></td></tr>)}
        {!loading&&classes.length===0&&<tr><td colSpan={5}>Chưa có lớp trong năm học này.</td></tr>}
      </tbody></table></div>
    </div>
  );
}
