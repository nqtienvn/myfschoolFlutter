import { apiFetch, publicApiFetch, setToken, clearToken, getToken } from './client';

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
  const data = await apiFetch('/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ phone, password })
  });

  if (data.user.role !== 'ADMIN') {
    throw new Error('Tài khoản không có quyền truy cập trang quản trị');
  }

  setToken(data.token);
  return data.user;
}

export function isAdminLoggedIn(): boolean {
  return !!getToken();
}

export function logout() {
  clearToken();
}

export interface PasswordResetValidation {
  valid: boolean;
  status: 'VALID' | 'INVALID' | 'EXPIRED' | 'USED' | 'DISABLED';
}

export async function requestPasswordReset(phone: string): Promise<void> {
  await publicApiFetch('/auth/password-reset/request', {
    method: 'POST',
    body: JSON.stringify({ phone }),
  });
}

export function validatePasswordReset(token: string): Promise<PasswordResetValidation> {
  return publicApiFetch('/auth/password-reset/validate', {
    method: 'POST',
    body: JSON.stringify({ token }),
  });
}

export async function confirmPasswordReset(
  token: string,
  newPassword: string,
  confirmPassword: string,
): Promise<void> {
  await publicApiFetch('/auth/password-reset/confirm', {
    method: 'POST',
    body: JSON.stringify({ token, newPassword, confirmPassword }),
  });
}
