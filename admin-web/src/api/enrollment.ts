import { apiFetch } from './client';

export async function importEnrollments(formData: FormData) {
  return apiFetch('/import/enrollments', {
    method: 'POST',
    body: formData
  });
}
