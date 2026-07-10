import { useEffect, useState } from 'react';
import { createAcademicYear, getAcademicYears, updateAcademicYear } from '../api/academicYear';
import { getAcademicYearMasterData, updateAcademicYearMasterData } from '../api/academicYearConfig';
import { getGradeLevels, getPeriods, getShifts, initializeMasterData } from '../api/masterData';
import { createSubject, deleteSubject, getSubjects } from '../api/subject';
import { getSemesters } from '../api/semester';

type TabKey = 'academic-years' | 'catalogs';
interface Props { initialTab?: TabKey; selectedYearId?: string; selectedYearStatus?: string; onYearCreated?: () => void; }
interface Year { id: number; name: string; startDate: string; endDate: string; status: string; }
interface Semester { id: number; academicYearId: number; name: string; order: number; startDate: string; endDate: string; status: string; }
interface CatalogItem { id: number; name: string; code?: string; order?: number; shiftName?: string; }

export default function MasterDataPage({ initialTab = 'catalogs', selectedYearId, selectedYearStatus, onYearCreated }: Props) {
  const [tab, setTab] = useState<TabKey>(initialTab);
  const [years, setYears] = useState<Year[]>([]);
  const [semesters, setSemesters] = useState<Semester[]>([]);
  const [gradeLevels, setGradeLevels] = useState<CatalogItem[]>([]);
  const [subjects, setSubjects] = useState<CatalogItem[]>([]);
  const [shifts, setShifts] = useState<CatalogItem[]>([]);
  const [periods, setPeriods] = useState<CatalogItem[]>([]);
  const [subjectIds, setSubjectIds] = useState<number[]>([]);
  const [shiftIds, setShiftIds] = useState<number[]>([]);
  const [periodIds, setPeriodIds] = useState<number[]>([]);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [subjectName, setSubjectName] = useState('');
  const [subjectCode, setSubjectCode] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  async function loadYears() {
    const [yearData, semesterData] = await Promise.all([getAcademicYears(), getSemesters()]);
    setYears((yearData || []) as Year[]);
    setSemesters((semesterData || []) as Semester[]);
  }

  async function loadCatalogs() {
    const [gradeData, subjectData, shiftData, periodData] = await Promise.all([
      getGradeLevels(), getSubjects(), getShifts(), getPeriods(),
    ]);
    setGradeLevels(gradeData || []);
    setSubjects(subjectData || []);
    setShifts(shiftData || []);
    setPeriods(periodData || []);
  }

  useEffect(() => { setTab(initialTab); }, [initialTab]);
  useEffect(() => { tab === 'academic-years' ? loadYears() : loadCatalogs(); }, [tab]);
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
    setSaving(true); setError(''); setMessage('');
    try {
      if (editingId) await updateAcademicYear(editingId, { startDate, endDate });
      else await createAcademicYear({ startDate, endDate });
      setMessage(editingId ? 'Đã cập nhật năm học.' : 'Đã tạo năm học và tự sinh hai học kỳ.');
      setEditingId(null); setStartDate(''); setEndDate('');
      await loadYears(); onYearCreated?.();
    } catch (cause: any) { setError(cause.message || 'Không thể lưu năm học.'); }
    finally { setSaving(false); }
  }

  async function seedCatalogs() {
    setSaving(true); setError('');
    try { await initializeMasterData(); await loadCatalogs(); setMessage('Đã khởi tạo danh mục chuẩn.'); }
    catch (cause: any) { setError(cause.message || 'Không thể khởi tạo danh mục.'); }
    finally { setSaving(false); }
  }

  async function addSubject() {
    if (!subjectName.trim() || !subjectCode.trim()) return setError('Nhập đủ tên và mã môn học.');
    try {
      await createSubject({ name: subjectName.trim(), code: subjectCode.trim().toUpperCase() });
      setSubjectName(''); setSubjectCode(''); await loadCatalogs(); setMessage('Đã thêm môn học.');
    } catch (cause: any) { setError(cause.message || 'Không thể thêm môn học.'); }
  }

  async function saveAppliedConfig() {
    if (!selectedYearId) return setError('Chọn năm học cần cấu hình ở thanh trên.');
    if (selectedYearStatus !== 'DRAFT') return setError('Chỉ năm học DRAFT mới được thay đổi cấu hình.');
    setSaving(true); setError('');
    try {
      await updateAcademicYearMasterData(selectedYearId, { subjectIds, shiftIds, periodIds });
      setMessage('Đã lưu danh mục áp dụng cho năm học.');
    } catch (cause: any) { setError(cause.message || 'Không thể lưu cấu hình.'); }
    finally { setSaving(false); }
  }

  const toggle = (items: number[], id: number, setter: (value: number[]) => void) =>
    setter(items.includes(id) ? items.filter(item => item !== id) : [...items, id]);

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div><span className="eyebrow">Bước {tab === 'academic-years' ? '1' : '2'}</span><h1>{tab === 'academic-years' ? 'Năm học & học kỳ' : 'Danh mục nền tảng'}</h1><p>{tab === 'academic-years' ? 'Mỗi năm học bắt đầu ở DRAFT với hai học kỳ.' : 'Master Data dùng chung; chỉ cấu hình phần áp dụng theo từng năm.'}</p></div>
      </section>
      {error && <div className="notice error">{error}</div>}
      {message && <div className="notice success">{message}</div>}

      {tab === 'academic-years' ? (
        <div className="master-data-layout">
          <div className="table-responsive"><table><thead><tr><th>Năm học</th><th>Thời gian</th><th>Học kỳ</th><th>Trạng thái</th><th></th></tr></thead><tbody>
            {[...years].sort((a,b) => b.id-a.id).map(year => (
              <tr key={year.id}><td><strong>{year.name}</strong></td><td>{year.startDate}<br/>{year.endDate}</td><td>{semesters.filter(s => s.academicYearId === year.id).sort((a,b)=>a.order-b.order).map(s => <div key={s.id}>{s.name} · {s.status}</div>)}</td><td><span className={`badge-status ${year.status === 'ACTIVE' ? 'active' : year.status === 'COMPLETED' ? 'completed' : 'preparing'}`}>{year.status}</span></td><td>{year.status === 'DRAFT' && <button className="secondary-button" onClick={() => { setEditingId(year.id); setStartDate(year.startDate); setEndDate(year.endDate); }}>Sửa</button>}</td></tr>
            ))}
            {years.length === 0 && <tr><td colSpan={5}>Chưa có năm học.</td></tr>}
          </tbody></table></div>
          <aside className="master-data-side"><div className="master-data-form-card"><h3>{editingId ? 'Sửa năm học DRAFT' : 'Tạo năm học'}</h3><div className="form-group"><label>Ngày bắt đầu</label><input type="date" value={startDate} onChange={e=>setStartDate(e.target.value)}/></div><div className="form-group"><label>Ngày kết thúc</label><input type="date" value={endDate} onChange={e=>setEndDate(e.target.value)}/></div><button onClick={saveYear} disabled={saving}>{saving ? 'Đang lưu…' : editingId ? 'Lưu thay đổi' : 'Tạo năm học'}</button>{editingId && <button className="secondary-button" onClick={()=>{setEditingId(null);setStartDate('');setEndDate('');}}>Hủy</button>}<small className="input-desc">Hệ thống tự tạo Học kỳ 1 và Học kỳ 2.</small></div></aside>
        </div>
      ) : (
        <>
          <div className="page-actions"><button className="secondary-button" onClick={seedCatalogs} disabled={saving}>Khởi tạo danh mục chuẩn</button><button onClick={saveAppliedConfig} disabled={!selectedYearId || selectedYearStatus !== 'DRAFT' || saving}>Lưu cấu hình năm học</button></div>
          {!selectedYearId && <div className="notice warning">Chọn năm học ở thanh trên để đánh dấu danh mục áp dụng.</div>}
          {selectedYearId && selectedYearStatus !== 'DRAFT' && <div className="notice warning">Cấu hình của năm học đã được khóa.</div>}
          <section className="form-grid"><div className="form-group"><label>Tên môn học mới</label><input value={subjectName} onChange={e=>setSubjectName(e.target.value)} placeholder="Ví dụ: Toán"/></div><div className="form-group"><label>Mã môn</label><input value={subjectCode} onChange={e=>setSubjectCode(e.target.value)} placeholder="TOAN"/></div><div className="form-group" style={{alignSelf:'end'}}><button onClick={addSubject}>Thêm môn</button></div></section>
          <div className="step-grid">
            <Catalog title="Khối lớp dùng chung" items={gradeLevels} selected={gradeLevels.map(i=>i.id)} readOnly />
            <Catalog title="Môn áp dụng" items={subjects} selected={subjectIds} onToggle={id=>toggle(subjectIds,id,setSubjectIds)} onDelete={async id=>{await deleteSubject(id);await loadCatalogs();}} />
            <Catalog title="Ca học áp dụng" items={shifts} selected={shiftIds} onToggle={id=>toggle(shiftIds,id,setShiftIds)} />
            <Catalog title="Tiết học áp dụng" items={periods} selected={periodIds} onToggle={id=>toggle(periodIds,id,setPeriodIds)} />
          </div>
        </>
      )}
    </div>
  );
}

function Catalog({ title, items, selected, onToggle, onDelete, readOnly = false }: { title: string; items: CatalogItem[]; selected: number[]; onToggle?: (id:number)=>void; onDelete?: (id:number)=>void; readOnly?: boolean }) {
  return <section className="step-card" style={{display:'block'}}><h3 style={{marginTop:0}}>{title}</h3><div style={{display:'grid',gap:8}}>{items.map(item=><label key={item.id} style={{display:'flex',gap:9,alignItems:'center',fontSize:13}}><input type="checkbox" checked={selected.includes(item.id)} disabled={readOnly} onChange={()=>onToggle?.(item.id)}/><span style={{flex:1}}><strong>{item.name}</strong>{item.code && <small style={{display:'block',color:'#687386'}}>{item.code}{item.shiftName ? ` · ${item.shiftName}` : ''}</small>}</span>{onDelete && <button className="danger" onClick={(event)=>{event.preventDefault();onDelete(item.id);}}>Xóa</button>}</label>)}{items.length===0&&<span className="input-desc">Chưa có dữ liệu.</span>}</div></section>;
}
