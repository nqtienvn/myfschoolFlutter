import { apiDownload, apiFetch } from './client';

export interface ResultSummaryItem {
  studentId: number;
  studentName: string;
  studentCode: string;
  academicYearId: number;
  semesterId: number;
  classId: number;
  className: string;
  gpa: number | null;
  rank: number | null;
  absentWithLeave: number;
  absentWithoutLeave: number;
  suggestedAcademicAbility: string | null;
  suggestedConduct: string | null;
  academicAbility: string | null;
  conduct: string | null;
  honor: string | null;
  status: 'DRAFT' | 'PUBLISHED';
  publishedAt: string | null;
}

export interface AcademicYearResultItem {
  studentId: number;
  studentName: string;
  studentCode: string;
  academicYearId: number;
  classId: number;
  className: string;
  semester1Average: number | null;
  semester1AcademicAbility: string | null;
  semester1Conduct: string | null;
  semester2Average: number | null;
  semester2AcademicAbility: string | null;
  semester2Conduct: string | null;
  annualAverage: number | null;
  rank: number | null;
  academicAbility: string | null;
  conduct: string | null;
  honor: string | null;
  status: 'DRAFT' | 'PUBLISHED';
  publishedAt: string | null;
}

export interface GradeImportResult {
  totalRows: number;
  importedRows: number;
  updatedScores: number;
  zeroFilledScores: number;
  errors: string[];
}

export function getResultSummary(academicYearId: string | number, semesterId: string | number, classId: string | number) {
  const query = new URLSearchParams({
    academicYearId: String(academicYearId),
    semesterId: String(semesterId),
    classId: String(classId),
  });
  return apiFetch(`/semester-results/admin/summary?${query}`) as Promise<ResultSummaryItem[]>;
}

export function overrideResult(studentId: number, payload: {
  academicYearId: number;
  semesterId: number;
  classId: number;
  academicAbility: string;
  conduct: string;
  honor: string;
}) {
  return apiFetch(`/semester-results/admin/students/${studentId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  }) as Promise<ResultSummaryItem>;
}

export function publishSemesterResults(payload: {
  academicYearId: number;
  semesterId: number;
  classId: number;
}) {
  return apiFetch('/semester-results/admin/publish', {
    method: 'POST',
    body: JSON.stringify(payload),
  }) as Promise<ResultSummaryItem[]>;
}

export function closeSemesterResults(academicYearId: number, semesterId: number) {
  return apiFetch('/semester-results/admin/close', {
    method: 'POST',
    body: JSON.stringify({ academicYearId, semesterId }),
  });
}

export function getAcademicYearResults(academicYearId: string | number, classId: string | number) {
  const query = new URLSearchParams({ academicYearId: String(academicYearId), classId: String(classId) });
  return apiFetch(`/semester-results/admin/annual?${query}`) as Promise<AcademicYearResultItem[]>;
}

export function calculateAcademicYearResults(academicYearId: number, classId: number) {
  return apiFetch('/semester-results/admin/annual/calculate', {
    method: 'POST', body: JSON.stringify({ academicYearId, classId }),
  }) as Promise<AcademicYearResultItem[]>;
}

export function publishAcademicYearResults(academicYearId: number, classId: number) {
  return apiFetch('/semester-results/admin/annual/publish', {
    method: 'POST', body: JSON.stringify({ academicYearId, classId }),
  }) as Promise<AcademicYearResultItem[]>;
}

export async function downloadGradeTemplate(academicYearId: number, semesterId: number,
  classId: number, subjectId: number) {
  const query = new URLSearchParams({
    academicYearId: String(academicYearId), semesterId: String(semesterId),
    classId: String(classId), subjectId: String(subjectId),
  });
  return apiDownload(`/result-files/template?${query}`);
}

export function importAdminScores(academicYearId: number, semesterId: number,
  classId: number, subjectId: number, file: File) {
  const query = new URLSearchParams({
    academicYearId: String(academicYearId), semesterId: String(semesterId),
    classId: String(classId), subjectId: String(subjectId),
  });
  const form = new FormData();
  form.append('file', file);
  return apiFetch(`/result-files/import?${query}`, { method: 'POST', body: form }) as Promise<GradeImportResult>;
}

export function downloadResultExport(academicYearId: number, semesterId?: number, classId?: number) {
  const query = new URLSearchParams({ academicYearId: String(academicYearId) });
  if (semesterId) query.set('semesterId', String(semesterId));
  if (classId) query.set('classId', String(classId));
  return apiDownload(`/result-files/export?${query}`);
}
