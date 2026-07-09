import { apiFetch } from './client';

export async function getClassSchedules(classId: number | string, semesterId: number | string) {
  return apiFetch(`/schedules/class?classId=${classId}&semesterId=${semesterId}`);
}

export interface CreateScheduleData {
  assignmentId: number;
  dayOfWeek: number;
  period: number;
  room?: string;
  shift: string;
}

export async function createSchedule(data: CreateScheduleData) {
  return apiFetch('/schedules', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
}

export async function deleteSchedule(id: number) {
  return apiFetch(`/schedules/${id}`, {
    method: 'DELETE'
  });
}

export async function importSchedules(formData: FormData) {
  return apiFetch('/import/schedules', {
    method: 'POST',
    body: formData
  });
}
