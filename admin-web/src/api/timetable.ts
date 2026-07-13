import { apiFetch } from './client';

export type TimetableStatus = 'DRAFT' | 'SCHEDULED' | 'ACTIVE' | 'ARCHIVED';

export interface TimetableItem {
  id: number;
  classId: number;
  className: string;
  semesterId: number;
  semesterName: string;
  version: number;
  status: TimetableStatus;
  effectiveFrom?: string;
  effectiveTo?: string;
  slotCount: number;
}

export interface AutoGenerateTimetableResult {
  academicYearId: number;
  semesterId: number;
  classCount: number;
  timetableCount: number;
  slotCount: number;
  timetables: TimetableItem[];
}

export function getTimetables(classId: number | string, semesterId: number | string) {
  const params = new URLSearchParams({ classId: String(classId), semesterId: String(semesterId) });
  return apiFetch(`/timetables?${params}`) as Promise<TimetableItem[]>;
}

export function createTimetable(data: {
  classId: number;
  semesterId: number;
  effectiveFrom?: string;
  copyFromTimetableId?: number;
}) {
  return apiFetch('/timetables', { method: 'POST', body: JSON.stringify(data) }) as Promise<TimetableItem>;
}

export function autoGenerateTimetables(data: {
  academicYearId: number;
  semesterId: number;
  classIds: number[];
  shiftIds: number[];
  periodIds: number[];
}) {
  return apiFetch('/timetables/auto-generate', {
    method: 'POST', body: JSON.stringify(data),
  }) as Promise<AutoGenerateTimetableResult>;
}

export function publishTimetable(id: number, effectiveFrom: string) {
  return apiFetch(`/timetables/${id}/publish`, {
    method: 'POST', body: JSON.stringify({ effectiveFrom }),
  }) as Promise<TimetableItem>;
}

export function scheduleTimetable(id: number, publishDate: string) {
  return apiFetch(`/timetables/${id}/schedule`, {
    method: 'POST', body: JSON.stringify({ effectiveFrom: publishDate }),
  }) as Promise<TimetableItem>;
}

export function deleteTimetable(id: number) {
  return apiFetch(`/timetables/${id}`, { method: 'DELETE' });
}
