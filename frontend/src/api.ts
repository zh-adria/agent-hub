export type AuthUser = {
  userId?: string;
  username?: string;
  displayName?: string;
  email?: string;
  tenantId?: string;
  roles?: string[];
  permissions?: string[];
};

export type LoginPayload = {
  username: string;
  password: string;
  tenantCode: string;
};

const TOKEN_KEY = 'accessToken';
const TENANT_KEY = 'tenantId';
const DEFAULT_TENANT_ID = import.meta.env.VITE_DEFAULT_TENANT_ID || 'default';

export function getAccessToken() {
  return localStorage.getItem(TOKEN_KEY) || '';
}

export function getTenantId() {
  return localStorage.getItem(TENANT_KEY) || DEFAULT_TENANT_ID;
}

export function setAuthSession(token: string, tenantId: string) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(TENANT_KEY, tenantId || DEFAULT_TENANT_ID);
}

export function clearAuthSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(TENANT_KEY);
}

export async function login(payload: LoginPayload) {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(data.message || '登录失败');
  }
  const token = data.accessToken || data.access_token;
  if (!token) {
    throw new Error('IAM 未返回 access token');
  }
  setAuthSession(token, payload.tenantCode || DEFAULT_TENANT_ID);
  return data;
}

export async function loadCurrentUser(): Promise<AuthUser> {
  const response = await apiFetch('/api/auth/me');
  if (!response.ok) {
    throw new Error('登录状态已失效');
  }
  const user = await response.json();
  if (user.tenantId) {
    localStorage.setItem(TENANT_KEY, user.tenantId);
  }
  return user;
}

export async function logout() {
  await apiFetch('/api/auth/logout', { method: 'POST' }).catch(() => undefined);
  clearAuthSession();
}

export function hasPermission(user: AuthUser | null | undefined, permission: string) {
  if (!permission) return true;
  const permissions = user?.permissions || [];
  return permissions.includes(permission);
}

export function hasAnyPermission(user: AuthUser | null | undefined, permissions: string[]) {
  return permissions.length === 0 || permissions.some(permission => hasPermission(user, permission));
}

export function formatScore(score: number | string | null | undefined): string {
  return Number(score || 0).toFixed(3);
}

export function formatDate(value: string | number | Date | null | undefined): string {
  if (!value) return '-';
  return new Date(value).toLocaleString();
}

export function formatTime(value: string | number | Date | null | undefined): string {
  if (!value) return '-';
  return new Date(value).toLocaleTimeString();
}

export function shortId(value: unknown, length = 10): string {
  const text = String(value ?? '');
  return text.length > length ? `${text.slice(0, length)}...` : text;
}

const SESSION_STATUS_LABELS: Record<string, string> = {
  ACTIVE: '进行中',
  active: '进行中',
  ENDED: '已结束',
  ended: '已结束',
  ERROR: '异常',
  error: '异常',
  closed: '已关闭'
};

export function formatStatus(status: string | null | undefined): string {
  return SESSION_STATUS_LABELS[status || ''] || status || '未知';
}

export function apiFetch(input: RequestInfo | URL, init: RequestInit = {}) {
  const headers = new Headers(init.headers || {});
  const token = getAccessToken();
  const tenantId = getTenantId();
  if (!headers.has('Authorization') && token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  if (!headers.has('X-Tenant-Id') && tenantId) {
    headers.set('X-Tenant-Id', tenantId);
  }
  return fetch(input, { ...init, headers });
}
