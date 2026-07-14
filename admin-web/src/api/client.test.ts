import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiFetch, getToken, setToken } from './client';

describe('apiFetch', () => {
  const storage = new Map<string, string>();
  const reload = vi.fn();
  const fetchMock = vi.fn<typeof fetch>();

  beforeEach(() => {
    storage.clear();
    reload.mockReset();
    fetchMock.mockReset();

    vi.stubGlobal('localStorage', {
      getItem: (key: string) => storage.get(key) ?? null,
      setItem: (key: string, value: string) => storage.set(key, value),
      removeItem: (key: string) => storage.delete(key),
    });
    vi.stubGlobal('window', { location: { reload } });
    vi.stubGlobal('fetch', fetchMock);
  });

  it('normalizes /api paths and sends the stored bearer token', async () => {
    setToken('admin-token');
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ success: true, data: { id: 7 } }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }));

    await expect(apiFetch('/api/users', {
      headers: { 'X-Request-Id': 'request-1' },
    })).resolves.toEqual({ id: 7 });

    expect(fetchMock).toHaveBeenCalledOnce();
    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toBe('http://localhost:8080/api/users');
    expect(options).toMatchObject({ cache: 'no-store' });
    expect(options?.headers).toMatchObject({
      Authorization: 'Bearer admin-token',
      'Content-Type': 'application/json',
      'X-Request-Id': 'request-1',
    });
  });

  it('does not force a JSON content type for FormData uploads', async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ success: true, data: null }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }));
    const body = new FormData();
    body.append('file', new Blob(['content']), 'students.csv');

    await apiFetch('/imports/students', { method: 'POST', body });

    const options = fetchMock.mock.calls[0][1];
    expect(options?.headers).not.toHaveProperty('Content-Type');
  });

  it('clears the session and reloads when the backend rejects authorization', async () => {
    setToken('expired-token');
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ success: false }), {
      status: 403,
      headers: { 'Content-Type': 'application/json' },
    }));

    await expect(apiFetch('/users')).rejects.toThrow('Unauthorized');

    expect(getToken()).toBeNull();
    expect(reload).toHaveBeenCalledOnce();
  });

  it('surfaces API, invalid JSON, and network failures', async () => {
    fetchMock.mockResolvedValueOnce(new Response(JSON.stringify({
      success: false,
      message: 'Academic year is required',
    }), {
      status: 400,
      headers: { 'Content-Type': 'application/json' },
    }));
    await expect(apiFetch('/classes')).rejects.toThrow('Academic year is required');

    fetchMock.mockResolvedValueOnce(new Response('<html>failure</html>', { status: 502 }));
    await expect(apiFetch('/classes')).rejects.toThrow('HTTP 502');

    fetchMock.mockRejectedValueOnce(new TypeError('connection refused'));
    await expect(apiFetch('/classes')).rejects.toThrow('Không thể kết nối tới máy chủ');
  });
});
