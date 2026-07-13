const DEFAULT_TOKEN = import.meta.env.VITE_DEFAULT_ACCESS_TOKEN || '';
const DEFAULT_TENANT_ID = import.meta.env.VITE_DEFAULT_TENANT_ID || '';

export function apiFetch(input: RequestInfo | URL, init: RequestInit = {}) {
  const headers = new Headers(init.headers || {});
  const token = localStorage.getItem('accessToken') || DEFAULT_TOKEN;
  const tenantId = localStorage.getItem('tenantId') || DEFAULT_TENANT_ID;
  if (!headers.has('Authorization')) {
    if (token) headers.set('Authorization', `Bearer ${token}`);
  }
  if (!headers.has('X-Tenant-Id')) {
    if (tenantId) headers.set('X-Tenant-Id', tenantId);
  }
  return fetch(input, { ...init, headers });
}
