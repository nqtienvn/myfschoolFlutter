import { apiFetch } from './client';

export async function getSemesters(academicYearId?: number | string) {
  const path = academicYearId ? `/semesters?academicYearId=${academicYearId}` : '/semesters';
  return apiFetch(path);
}

export async function createSemester(data: { name: string; academicYearId: number; order: number; startDate: string; endDate: string }) {
  return apiFetch('/semesters', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
}

export async function setCurrentSemester(semesterId: number) {
  return apiFetch(`/semesters/${semesterId}/set-current`, {
    method: 'PUT'
  });
}

export async function deleteSemester(semesterId: number) {
  return apiFetch(`/semesters/${semesterId}`, {
    method: 'DELETE'
  });
}
