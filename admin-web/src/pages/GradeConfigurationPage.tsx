import { useEffect, useState } from 'react';
import { applyGradeTemplateToYear, createGradeTemplate, getGradeTemplates, getYearGradeConfig, type GradeConfig, type GradeConfigItem } from '../api/gradeConfiguration';

const defaults: GradeConfigItem[] = [
  { code: 'TX', displayName: 'Thường xuyên', weight: 1, quantity: 2, entryRole: 'SUBJECT_TEACHER', assessmentType: 'SCORE', requiredEntry: true, displayOrder: 0 },
  { code: 'GK', displayName: 'Giữa kỳ', weight: 2, quantity: 1, entryRole: 'ADMIN', assessmentType: 'SCORE', requiredEntry: true, displayOrder: 1 },
  { code: 'CK', displayName: 'Cuối kỳ', weight: 3, quantity: 1, entryRole: 'ADMIN', assessmentType: 'SCORE', requiredEntry: true, displayOrder: 2 },
];

const assessmentLabel = (type: GradeConfigItem['assessmentType']) => ({
  SCORE: 'Điểm số',
  PASS_FAIL: 'Đạt / Chưa đạt',
  COMMENT: 'Nhận xét',
}[type]);

export default function GradeConfigurationPage({ selectedYearId, selectedYearStatus }: { selectedYearId: string; selectedYearStatus?: string }) {
  const [templates, setTemplates] = useState<GradeConfig[]>([]);
  const [name, setName] = useState('Cấu hình chuẩn');
  const [items, setItems] = useState<GradeConfigItem[]>(defaults);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [yearConfig, setYearConfig] = useState<GradeConfig | null>(null);

  const load = () => getGradeTemplates().then(setTemplates).catch((e: any) => setError(e.message));

  useEffect(() => {
    void load();
  }, []);

  useEffect(() => {
    setYearConfig(null);
    if (selectedYearId) {
      void getYearGradeConfig(selectedYearId).then(setYearConfig).catch(() => setYearConfig(null));
    }
  }, [selectedYearId]);

  const update = (index: number, patch: Partial<GradeConfigItem>) =>
    setItems(rows => rows.map((row, i) => i === index ? { ...row, ...patch } : row));

  async function save() {
    if (!name.trim() || items.length === 0) return setError('Nhập tên mẫu và ít nhất một đầu điểm.');
    setSaving(true);
    setError('');
    try {
      await createGradeTemplate(name.trim(), items.map((item, index) => ({
        ...item,
        displayOrder: index,
        code: item.code.trim().toUpperCase()
      })));
      setMessage('Đã lưu mẫu cấu hình đầu điểm.');
      setShowForm(false);
      setName('Cấu hình chuẩn');
      setItems(defaults);
      await load();
    } catch (e: any) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  async function apply(templateId: number) {
    if (!selectedYearId) return setError('Hãy chọn năm học ở header.');
    setSaving(true);
    setError('');
    try {
      const config = await applyGradeTemplateToYear(selectedYearId, templateId);
      setYearConfig(config);
      setMessage('Đã áp dụng mẫu cho năm học đang chọn. Hãy kiểm tra dữ liệu lại.');
    } catch (e: any) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div>
          <span className="eyebrow">Bước 2</span>
          <h1>Cấu hình đầu điểm</h1>
          <p>Tạo mẫu dùng lại cho nhiều năm học. Chỉ loại Điểm số tham gia tính điểm trung bình.</p>
        </div>
        <div className="page-heading-actions">
          <button type="button" onClick={() => setShowForm(v => !v)}>
            {showForm ? '✕ Đóng' : '＋ Tạo mẫu đầu điểm'}
          </button>
        </div>
      </section>

      {error && <div className="notice error">{error}</div>}
      {message && <div className="notice success">{message}</div>}

      {selectedYearId && (
        <div className={`notice ${yearConfig ? 'success' : 'warning'}`}>
          {yearConfig ? `Năm học đang chọn đã áp dụng mẫu “${yearConfig.name}”.` : 'Năm học đang chọn chưa có cấu hình đầu điểm. Chọn một mẫu bên dưới để áp dụng.'}
        </div>
      )}

      {showForm && (
        <section className="panel" style={{ padding: '24px', border: '1px solid var(--border)', borderRadius: '12px', background: '#fff', display: 'grid', gap: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: '1px solid var(--border)', paddingBottom: '12px', flexWrap: 'wrap', gap: '12px' }}>
            <h3 style={{ margin: 0, fontSize: '15px' }}>Tạo mẫu đầu điểm</h3>
            <label className="form-group" style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '10px', flexDirection: 'row' }}>
              <span style={{ fontWeight: 'bold', fontSize: '13px', whiteSpace: 'nowrap' }}>Tên mẫu:</span>
              <input value={name} onChange={e => setName(e.target.value)} style={{ width: '280px', padding: '6px 12px', height: '36px' }} />
            </label>
          </div>

          <div style={{ display: 'grid', gap: '12px' }}>
            {items.map((item, index) => (
              <div key={`${item.code}-${index}`} style={{ display: 'grid', gridTemplateColumns: '90px minmax(150px, 1.5fr) 150px 80px 80px minmax(150px, 1fr) 100px auto', gap: '12px', alignItems: 'end', padding: '12px 16px', background: '#f8f9fa', borderRadius: '8px', border: '1px solid var(--border)' }}>
                <div className="form-group" style={{ margin: 0 }}><label>Mã</label><input value={item.code} onChange={e => update(index, { code: e.target.value })} style={{ height: '36px' }} /></div>
                <div className="form-group" style={{ margin: 0 }}><label>Tên đầu điểm</label><input value={item.displayName} onChange={e => update(index, { displayName: e.target.value })} style={{ height: '36px' }} /></div>
                <div className="form-group" style={{ margin: 0 }}><label>Loại dữ liệu</label>
                  <select value={item.assessmentType} onChange={e => update(index, { assessmentType: e.target.value as GradeConfigItem['assessmentType'] })} style={{ height: '36px' }}>
                    <option value="SCORE">Điểm số (tính ĐTB)</option>
                    <option value="PASS_FAIL">Đạt / Chưa đạt</option>
                    <option value="COMMENT">Nhận xét</option>
                  </select>
                </div>
                <div className="form-group" style={{ margin: 0 }}><label>Hệ số</label><input type="number" min={1} value={item.weight} onChange={e => update(index, { weight: Number(e.target.value) })} style={{ height: '36px' }} /></div>
                <div className="form-group" style={{ margin: 0 }}><label>Số lượng</label><input type="number" min={1} value={item.quantity} onChange={e => update(index, { quantity: Number(e.target.value) })} style={{ height: '36px' }} /></div>
                <div className="form-group" style={{ margin: 0 }}><label>Người nhập</label>
                  <select value={item.entryRole} onChange={e => update(index, { entryRole: e.target.value as GradeConfigItem['entryRole'] })} style={{ height: '36px' }}>
                    <option value="SUBJECT_TEACHER">Giáo viên bộ môn</option>
                    <option value="ADMIN">Admin/nhà trường</option>
                    <option value="SUBJECT_TEACHER_AND_ADMIN">Giáo viên và admin</option>
                  </select>
                </div>
                <label style={{ height: '36px', display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', fontWeight: 700 }}>
                  <input type="checkbox" checked={item.requiredEntry} onChange={e => update(index, { requiredEntry: e.target.checked })} /> Bắt buộc
                </label>
                <button className="danger" onClick={() => setItems(rows => rows.filter((_, i) => i !== index))} style={{ height: '36px', minHeight: '36px', padding: '0 12px' }}>Xóa</button>
              </div>
            ))}
          </div>

          <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end', borderTop: '1px solid var(--border)', paddingTop: '16px' }}>
            <button type="button" className="secondary-button" onClick={() => setItems(rows => [...rows, { code: `D${rows.length + 1}`, displayName: 'Đầu điểm mới', weight: 1, quantity: 1, entryRole: 'SUBJECT_TEACHER', assessmentType: 'SCORE', requiredEntry: true, displayOrder: rows.length }])} style={{ height: '38px' }}>+ Thêm đầu điểm</button>
            <button onClick={save} disabled={saving} style={{ height: '38px' }}>{saving ? 'Đang lưu…' : 'Lưu mẫu cấu hình'}</button>
            <button type="button" className="secondary-button" onClick={() => { setShowForm(false); setName('Cấu hình chuẩn'); setItems(defaults); }} style={{ height: '38px' }}>Đóng</button>
          </div>
        </section>
      )}

      <section className="step-card" style={{ display: 'block', width: '100%', margin: 0 }}>
        <h3>Mẫu đã lưu</h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))', gap: '16px', marginTop: '16px' }}>
          {templates.map(template => (
            <article key={template.id} style={{ border: '1px solid var(--border)', borderRadius: 12, padding: 18, background: 'white', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <div>
                <strong>{template.name}</strong>
                <small style={{ display: 'block', color: 'var(--text-secondary)', marginTop: '4px', fontSize: '12px' }}>Phiên bản {template.version} · {template.items.length} loại đầu điểm</small>
                <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginTop: 12 }}>
                  {template.items.map(item => <span className="badge-status active" key={item.id}>{item.displayName} · {assessmentLabel(item.assessmentType)} · SL {item.quantity}{item.assessmentType === 'SCORE' ? ` · HS ${item.weight}` : ''}{item.requiredEntry ? ' · Bắt buộc' : ''}</span>)}
                </div>
              </div>
              {!yearConfig && (
                <button style={{ marginTop: 16, width: '100%' }} disabled={!selectedYearId || selectedYearStatus === 'COMPLETED' || saving} onClick={() => apply(template.id)}>
                  Áp dụng cho năm học đang chọn
                </button>
              )}
            </article>
          ))}
        </div>
        {templates.length === 0 && <div className="notice warning" style={{ marginTop: '12px' }}>Chưa có mẫu. Hãy tạo mẫu đầu tiên trước khi tạo năm học.</div>}
      </section>
    </div>
  );
}
