import { apiFetch } from './client';

export async function getSubjects() {
  return apiFetch('/subjects');
}

export async function createSubject(data: { name: string; code: string }) {
  return apiFetch('/subjects', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
}

export async function deleteSubject(id: number) {
  return apiFetch(`/subjects/${id}`, {
    method: 'DELETE'
  });
}
