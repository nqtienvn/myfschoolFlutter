import { useState, useEffect } from 'react';
import { getAcademicYears } from '../api/academicYear';
import { getSemesters } from '../api/semester';
import { getClasses } from '../api/class';
import { getClassTuitionBills, deleteTuitionBill } from '../api/tuitionBill';

interface AcademicYearItem { id: number; name: string; status: string; }
interface ClassItem { id: number; name: string; academicYearId: number; academicYearName: string; }
interface SemesterItem { id: number; name: string; academicYearId: number; academicYearName: string; isCurrent: boolean; order: number; }
interface TuitionBill { id: number; studentName: string; studentCode: string; name: string; amount: number; dueDate: string; status: string; feeTemplateId: number | null; feeTemplateName: string | null; }

export default function TuitionPage({ selectedYearId, selectedSemesterId }: { selectedYearId?: string; selectedSemesterId?: string }) {
  const [academicYears, setAcademicYears] = useState<AcademicYearItem[]>([]);
  const [academicYearId, setAcademicYearId] = useState(selectedYearId || '');
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [semesters, setSemesters] = useState<SemesterItem[]>([]);
  const [items, setItems] = useState<TuitionBill[]>([]);
  const [loadingList, setLoadingList] = useState(false);
  const [classId, setClassId] = useState('');
  const [semesterId, setSemesterId] = useState(selectedSemesterId || '');
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  useEffect(() => { fetchAcademicYears(); }, []);

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

  useEffect(() => {
    if (!academicYearId) return;
    setClassId('');
    if (!selectedSemesterId) {
      setSemesterId('');
    }
    getClasses({ academicYearId, page: 0, size: 100 }).then((d: any) => setClasses(d.content || [])).catch(() => {});
    getSemesters(academicYearId).then((d: any) => {
      const list = d || [];
      setSemesters(list);
      if (!selectedSemesterId) {
        const current = list.find((s: SemesterItem) => s.isCurrent) || list[0];
        if (current) setSemesterId(String(current.id));
      }
    }).catch(() => {});
  }, [academicYearId, selectedSemesterId]);

  useEffect(() => {
    if (!classId || !semesterId) {
      setItems([]);
      return;
    }
    fetchItems();
  }, [classId, semesterId]);

  async function fetchAcademicYears() {
    const data = await getAcademicYears() as AcademicYearItem[];
    setAcademicYears(data);
    const active = data.find(y => y.status === 'ACTIVE') || data[0];
    if (active) setAcademicYearId(String(active.id));
  }

  async function fetchItems() {
    setLoadingList(true);
    try {
      const data = await getClassTuitionBills(classId, semesterId) as TuitionBill[];
      setItems(data || []);
    } catch (err: any) {
      setError(err.message || 'Không thể tải danh sách hóa đơn học phí');
    } finally {
      setLoadingList(false);
    }
  }

  async function deleteItem(id: number) {
    if (!confirm('Bạn có chắc chắn muốn xóa hóa đơn học phí này?')) return;
    setError('');
    setSuccessMsg('');
    try {
      await deleteTuitionBill(id);
      setSuccessMsg('Đã xóa hóa đơn học phí.');
      fetchItems();
    } catch (err: any) {
      setError(err.message || 'Lỗi khi xóa hóa đơn');
    }
  }

  return (
    <div>
      <h2>Học phí & Thanh toán</h2>
      {error && <div className="error">{error}</div>}
      {successMsg && <div style={{ color: '#16a34a', fontFamily: 'ui-monospace, monospace', fontSize: '13px', marginBottom: '16px' }}>[SUCCESS] {successMsg}</div>}

      <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))' }}>
        <div className="form-group"><label>Năm học</label><select value={academicYearId} onChange={e => setAcademicYearId(e.target.value)}><option value="">Chọn năm học</option>{academicYears.map(y => <option key={y.id} value={y.id}>{y.name} {y.status === 'ACTIVE' ? '(Đang mở)' : ''}</option>)}</select><span className="input-desc">Năm học áp dụng</span></div>
        <div className="form-group"><label>Lớp học</label><select value={classId} onChange={e => setClassId(e.target.value)} disabled={!academicYearId}><option value="">Chọn lớp học</option>{classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select><span className="input-desc">Lớp học cần quản lý học phí</span></div>
        <div className="form-group"><label>Học kỳ</label><select value={semesterId} onChange={e => setSemesterId(e.target.value)} disabled={!academicYearId}><option value="">Chọn học kỳ</option>{semesters.map(s => <option key={s.id} value={s.id}>{s.name} - {s.academicYearName} {s.isCurrent ? '(Hiện tại)' : ''}</option>)}</select><span className="input-desc">Áp dụng cho kỳ học</span></div>
      </div>

      {classId && semesterId && <div style={{ marginTop: '32px' }}>
        <h3 style={{ fontSize: '15px', fontWeight: 700, textTransform: 'uppercase', marginBottom: '16px', borderBottom: '1px dashed #000000', paddingBottom: '8px' }}>Danh sách Hóa đơn lớp: {classes.find(c => c.id === +classId)?.name}</h3>
        {loadingList ? <p style={{ fontSize: '13px', fontFamily: 'ui-monospace, monospace' }}>Đang tải danh sách hóa đơn học phí...</p> : items.length === 0 ? <p style={{ fontSize: '13px', color: '#a3a3a3', fontStyle: 'italic' }}>Chưa có hóa đơn học phí nào được tạo cho lớp học này. Tạo ở trang Mẫu phí rồi bấm Sinh bill.</p> : (
          <table><thead><tr><th>ID</th><th>Học sinh</th><th>Mã số HS</th><th>Nội dung khoản thu</th><th>Template</th><th>Số tiền nộp</th><th>Hạn nộp</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>{items.map(b => <tr key={b.id}><td>{b.id}</td><td style={{ fontWeight: 600 }}>{b.studentName}</td><td>{b.studentCode}</td><td>{b.name}</td><td>{b.feeTemplateName || '-'}</td><td style={{ fontFamily: 'ui-monospace, monospace', fontWeight: 600 }}>{b.amount?.toLocaleString()} VNĐ</td><td style={{ fontFamily: 'ui-monospace, monospace', fontSize: '12px' }}>{b.dueDate}</td><td>{b.status}</td><td><button className="danger" onClick={() => deleteItem(b.id)}>Xóa</button></td></tr>)}</tbody></table>
        )}
      </div>}
    </div>
  );
}
