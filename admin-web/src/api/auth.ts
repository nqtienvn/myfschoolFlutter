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
  const data: LoginResponse = await apiFetch('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ phone, password }),
  });
  setToken(data.token);
  return data.user;
}

export function isAdminLoggedIn(): boolean {
  return !!getToken();
}

export function logout() {
  clearToken();
}
