import { apiFetch } from './client';

export interface GetUsersParams {
  role?: string;
  status?: string;
  keyword?: string;
  page?: number;
  size?: number;
}

export async function getUsers(params: GetUsersParams = {}) {
  const queryParams = new URLSearchParams();
  if (params.role) queryParams.append('role', params.role);
  if (params.status) queryParams.append('status', params.status);
  if (params.keyword) queryParams.append('keyword', params.keyword);
  if (params.page !== undefined) queryParams.append('page', String(params.page));
  if (params.size !== undefined) queryParams.append('size', String(params.size));

  return apiFetch(`/admin/users?${queryParams.toString()}`);
}

export async function updateUserStatus(userId: number, status: string) {
  return apiFetch(`/admin/users/${userId}/status`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status })
  });
}

export async function importTeachers(formData: FormData) {
  return apiFetch('/import/teachers', {
    method: 'POST',
    body: formData
  });
}
