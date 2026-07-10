import { useEffect, useMemo, useState } from 'react';
import { getClasses } from '../api/class';
import { createStudentEnrollment, getStudentAccountsByClass, type CreateStudentEnrollmentRequest, type StudentAccountByClass, type StudentEnrollmentResult } from '../api/studentEnrollment';

interface ClassItem { id: number; name: string; gradeLevel: number; }
const emptyForm = {
  classId: '', studentCode: '', studentName: '', dateOfBirth: '', gender: 'MALE', studentAddress: '', studentCitizenId: '',
  parentName: '', relationship: 'FATHER', parentPhone: '', parentEmail: '', parentCitizenId: '', parentOccupation: '', parentAddress: '',
};

export default function StudentEnrollmentPage({ selectedYearId, editable = true }: { selectedYearId?: string; editable?: boolean }) {
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [grade, setGrade] = useState('10');
  const [form, setForm] = useState(emptyForm);
  const [result, setResult] = useState<StudentEnrollmentResult | null>(null);
  const [accounts, setAccounts] = useState<StudentAccountByClass[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadingAccounts, setLoadingAccounts] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setForm(emptyForm); setResult(null); setError('');
    if (!selectedYearId) return setClasses([]);
    getClasses({ academicYearId: selectedYearId, page: 0, size: 500 })
      .then((data: any) => setClasses(data.content || []))
      .catch((cause: any) => setError(cause.message || 'Không thể tải danh sách lớp.'));
  }, [selectedYearId]);

  const filteredClasses = useMemo(() => classes.filter(item => String(item.gradeLevel) === grade), [classes, grade]);
  const set = (key: keyof typeof emptyForm, value: string) => setForm(current => ({ ...current, [key]: value }));

  async function loadAccounts(classId: string) {
    if (!selectedYearId || !classId) { setAccounts([]); return; }
    setLoadingAccounts(true);
    try {
      setAccounts(await getStudentAccountsByClass(Number(selectedYearId), Number(classId)));
    } catch (cause: any) {
      setError(cause.message || 'Không thể tải danh sách tài khoản theo lớp.');
    } finally { setLoadingAccounts(false); }
  }

  useEffect(() => {
    loadAccounts(form.classId);
  }, [selectedYearId, form.classId]);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!selectedYearId) return setError('Hãy chọn năm học cần thêm học sinh.');
    setLoading(true); setError(''); setResult(null);
    try {
      const data = await createStudentEnrollment({
        ...form,
        academicYearId: Number(selectedYearId),
        classId: Number(form.classId),
        gender: form.gender as CreateStudentEnrollmentRequest['gender'],
        relationship: form.relationship as CreateStudentEnrollmentRequest['relationship'],
      });
      setResult(data);
      await loadAccounts(form.classId);
      setForm(current => ({ ...emptyForm, classId: current.classId }));
    } catch (cause: any) {
      setError(cause.message || 'Không thể thêm học sinh.');
    } finally { setLoading(false); }
  }

  return <div className="page-stack">
    <div className="page-heading"><div><span className="eyebrow">Bước 5</span><h1>Thêm học sinh thủ công</h1><p>Tạo tài khoản học sinh, phụ huynh và xếp lớp trong một thao tác.</p></div></div>
    {!selectedYearId && <div className="notice warning">Chọn năm học DRAFT ở thanh phía trên.</div>}
    {!editable && selectedYearId && <div className="notice warning">Không thể thêm học sinh vì năm học đã được kích hoạt hoặc hoàn tất.</div>}
    {error && <div className="notice error">{error}</div>}
    {result && <div className="notice success">Đã thêm học sinh <strong>{result.studentCode}</strong> vào lớp <strong>{result.className}</strong>. {result.parentReused ? 'Đã liên kết với tài khoản phụ huynh hiện có.' : 'Đã tạo tài khoản phụ huynh mới.'}</div>}

    <form className="page-stack" onSubmit={submit}>
      <section className="form-grid">
        <div style={{ gridColumn: '1 / -1' }}><h2>Thông tin xếp lớp</h2></div>
        <div className="form-group"><label>Khối</label><select value={grade} onChange={e => { setGrade(e.target.value); set('classId', ''); }}>{Array.from({ length: 12 }, (_, index) => index + 1).map(value => <option key={value}>{value}</option>)}</select></div>
        <div className="form-group"><label>Lớp</label><select required value={form.classId} onChange={e => set('classId', e.target.value)}><option value="">Chọn lớp</option>{filteredClasses.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div>
      </section>

      <section className="form-grid">
        <div style={{ gridColumn: '1 / -1' }}><h2>Học sinh</h2><small className="input-desc">Mã học sinh là tên đăng nhập ban đầu.</small></div>
        <div className="form-group"><label>Mã học sinh</label><input required maxLength={20} value={form.studentCode} onChange={e => set('studentCode', e.target.value.toUpperCase())} /></div>
        <div className="form-group"><label>Họ và tên</label><input required value={form.studentName} onChange={e => set('studentName', e.target.value)} /></div>
        <div className="form-group"><label>Ngày sinh</label><input required type="date" value={form.dateOfBirth} onChange={e => set('dateOfBirth', e.target.value)} /></div>
        <div className="form-group"><label>Giới tính</label><select value={form.gender} onChange={e => set('gender', e.target.value)}><option value="MALE">Nam</option><option value="FEMALE">Nữ</option><option value="OTHER">Khác</option></select></div>
        <div className="form-group"><label>CCCD/Định danh</label><input value={form.studentCitizenId} onChange={e => set('studentCitizenId', e.target.value)} /></div>
        <div className="form-group"><label>Địa chỉ</label><input value={form.studentAddress} onChange={e => set('studentAddress', e.target.value)} /></div>
      </section>

      <section className="form-grid">
        <div style={{ gridColumn: '1 / -1' }}><h2>Phụ huynh</h2><small className="input-desc">Nếu số điện thoại đã tồn tại ở vai trò phụ huynh, hệ thống tự liên kết tài khoản đó với học sinh mới.</small></div>
        <div className="form-group"><label>Họ và tên</label><input required value={form.parentName} onChange={e => set('parentName', e.target.value)} /></div>
        <div className="form-group"><label>Quan hệ</label><select value={form.relationship} onChange={e => set('relationship', e.target.value)}><option value="FATHER">Bố</option><option value="MOTHER">Mẹ</option><option value="GUARDIAN">Người giám hộ</option></select></div>
        <div className="form-group"><label>Số điện thoại</label><input required pattern="0[0-9]{9}" value={form.parentPhone} onChange={e => set('parentPhone', e.target.value)} /></div>
        <div className="form-group"><label>Email</label><input type="email" value={form.parentEmail} onChange={e => set('parentEmail', e.target.value)} /></div>
        <div className="form-group"><label>CCCD/Định danh</label><input value={form.parentCitizenId} onChange={e => set('parentCitizenId', e.target.value)} /></div>
        <div className="form-group"><label>Nghề nghiệp</label><input value={form.parentOccupation} onChange={e => set('parentOccupation', e.target.value)} /></div>
        <div className="form-group"><label>Địa chỉ</label><input value={form.parentAddress} onChange={e => set('parentAddress', e.target.value)} /></div>
      </section>
      <div className="notice">Mật khẩu mặc định: <strong>12345678</strong>. Tài khoản mới phải đổi mật khẩu ở lần đăng nhập đầu tiên.</div>
      <div className="page-actions"><button disabled={loading || !selectedYearId || !editable}>{loading ? 'Đang tạo...' : 'Tạo học sinh & xếp lớp'}</button></div>
    </form>

    <section className="page-stack">
      <div className="class-list-heading">
        <div><h2>Danh sách tài khoản theo lớp</h2><p>Tài khoản học sinh và phụ huynh của lớp đang chọn.</p></div>
        <span>{accounts.length} học sinh</span>
      </div>
      {!form.classId && <div className="notice">Chọn lớp ở phía trên để xem danh sách tài khoản.</div>}
      {form.classId && loadingAccounts && <div className="notice">Đang tải danh sách tài khoản...</div>}
      {form.classId && !loadingAccounts && <div className="table-responsive"><table className="classes-table">
        <thead><tr><th>Học sinh</th><th>Tài khoản học sinh</th><th>Phụ huynh</th><th>Tài khoản phụ huynh</th><th>Quan hệ</th></tr></thead>
        <tbody>
          {accounts.flatMap(student => student.guardians.length > 0
            ? student.guardians.map((guardian, index) => <tr key={`${student.studentId}-${guardian.parentId}`}>
                <td>{index === 0 && <><strong>{student.studentName}</strong><br /><small>{student.studentCode}</small></>}</td>
                <td>{index === 0 && <strong>{student.studentUsername}</strong>}</td>
                <td><strong>{guardian.parentName}</strong>{guardian.parentEmail && <><br /><small>{guardian.parentEmail}</small></>}</td>
                <td>{guardian.parentUsername}</td>
                <td>{{ FATHER: 'Bố', MOTHER: 'Mẹ', GUARDIAN: 'Người giám hộ' }[guardian.relationship]}</td>
              </tr>)
            : [<tr key={`${student.studentId}-none`}><td><strong>{student.studentName}</strong><br /><small>{student.studentCode}</small></td><td><strong>{student.studentUsername}</strong></td><td colSpan={3}>Chưa có phụ huynh</td></tr>])}
          {accounts.length === 0 && <tr><td colSpan={5}>Lớp chưa có học sinh.</td></tr>}
        </tbody>
      </table></div>}
    </section>
  </div>;
}
