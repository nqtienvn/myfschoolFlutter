import { apiFetch } from './client';

export interface GetTeachingAssignmentsParams {
  classId?: number | string;
  semesterId?: number | string;
}

export async function getTeachingAssignments(params: GetTeachingAssignmentsParams = {}) {
  const queryParams = new URLSearchParams();
  if (params.classId !== undefined) queryParams.append('classId', String(params.classId));
  if (params.semesterId !== undefined) queryParams.append('semesterId', String(params.semesterId));

  return apiFetch(`/teaching-assignments?${queryParams.toString()}`);
}

export interface TeachingAssignmentData {
  classId: number;
  subjectId: number;
  teacherId: number;
  semesterId?: number;
  effectiveFrom?: string;
}

export async function createTeachingAssignment(data: TeachingAssignmentData) {
  return apiFetch('/teaching-assignments', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
}

export async function updateTeachingAssignment(id: number, data: TeachingAssignmentData) {
  return apiFetch(`/teaching-assignments/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
}

export async function deleteTeachingAssignment(id: number) {
  return apiFetch(`/teaching-assignments/${id}`, {
    method: 'DELETE'
  });
}
