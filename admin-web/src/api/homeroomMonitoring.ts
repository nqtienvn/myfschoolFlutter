import { apiFetch } from './client';

export type RiskSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type RiskStatus = 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED';

export interface RiskConfig {
  id: number | null;
  academicYearId: number;
  minGpa: number | null;
  minAttendanceRate: number | null;
  maxUnexcusedAbsences: number | null;
  conductRiskValues: string | null;
  includeOverdueTuition: boolean;
  overdueTuitionDays: number;
  gpaSeverity: RiskSeverity;
  attendanceSeverity: RiskSeverity;
  absenceSeverity: RiskSeverity;
  conductSeverity: RiskSeverity;
  tuitionSeverity: RiskSeverity;
}

export interface RiskFlag {
  id: number;
  academicYearId: number;
  semesterId: number;
  classId: number;
  className: string;
  studentId: number;
  studentName: string;
  studentCode: string;
  riskType: string;
  severity: RiskSeverity;
  metricValue: string | null;
  thresholdValue: string | null;
  message: string;
  status: RiskStatus;
  detectedAt: string;
}

export interface ClassSummary {
  academicYearId: number;
  semesterId: number;
  classId: number;
  className: string;
  gradeLevel: number;
  studentCount: number;
  attendanceRate: number;
  openRiskCount: number;
  averageGpa: number;
  academicAbilityDistribution: Record<string, number>;
  conductDistribution: Record<string, number>;
  submittedSubjectReviews: number;
  expectedSubjectReviews: number;
  reviewProgressRate: number;
  parentContactCount: number;
  meetingCount: number;
  meetingParticipationRate: number;
  rewardCount: number;
  violationCount: number;
}

export function buildMonitoringQuery(filters: {
  academicYearId: string | number;
  semesterId: string | number;
  classId?: string | number;
  gradeLevel?: string | number;
  status?: RiskStatus;
}) {
  const query = new URLSearchParams({
    academicYearId: String(filters.academicYearId),
    semesterId: String(filters.semesterId),
  });
  if (filters.classId) query.set('classId', String(filters.classId));
  if (filters.gradeLevel) query.set('gradeLevel', String(filters.gradeLevel));
  if (filters.status) query.set('status', filters.status);
  return query.toString();
}

export function getRiskConfig(academicYearId: string | number): Promise<RiskConfig> {
  return apiFetch(`/homeroom/risk-config?academicYearId=${encodeURIComponent(String(academicYearId))}`);
}

export function saveRiskConfig(config: RiskConfig): Promise<RiskConfig> {
  return apiFetch('/homeroom/risk-config', {
    method: 'PUT',
    body: JSON.stringify({ ...config, id: undefined, academicYearId: Number(config.academicYearId) }),
  });
}

export function getClassSummaries(filters: {
  academicYearId: string | number;
  semesterId: string | number;
  classId?: string | number;
  gradeLevel?: string | number;
}): Promise<ClassSummary[]> {
  return apiFetch(`/homeroom/reports/class-summary?${buildMonitoringQuery(filters)}`);
}

export function getRiskFlags(filters: {
  academicYearId: string | number;
  semesterId: string | number;
  classId: string | number;
  status?: RiskStatus;
}): Promise<RiskFlag[]> {
  return apiFetch(`/homeroom/risks?${buildMonitoringQuery(filters)}`);
}

export function recalculateRiskFlags(filters: {
  academicYearId: string | number;
  semesterId: string | number;
  classId: string | number;
}): Promise<RiskFlag[]> {
  return apiFetch(`/homeroom/risks/recalculate?${buildMonitoringQuery(filters)}`, { method: 'POST' });
}

export function updateRiskStatus(id: number, action: 'acknowledge' | 'resolve'): Promise<RiskFlag> {
  return apiFetch(`/homeroom/risks/${id}/${action}`, { method: 'PUT' });
}
