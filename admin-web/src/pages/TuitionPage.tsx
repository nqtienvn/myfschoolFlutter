import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

interface AcademicYearItem { id: number; name: string; status: string; }
interface ClassItem { id: number; name: string; academicYearId: number; academicYearName: string; }
interface SemesterItem { id: number; name: string; academicYearId: number; academicYearName: string; isCurrent: boolean; order: number; }
interface StudentSummary { id: number; name: string; studentCode: string; }

export default function TuitionPage() {
  const [academicYears, setAcademicYears] = useState<AcademicYearItem[]>([]);
  const [academicYearId, setAcademicYearId] = useState('');
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [semesters, setSemesters] = useState<SemesterItem[]>([]);
  const [items, setItems] = useState<any[]>([]);
  const [loadingList, setLoadingList] = useState(false);
  const [classId, setClassId] = useState('');
  const [semesterId, setSemesterId] = useState('');
  const [billName, setBillName] = useState('Học phí Học kỳ 1');
  const [billAmount, setBillAmount] = useState(3000000);
  const [dueDate, setDueDate] = useState('');
  const [progress, setProgress] = useState({ active: false, current: 0, total: 0, label: '' });
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  useEffect(() => {
    fetchAcademicYears();
    const date = new Date();
    setDueDate(new Date(date.getFullYear(), date.getMonth() + 1, 0).toISOString().split('T')[0]);
  }, []);

  useEffect(() => {
    if (!academicYearId) return;
    setClassId('');
    setSemesterId('');
    apiFetch(`/classes?academicYearId=${academicYearId}&page=0&size=100`).then((d: any) => setClasses(d.content || [])).catch(() => {});
    apiFetch(`/semesters?academicYearId=${academicYearId}`).then((d: any) => {
      const list = d || [];
      setSemesters(list);
      const current = list.find((s: SemesterItem) => s.isCurrent);
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
    setLoadingList(true);
    try {
      const data = await apiFetch(`/tuition/bills/class?classId=${classId}&semesterId=${semesterId}`) as any;
      setItems(data || []);
    } catch (err: any) {
      setError(err.message || 'Không thể tải danh sách hóa đơn học phí');
    } finally {
      setLoadingList(false);
    }
  }

  async function handleCreateClassBills() {
    setError('');
    setSuccessMsg('');
    if (!classId || !semesterId) return setError('Vui lòng chọn Lớp học và Học kỳ.');
    if (!billName.trim()) return setError('Vui lòng nhập Tên khoản thu học phí.');
    if (isNaN(billAmount) || billAmount <= 0) return setError('Số tiền học phí phải lớn hơn 0.');
    if (!dueDate) return setError('Vui lòng chọn Hạn nộp học phí.');

    try {
      setProgress({ active: true, current: 0, total: 1, label: 'Đang tải danh sách học sinh trong lớp...' });
      const students = await apiFetch(`/classes/${classId}/students`) as StudentSummary[];
      if (!students || students.length === 0) {
        setError('Lớp học này hiện chưa có học sinh nào. Không thể tạo hóa đơn học phí.');
        setProgress({ active: false, current: 0, total: 0, label: '' });
        return;
      }
      setProgress({ active: true, current: 0, total: students.length, label: 'Bắt đầu tạo hóa đơn...' });
      for (let idx = 0; idx < students.length; idx++) {
        const student = students[idx];
        setProgress(p => ({ ...p, current: idx, label: `Đang tạo hóa đơn: ${student.name} (${idx + 1}/${students.length})...` }));
        await apiFetch('/tuition/bills', {
          method: 'POST',
          body: JSON.stringify({ studentId: student.id, classId: +classId, semesterId: +semesterId, name: billName.trim(), amount: billAmount, dueDate }),
        });
      }
      setProgress(p => ({ ...p, current: students.length, label: 'Đã tạo hóa đơn thành công cho cả lớp!' }));
      setSuccessMsg(`Đã phát hành thành công ${students.length} hóa đơn học phí cho lớp học!`);
      setTimeout(() => setProgress(p => ({ ...p, active: false })), 2000);
      fetchItems();
    } catch (err: any) {
      setError(`Lỗi tạo hóa đơn tại bản ghi học sinh: ${err.message || 'Lỗi không xác định'}`);
      setProgress(p => ({ ...p, active: false }));
    }
  }

  async function deleteItem(id: number) {
    if (!confirm('Bạn có chắc chắn muốn xóa hóa đơn học phí này?')) return;
    setError('');
    setSuccessMsg('');
    try {
      await apiFetch(`/tuition/bills/${id}`, { method: 'DELETE' });
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
      {progress.active && <div className="progress-container"><div>{progress.label}</div><div className="progress-bar-bg"><div className="progress-bar-fill" style={{ width: `${progress.total ? (progress.current / progress.total) * 100 : 0}%` }} /></div><div className="progress-text">Đã hoàn thành: {progress.current} / {progress.total} hóa đơn</div></div>}

      <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))' }}>
        <div className="form-group"><label>Năm học</label><select value={academicYearId} onChange={e => setAcademicYearId(e.target.value)}><option value="">Chọn năm học</option>{academicYears.map(y => <option key={y.id} value={y.id}>{y.name} {y.status === 'ACTIVE' ? '(Đang mở)' : ''}</option>)}</select><span className="input-desc">Năm học áp dụng</span></div>
        <div className="form-group"><label>Lớp học</label><select value={classId} onChange={e => setClassId(e.target.value)} disabled={!academicYearId}><option value="">Chọn lớp học</option>{classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select><span className="input-desc">Lớp học cần quản lý học phí</span></div>
        <div className="form-group"><label>Học kỳ</label><select value={semesterId} onChange={e => setSemesterId(e.target.value)} disabled={!academicYearId}><option value="">Chọn học kỳ</option>{semesters.map(s => <option key={s.id} value={s.id}>{s.name} - {s.academicYearName} {s.isCurrent ? '(Hiện tại)' : ''}</option>)}</select><span className="input-desc">Áp dụng cho kỳ học</span></div>
      </div>

      {classId && semesterId && !progress.active && <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', marginTop: '16px', background: '#fbfbfb' }}>
        <div style={{ gridColumn: 'span 4', fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', marginBottom: '8px' }}>[ TẠO KHOẢN THU HỌC PHÍ HÀNG LOẠT CHO CẢ LỚP ]</div>
        <div className="form-group"><label>Tên khoản thu</label><input placeholder="VD: Học phí học kỳ 1, Tiền đồng phục..." value={billName} onChange={e => setBillName(e.target.value)} /><span className="input-desc">Tên nội dung hóa đơn hiển thị lên app</span></div>
        <div className="form-group"><label>Số tiền (VNĐ)</label><input type="number" value={billAmount} onChange={e => setBillAmount(+e.target.value)} /><span className="input-desc">Số tiền nộp</span></div>
        <div className="form-group"><label>Hạn thanh toán</label><input type="date" value={dueDate} onChange={e => setDueDate(e.target.value)} /><span className="input-desc">Hạn cuối phụ huynh phải thanh toán</span></div>
        <div className="form-group"><label style={{ visibility: 'hidden' }}>Thao tác</label><button onClick={handleCreateClassBills} style={{ height: '38px', width: '100%' }}>Phát hành hóa đơn</button><span className="input-desc" style={{ visibility: 'hidden' }}>&nbsp;</span></div>
      </div>}

      {classId && semesterId && <div style={{ marginTop: '32px' }}>
        <h3 style={{ fontSize: '15px', fontWeight: 700, textTransform: 'uppercase', marginBottom: '16px', borderBottom: '1px dashed #000000', paddingBottom: '8px' }}>Danh sách Hóa đơn lớp: {classes.find(c => c.id === +classId)?.name}</h3>
        {loadingList ? <p style={{ fontSize: '13px', fontFamily: 'ui-monospace, monospace' }}>Đang tải danh sách hóa đơn học phí...</p> : items.length === 0 ? <p style={{ fontSize: '13px', color: '#a3a3a3', fontStyle: 'italic' }}>Chưa có hóa đơn học phí nào được tạo cho lớp học này.</p> : (
          <table><thead><tr><th>ID</th><th>Học sinh</th><th>Mã số HS</th><th>Nội dung khoản thu</th><th>Số tiền nộp</th><th>Hạn nộp</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>{items.map((b: any) => <tr key={b.id}><td>{b.id}</td><td style={{ fontWeight: 600 }}>{b.studentName}</td><td>{b.studentCode}</td><td>{b.name}</td><td style={{ fontFamily: 'ui-monospace, monospace', fontWeight: 600 }}>{b.amount?.toLocaleString()} VNĐ</td><td style={{ fontFamily: 'ui-monospace, monospace', fontSize: '12px' }}>{b.dueDate}</td><td>{b.status}</td><td><button className="danger" onClick={() => deleteItem(b.id)}>Xóa</button></td></tr>)}</tbody></table>
        )}
      </div>}
    </div>
  );
}
