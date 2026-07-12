const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

function resolveApiUrl(path: string): string {
  const base = API_BASE.replace(/\/+$/, '');
  let normalizedPath = path.startsWith('/') ? path : `/${path}`;
  if (base.endsWith('/api') && normalizedPath.startsWith('/api/')) {
    normalizedPath = normalizedPath.slice(4);
  }
  return `${base}${normalizedPath}`;
}

export function getToken(): string | null {
  return localStorage.getItem('admin_token');
}

export function setToken(token: string) {
  localStorage.setItem('admin_token', token);
}

export function clearToken() {
  localStorage.removeItem('admin_token');
}

export async function apiFetch(path: string, options: RequestInit = {}): Promise<any> {
  const token = getToken();
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string> || {}),
  };
  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }
  if (token) headers['Authorization'] = `Bearer ${token}`;

  let res: Response;
  try {
    res = await fetch(resolveApiUrl(path), { ...options, headers, cache: 'no-store' });
  } catch {
    throw new Error('Không thể kết nối tới máy chủ. Vui lòng kiểm tra backend và thử lại.');
  }

  if (res.status === 401 || res.status === 403) {
    clearToken();
    window.location.reload();
    throw new Error('Unauthorized');
  }

  let json: any;
  try {
    json = await res.json();
  } catch {
    throw new Error(`Máy chủ trả về dữ liệu không hợp lệ (HTTP ${res.status}).`);
  }
  if (!res.ok || !json.success) throw new Error(json.message || `Thao tác thất bại (HTTP ${res.status}).`);
  return json.data;
}
