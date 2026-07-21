import { useEffect, useMemo, useState } from 'react';
import { getClasses } from '../api/class';
import {
  createStudentEnrollment,
  getStudentAccountsByClass,
  type CreateStudentEnrollmentRequest,
  type StudentAccountByClass,
  type StudentEnrollmentResult,
} from '../api/studentEnrollment';

interface ClassItem { id: number; name: string; gradeLevel: number; }

const emptyForm = {
  classId: '', studentCode: '', studentName: '', dateOfBirth: '', gender: 'MALE', studentAddress: '', studentCitizenId: '', studentEmail: '',
  parentName: '', relationship: 'FATHER', parentPhone: '', parentEmail: '', parentCitizenId: '', parentOccupation: '', parentAddress: '',
};

const relationshipLabels = { FATHER: 'Bố', MOTHER: 'Mẹ', GUARDIAN: 'Người giám hộ' } as const;
const gradeOptions = Array.from({ length: 12 }, (_, index) => String(index + 1));

export default function StudentEnrollmentPage({ selectedYearId, editable = true }: { selectedYearId?: string; editable?: boolean }) {
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [formGrade, setFormGrade] = useState('10');
  const [form, setForm] = useState(emptyForm);
  const [result, setResult] = useState<StudentEnrollmentResult | null>(null);
  const [accountGrade, setAccountGrade] = useState('10');
  const [accountClassId, setAccountClassId] = useState('');
  const [accounts, setAccounts] = useState<StudentAccountByClass[]>([]);
  const [loading, setLoading] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [loadingAccounts, setLoadingAccounts] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setForm(emptyForm);
    setResult(null);
    setAccounts([]);
    setAccountClassId('');
    setError('');
    if (!selectedYearId) { setClasses([]); return; }
    getClasses({ academicYearId: selectedYearId, page: 0, size: 500 })
      .then((data: any) => setClasses(data.content || []))
      .catch((cause: any) => setError(cause.message || 'Không thể tải danh sách lớp.'));
  }, [selectedYearId]);

  const formClasses = useMemo(
    () => classes.filter(item => String(item.gradeLevel) === formGrade),
    [classes, formGrade],
  );
  const accountClasses = useMemo(
    () => classes.filter(item => String(item.gradeLevel) === accountGrade),
    [classes, accountGrade],
  );
  const selectedClass = classes.find(item => String(item.id) === form.classId);
  const selectedAccountClass = classes.find(item => String(item.id) === accountClassId);
  const parentAccountCount = new Set(accounts.flatMap(student => student.guardians.map(item => item.parentId))).size;

  const set = (key: keyof typeof emptyForm, value: string) => {
    setForm(current => ({ ...current, [key]: value }));
    setResult(null);
  };

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
    loadAccounts(accountClassId);
  }, [selectedYearId, accountClassId]);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!selectedYearId) { setError('Hãy chọn năm học cần thêm học sinh.'); return; }
    setLoading(true);
    setError('');
    setResult(null);
    const createdClassId = form.classId;
    try {
      const data = await createStudentEnrollment({
        ...form,
        academicYearId: Number(selectedYearId),
        classId: Number(createdClassId),
        gender: form.gender as CreateStudentEnrollmentRequest['gender'],
        relationship: form.relationship as CreateStudentEnrollmentRequest['relationship'],
      });
      setResult(data);
      setShowForm(false);
      setAccountGrade(formGrade);
      if (accountClassId === createdClassId) await loadAccounts(createdClassId);
      else setAccountClassId(createdClassId);
      setForm(current => ({ ...emptyForm, classId: current.classId }));
    } catch (cause: any) {
      setError(cause.message || 'Không thể thêm học sinh.');
    } finally { setLoading(false); }
  }

  return <div className="page-stack student-enrollment-page">
    <header className="page-heading enrollment-heading">
      <div><span className="eyebrow">Tài khoản người dùng</span><h1>Quản lý phụ huynh, học sinh</h1><p>Tạo tài khoản, liên kết gia đình và xếp lớp trong một luồng duy nhất.</p></div>
      <div className="page-heading-actions">
        <button type="button" onClick={() => setShowForm(v => !v)}>{showForm ? '✕ Đóng' : '＋ Thêm học sinh'}</button>
      </div>
    </header>

    {!selectedYearId && <div className="notice warning">Chọn năm học ở thanh phía trên để bắt đầu.</div>}
    {!editable && selectedYearId && <div className="notice warning">Năm học đã hoàn tất nên không thể tạo tài khoản mới.</div>}
    {error && <div className="notice error" role="alert">{error}</div>}

    {result && <section className="enrollment-success" aria-live="polite">
      <div className="success-check">✓</div>
      <div><strong>Đã thêm {result.studentCode} vào lớp {result.className}</strong><p>{result.parentReused ? 'Đã liên kết với tài khoản phụ huynh hiện có.' : 'Đã tạo mới tài khoản phụ huynh.'} Danh sách bên dưới đã được cập nhật.</p></div>
      <div className="created-accounts">
        <span>Học sinh <strong>TK: {result.studentUsername}</strong><strong>{result.studentCredentialsEmailed ? 'Đã gửi thông tin qua email' : 'Chưa có email nhận'}</strong></span>
        <span>Phụ huynh <strong>TK: {result.parentUsername}</strong><strong>{result.parentReused ? 'Tài khoản hiện có' : result.parentCredentialsEmailed ? 'Đã gửi thông tin qua email' : 'Chưa gửi được email'}</strong></span>
      </div>
    </section>}

    {showForm && <form className="enrollment-form" onSubmit={submit}>
      <section className="enrollment-class-step">
        <div className="enrollment-section-heading"><span>1</span><div><h2>Chọn lớp tiếp nhận</h2><p>Học sinh sẽ được ghi danh vào lớp này trong năm học đang chọn.</p></div></div>
        <div className="enrollment-class-fields">
          <div className="form-group"><label>Khối <em>*</em></label><select value={formGrade} onChange={e => { setFormGrade(e.target.value); set('classId', ''); }}>{gradeOptions.map(value => <option key={value}>{value}</option>)}</select></div>
          <div className="form-group"><label>Lớp học <em>*</em></label><select required value={form.classId} onChange={e => set('classId', e.target.value)}><option value="">Chọn lớp</option>{formClasses.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div>
          <div className="selected-class-summary"><span>Lớp đang chọn</span><strong>{selectedClass?.name || 'Chưa chọn lớp'}</strong><small>{selectedClass ? `Khối ${selectedClass.gradeLevel}` : 'Chọn lớp trước khi nhập thông tin'}</small></div>
        </div>
      </section>

      <div className="enrollment-people-grid">
        <section className="enrollment-person-card">
          <div className="enrollment-section-heading"><span>2</span><div><h2>Thông tin học sinh</h2><p>Hệ thống tự sinh số đăng nhập gồm 10 chữ số, không bắt đầu bằng số 0 và không trùng tài khoản khác.</p></div></div>
          <div className="enrollment-fields">
            <div className="form-group"><label>Mã học sinh <em>*</em></label><input required maxLength={20} placeholder="VD: HS2026001" value={form.studentCode} onChange={e => set('studentCode', e.target.value.toUpperCase())} /></div>
            <div className="form-group wide"><label>Họ và tên <em>*</em></label><input required placeholder="Nguyễn Văn An" value={form.studentName} onChange={e => set('studentName', e.target.value)} /></div>
            <div className="form-group"><label>Ngày sinh <em>*</em></label><input required type="date" value={form.dateOfBirth} onChange={e => set('dateOfBirth', e.target.value)} /></div>
            <div className="form-group"><label>Giới tính <em>*</em></label><select value={form.gender} onChange={e => set('gender', e.target.value)}><option value="MALE">Nam</option><option value="FEMALE">Nữ</option><option value="OTHER">Khác</option></select></div>
            <div className="form-group"><label>CCCD/Định danh</label><input placeholder="Không bắt buộc" value={form.studentCitizenId} onChange={e => set('studentCitizenId', e.target.value)} /></div>
            <div className="form-group wide"><label>Email học sinh <em>*</em></label><input required type="email" placeholder="hocsinh@gmail.com" value={form.studentEmail} onChange={e => set('studentEmail', e.target.value)} /></div>
            <div className="form-group wide"><label>Địa chỉ</label><input placeholder="Địa chỉ hiện tại" value={form.studentAddress} onChange={e => set('studentAddress', e.target.value)} /></div>
          </div>
        </section>

        <section className="enrollment-person-card">
          <div className="enrollment-section-heading"><span>3</span><div><h2>Thông tin phụ huynh</h2><p>Số điện thoại là tên đăng nhập và dùng để tìm tài khoản có sẵn.</p></div></div>
          <div className="enrollment-fields">
            <div className="form-group wide"><label>Họ và tên <em>*</em></label><input required placeholder="Nguyễn Văn Bình" value={form.parentName} onChange={e => set('parentName', e.target.value)} /></div>
            <div className="form-group"><label>Quan hệ <em>*</em></label><select value={form.relationship} onChange={e => set('relationship', e.target.value)}><option value="FATHER">Bố</option><option value="MOTHER">Mẹ</option><option value="GUARDIAN">Người giám hộ</option></select></div>
            <div className="form-group"><label>Số điện thoại <em>*</em></label><input required inputMode="numeric" pattern="0[0-9]{9}" maxLength={10} placeholder="09xxxxxxxx" value={form.parentPhone} onChange={e => set('parentPhone', e.target.value.replace(/\D/g, ''))} /></div>
            <div className="form-group wide"><label>Email <small>(bắt buộc nếu tạo phụ huynh mới)</small></label><input type="email" placeholder="phuhuynh@email.com" value={form.parentEmail} onChange={e => set('parentEmail', e.target.value)} /></div>
            <div className="form-group"><label>CCCD/Định danh</label><input placeholder="Không bắt buộc" value={form.parentCitizenId} onChange={e => set('parentCitizenId', e.target.value)} /></div>
            <div className="form-group"><label>Nghề nghiệp</label><input placeholder="Không bắt buộc" value={form.parentOccupation} onChange={e => set('parentOccupation', e.target.value)} /></div>
            <div className="form-group wide"><label>Địa chỉ</label><input placeholder="Địa chỉ hiện tại" value={form.parentAddress} onChange={e => set('parentAddress', e.target.value)} /></div>
          </div>
        </section>
      </div>

      <footer className="enrollment-form-footer">
        <div className="password-note"><span>i</span><p>Mật khẩu tạm được tạo ngẫu nhiên<small>Thông tin đăng nhập được gửi qua email và không hiển thị trong trang quản trị.</small></p></div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button disabled={loading || !selectedYearId || !editable || !form.classId}>{loading ? 'Đang tạo tài khoản...' : 'Tạo tài khoản & xếp lớp'}</button>
          <button type="button" className="secondary-button" onClick={() => setShowForm(false)}>Đóng</button>
        </div>
      </footer>
    </form>}

    <section className="account-directory">
      <div className="account-directory-heading">
        <div><span className="eyebrow">Danh sách sau khi tạo</span><h2>Tài khoản học sinh & phụ huynh</h2><p>Lọc theo lớp để tra cứu tên đăng nhập và email khôi phục của từng gia đình.</p></div>
        <div className="account-totals"><span><strong>{accounts.length}</strong> Học sinh</span><span><strong>{parentAccountCount}</strong> Phụ huynh</span></div>
      </div>

      <div className="account-filters">
        <div className="form-group"><label>Khối</label><select value={accountGrade} onChange={e => { setAccountGrade(e.target.value); setAccountClassId(''); }}>{gradeOptions.map(value => <option key={value}>{value}</option>)}</select></div>
        <div className="form-group"><label>Lớp học</label><select value={accountClassId} onChange={e => setAccountClassId(e.target.value)}><option value="">Chọn lớp để xem tài khoản</option>{accountClasses.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div>
        <div className="active-class-filter"><span>Đang xem</span><strong>{selectedAccountClass?.name || 'Chưa chọn lớp'}</strong></div>
      </div>

      {!accountClassId && <div className="account-empty"><span>⌕</span><strong>Chọn một lớp để xem danh sách</strong><p>Tài khoản học sinh và phụ huynh sẽ hiển thị tại đây.</p></div>}
      {accountClassId && loadingAccounts && <div className="account-empty"><span className="loading-dot">•••</span><strong>Đang tải danh sách tài khoản</strong></div>}
      {accountClassId && !loadingAccounts && accounts.length === 0 && <div className="account-empty"><span>0</span><strong>Lớp chưa có học sinh</strong><p>Hãy sử dụng biểu mẫu phía trên để thêm học sinh đầu tiên.</p></div>}
      {accountClassId && !loadingAccounts && accounts.length > 0 && <div className="table-responsive account-table-wrap"><table className="account-table">
        <thead><tr><th>Tên học sinh</th><th>Tài khoản học sinh</th><th>Email học sinh</th><th>Tên phụ huynh</th><th>Tài khoản phụ huynh</th><th>Email phụ huynh</th><th>Quan hệ với học sinh</th></tr></thead>
        <tbody>{accounts.flatMap(student => {
          const guardians: Array<StudentAccountByClass['guardians'][number] | null> = student.guardians.length > 0 ? student.guardians : [null];
          return guardians.map(guardian => <tr key={`${student.studentId}-${guardian?.parentId || 'none'}`}>
            <td><strong>{student.studentName}</strong><small className="account-secondary-text">{student.studentCode}</small></td>
            <td><b className="login-chip">{student.studentUsername}</b></td>
            <td>{student.studentEmail || <span className="muted-text">Chưa bổ sung email</span>}</td>
            <td>{guardian ? <strong>{guardian.parentName}</strong> : <span className="muted-text">Chưa có phụ huynh</span>}</td>
            <td>{guardian ? <b className="login-chip">{guardian.parentUsername}</b> : '—'}</td>
            <td>{guardian?.parentEmail || '—'}</td>
            <td>{guardian ? relationshipLabels[guardian.relationship] : '—'}</td>
          </tr>);
        })}</tbody>
      </table></div>}
    </section>
  </div>;
}
