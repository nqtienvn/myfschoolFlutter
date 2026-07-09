import { apiFetch } from './client';

export interface GetClassesParams {
  academicYearId?: number | string;
  page?: number;
  size?: number;
}

export async function getClasses(params: GetClassesParams = {}) {
  const queryParams = new URLSearchParams();
  if (params.academicYearId !== undefined) queryParams.append('academicYearId', String(params.academicYearId));
  if (params.page !== undefined) queryParams.append('page', String(params.page));
  if (params.size !== undefined) queryParams.append('size', String(params.size));

  return apiFetch(`/classes?${queryParams.toString()}`);
}

export async function createClass(data: {
  name: string;
  gradeLevel: number;
  academicYearId: number;
  schoolName: string;
  teacherCode?: string;
}) {
  return apiFetch('/classes', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
}

export async function deleteClass(id: number) {
  return apiFetch(`/classes/${id}`, {
    method: 'DELETE'
  });
}
