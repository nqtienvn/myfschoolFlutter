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
  period: number;
  room?: string;
  shift: 'MORNING' | 'AFTERNOON';
}

export function getTimetableSlots(timetableId: number) {
  return apiFetch(`/schedules/timetable/${timetableId}`) as Promise<ScheduleSlotItem[]>;
}

export function createScheduleSlot(data: {
  timetableId: number;
  assignmentId: number;
  dayOfWeek: number;
  period: number;
  shift: 'MORNING' | 'AFTERNOON';
}) {
  return apiFetch('/schedules', { method: 'POST', body: JSON.stringify(data) }) as Promise<ScheduleSlotItem>;
}

export function deleteScheduleSlot(id: number) {
  return apiFetch(`/schedules/${id}`, { method: 'DELETE' });
}
