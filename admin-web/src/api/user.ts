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
  subjects: { id: number; name: string; code: string }[];
  teachingAssignments: TeacherYearAssignment[];
  homeroomClasses: TeacherHomeroom[];
}

export interface TeacherYearAssignment {
  id: number;
  classId: number;
  className: string;
  subjectId: number;
  subjectName: string;
  subjectCode: string;
}

export interface TeacherHomeroom {
  id: number;
  classId: number;
  className: string;
}

export interface TeacherManagementSummary {
  total: number;
  active: number;
  locked: number;
  unassigned: number;
  homeroom: number;
}

export interface TeacherAccountCredential {
  teacher: TeacherItem;
  temporaryPassword: string;
}

export interface TeacherPage {
  content: TeacherItem[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface GetTeachersParams {
  status?: string;
  keyword?: string;
  subjectId?: number;
  academicYearId?: number;
  page?: number;
  size?: number;
}

export interface CreateTeacherAccountRequest {
  phone: string;
  name: string;
  email?: string;
  subjectIds: number[];
}

export interface UpdateTeacherProfileRequest {
  phone: string;
  name: string;
  email?: string;
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

export async function createTeacherAccount(data: CreateTeacherAccountRequest): Promise<TeacherAccountCredential> {
  return apiFetch('/admin/users/teachers', {
    method: 'POST',
    body: JSON.stringify(data)
  });
}

export async function getTeachers(params: GetTeachersParams = {}): Promise<TeacherPage> {
  const queryParams = new URLSearchParams();
  if (params.status) queryParams.append('status', params.status);
  if (params.keyword) queryParams.append('keyword', params.keyword);
  if (params.subjectId !== undefined) queryParams.append('subjectId', String(params.subjectId));
  if (params.academicYearId !== undefined) queryParams.append('academicYearId', String(params.academicYearId));
  if (params.page !== undefined) queryParams.append('page', String(params.page));
  if (params.size !== undefined) queryParams.append('size', String(params.size));

  return apiFetch(`/admin/users/teachers?${queryParams.toString()}`);
}

export async function getTeacherManagementSummary(academicYearId?: number): Promise<TeacherManagementSummary> {
  const query = academicYearId === undefined ? '' : `?academicYearId=${academicYearId}`;
  return apiFetch(`/admin/users/teachers/summary${query}`);
}

export async function updateTeacherProfile(
  teacherId: number,
  data: UpdateTeacherProfileRequest,
  academicYearId?: number,
): Promise<TeacherItem> {
  const query = academicYearId === undefined ? '' : `?academicYearId=${academicYearId}`;
  return apiFetch(`/admin/users/teachers/${teacherId}${query}`, {
    method: 'PUT',
    body: JSON.stringify(data)
  });
}

export async function resetTeacherPassword(teacherId: number): Promise<TeacherAccountCredential> {
  return apiFetch(`/admin/users/teachers/${teacherId}/reset-password`, { method: 'POST' });
}

export async function updateTeacherSubjects(teacherId: number, subjectIds: number[]) {
  return apiFetch(`/admin/users/teachers/${teacherId}/subjects`, {
    method: 'PUT',
    body: JSON.stringify({ subjectIds })
  });
}
