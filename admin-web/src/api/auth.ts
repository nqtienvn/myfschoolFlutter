import { apiFetch, setToken, clearToken, getToken } from './client';

export interface AdminUser {
  id: number;
  phone: string;
  name: string;
  email: string | null;
  role: string;
  status: string;
  createdAt: string;
}

export interface LoginResponse {
  token: string;
  user: AdminUser;
}

export async function login(phone: string, password: string): Promise<AdminUser> {
  const mockUser: AdminUser = {
    id: 1,
    phone: phone || '0900000000',
    name: 'Quản trị viên',
    email: 'admin@school.edu.vn',
    role: 'ADMIN',
    status: 'ACTIVE',
    createdAt: new Date().toISOString()
  };
  setToken("mock-token-for-dev");
  return mockUser;
}

export function isAdminLoggedIn(): boolean {
  return !!getToken();
}

export function logout() {
  clearToken();
}
