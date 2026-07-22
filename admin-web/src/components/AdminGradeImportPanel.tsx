import { useEffect, useMemo, useRef, useState } from 'react';
import {
  downloadAdminGradeImportTemplate,
  getAdminGradeImportBatch,
  getAdminGradeImportBatches,
  getAdminGradeImportContext,
  getAdminGradeImportItems,
  importAdminGradeFile,
  updateAdminGradeImportRow,
  type AdminGradeImportBatch,
  type AdminGradeImportCell,
  type AdminGradeImportContext,
  type AdminGradeImportItem,
  type AdminGradeImportRow,
  type AdminGradeImportTable,
} from '../api/adminGradeImport';

const PAGE_SIZE = 20;

function saveBlob(blob: Blob, suggestedName: string, responseName?: string | null) {
  const href = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = href;
  anchor.download = responseName || suggestedName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(href);
}

function valueOf(cell: AdminGradeImportCell | undefined, assessmentType: AdminGradeImportTable['assessmentType']) {
  if (!cell?.isGraded) return '';
  return assessmentType === 'SCORE' ? String(cell.score ?? '') : cell.comment || '';
}

export default function AdminGradeImportPanel({ disabled }: { disabled: boolean }) {
  const [context, setContext] = useState<AdminGradeImportContext | null>(null);
  const [items, setItems] = useState<AdminGradeImportItem[]>([]);
  const [batches, setBatches] = useState<AdminGradeImportBatch[]>([]);
  const [table, setTable] = useState<AdminGradeImportTable | null>(null);
  const [selectedBatchId, setSelectedBatchId] = useState('');
  const [classFilter, setClassFilter] = useState('');
  const [page, setPage] = useState(1);
  const [editingStudentId, setEditingStudentId] = useState<number | null>(null);
  const [draftValues, setDraftValues] = useState<Record<number, string>>({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const fileRef = useRef<HTMLInputElement>(null);

  const showError = (cause: unknown) => {
    setMessage('');
    setError(cause instanceof Error ? cause.message : 'Không thể xử lý import điểm.');
  };

  async function loadTable(batchId: number) {
    const loaded = await getAdminGradeImportBatch(batchId);
    setTable(loaded); setSelectedBatchId(String(batchId)); setClassFilter(''); setPage(1); setEditingStudentId(null);
  }

  async function loadInitial() {
    setBusy(true); setError('');
    try {
      const [importContext, importItems, importBatches] = await Promise.all([
        getAdminGradeImportContext(), getAdminGradeImportItems(), getAdminGradeImportBatches(),
      ]);
      setContext(importContext); setItems(importItems || []); setBatches(importBatches || []);
      if (importBatches?.[0]) await loadTable(importBatches[0].id);
      else setTable(null);
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  useEffect(() => { void loadInitial(); }, []);

  async function downloadTemplate(item: AdminGradeImportItem) {
    setBusy(true); setError(''); setMessage('');
    try {
      const file = await downloadAdminGradeImportTemplate(item.itemCode);
      saveBlob(file.blob, `template-${item.itemCode}.xlsx`, file.filename);
      setMessage(`Đã tải template cho đầu điểm ${item.displayName}.`);
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  async function importFile(file?: File) {
    if (!file || disabled) return;
    setBusy(true); setError(''); setMessage('');
    try {
      const result = await importAdminGradeFile(file);
      const importBatches = await getAdminGradeImportBatches();
      setBatches(importBatches || []);
      await loadTable(result.batchId);
      setMessage(`Đã lưu ${result.updatedScores} ô điểm nháp của ${result.totalRows} học sinh cho đầu điểm ${result.itemName}.`);
    } catch (cause) { showError(cause); } finally {
      setBusy(false); if (fileRef.current) fileRef.current.value = '';
    }
  }

  function beginEdit(row: AdminGradeImportRow) {
    if (!table || disabled) return;
    const values: Record<number, string> = {};
    table.subjects.forEach(subject => {
      values[subject.id] = valueOf(row.cells.find(cell => cell.subjectId === subject.id), table.assessmentType);
    });
    setDraftValues(values); setEditingStudentId(row.studentId); setError(''); setMessage('');
  }

  async function saveRow(row: AdminGradeImportRow) {
    if (!table) return;
    setBusy(true); setError(''); setMessage('');
    try {
      const updated = await updateAdminGradeImportRow(table.batchId, row.studentId,
        table.subjects.map(subject => ({ subjectId: subject.id, value: draftValues[subject.id] || '' })));
      setTable(updated); setEditingStudentId(null); setMessage(`Đã lưu thay đổi điểm của ${row.studentName}.`);
    } catch (cause) { showError(cause); } finally { setBusy(false); }
  }

  const classes = useMemo(() => table ? [...new Map(table.rows.map(row => [row.classId, row.className])).entries()]
    .map(([id, name]) => ({ id, name })).sort((left, right) => left.name.localeCompare(right.name)) : [], [table]);
  const filteredRows = useMemo(() => table?.rows.filter(row => !classFilter || row.classId === Number(classFilter)) || [], [table, classFilter]);
  const pageRows = filteredRows.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);
  const pageCount = Math.max(1, Math.ceil(filteredRows.length / PAGE_SIZE));

  function renderEditor(subjectId: number) {
    if (!table) return null;
    const value = draftValues[subjectId] || '';
    if (table.assessmentType === 'SCORE') return <input type="number" min="0" max="10" step="0.1" value={value}
      onChange={event => setDraftValues(values => ({ ...values, [subjectId]: event.target.value }))} />;
    if (table.assessmentType === 'PASS_FAIL') return <select value={value}
      onChange={event => setDraftValues(values => ({ ...values, [subjectId]: event.target.value }))}>
      <option value="">—</option><option value="PASS">Đạt</option><option value="FAIL">Chưa đạt</option>
    </select>;
    return <textarea rows={2} value={value}
      onChange={event => setDraftValues(values => ({ ...values, [subjectId]: event.target.value }))} />;
  }

  return <section className="panel result-panel">
    <div className="result-panel-heading"><div><h2>Import đầu điểm toàn trường</h2>
      <p>Mỗi file thuộc một đầu điểm. Điểm được lưu nháp theo học kỳ đang hoạt động, chưa tính điểm trung bình hay công bố kết quả.</p></div>
      {context && <span className="result-ready-chip">{context.semesterName} · {context.academicYearName}</span>}</div>
    {error && <div className="notice error" role="alert">{error}</div>}
    {message && <div className="notice success">{message}</div>}
    <div className="result-file-actions">
      {items.map(item => <button key={item.itemCode} className="excel-action" disabled={busy || disabled}
        onClick={() => void downloadTemplate(item)}><span>↓</span><div><strong>{item.displayName}</strong>
          <small>Tải template {item.itemCode}</small></div></button>)}
      <button className="excel-action primary" disabled={busy || disabled || !items.length} onClick={() => fileRef.current?.click()}>
        <span>↑</span><div><strong>Import file đầu điểm</strong><small>Hệ thống tự nhận diện từ template</small></div></button>
      <input ref={fileRef} hidden type="file" accept=".xlsx" onChange={event => void importFile(event.target.files?.[0])} />
    </div>
    {batches.length > 0 && <section className="result-import-history">
      <label>Đợt import đã lưu<select value={selectedBatchId} onChange={event => void loadTable(Number(event.target.value))}>
        {batches.map(batch => <option key={batch.id} value={batch.id}>{batch.itemName} · {batch.fileName}</option>)}
      </select></label>
    </section>}
    {!table ? <div className="result-empty"><span>01</span><strong>Chưa có đợt import trong học kỳ đang hoạt động</strong>
      <p>Tải đúng template của đầu điểm, nhập các cột môn học rồi upload file.</p></div> : <>
      <div className="result-panel-heading result-import-table-heading"><div><h3>{table.itemName}</h3>
        <p>Danh sách giữ nguyên thứ tự dòng trong file Excel. Lọc lớp không làm thay đổi thứ tự.</p></div>
        <label>Lọc lớp<select value={classFilter} onChange={event => { setClassFilter(event.target.value); setPage(1); }}>
          <option value="">Tất cả lớp</option>{classes.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label></div>
      <div className="table-responsive"><table className="result-grade-table"><thead><tr><th>#</th><th>Học sinh</th><th>Lớp</th>
        {table.subjects.map(subject => <th key={subject.id}>{subject.name}<small>{subject.code}</small></th>)}<th /></tr></thead><tbody>
        {pageRows.map(row => <tr key={row.studentId}><td>{row.sourceOrder}</td><td><strong>{row.studentName}</strong><small>{row.studentCode}</small></td><td>{row.className}</td>
          {table.subjects.map(subject => <td key={subject.id}>{editingStudentId === row.studentId
            ? renderEditor(subject.id)
            : valueOf(row.cells.find(cell => cell.subjectId === subject.id), table.assessmentType) || '—'}</td>)}
          <td>{editingStudentId === row.studentId ? <><button className="secondary-button" disabled={busy} onClick={() => void saveRow(row)}>Lưu</button>
            <button className="secondary-button" disabled={busy} onClick={() => setEditingStudentId(null)}>Hủy</button></>
            : <button className="secondary-button" disabled={disabled || busy} onClick={() => beginEdit(row)}>Sửa điểm</button>}</td></tr>)}
      </tbody></table></div>
      {filteredRows.length > PAGE_SIZE && <div className="result-pagination"><span>{(page - 1) * PAGE_SIZE + 1}–{Math.min(page * PAGE_SIZE, filteredRows.length)} / {filteredRows.length}</span>
        <div><button className="secondary-button" disabled={page <= 1} onClick={() => setPage(page - 1)}>Trước</button><strong>{page}/{pageCount}</strong>
          <button className="secondary-button" disabled={page >= pageCount} onClick={() => setPage(page + 1)}>Sau</button></div></div>}
    </>}
  </section>;
}
