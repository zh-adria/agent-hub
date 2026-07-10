const DEFAULT_TOKEN = 'mock-token';
const DEFAULT_TENANT_ID = 'tenant-001';

export function apiFetch(input: RequestInfo | URL, init: RequestInit = {}) {
  const headers = new Headers(init.headers || {});
  if (!headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${localStorage.getItem('accessToken') || DEFAULT_TOKEN}`);
  }
  if (!headers.has('X-Tenant-Id')) {
    headers.set('X-Tenant-Id', localStorage.getItem('tenantId') || DEFAULT_TENANT_ID);
  }
  return fetch(input, { ...init, headers });
}
