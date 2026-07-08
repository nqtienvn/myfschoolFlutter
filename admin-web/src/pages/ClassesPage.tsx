import { useState, useEffect, useRef } from 'react';
import { apiFetch } from '../api/client';

interface ClassItem { 
  id: number; 
  name: string; 
  gradeLevel: number; 
  academicYear: string; 
}

export default function ClassesPage() {
  const [items, setItems] = useState<ClassItem[]>([]);
  const [activeTab, setActiveTab] = useState<'manual' | 'generator' | 'csv'>('manual');
  const [error, setError] = useState('');
  
  // Progress Tracker State
  const [progress, setProgress] = useState<{ active: boolean; current: number; total: number; label: string }>({
    active: false,
    current: 0,
    total: 0,
    label: '',
  });

  // Form State: Manual
  const [manualName, setManualName] = useState('');
  const [manualGrade, setManualGrade] = useState(10);
  const [manualYear, setManualYear] = useState('2026-2027');

  // Form State: Auto-Generator
  const [genPrefix, setGenPrefix] = useState('A');
  const [genCount, setGenCount] = useState(5);
  const [genGrade, setGenGrade] = useState(10);
  const [genYear, setGenYear] = useState('2026-2027');

  // Form State: CSV
  const fileInputRef = useRef<HTMLInputElement>(null);

  async function fetchItems() {
    try {
      const data = await apiFetch('/classes?page=0&size=100');
      setItems(data.content || []);
    } catch (err: any) { 
      setError(err.message || 'Không thể tải danh sách lớp học'); 
    }
  }

  useEffect(() => { 
    fetchItems(); 
  }, []);

  // --- 1. Tạo thủ công (Manual Create) ---
  async function handleManualCreate() {
    setError('');
    
    if (!manualName.trim()) {
      setError('Tên lớp không được để trống.');
      return;
    }
    if (manualName.length > 20) {
      setError('Tên lớp không được dài quá 20 ký tự.');
      return;
    }
    if (isNaN(manualGrade) || manualGrade < 1 || manualGrade > 12) {
      setError('Khối lớp phải là số nguyên từ 1 đến 12.');
      return;
    }
    if (!manualYear.trim()) {
      setError('Năm học không được để trống.');
      return;
    }
    if (manualYear.length > 9) {
      setError('Năm học không được dài quá 9 ký tự (Ví dụ: 2025-2026).');
      return;
    }

    try {
      await apiFetch('/classes', {
        method: 'POST',
        body: JSON.stringify({ 
          name: manualName.trim(), 
          gradeLevel: manualGrade, 
          academicYear: manualYear.trim(), 
          schoolName: 'FPT Schools' 
        }),
      });
      setManualName(''); 
      fetchItems();
    } catch (err: any) { 
      setError(err.message || 'Lỗi lưu thông tin lớp học'); 
    }
  }

  // --- 2. Tự động sinh lớp (Auto Generator) ---
  async function handleAutoGenerate() {
    setError('');
    
    // Validations
    if (!genPrefix.trim()) {
      setError('Tiền tố tên lớp không được để trống (Ví dụ: A, B, C).');
      return;
    }
    if (isNaN(genCount) || genCount < 1 || genCount > 30) {
      setError('Số lượng lớp cần tạo phải từ 1 đến 30.');
      return;
    }
    if (isNaN(genGrade) || genGrade < 1 || genGrade > 12) {
      setError('Khối lớp phải là số nguyên từ 1 đến 12.');
      return;
    }
    if (!genYear.trim()) {
      setError('Năm học không được để trống.');
      return;
    }
    if (genYear.length > 9) {
      setError('Năm học không được dài quá 9 ký tự.');
      return;
    }

    setProgress({ active: true, current: 0, total: genCount, label: 'Chuẩn bị tạo...' });

    try {
      for (let i = 1; i <= genCount; i++) {
        const className = `${genGrade}${genPrefix.trim().toUpperCase()}${i}`;
        
        setProgress(p => ({ 
          ...p, 
          current: i - 1, 
          label: `Đang tạo lớp ${className} (${i}/${genCount})...` 
        }));

        await apiFetch('/classes', {
          method: 'POST',
          body: JSON.stringify({ 
            name: className, 
            gradeLevel: genGrade, 
            academicYear: genYear.trim(), 
            schoolName: 'FPT Schools' 
          }),
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

  // --- 3. Import từ file CSV ---
  function handleCsvFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const files = e.target.files;
    if (!files || files.length === 0) return;
    processCsvFile(files[0]);
  }

  function processCsvFile(file: File) {
    setError('');
    if (!file.name.endsWith('.csv')) {
      setError('Chỉ chấp nhận tệp định dạng CSV (.csv)');
      return;
    }

    const reader = new FileReader();
    reader.onload = async (event) => {
      const text = event.target?.result as string;
      if (!text) {
        setError('Tệp CSV rỗng hoặc không đọc được.');
        return;
      }

      // Tách dòng
      const lines = text.split('\n').map(l => l.trim()).filter(l => l.length > 0);
      if (lines.length === 0) {
        setError('Tệp không chứa dữ liệu.');
        return;
      }

      // Check header nếu có (nếu dòng đầu chứa chữ, ta bỏ qua)
      let startIndex = 0;
      const firstLineParts = lines[0].split(',');
      if (isNaN(Number(firstLineParts[1].trim()))) {
        // Dòng đầu tiên cột 2 không phải là số -> đây là Header (Ví dụ: Tên lớp, Khối, Năm học)
        startIndex = 1;
      }

      const parsedClasses: { name: string; gradeLevel: number; academicYear: string }[] = [];
      
      // Parse & Validate trước khi gọi API
      for (let i = startIndex; i < lines.length; i++) {
        const parts = lines[i].split(',').map(p => p.trim());
        if (parts.length < 3) {
          setError(`Lỗi định dạng dòng số ${i + 1}: Thiếu cột thông tin.`);
          return;
        }

        const name = parts[0];
        const gradeLevel = parseInt(parts[1]);
        const academicYear = parts[2];

        if (!name) {
          setError(`Lỗi dòng ${i + 1}: Tên lớp không được để trống.`);
          return;
        }
        if (name.length > 20) {
          setError(`Lỗi dòng ${i + 1}: Tên lớp quá 20 ký tự.`);
          return;
        }
        if (isNaN(gradeLevel) || gradeLevel < 1 || gradeLevel > 12) {
          setError(`Lỗi dòng ${i + 1}: Khối lớp '${parts[1]}' không hợp lệ (phải từ 1-12).`);
          return;
        }
        if (!academicYear || academicYear.length > 9) {
          setError(`Lỗi dòng ${i + 1}: Năm học '${academicYear}' quá 9 ký tự.`);
          return;
        }

        parsedClasses.push({ name, gradeLevel, academicYear });
      }

      if (parsedClasses.length === 0) {
        setError('Không tìm thấy bản ghi lớp học nào hợp lệ để tạo.');
        return;
      }

      // Bắt đầu đẩy lên API tuần tự
      const total = parsedClasses.length;
      setProgress({ active: true, current: 0, total, label: 'Bắt đầu tải danh sách...' });

      try {
        for (let idx = 0; idx < total; idx++) {
          const c = parsedClasses[idx];
          
          setProgress(p => ({ 
            ...p, 
            current: idx, 
            label: `Đang import: ${c.name} (${idx + 1}/${total})...` 
          }));

          await apiFetch('/classes', {
            method: 'POST',
            body: JSON.stringify({
              name: c.name,
              gradeLevel: c.gradeLevel,
              academicYear: c.academicYear,
              schoolName: 'FPT Schools'
            })
          });
        }

        setProgress(p => ({ ...p, current: total, label: 'Đã nhập thành công toàn bộ lớp học!' }));
        setTimeout(() => setProgress(p => ({ ...p, active: false })), 2000);
        fetchItems();
      } catch (err: any) {
        setError(`Lỗi import tại bản ghi thứ ${progress.current + 1}: ${err.message || 'Lỗi không xác định'}`);
        setProgress(p => ({ ...p, active: false }));
      }
    };

    reader.onerror = () => {
      setError('Đọc tệp tin thất bại.');
    };

    reader.readAsText(file);
  }

  function downloadCsvTemplate() {
    const headers = 'Tên lớp,Khối,Năm học\n';
    const row1 = '10A1,10,2025-2026\n';
    const row2 = '10A2,10,2025-2026\n';
    const row3 = '11B1,11,2025-2026\n';
    
    const csvContent = 'data:text/csv;charset=utf-8,' + encodeURIComponent(headers + row1 + row2 + row3);
    const link = document.createElement('a');
    link.setAttribute('href', csvContent);
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
      
      {/* Tabs Menu */}
      <div className="tabs-container">
        <button 
          className={`tab-btn ${activeTab === 'manual' ? 'active' : ''}`}
          onClick={() => { setActiveTab('manual'); setError(''); }}
        >
          Tạo thủ công
        </button>
        <button 
          className={`tab-btn ${activeTab === 'generator' ? 'active' : ''}`}
          onClick={() => { setActiveTab('generator'); setError(''); }}
        >
          Tự động sinh lớp
        </button>
        <button 
          className={`tab-btn ${activeTab === 'csv' ? 'active' : ''}`}
          onClick={() => { setActiveTab('csv'); setError(''); }}
        >
          Import từ CSV
        </button>
      </div>

      {error && <div className="error">{error}</div>}

      {/* Progress Tracker UI */}
      {progress.active && (
        <div className="progress-container">
          <div>{progress.label}</div>
          <div className="progress-bar-bg">
            <div 
              className="progress-bar-fill" 
              style={{ width: `${(progress.current / progress.total) * 100}%` }}
            ></div>
          </div>
          <div className="progress-text">Đã hoàn thành: {progress.current} / {progress.total} lớp</div>
        </div>
      )}

      {/* Form Content: 1. Manual */}
      {activeTab === 'manual' && !progress.active && (
        <div className="form-grid">
          <div className="form-group">
            <label>Tên lớp</label>
            <input 
              placeholder="VD: 10A1, 12C3..." 
              value={manualName} 
              onChange={e => setManualName(e.target.value)} 
            />
            <span className="input-desc">Chữ và số viết liền, tối đa 20 ký tự</span>
          </div>

          <div className="form-group">
            <label>Khối lớp</label>
            <input 
              type="number" 
              placeholder="Nhập số từ 1 đến 12" 
              value={manualGrade} 
              onChange={e => setManualGrade(+e.target.value)} 
            />
            <span className="input-desc">Giá trị số nguyên từ 1 đến 12</span>
          </div>

          <div className="form-group">
            <label>Năm học</label>
            <input 
              placeholder="VD: 2025-2026" 
              value={manualYear} 
              onChange={e => setManualYear(e.target.value)} 
            />
            <span className="input-desc">Định dạng YYYY-YYYY, tối đa 9 ký tự</span>
          </div>

          <div className="form-group">
            <label style={{ visibility: 'hidden' }}>Thao tác</label>
            <button onClick={handleManualCreate} style={{ width: '100%', height: '38px' }}>Tạo lớp</button>
            <span className="input-desc" style={{ visibility: 'hidden' }}>&nbsp;</span>
          </div>
        </div>
      )}

      {/* Form Content: 2. Auto-Generator */}
      {activeTab === 'generator' && !progress.active && (
        <div className="form-grid">
          <div className="form-group">
            <label>Ký tự lớp</label>
            <input 
              placeholder="VD: A, B, C..." 
              value={genPrefix} 
              onChange={e => setGenPrefix(e.target.value)} 
            />
            <span className="input-desc">Chữ cái đại diện lớp (VD: A, B)</span>
          </div>

          <div className="form-group">
            <label>Số lượng lớp cần tạo</label>
            <input 
              type="number" 
              placeholder="Nhập số lượng" 
              value={genCount} 
              onChange={e => setGenCount(+e.target.value)} 
            />
            <span className="input-desc">Giới hạn sinh từ 1 đến 30 lớp</span>
          </div>

          <div className="form-group">
            <label>Khối lớp</label>
            <input 
              type="number" 
              placeholder="Nhập số từ 1 đến 12" 
              value={genGrade} 
              onChange={e => setGenGrade(+e.target.value)} 
            />
            <span className="input-desc">Tên lớp sẽ bắt đầu bằng số khối này</span>
          </div>

          <div className="form-group">
            <label>Năm học</label>
            <input 
              placeholder="VD: 2026-2027" 
              value={genYear} 
              onChange={e => setGenYear(e.target.value)} 
            />
            <span className="input-desc">Định dạng YYYY-YYYY, tối đa 9 ký tự</span>
          </div>

          <div className="form-group" style={{ gridColumn: 'span 4', display: 'flex', alignItems: 'flex-end' }}>
            <button onClick={handleAutoGenerate} style={{ width: '100%', height: '40px' }}>
              Bắt đầu sinh lớp hàng loạt (Sinh các lớp: {genGrade}{genPrefix.toUpperCase()}1 → {genGrade}{genPrefix.toUpperCase()}{genCount || 1})
            </button>
          </div>
        </div>
      )}

      {/* Form Content: 3. Import CSV */}
      {activeTab === 'csv' && !progress.active && (
        <div>
          <input 
            type="file" 
            ref={fileInputRef} 
            style={{ display: 'none' }} 
            accept=".csv"
            onChange={handleCsvFileSelect}
          />
          <div 
            className="file-upload-zone"
            onClick={() => fileInputRef.current?.click()}
          >
            <p>Bấm vào đây để chọn tệp CSV danh sách lớp học của trường</p>
            <span>Định dạng tệp yêu cầu: .csv (Tên lớp, Khối, Năm học)</span>
            <div 
              className="csv-template-link"
              onClick={(e) => { 
                e.stopPropagation(); 
                downloadCsvTemplate(); 
              }}
            >
              Tải tệp mẫu tại đây (.csv)
            </div>
          </div>
        </div>
      )}

      {/* Classes Table */}
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Tên lớp</th>
            <th>Khối</th>
            <th>Năm học</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {items.map(c => (
            <tr key={c.id}>
              <td>{c.id}</td>
              <td>{c.name}</td>
              <td>{c.gradeLevel}</td>
              <td>{c.academicYear}</td>
              <td>
                <button className="danger" onClick={() => deleteItem(c.id)}>
                  Xóa
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
