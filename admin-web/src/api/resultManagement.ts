import { apiFetch } from './client';

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
  violationCount: number;
  absentWithLeave: number;
  absentWithoutLeave: number;
  suggestedAcademicAbility: string | null;
  suggestedConduct: string | null;
  academicAbility: string | null;
  conduct: string | null;
  honor: string | null;
  generalComment: string | null;
  reportStatus: 'DRAFT' | 'SUBMITTED' | 'PUBLISHED';
  status: 'DRAFT' | 'PUBLISHED';
  publishedAt: string | null;
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

export interface ViolationItem {
  id: number;
  studentId: number;
  studentName: string;
  classId: number;
  eventType: 'VIOLATION';
  category: string | null;
  title: string;
  description: string | null;
  eventDate: string;
  status: 'DRAFT' | 'SUBMITTED';
}

export function getStudentViolations(studentId: number, academicYearId: number, semesterId: number, classId: number) {
  const query = new URLSearchParams({
    academicYearId: String(academicYearId),
    semesterId: String(semesterId),
    classId: String(classId),
  });
  return apiFetch(`/students/${studentId}/events?${query}`) as Promise<ViolationItem[]>;
}

export function saveViolation(studentId: number, payload: {
  academicYearId: number;
  semesterId: number;
  classId: number;
  category: string;
  title: string;
  description: string;
  eventDate: string;
}, violationId?: number) {
  return apiFetch(violationId ? `/student-events/${violationId}` : `/students/${studentId}/events`, {
    method: violationId ? 'PUT' : 'POST',
    body: JSON.stringify({ ...payload, eventType: 'VIOLATION' }),
  }) as Promise<ViolationItem>;
}

export function deleteViolation(violationId: number, academicYearId: number) {
  return apiFetch(`/student-events/${violationId}?academicYearId=${academicYearId}`, { method: 'DELETE' });
}
