import { apiFetch } from './client';

export interface ScheduleSlotItem {
  id: number;
  timetableId: number;
  timetableVersion: number;
  assignmentId: number;
  subjectName: string;
  teacherName: string;
  dayOfWeek: number;
  dayOfWeekName: string;
  periodId: number;
  periodName: string;
  period: number;
  shiftId: number;
  shiftName: string;
  room?: string;
  shift: 'MORNING' | 'AFTERNOON';
}

export interface AssignmentAvailabilityItem {
  dayOfWeek: number;
  periodId: number;
  assignmentIds: number[];
}

export function getTimetableSlots(timetableId: number) {
  return apiFetch(`/schedules/timetable/${timetableId}`) as Promise<ScheduleSlotItem[]>;
}

export function getAssignmentAvailability(classId: number | string, semesterId: number | string) {
  const params = new URLSearchParams({ classId: String(classId), semesterId: String(semesterId) });
  return apiFetch(`/schedules/assignment-availability?${params}`) as Promise<AssignmentAvailabilityItem[]>;
}

export function createScheduleSlot(data: {
  timetableId: number;
  assignmentId: number;
  dayOfWeek: number;
  periodId: number;
}) {
  return apiFetch('/schedules', { method: 'POST', body: JSON.stringify(data) }) as Promise<ScheduleSlotItem>;
}

export function deleteScheduleSlot(id: number) {
  return apiFetch(`/schedules/${id}`, { method: 'DELETE' });
}
