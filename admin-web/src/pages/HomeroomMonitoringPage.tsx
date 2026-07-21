import { useEffect, useMemo, useState } from 'react';
import { getClasses } from '../api/class';
import {
  getClassSummaries,
  getRiskConfig,
  getRiskFlags,
  recalculateRiskFlags,
  saveRiskConfig,
  updateRiskStatus,
  type ClassSummary,
  type RiskConfig,
  type RiskFlag,
  type RiskSeverity,
  type RiskStatus,
} from '../api/homeroomMonitoring';

interface ClassItem { id: number; name: string; gradeLevel: number }

const SEVERITIES: RiskSeverity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export default function HomeroomMonitoringPage({
  selectedYearId,
  selectedSemesterId,
}: {
  selectedYearId?: string;
  selectedSemesterId?: string;
}) {
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [classId, setClassId] = useState('');
  const [gradeLevel, setGradeLevel] = useState('');
  const [riskStatus, setRiskStatus] = useState<'' | RiskStatus>('OPEN');
  const [summaries, setSummaries] = useState<ClassSummary[]>([]);
  const [risks, setRisks] = useState<RiskFlag[]>([]);
  const [config, setConfig] = useState<RiskConfig | null>(null);
  const [savingConfig, setSavingConfig] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setClasses([]);
    setClassId('');
    setGradeLevel('');
    setRiskStatus('OPEN');
    setSummaries([]);
    setRisks([]);
    setConfig(null);
    setError('');
    if (!selectedYearId) return;
    Promise.all([
      getClasses({ academicYearId: selectedYearId, size: 200 }),
      getRiskConfig(selectedYearId),
    ]).then(([classRows, riskConfig]) => {
      setClasses((classRows || []) as ClassItem[]);
      setConfig(riskConfig);
    }).catch((err) => setError(err.message || 'Không thể tải cấu hình theo dõi.'));
  }, [selectedYearId]);

  useEffect(() => {
    setSummaries([]);
    setRisks([]);
    if (!selectedYearId || !selectedSemesterId) return;
    setLoading(true);
    setError('');
    getClassSummaries({
      academicYearId: selectedYearId,
      semesterId: selectedSemesterId,
      classId: classId || undefined,
      gradeLevel: gradeLevel || undefined,
    }).then(setSummaries)
      .catch((err) => setError(err.message || 'Không thể tải báo cáo lớp.'))
      .finally(() => setLoading(false));
  }, [selectedYearId, selectedSemesterId, classId, gradeLevel]);

  useEffect(() => {
    setRisks([]);
    if (!selectedYearId || !selectedSemesterId || !classId) return;
    getRiskFlags({
      academicYearId: selectedYearId,
      semesterId: selectedSemesterId,
      classId,
      status: riskStatus || undefined,
    }).then(setRisks).catch((err) => setError(err.message || 'Không thể tải cảnh báo học sinh.'));
  }, [selectedYearId, selectedSemesterId, classId, riskStatus]);

  const totals = useMemo(() => summaries.reduce((value, item) => ({
    students: value.students + item.studentCount,
    risks: value.risks + item.openRiskCount,
    contacts: value.contacts + item.parentContactCount,
    meetings: value.meetings + item.meetingCount,
  }), { students: 0, risks: 0, contacts: 0, meetings: 0 }), [summaries]);

  const gradeOptions = useMemo(() => Array.from(new Set(classes.map((item) => item.gradeLevel))).sort(), [classes]);

  async function persistConfig() {
    if (!config || !selectedYearId) return;
    setSavingConfig(true);
    setError('');
    try {
      setConfig(await saveRiskConfig({ ...config, academicYearId: Number(selectedYearId) }));
    } catch (err: any) {
      setError(err.message || 'Không thể lưu cấu hình cảnh báo.');
    } finally {
      setSavingConfig(false);
    }
  }

  async function recalculate() {
    if (!selectedYearId || !selectedSemesterId || !classId) return;
    setLoading(true);
    try {
      const rows = await recalculateRiskFlags({
        academicYearId: selectedYearId,
        semesterId: selectedSemesterId,
        classId,
      });
      setRisks(riskStatus ? rows.filter((item) => item.status === riskStatus) : rows);
      setSummaries(await getClassSummaries({
        academicYearId: selectedYearId,
        semesterId: selectedSemesterId,
        classId,
      }));
    } catch (err: any) {
      setError(err.message || 'Không thể tính lại cảnh báo.');
    } finally {
      setLoading(false);
    }
  }

  async function changeRisk(id: number, action: 'acknowledge' | 'resolve') {
    const updated = await updateRiskStatus(id, action);
    setRisks((current) => riskStatus && updated.status !== riskStatus
      ? current.filter((item) => item.id !== id)
      : current.map((item) => item.id === id ? updated : item));
  }

  if (!selectedYearId || !selectedSemesterId) {
    return <div className="empty-state">Chọn năm học và học kỳ để xem báo cáo GVCN.</div>;
  }

  return (
    <main className="page-stack monitoring-page" aria-label="Theo dõi học sinh và báo cáo GVCN">
      <div className="page-heading"><div><h1>Theo dõi học sinh</h1><p>Cấu hình ngưỡng năm học, cảnh báo nội bộ và báo cáo tổng hợp từ backend.</p></div></div>
      {error && <div className="alert error" role="alert">{error}</div>}

      <div className="summary-cards monitoring-summary">
        <div className="summary-card"><span>Học sinh</span><strong>{totals.students}</strong></div>
        <div className="summary-card"><span>Đang cảnh báo</span><strong>{totals.risks}</strong></div>
        <div className="summary-card"><span>Liên hệ PH</span><strong>{totals.contacts}</strong></div>
        <div className="summary-card"><span>Lịch họp</span><strong>{totals.meetings}</strong></div>
      </div>

      {config && <section className="panel risk-config-panel">
        <div className="panel-heading"><div><h2>Ngưỡng cảnh báo của năm học</h2><p>Giá trị được lưu ở backend và áp dụng cho job tự động.</p></div><button className="btn btn-primary" disabled={savingConfig} onClick={persistConfig}>{savingConfig ? 'Đang lưu…' : 'Lưu cấu hình'}</button></div>
        <div className="monitoring-config-grid">
          <ConfigNumber label="GPA tối thiểu" value={config.minGpa} onChange={(value) => setConfig({ ...config, minGpa: value })} />
          <ConfigNumber label="Chuyên cần tối thiểu (%)" value={config.minAttendanceRate} onChange={(value) => setConfig({ ...config, minAttendanceRate: value })} />
          <ConfigNumber label="Vắng không phép tối đa" value={config.maxUnexcusedAbsences} onChange={(value) => setConfig({ ...config, maxUnexcusedAbsences: value })} />
          <label>Hạnh kiểm cần theo dõi<input value={config.conductRiskValues || ''} onChange={(event) => setConfig({ ...config, conductRiskValues: event.target.value })} placeholder="Ví dụ: Yếu, Trung bình" /></label>
          <label className="checkbox-row"><input type="checkbox" checked={config.includeOverdueTuition} onChange={(event) => setConfig({ ...config, includeOverdueTuition: event.target.checked })} />Dùng học phí quá hạn</label>
          <ConfigNumber label="Số ngày quá hạn" value={config.overdueTuitionDays} onChange={(value) => setConfig({ ...config, overdueTuitionDays: value || 0 })} />
        </div>
        <div className="severity-grid">
          {([
            ['GPA', 'gpaSeverity'], ['Chuyên cần', 'attendanceSeverity'], ['Vắng không phép', 'absenceSeverity'],
            ['Hạnh kiểm', 'conductSeverity'], ['Học phí', 'tuitionSeverity'],
          ] as const).map(([label, key]) => <label key={key}>{label}<select value={config[key]} onChange={(event) => setConfig({ ...config, [key]: event.target.value as RiskSeverity })}>{SEVERITIES.map((item) => <option key={item}>{item}</option>)}</select></label>)}
        </div>
      </section>}

      <section className="panel">
        <div className="filter-row">
          <label>Khối<select value={gradeLevel} onChange={(event) => { setGradeLevel(event.target.value); setClassId(''); }}><option value="">Tất cả</option>{gradeOptions.map((grade) => <option key={grade} value={grade}>Khối {grade}</option>)}</select></label>
          <label>Lớp<select value={classId} onChange={(event) => setClassId(event.target.value)}><option value="">Tất cả lớp</option>{classes.filter((item) => !gradeLevel || String(item.gradeLevel) === gradeLevel).map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
        </div>
        {loading && <div className="loading-state">Đang tổng hợp dữ liệu…</div>}
        {!loading && <div className="table-responsive"><table><thead><tr><th>Lớp</th><th>Sĩ số</th><th>Chuyên cần</th><th>GPA</th><th>Nguy cơ</th><th>Liên hệ / họp</th><th>Khen / vi phạm</th></tr></thead><tbody>{summaries.map((item) => <tr key={item.classId}><td><strong>{item.className}</strong><small className="table-subtext">Khối {item.gradeLevel}</small></td><td>{item.studentCount}</td><td>{item.attendanceRate}%</td><td>{item.averageGpa}</td><td>{item.openRiskCount}</td><td>{item.parentContactCount} / {item.meetingCount}</td><td>{item.rewardCount} / {item.violationCount}</td></tr>)}</tbody></table>{summaries.length === 0 && <div className="empty-state">Chưa có lớp phù hợp phạm vi đã chọn.</div>}</div>}
      </section>

      {classId && <section className="panel">
        <div className="panel-heading"><div><h2>Drill-down cảnh báo</h2><p>Chỉ dữ liệu của lớp và học kỳ đang chọn.</p></div><button className="btn btn-secondary" onClick={recalculate}>Tính lại từ nguồn</button></div>
        <div className="filter-row"><label>Trạng thái<select value={riskStatus} onChange={(event) => setRiskStatus(event.target.value as '' | RiskStatus)}><option value="">Tất cả</option><option value="OPEN">Mới</option><option value="ACKNOWLEDGED">Đã ghi nhận</option><option value="RESOLVED">Đã xử lý</option></select></label></div>
        <div className="risk-list">{risks.map((item) => <article key={item.id} className={`risk-row severity-${item.severity.toLowerCase()}`}><div><strong>{item.studentName}</strong><small>{item.studentCode} · {item.riskType}</small><p>{item.message} · Giá trị {item.metricValue || '—'} / ngưỡng {item.thresholdValue || '—'}</p></div><span className={`badge-status ${item.status === 'RESOLVED' ? 'active' : 'draft'}`}>{item.status}</span><div className="risk-actions">{item.status === 'OPEN' && <button className="btn btn-secondary" onClick={() => changeRisk(item.id, 'acknowledge')}>Ghi nhận</button>}{item.status !== 'RESOLVED' && <button className="btn btn-primary" onClick={() => changeRisk(item.id, 'resolve')}>Đã xử lý</button>}</div></article>)}</div>
        {risks.length === 0 && <div className="empty-state">Không có cảnh báo phù hợp bộ lọc.</div>}
      </section>}
    </main>
  );
}

function ConfigNumber({ label, value, onChange }: { label: string; value: number | null; onChange: (value: number | null) => void }) {
  return <label>{label}<input type="number" min="0" value={value ?? ''} onChange={(event) => onChange(event.target.value === '' ? null : Number(event.target.value))} /></label>;
}
