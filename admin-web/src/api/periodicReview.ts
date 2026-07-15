import { apiFetch } from './client';

export type PeriodicReportStatus = 'DRAFT' | 'PUBLISHED';

export interface SubjectReviewItem {
  id: number | null;
  subjectName: string;
  subjectTeacherName: string;
  comment: string | null;
  strengths: string | null;
  improvements: string | null;
  status: 'DRAFT' | 'SUBMITTED' | 'RETURNED';
  returnReason: string | null;
}

export interface PeriodicReportItem {
  id: number | null;
  academicYearId: number;
  semesterId: number;
  classId: number;
  className: string;
  studentId: number;
  studentName: string;
  studentCode: string;
  homeroomTeacherName: string | null;
  generalComment: string | null;
  conduct: string | null;
  suggestedConduct: string | null;
  status: PeriodicReportStatus;
  publishedAt: string | null;
  submittedSubjects: number;
  totalSubjects: number;
  missingSubjects: string[];
  subjectReviews: SubjectReviewItem[];
}

export interface PeriodicReportFilters {
  academicYearId: string | number;
  semesterId: string | number;
  classId?: string | number;
  status?: PeriodicReportStatus;
}

export function buildPeriodicReportQuery(filters: PeriodicReportFilters) {
  const query = new URLSearchParams({
    academicYearId: String(filters.academicYearId),
    semesterId: String(filters.semesterId),
  });
  if (filters.classId) query.set('classId', String(filters.classId));
  if (filters.status) query.set('status', filters.status);
  return query.toString();
}

export function getPeriodicReports(filters: PeriodicReportFilters): Promise<PeriodicReportItem[]> {
  return apiFetch(`/periodic-reports/admin?${buildPeriodicReportQuery(filters)}`);
}

export function reopenPeriodicReport(id: number, academicYearId: string | number, reason: string) {
  return apiFetch(`/periodic-reports/admin/${id}/reopen`, {
    method: 'PUT',
    body: JSON.stringify({ academicYearId: Number(academicYearId), reason }),
  }) as Promise<PeriodicReportItem>;
}
