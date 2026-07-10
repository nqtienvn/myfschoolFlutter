import { apiFetch } from './client';

export interface GetUsersParams {
  role?: string;
  status?: string;
  keyword?: string;
  page?: number;
  size?: number;
}

export interface TeacherItem {
  id: number;
  userId: number;
  phone: string;
  name: string;
  email: string;
  status: string;
  employeeCode: string;
  department: string;
  avatar: string;
  subjects: { id: number; name: string; code: string }[];
}

export interface GetTeachersParams {
  status?: string;
  keyword?: string;
  subjectId?: number;
  page?: number;
  size?: number;
}

export interface CreateTeacherAccountRequest {
  phone: string;
  name: string;
  email?: string;
  employeeCode: string;
  department?: string;
  subjectIds: number[];
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

export async function createTeacherAccount(data: CreateTeacherAccountRequest) {
  return apiFetch('/admin/users/teachers', {
    method: 'POST',
    body: JSON.stringify(data)
  });
}

export async function getTeachers(params: GetTeachersParams = {}) {
  const queryParams = new URLSearchParams();
  if (params.status) queryParams.append('status', params.status);
  if (params.keyword) queryParams.append('keyword', params.keyword);
  if (params.subjectId !== undefined) queryParams.append('subjectId', String(params.subjectId));
  if (params.page !== undefined) queryParams.append('page', String(params.page));
  if (params.size !== undefined) queryParams.append('size', String(params.size));

  return apiFetch(`/admin/users/teachers?${queryParams.toString()}`);
}

export async function updateTeacherSubjects(teacherId: number, subjectIds: number[]) {
  return apiFetch(`/admin/users/teachers/${teacherId}/subjects`, {
    method: 'PUT',
    body: JSON.stringify({ subjectIds })
  });
}
