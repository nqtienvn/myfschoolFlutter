import { apiFetch } from './client';

export interface ClassAttendanceSummary {
  studentId: number;
  studentCode: string;
  studentName: string;
  presentCount: number;
  absentCount: number;
  absentWithLeaveCount: number;
  absentWithoutLeaveCount: number;
  attendanceRate: number;
  suggestedConduct: string;
}

export type AttendanceCorrectionStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface AttendanceCorrectionRequest {
  id: number;
  classId: number;
  className: string;
  teacherId: number;
  teacherName: string;
  date: string;
  shift: 'MORNING' | 'AFTERNOON';
  status: AttendanceCorrectionStatus;
  originalPresentCount: number;
  originalAbsentWithLeaveCount: number;
  originalAbsentWithoutLeaveCount: number;
  presentCount: number;
  absentWithLeaveCount: number;
  absentWithoutLeaveCount: number;
  reason: string;
  changes: AttendanceCorrectionEntry[];
  createdAt: string;
  reviewedByName?: string | null;
  reviewedAt?: string | null;
}

export interface AttendanceCorrectionEntry {
  studentId: number;
  studentName: string;
  studentCode: string;
  oldStatus?: 'PRESENT' | 'ABSENT_WITH_LEAVE' | 'ABSENT_WITHOUT_LEAVE' | null;
  newStatus: 'PRESENT' | 'ABSENT_WITH_LEAVE' | 'ABSENT_WITHOUT_LEAVE';
}

export interface AttendanceCorrectionFilters {
  status?: AttendanceCorrectionStatus;
  date?: string;
  classId?: number | string;
  teacherId?: number | string;
}

export async function getClassAttendanceSummary(
  classId: number | string,
  semesterId: number | string,
  academicYearId: number | string,
): Promise<ClassAttendanceSummary[]> {
  const query = new URLSearchParams({
    classId: String(classId),
    semesterId: String(semesterId),
    academicYearId: String(academicYearId),
  });
  return apiFetch(`/attendance/class-summary?${query.toString()}`);
}

export function getAttendanceCorrections(
  academicYearId: number | string,
  filters: AttendanceCorrectionFilters = {},
) {
  const query = new URLSearchParams({ academicYearId: String(academicYearId) });
  if (filters.status) query.set('status', filters.status);
  if (filters.date) query.set('date', filters.date);
  if (filters.classId) query.set('classId', String(filters.classId));
  if (filters.teacherId) query.set('teacherId', String(filters.teacherId));
  return apiFetch(`/attendance/admin/corrections?${query.toString()}`) as Promise<AttendanceCorrectionRequest[]>;
}

export function getPendingAttendanceCorrectionCount(academicYearId: number | string) {
  const query = new URLSearchParams({ academicYearId: String(academicYearId) });
  return apiFetch(`/attendance/admin/corrections/pending-count?${query.toString()}`) as Promise<number>;
}

export function reviewAttendanceCorrection(
  id: number,
  academicYearId: number | string,
  approve: boolean,
) {
  const query = new URLSearchParams({ academicYearId: String(academicYearId) });
  return apiFetch(
    `/attendance/admin/corrections/${id}/${approve ? 'approve' : 'reject'}?${query.toString()}`,
    { method: 'PUT' },
  ) as Promise<AttendanceCorrectionRequest>;
}
