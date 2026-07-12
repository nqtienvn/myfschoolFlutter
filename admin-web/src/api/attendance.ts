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

export interface AdminAttendanceDay {
  classId: number;
  className: string;
  date: string;
  shift: 'MORNING' | 'AFTERNOON';
  scheduledPeriods: number;
  totalStudents: number;
  submitted: boolean;
  presentCount: number;
  absentWithLeaveCount: number;
  absentWithoutLeaveCount: number;
}

export interface AttendanceCorrectionRequest {
  id: number;
  classId: number;
  className: string;
  teacherName: string;
  date: string;
  shift: 'MORNING' | 'AFTERNOON';
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  presentCount: number;
  absentWithLeaveCount: number;
  absentWithoutLeaveCount: number;
  createdAt: string;
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

export function getAdminDailyAttendance(academicYearId: number | string, date: string) {
  const query = new URLSearchParams({ academicYearId: String(academicYearId), date });
  return apiFetch(`/attendance/admin/daily?${query}`) as Promise<AdminAttendanceDay[]>;
}

export function adjustAdminDailyAttendance(data: {
  academicYearId: number;
  classId: number;
  date: string;
  shift: 'MORNING' | 'AFTERNOON';
  presentCount: number;
  absentWithLeaveCount: number;
  absentWithoutLeaveCount: number;
}) {
  return apiFetch('/attendance/admin/daily', {
    method: 'PUT',
    body: JSON.stringify(data),
  }) as Promise<AdminAttendanceDay>;
}

export function getPendingAttendanceCorrections(academicYearId: number | string, date: string) {
  const query = new URLSearchParams({ academicYearId: String(academicYearId), date });
  return apiFetch(`/attendance/admin/corrections?${query}`) as Promise<AttendanceCorrectionRequest[]>;
}

export function reviewAttendanceCorrection(id: number, approve: boolean) {
  return apiFetch(`/attendance/admin/corrections/${id}/${approve ? 'approve' : 'reject'}`, {
    method: 'PUT',
  }) as Promise<AttendanceCorrectionRequest>;
}
