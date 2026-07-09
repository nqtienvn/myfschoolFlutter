import { apiFetch } from './client';

export async function getAcademicYears() {
  return apiFetch('/academic-years');
}

export async function createAcademicYear(data: { name: string; startDate: string; endDate: string; status: string }) {
  return apiFetch('/academic-years', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
}

export async function updateAcademicYearStatus(id: number, status: string) {
  return apiFetch(`/academic-years/${id}/status`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status })
  });
}

export async function initializeAcademicYear(targetYearId: number, data: any) {
  return apiFetch(`/academic-years/${targetYearId}/initialize`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
}

export async function generate10Years() {
  return apiFetch('/academic-years/generate-10-years', {
    method: 'POST'
  });
}

export async function openAcademicYear(id: number) {
  return apiFetch(`/academic-years/${id}/open`, {
    method: 'POST'
  });
}

export async function openSemester2(id: number) {
  return apiFetch(`/academic-years/${id}/open-hk2`, {
    method: 'POST'
  });
}

export async function completeAcademicYear(id: number) {
  return apiFetch(`/academic-years/${id}/complete`, {
    method: 'POST'
  });
}

export async function getAcademicYearArchiveStats(id: number) {
  return apiFetch(`/academic-years/${id}/archive-stats`);
}
