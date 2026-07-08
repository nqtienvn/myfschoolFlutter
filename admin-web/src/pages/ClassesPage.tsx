import { useState, useEffect, useRef } from 'react';
import { apiFetch } from '../api/client';

interface AcademicYearItem { id: number; name: string; status: string; }
interface ClassItem { id: number; name: string; gradeLevel: number; academicYearId: number; academicYearName: string; }

export default function ClassesPage() {
  const [academicYears, setAcademicYears] = useState<AcademicYearItem[]>([]);
  const [academicYearId, setAcademicYearId] = useState('');
  const [items, setItems] = useState<ClassItem[]>([]);
  const [activeTab, setActiveTab] = useState<'manual' | 'generator' | 'csv'>('manual');
  const [error, setError] = useState('');
  const [progress, setProgress] = useState({ active: false, current: 0, total: 0, label: '' });
  const [manualName, setManualName] = useState('');
  const [manualGrade, setManualGrade] = useState(10);
  const [genPrefix, setGenPrefix] = useState('A');
  const [genCount, setGenCount] = useState(5);
  const [genGrade, setGenGrade] = useState(10);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => { fetchAcademicYears(); }, []);
  useEffect(() => { if (academicYearId) fetchItems(); }, [academicYearId]);

  async function fetchAcademicYears() {
    const data = await apiFetch('/academic-years') as AcademicYearItem[];
    setAcademicYears(data);
    const active = data.find(y => y.status === 'ACTIVE') || data[0];
    if (active) setAcademicYearId(String(active.id));
  }

  async function fetchItems() {
    try {
      const data = await apiFetch(`/classes?academicYearId=${academicYearId}&page=0&size=100`);
      setItems(data.content || []);
    } catch (err: any) {
      setError(err.message || 'Không thể tải danh sách lớp học');
    }
  }

  function requireYear() {
    if (!academicYearId) {
      setError('Vui lòng chọn năm học.');
      return false;
    }
    return true;
  }

  async function handleManualCreate() {
    setError('');
    if (!requireYear()) return;
    if (!manualName.trim()) return setError('Tên lớp không được để trống.');
    if (manualName.length > 20) return setError('Tên lớp không được dài quá 20 ký tự.');
    if (isNaN(manualGrade) || manualGrade < 1 || manualGrade > 12) return setError('Khối lớp phải là số nguyên từ 1 đến 12.');

    try {
      await apiFetch('/classes', {
        method: 'POST',
        body: JSON.stringify({ name: manualName.trim(), gradeLevel: manualGrade, academicYearId: +academicYearId, schoolName: 'FPT Schools' }),
      });
      setManualName('');
      fetchItems();
    } catch (err: any) {
      setError(err.message || 'Lỗi lưu thông tin lớp học');
    }
  }

  async function handleAutoGenerate() {
    setError('');
    if (!requireYear()) return;
    if (!genPrefix.trim()) return setError('Tiền tố tên lớp không được để trống (Ví dụ: A, B, C).');
    if (isNaN(genCount) || genCount < 1 || genCount > 30) return setError('Số lượng lớp cần tạo phải từ 1 đến 30.');
    if (isNaN(genGrade) || genGrade < 1 || genGrade > 12) return setError('Khối lớp phải là số nguyên từ 1 đến 12.');

    setProgress({ active: true, current: 0, total: genCount, label: 'Chuẩn bị tạo...' });
    try {
      for (let i = 1; i <= genCount; i++) {
        const className = `${genGrade}${genPrefix.trim().toUpperCase()}${i}`;
        setProgress(p => ({ ...p, current: i - 1, label: `Đang tạo lớp ${className} (${i}/${genCount})...` }));
        await apiFetch('/classes', {
          method: 'POST',
          body: JSON.stringify({ name: className, gradeLevel: genGrade, academicYearId: +academicYearId, schoolName: 'FPT Schools' }),
        });
      }
      setProgress(p => ({ ...p, current: genCount, label: 'Đã hoàn thành tạo lớp học!' }));
      setTimeout(() => setProgress(p => ({ ...p, active: false })), 2000);
      fetchItems();
    } catch (err: any) {
      setError(`Lỗi trong quá trình tạo lớp: ${err.message || 'Lỗi không xác định'}`);
      setProgress(p => ({ ...p, active: false }));
    }
  }

  function handleCsvFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) processCsvFile(file);
  }

  function processCsvFile(file: File) {
    setError('');
    if (!requireYear()) return;
    if (!file.name.endsWith('.csv')) return setError('Chỉ chấp nhận tệp định dạng CSV (.csv)');

    const reader = new FileReader();
    reader.onload = async (event) => {
      const text = event.target?.result as string;
      const lines = (text || '').split('\n').map(l => l.trim()).filter(Boolean);
      if (lines.length === 0) return setError('Tệp không chứa dữ liệu.');

      let startIndex = 0;
      const firstLineParts = lines[0].split(',');
      if (isNaN(Number(firstLineParts[1]?.trim()))) startIndex = 1;

      const parsedClasses: { name: string; gradeLevel: number }[] = [];
      for (let i = startIndex; i < lines.length; i++) {
        const parts = lines[i].split(',').map(p => p.trim());
        if (parts.length < 2) return setError(`Lỗi định dạng dòng số ${i + 1}: Thiếu cột thông tin.`);
        const name = parts[0];
        const gradeLevel = parseInt(parts[1]);
        if (!name) return setError(`Lỗi dòng ${i + 1}: Tên lớp không được để trống.`);
        if (name.length > 20) return setError(`Lỗi dòng ${i + 1}: Tên lớp quá 20 ký tự.`);
        if (isNaN(gradeLevel) || gradeLevel < 1 || gradeLevel > 12) return setError(`Lỗi dòng ${i + 1}: Khối lớp '${parts[1]}' không hợp lệ (phải từ 1-12).`);
        parsedClasses.push({ name, gradeLevel });
      }
      if (parsedClasses.length === 0) return setError('Không tìm thấy bản ghi lớp học nào hợp lệ để tạo.');

      setProgress({ active: true, current: 0, total: parsedClasses.length, label: 'Bắt đầu tải danh sách...' });
      try {
        for (let idx = 0; idx < parsedClasses.length; idx++) {
          const c = parsedClasses[idx];
          setProgress(p => ({ ...p, current: idx, label: `Đang import: ${c.name} (${idx + 1}/${parsedClasses.length})...` }));
          await apiFetch('/classes', {
            method: 'POST',
            body: JSON.stringify({ name: c.name, gradeLevel: c.gradeLevel, academicYearId: +academicYearId, schoolName: 'FPT Schools' })
          });
        }
        setProgress(p => ({ ...p, current: parsedClasses.length, label: 'Đã nhập thành công toàn bộ lớp học!' }));
        setTimeout(() => setProgress(p => ({ ...p, active: false })), 2000);
        fetchItems();
      } catch (err: any) {
        setError(`Lỗi import: ${err.message || 'Lỗi không xác định'}`);
        setProgress(p => ({ ...p, active: false }));
      }
    };
    reader.onerror = () => setError('Đọc tệp tin thất bại.');
    reader.readAsText(file);
  }

  function downloadCsvTemplate() {
    const content = 'Tên lớp,Khối\n10A1,10\n10A2,10\n11B1,11\n';
    const link = document.createElement('a');
    link.setAttribute('href', 'data:text/csv;charset=utf-8,' + encodeURIComponent(content));
    link.setAttribute('download', 'template_lophoc.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  async function deleteItem(id: number) {
    if (!confirm('Xóa lớp này?')) return;
    try {
      await apiFetch(`/classes/${id}`, { method: 'DELETE' });
      fetchItems();
    } catch (err: any) {
      alert(err.message);
    }
  }

  return (
    <div>
      <h2>Quản lý lớp học</h2>
      <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))' }}>
        <div className="form-group">
          <label>Năm học</label>
          <select value={academicYearId} onChange={e => setAcademicYearId(e.target.value)}>
            <option value="">Chọn năm học</option>
            {academicYears.map(y => <option key={y.id} value={y.id}>{y.name} {y.status === 'ACTIVE' ? '(Đang mở)' : ''}</option>)}
          </select>
          <span className="input-desc">Năm học áp dụng cho danh sách lớp</span>
        </div>
      </div>

      <div className="tabs-container">
        <button className={`tab-btn ${activeTab === 'manual' ? 'active' : ''}`} onClick={() => { setActiveTab('manual'); setError(''); }}>Tạo thủ công</button>
        <button className={`tab-btn ${activeTab === 'generator' ? 'active' : ''}`} onClick={() => { setActiveTab('generator'); setError(''); }}>Tự động sinh lớp</button>
        <button className={`tab-btn ${activeTab === 'csv' ? 'active' : ''}`} onClick={() => { setActiveTab('csv'); setError(''); }}>Import từ CSV</button>
      </div>

      {error && <div className="error">{error}</div>}
      {progress.active && (
        <div className="progress-container">
          <div>{progress.label}</div>
          <div className="progress-bar-bg"><div className="progress-bar-fill" style={{ width: `${progress.total ? (progress.current / progress.total) * 100 : 0}%` }} /></div>
          <div className="progress-text">Đã hoàn thành: {progress.current} / {progress.total} lớp</div>
        </div>
      )}

      {activeTab === 'manual' && !progress.active && (
        <div className="form-grid">
          <div className="form-group"><label>Tên lớp</label><input placeholder="VD: 10A1, 12C3..." value={manualName} onChange={e => setManualName(e.target.value)} /><span className="input-desc">Chữ và số viết liền, tối đa 20 ký tự</span></div>
          <div className="form-group"><label>Khối lớp</label><input type="number" value={manualGrade} onChange={e => setManualGrade(+e.target.value)} /><span className="input-desc">Giá trị số nguyên từ 1 đến 12</span></div>
          <div className="form-group"><label style={{ visibility: 'hidden' }}>Thao tác</label><button onClick={handleManualCreate} style={{ width: '100%', height: '38px' }}>Tạo lớp</button><span className="input-desc" style={{ visibility: 'hidden' }}>&nbsp;</span></div>
        </div>
      )}

      {activeTab === 'generator' && !progress.active && (
        <div className="form-grid">
          <div className="form-group"><label>Ký tự lớp</label><input placeholder="VD: A, B, C..." value={genPrefix} onChange={e => setGenPrefix(e.target.value)} /><span className="input-desc">Chữ cái đại diện lớp</span></div>
          <div className="form-group"><label>Số lượng lớp cần tạo</label><input type="number" value={genCount} onChange={e => setGenCount(+e.target.value)} /><span className="input-desc">Giới hạn sinh từ 1 đến 30 lớp</span></div>
          <div className="form-group"><label>Khối lớp</label><input type="number" value={genGrade} onChange={e => setGenGrade(+e.target.value)} /><span className="input-desc">Tên lớp sẽ bắt đầu bằng số khối này</span></div>
          <div className="form-group" style={{ gridColumn: 'span 4', display: 'flex', alignItems: 'flex-end' }}><button onClick={handleAutoGenerate} style={{ width: '100%', height: '40px' }}>Bắt đầu sinh lớp hàng loạt</button></div>
        </div>
      )}

      {activeTab === 'csv' && !progress.active && (
        <div>
          <input type="file" ref={fileInputRef} style={{ display: 'none' }} accept=".csv" onChange={handleCsvFileSelect} />
          <div className="file-upload-zone" onClick={() => fileInputRef.current?.click()}>
            <p>Bấm vào đây để chọn tệp CSV danh sách lớp học của trường</p>
            <span>Định dạng: .csv (Tên lớp, Khối). Năm học lấy từ dropdown.</span>
            <div className="csv-template-link" onClick={(e) => { e.stopPropagation(); downloadCsvTemplate(); }}>Tải tệp mẫu tại đây (.csv)</div>
          </div>
        </div>
      )}

      <table>
        <thead><tr><th>ID</th><th>Tên lớp</th><th>Khối</th><th>Năm học</th><th>Thao tác</th></tr></thead>
        <tbody>
          {items.map(c => <tr key={c.id}><td>{c.id}</td><td>{c.name}</td><td>{c.gradeLevel}</td><td>{c.academicYearName}</td><td><button className="danger" onClick={() => deleteItem(c.id)}>Xóa</button></td></tr>)}
        </tbody>
      </table>
    </div>
  );
}
