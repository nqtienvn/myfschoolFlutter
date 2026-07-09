import { apiFetch } from './client';

export interface GetAttendanceSessionsParams {
  classId: number | string;
  date: string;
  shift: string;
}

export async function getAttendanceSessions(params: GetAttendanceSessionsParams) {
  const queryParams = new URLSearchParams();
  queryParams.append('classId', String(params.classId));
  queryParams.append('date', params.date);
  queryParams.append('shift', params.shift);

  return apiFetch(`/attendance-sessions?${queryParams.toString()}`);
}

export interface CreateAttendanceSessionData {
  classId: number;
  teacherId: number;
  date: string;
  shift: string;
}

export async function createAttendanceSession(data: CreateAttendanceSessionData) {
  return apiFetch('/attendance-sessions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
}

export interface AttendanceDetailEntry {
  studentId: number;
  status: string;
  note: string;
}

export async function saveAttendanceDetails(sessionId: number, entries: AttendanceDetailEntry[]) {
  return apiFetch(`/attendance-sessions/${sessionId}/details`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sessionId, entries })
  });
}

export async function closeAttendanceSession(sessionId: number) {
  return apiFetch(`/attendance-sessions/${sessionId}/close`, {
    method: 'POST'
  });
}
