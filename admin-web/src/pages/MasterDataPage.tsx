import { useEffect, useState } from 'react';
import { createAcademicYear, getAcademicYears, updateAcademicYear } from '../api/academicYear';
import { getAcademicYearMasterData, updateAcademicYearMasterData } from '../api/academicYearConfig';
import { getPeriods, getShifts } from '../api/masterData';
import { createSubject, deleteSubject, getSubjects } from '../api/subject';
import { getSemesters } from '../api/semester';
import { getGradeTemplates, type GradeConfig } from '../api/gradeConfiguration';
import ShiftPeriodSelector from '../components/ShiftPeriodSelector';

type TabKey = 'academic-years' | 'catalogs';
interface Props { initialTab?: TabKey; selectedYearId?: string; selectedYearStatus?: string; onYearCreated?: () => void; }
interface Year { id: number; name: string; startDate: string; endDate: string; status: string; }
interface Semester { id: number; academicYearId: number; name: string; order: number; startDate: string; endDate: string; status: string; }
interface CatalogItem { id: number; name: string; code?: string; order?: number; shiftId?: number; shiftName?: string; }

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'NHÁP',
  NOT_STARTED: 'CHƯA BẮT ĐẦU',
  ACTIVE: 'ĐANG HOẠT ĐỘNG',
  COMPLETED: 'ĐÃ HOÀN THÀNH',
};

export default function MasterDataPage({ initialTab = 'catalogs', selectedYearId, selectedYearStatus, onYearCreated }: Props) {
  const [tab, setTab] = useState<TabKey>(initialTab);
  const [years, setYears] = useState<Year[]>([]);
  const [semesters, setSemesters] = useState<Semester[]>([]);

  const [subjects, setSubjects] = useState<CatalogItem[]>([]);
  const [shifts, setShifts] = useState<CatalogItem[]>([]);
  const [periods, setPeriods] = useState<CatalogItem[]>([]);
  const [deleteTarget, setDeleteTarget] = useState<{ id: number; name: string } | null>(null);
  const [subjectIds, setSubjectIds] = useState<number[]>([]);
  const [shiftIds, setShiftIds] = useState<number[]>([]);
  const [periodIds, setPeriodIds] = useState<number[]>([]);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [showYearForm, setShowYearForm] = useState(false);
  const [showSubjectForm, setShowSubjectForm] = useState(false);
  const [subjectName, setSubjectName] = useState('');
  const [subjectCode, setSubjectCode] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [gradeTemplates, setGradeTemplates] = useState<GradeConfig[]>([]);
  const [gradeConfigTemplateId, setGradeConfigTemplateId] = useState('');

  async function loadYears() {
    const [yearData, semesterData, templateData] = await Promise.all([
      getAcademicYears(),
      getSemesters(selectedYearId),
      getGradeTemplates(),
    ]);
    setYears((yearData || []) as Year[]);
    setSemesters((semesterData || []) as Semester[]);
    setGradeTemplates(templateData || []);
    setGradeConfigTemplateId(current => current || (templateData?.[0] ? String(templateData[0].id) : ''));
  }

  async function loadCatalogs() {
    const [subjectData, shiftData, periodData] = await Promise.all([
      getSubjects(), getShifts(), getPeriods(),
    ]);
    setSubjects(subjectData || []);
    setShifts(shiftData || []);
    setPeriods(periodData || []);
  }

  useEffect(() => { setTab(initialTab); }, [initialTab]);
  useEffect(() => { tab === 'academic-years' ? loadYears() : loadCatalogs(); }, [tab, selectedYearId]);
  useEffect(() => {
    setEditingId(null);
    setStartDate('');
    setEndDate('');
    setShowYearForm(false);
    setMessage('');
    setError('');
  }, [selectedYearId]);
  useEffect(() => {
    if (!selectedYearId || tab !== 'catalogs') return;
    getAcademicYearMasterData(selectedYearId).then(config => {
      setSubjectIds(config.subjectIds || []);
      setShiftIds(config.shiftIds || []);
      setPeriodIds(config.periodIds || []);
    }).catch((cause: any) => setError(cause.message || 'Không thể tải cấu hình năm học.'));
  }, [selectedYearId, tab]);

  async function saveYear() {
    if (!startDate || !endDate) return setError('Vui lòng nhập ngày bắt đầu và kết thúc.');
    const startYear = Number(startDate.slice(0, 4));
    const endYear = Number(endDate.slice(0, 4));
    if (endYear !== startYear + 1) return setError(`Ngày kết thúc phải thuộc năm ${startYear + 1}.`);
    if (!editingId && !gradeConfigTemplateId) return setError('Hãy tạo và chọn mẫu cấu hình đầu điểm trước khi tạo năm học.');
    setSaving(true); setError(''); setMessage('');
    try {
      if (editingId) await updateAcademicYear(editingId, { startDate, endDate });
      else await createAcademicYear({ startDate, endDate, gradeConfigTemplateId: Number(gradeConfigTemplateId) });
      setMessage(editingId ? 'Đã cập nhật năm học.' : 'Đã tạo năm học và tự sinh hai học kỳ.');
      setEditingId(null); setStartDate(''); setEndDate('');
      if (!editingId) setShowYearForm(false);
      await loadYears(); onYearCreated?.();
    } catch (cause: any) { setError(cause.message || 'Không thể lưu năm học.'); }
    finally { setSaving(false); }
  }


  async function addSubject() {
    if (!subjectName.trim() || !subjectCode.trim()) return setError('Nhập đủ tên và mã môn học.');
    try {
      await createSubject({ name: subjectName.trim(), code: subjectCode.trim().toUpperCase() });
      setSubjectName(''); setSubjectCode(''); setShowSubjectForm(false); await loadCatalogs(); setMessage('Đã thêm môn học.');
    } catch (cause: any) { setError(cause.message || 'Không thể thêm môn học.'); }
  }

  async function saveAppliedConfig() {
    if (!selectedYearId) return setError('Chọn năm học cần cấu hình ở thanh trên.');
    if (selectedYearStatus === 'COMPLETED') return setError('Năm học đã hoàn tất nên không thể thay đổi cấu hình.');
    setSaving(true); setError('');
    try {
      await updateAcademicYearMasterData(selectedYearId, { subjectIds, shiftIds, periodIds });
      setMessage('Đã lưu danh mục áp dụng cho năm học.');
    } catch (cause: any) { setError(cause.message || 'Không thể lưu cấu hình.'); }
    finally { setSaving(false); }
  }

  const toggle = (items: number[], id: number, setter: (value: number[]) => void) =>
    setter(items.includes(id) ? items.filter(item => item !== id) : [...items, id]);

  const nextYear = startDate ? Number(startDate.slice(0, 4)) + 1 : null;
  const endDateMin = nextYear ? `${nextYear}-01-01` : undefined;
  const endDateMax = nextYear ? `${nextYear}-12-31` : undefined;

  function changeStartDate(value: string) {
    setStartDate(value);
    if (!value || !endDate) return;
    const expectedEndYear = Number(value.slice(0, 4)) + 1;
    if (Number(endDate.slice(0, 4)) !== expectedEndYear) setEndDate('');
  }

  function toggleShift(shiftId: number) {
    if (shiftIds.includes(shiftId)) {
      const removedPeriodIds = new Set(periods.filter(period => period.shiftId === shiftId).map(period => period.id));
      setShiftIds(current => current.filter(id => id !== shiftId));
      setPeriodIds(current => current.filter(id => !removedPeriodIds.has(id)));
      return;
    }
    const addedPeriodIds = periods.filter(period => period.shiftId === shiftId).map(period => period.id);
    setShiftIds(current => [...current, shiftId]);
    setPeriodIds(current => Array.from(new Set([...current, ...addedPeriodIds])));
  }

  const selectedYear = years.find(year => String(year.id) === selectedYearId);
  const selectedYearSemesters = selectedYear
    ? semesters.filter(semester => semester.academicYearId === selectedYear.id).sort((a, b) => a.order - b.order)
    : [];

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div><span className="eyebrow">Bước {tab === 'academic-years' ? '1' : '2'}</span><h1>{tab === 'academic-years' ? 'Năm học & học kỳ' : 'Danh mục nền tảng'}</h1></div>
        <div className="page-heading-actions">
          {tab === 'academic-years' && <button onClick={() => setShowYearForm(v => !v)}>{showYearForm ? '✕ Đóng' : '＋ Tạo năm học'}</button>}
          {tab === 'catalogs' && <button onClick={() => setShowSubjectForm(v => !v)}>{showSubjectForm ? '✕ Đóng' : '＋ Thêm môn học'}</button>}
        </div>
      </section>
      {error && <div className="notice error">{error}</div>}
      {message && <div className="notice success">{message}</div>}

      {tab === 'academic-years' ? (
        <div className="master-data-layout">
          <div className="table-responsive"><table><thead><tr><th>Năm học đang cấu hình</th><th>Thời gian</th><th>Học kỳ</th>{selectedYear?.status === 'DRAFT' && <th>Thao tác</th>}</tr></thead><tbody>
            {selectedYear && (
              <tr key={selectedYear.id}>
                <td><strong>{selectedYear.name}</strong></td>
                <td>{selectedYear.startDate}<br />{selectedYear.endDate}</td>
                <td>{selectedYearSemesters.length > 0 ? selectedYearSemesters.map(semester => <div key={semester.id}>{semester.name} · {semester.startDate} → {semester.endDate} · {STATUS_LABELS[semester.status] || semester.status}</div>) : 'Chưa có học kỳ.'}</td>
                {selectedYear.status === 'DRAFT' && <td><button className="secondary-button" onClick={() => { setEditingId(selectedYear.id); setStartDate(selectedYear.startDate); setEndDate(selectedYear.endDate); }}>Sửa</button></td>}
              </tr>
            )}
            {!selectedYear && <tr><td colSpan={3}>{selectedYearId ? 'Không tìm thấy dữ liệu của năm học đang chọn.' : 'Chọn năm học trên header để xem cấu hình.'}</td></tr>}
          </tbody></table></div>
          {(showYearForm || editingId) && <aside className="master-data-side"><div className="master-data-form-card">
            <h3>{editingId ? 'Sửa năm học DRAFT' : 'Tạo năm học'}</h3>
            <div className="form-group"><label>Ngày bắt đầu</label><input type="date" value={startDate} onChange={e => changeStartDate(e.target.value)} /></div>
            <div className="form-group"><label>Ngày kết thúc</label><input type="date" value={endDate} min={endDateMin} max={endDateMax} disabled={!startDate} onChange={e => setEndDate(e.target.value)} /><small className="input-desc">{nextYear ? `Chỉ chọn ngày trong năm ${nextYear}.` : 'Chọn ngày bắt đầu trước.'}</small></div>
            {!editingId && <label className="form-group">Mẫu cấu hình đầu điểm<select value={gradeConfigTemplateId} onChange={e => setGradeConfigTemplateId(e.target.value)}><option value="">Chưa có mẫu cấu hình</option>{gradeTemplates.map(template => <option key={template.id} value={template.id}>{template.name} · v{template.version}</option>)}</select><small className="input-desc">Quản lý mẫu tại tab “Cấu hình đầu điểm”.</small></label>}
            <button onClick={saveYear} disabled={saving || (!editingId && !gradeConfigTemplateId)}>{saving ? 'Đang lưu…' : editingId ? 'Lưu thay đổi' : 'Tạo năm học'}</button>
            <button type="button" className="secondary-button" onClick={() => { setEditingId(null); setStartDate(''); setEndDate(''); setShowYearForm(false); }}>Đóng</button>
            <small className="input-desc">Năm học chỉ được tạo sau khi cấu hình đầu điểm hợp lệ.</small>
          </div></aside>}
        </div>
      ) : (
        <>
          {!selectedYearId && <div className="notice warning">Chọn năm học ở thanh trên để đánh dấu danh mục áp dụng.</div>}
          {selectedYearId && selectedYearStatus === 'ACTIVE' && <div className="notice warning">Năm học đang hoạt động. Các thay đổi môn, ca và tiết sẽ áp dụng ngay sau khi lưu.</div>}
          {selectedYearId && selectedYearStatus === 'COMPLETED' && <div className="notice warning">Cấu hình của năm học đã hoàn tất và bị khóa.</div>}
          {showSubjectForm && <section className="form-grid" style={{ gridTemplateColumns: '2.5fr 1fr auto auto', alignItems: 'end' }}>
            <div className="form-group">
              <label>Tên môn học mới</label>
              <input value={subjectName} onChange={e => setSubjectName(e.target.value)} placeholder="Ví dụ: Toán" />
            </div>
            <div className="form-group">
              <label>Mã môn</label>
              <input value={subjectCode} onChange={e => setSubjectCode(e.target.value)} placeholder="TOAN" maxLength={10} />
            </div>
            <button onClick={addSubject} style={{ height: 39, minHeight: 39, whiteSpace: 'nowrap' }}>
              Thêm môn
            </button>
            <button type="button" className="secondary-button" onClick={() => { setShowSubjectForm(false); setSubjectName(''); setSubjectCode(''); }} style={{ height: 39, minHeight: 39 }}>Đóng</button>
          </section>}
          <div className="step-grid">
            <Catalog title="Môn áp dụng" items={subjects} selected={subjectIds} onToggle={id => toggle(subjectIds, id, setSubjectIds)} onDelete={id => {
              const item = subjects.find(s => s.id === id);
              if (item) setDeleteTarget({ id, name: item.name });
            }} />
            <ShiftPeriodCatalog shifts={shifts} periods={periods} selectedShiftIds={shiftIds} selectedPeriodIds={periodIds} onToggleShift={toggleShift} onTogglePeriod={id => toggle(periodIds, id, setPeriodIds)} />
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 24, padding: '16px 20px', borderTop: '1px solid var(--line)', background: 'white', borderRadius: '12px', boxShadow: '0 4px 12px rgba(24,38,61,.03)' }}>
            <button onClick={saveAppliedConfig} disabled={!selectedYearId || selectedYearStatus === 'COMPLETED' || saving}>
              {saving ? 'Đang lưu cấu hình…' : 'Lưu cấu hình năm học'}
            </button>
          </div>
        </>
      )}
      {deleteTarget && (
        <div className="modal-backdrop" style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          backgroundColor: 'rgba(0,0,0,0.4)', display: 'flex',
          alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          backdropFilter: 'blur(4px)'
        }}>
          <div className="modal-content" style={{
            background: 'white', padding: '24px', borderRadius: '16px',
            width: 'min(90%, 400px)', boxShadow: '0 20px 48px rgba(0,0,0,0.15)'
          }}>
            <h3 style={{ marginTop: 0, fontSize: 18, color: 'var(--navy)' }}>Xác nhận xóa</h3>
            <p style={{ fontSize: 14, color: 'var(--muted)', margin: '12px 0 24px', lineHeight: 1.5 }}>
              Bạn có chắc chắn muốn xóa môn học <strong>{deleteTarget.name}</strong> chứ? Thao tác này không thể hoàn tác.
            </p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
              <button
                type="button"
                onClick={() => setDeleteTarget(null)}
                style={{
                  border: '1px solid var(--line)',
                  background: '#f1f3f5',
                  color: '#495057',
                  padding: '10px 16px',
                  borderRadius: '8px',
                  fontWeight: 650,
                  fontSize: 13,
                  cursor: 'pointer'
                }}
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={async () => {
                  const targetId = deleteTarget.id;
                  setDeleteTarget(null);
                  try {
                    await deleteSubject(targetId);
                    await loadCatalogs();
                    setMessage('Đã xóa môn học.');
                  } catch (cause: any) {
                    setError(cause.message || 'Không thể xóa môn học.');
                  }
                }}
                style={{
                  border: '1px solid #dc3545',
                  background: '#dc3545',
                  color: 'white',
                  padding: '10px 16px',
                  borderRadius: '8px',
                  fontWeight: 650,
                  fontSize: 13,
                  cursor: 'pointer'
                }}
              >
                Xóa
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function Catalog({ title, items, selected, onToggle, onDelete, readOnly = false }: { title: string; items: CatalogItem[]; selected: number[]; onToggle?: (id: number) => void; onDelete?: (id: number) => void; readOnly?: boolean }) {
  return <section className="step-card" style={{ display: 'block' }}><h3 style={{ marginTop: 0 }}>{title}</h3><div style={{ display: 'grid', gap: 8 }}>{items.map(item => <label key={item.id} style={{ display: 'flex', gap: 9, alignItems: 'center', fontSize: 13 }}><input type="checkbox" checked={selected.includes(item.id)} disabled={readOnly} onChange={() => onToggle?.(item.id)} /><span style={{ flex: 1 }}><strong>{item.name}</strong>{item.code && <small style={{ display: 'block', color: '#687386' }}>{item.code}{item.shiftName ? ` · ${item.shiftName}` : ''}</small>}</span>{onDelete && <button className="danger" onClick={(event) => { event.preventDefault(); onDelete(item.id); }}>Xóa</button>}</label>)}{items.length === 0 && <span className="input-desc">Chưa có dữ liệu.</span>}</div></section>;
}

function ShiftPeriodCatalog({ shifts, periods, selectedShiftIds, selectedPeriodIds, onToggleShift, onTogglePeriod }: {
  shifts: CatalogItem[];
  periods: CatalogItem[];
  selectedShiftIds: number[];
  selectedPeriodIds: number[];
  onToggleShift: (id: number) => void;
  onTogglePeriod: (id: number) => void;
}) {
  return <section className="step-card" style={{ display: 'block' }}>
    <h3 style={{ marginTop: 0 }}>Cấu hình Ca & Tiết học</h3>
    <p className="input-desc" style={{ marginBottom: 16 }}>Chọn Ca học để áp dụng cho năm học, sau đó chọn các Tiết học tương ứng thuộc ca đó.</p>
    <ShiftPeriodSelector
      shifts={shifts}
      periods={periods.filter((period): period is CatalogItem & { shiftId: number } => period.shiftId !== undefined)}
      selectedShiftIds={selectedShiftIds}
      selectedPeriodIds={selectedPeriodIds}
      onToggleShift={onToggleShift}
      onTogglePeriod={onTogglePeriod}
    />
  </section>;
}
