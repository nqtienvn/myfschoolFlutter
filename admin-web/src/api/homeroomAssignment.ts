import { apiFetch } from './client';

export interface HomeroomAssignmentItem {
  id: number;
  classId: number;
  className: string;
  teacherId: number;
  teacherName: string;
  academicYearId: number;
  academicYearName: string;
  effectiveFrom: string;
  effectiveTo?: string | null;
}

export function getHomeroomAssignment(classId: number, academicYearId: number | string) {
  return apiFetch(`/homeroom-assignments/current?classId=${classId}&academicYearId=${academicYearId}`) as Promise<HomeroomAssignmentItem | null>;
}

export function createHomeroomAssignment(data: { classId: number; teacherId: number; academicYearId: number; effectiveFrom: string }) {
  return apiFetch('/homeroom-assignments', { method: 'POST', body: JSON.stringify(data) }) as Promise<HomeroomAssignmentItem>;
}

export function updateHomeroomAssignment(id: number, data: { classId: number; teacherId: number; academicYearId: number; effectiveFrom: string }) {
  return apiFetch(`/homeroom-assignments/${id}`, { method: 'PUT', body: JSON.stringify(data) }) as Promise<HomeroomAssignmentItem>;
}
