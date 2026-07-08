import { useEffect, useState } from 'react';
import { apiFetch } from '../api/client';

interface AcademicYearItem { id: number; name: string; status: string; }
interface ClassItem { id: number; name: string; academicYearId: number; academicYearName: string; }
interface SemesterItem { id: number; name: string; academicYearId: number; academicYearName: string; isCurrent: boolean; order: number; }
interface FeeCategory { id: number; name: string; description: string; }
interface FeeTemplate { id: number; feeCategoryId: number; feeCategoryName: string; classId: number; className: string; semesterId: number; semesterName: string; name: string; amount: number; dueDate: string; studentCount: number; }
interface GenerateResult { totalStudents: number; created: number; skipped: number; }

const monthEnd = () => {
  const date = new Date();
  return new Date(date.getFullYear(), date.getMonth() + 1, 0).toISOString().split('T')[0];
};

export default function FeeTemplatesPage() {
  const [academicYears, setAcademicYears] = useState<AcademicYearItem[]>([]);
  const [academicYearId, setAcademicYearId] = useState('');
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [semesters, setSemesters] = useState<SemesterItem[]>([]);
  const [categories, setCategories] = useState<FeeCategory[]>([]);
  const [items, setItems] = useState<FeeTemplate[]>([]);
  const [classId, setClassId] = useState('');
  const [semesterId, setSemesterId] = useState('');
  const [feeCategoryId, setFeeCategoryId] = useState('');
  const [name, setName] = useState('Học phí học kỳ');
  const [amount, setAmount] = useState(3000000);
  const [dueDate, setDueDate] = useState(monthEnd());
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  useEffect(() => {
    fetchAcademicYears();
    apiFetch('/fee-categories').then((d: FeeCategory[]) => {
      setCategories(d || []);
      if (d?.[0]) setFeeCategoryId(String(d[0].id));
    }).catch(() => {});
  }, []);

  useEffect(() => {
    if (!academicYearId) return;
    setClassId('');
    setSemesterId('');
    setItems([]);
    apiFetch(`/classes?academicYearId=${academicYearId}&page=0&size=100`).then((d: any) => setClasses(d.content || [])).catch(() => {});
    apiFetch(`/semesters?academicYearId=${academicYearId}`).then((d: SemesterItem[]) => {
      const list = d || [];
      setSemesters(list);
      const current = list.find(s => s.isCurrent) || list[0];
      if (current) setSemesterId(String(current.id));
    }).catch(() => {});
  }, [academicYearId]);

  useEffect(() => {
    if (!classId || !semesterId) {
      setItems([]);
      return;
    }
    fetchItems();
  }, [classId, semesterId]);

  async function fetchAcademicYears() {
    const data = await apiFetch('/academic-years') as AcademicYearItem[];
    setAcademicYears(data);
    const active = data.find(y => y.status === 'ACTIVE') || data[0];
    if (active) setAcademicYearId(String(active.id));
  }

  async function fetchItems() {
    try {
      setItems(await apiFetch(`/fee-templates?classId=${classId}&semesterId=${semesterId}`));
    } catch (err: any) {
      setError(err.message || 'Không thể tải mẫu phí');
    }
  }

  async function createItem() {
    setError('');
    setSuccessMsg('');
    if (!classId || !semesterId || !feeCategoryId) return setError('Vui lòng chọn đủ danh mục, lớp và học kỳ.');
    if (!name.trim()) return setError('Tên mẫu phí không được để trống.');
    if (isNaN(amount) || amount <= 0) return setError('Số tiền phải lớn hơn 0.');
    if (!dueDate) return setError('Vui lòng chọn hạn thanh toán.');
    try {
      await apiFetch('/fee-templates', { method: 'POST', body: JSON.stringify({ feeCategoryId: +feeCategoryId, classId: +classId, semesterId: +semesterId, name: name.trim(), amount, dueDate }) });
      setSuccessMsg('Đã tạo mẫu phí.');
      fetchItems();
    } catch (err: any) {
      setError(err.message || 'Lỗi tạo mẫu phí');
    }
  }

  async function generateBills(id: number) {
    if (!confirm('Sinh hóa đơn cho toàn bộ học sinh active trong lớp?')) return;
    setError('');
    setSuccessMsg('');
    try {
      const result = await apiFetch(`/fee-templates/${id}/generate`, { method: 'POST' }) as GenerateResult;
      setSuccessMsg(`Đã sinh hóa đơn: ${result.created} tạo mới, ${result.skipped} bỏ qua, ${result.totalStudents} học sinh.`);
    } catch (err: any) {
      setError(err.message || 'Lỗi sinh hóa đơn');
    }
  }

  async function deleteItem(id: number) {
    if (!confirm('Xóa mẫu phí này?')) return;
    setError('');
    setSuccessMsg('');
    try {
      await apiFetch(`/fee-templates/${id}`, { method: 'DELETE' });
      setSuccessMsg('Đã xóa mẫu phí.');
      fetchItems();
    } catch (err: any) {
      setError(err.message || 'Lỗi xóa mẫu phí');
    }
  }

  return (
    <div>
      <h2>Mẫu phí</h2>
      {error && <div className="error">{error}</div>}
      {successMsg && <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: '16px' }}>[SUCCESS] {successMsg}</div>}

      <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))' }}>
        <div className="form-group"><label>Năm học</label><select value={academicYearId} onChange={e => setAcademicYearId(e.target.value)}><option value="">Chọn năm học</option>{academicYears.map(y => <option key={y.id} value={y.id}>{y.name} {y.status === 'ACTIVE' ? '(Đang mở)' : ''}</option>)}</select><span className="input-desc">Năm học áp dụng</span></div>
        <div className="form-group"><label>Lớp học</label><select value={classId} onChange={e => setClassId(e.target.value)} disabled={!academicYearId}><option value="">Chọn lớp</option>{classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select><span className="input-desc">Lớp cần cấu hình phí</span></div>
        <div className="form-group"><label>Học kỳ</label><select value={semesterId} onChange={e => setSemesterId(e.target.value)} disabled={!academicYearId}><option value="">Chọn học kỳ</option>{semesters.map(s => <option key={s.id} value={s.id}>{s.name} - {s.academicYearName} {s.isCurrent ? '(Hiện tại)' : ''}</option>)}</select><span className="input-desc">Kỳ thu phí</span></div>
      </div>

      {classId && semesterId && <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', background: '#fbfbfb' }}>
        <div style={{ gridColumn: 'span 4', fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', marginBottom: '8px' }}>[ TẠO MẪU PHÍ ]</div>
        <div className="form-group"><label>Danh mục phí</label><select value={feeCategoryId} onChange={e => setFeeCategoryId(e.target.value)}><option value="">Chọn danh mục</option>{categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select><span className="input-desc">Tạo danh mục trước nếu trống</span></div>
        <div className="form-group"><label>Tên mẫu phí</label><input value={name} onChange={e => setName(e.target.value)} /><span className="input-desc">Tên hiển thị trên hóa đơn</span></div>
        <div className="form-group"><label>Số tiền</label><input type="number" value={amount} onChange={e => setAmount(+e.target.value)} /><span className="input-desc">VNĐ</span></div>
        <div className="form-group"><label>Hạn thanh toán</label><input type="date" value={dueDate} onChange={e => setDueDate(e.target.value)} /><span className="input-desc">Ngày cuối phụ huynh đóng</span></div>
        <div className="form-group"><label style={{ visibility: 'hidden' }}>Thao tác</label><button onClick={createItem}>Tạo mẫu</button><span className="input-desc" style={{ visibility: 'hidden' }}>&nbsp;</span></div>
      </div>}

      {classId && semesterId && <div style={{ marginTop: '32px' }}>
        <h3 style={{ fontSize: '15px', fontWeight: 700, textTransform: 'uppercase', marginBottom: '16px', borderBottom: '1px dashed #000000', paddingBottom: '8px' }}>Mẫu phí lớp: {classes.find(c => c.id === +classId)?.name}</h3>
        {items.length === 0 ? <p style={{ fontSize: '13px', color: '#a3a3a3', fontStyle: 'italic' }}>Chưa có mẫu phí cho lớp/học kỳ này.</p> : (
          <table><thead><tr><th>ID</th><th>Danh mục</th><th>Tên mẫu</th><th>Số tiền</th><th>Hạn nộp</th><th>HS áp dụng</th><th>Thao tác</th></tr></thead><tbody>{items.map(t => <tr key={t.id}><td>{t.id}</td><td>{t.feeCategoryName}</td><td style={{ fontWeight: 600 }}>{t.name}</td><td style={{ fontFamily: 'ui-monospace, monospace', fontWeight: 600 }}>{t.amount?.toLocaleString()} VNĐ</td><td>{t.dueDate}</td><td>{t.studentCount}</td><td><button onClick={() => generateBills(t.id)} style={{ marginRight: '8px' }}>Sinh bill</button><button className="danger" onClick={() => deleteItem(t.id)}>Xóa</button></td></tr>)}</tbody></table>
        )}
      </div>}
    </div>
  );
}
