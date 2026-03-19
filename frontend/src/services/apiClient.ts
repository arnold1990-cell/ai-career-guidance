import axios from 'axios';
import { authStore } from '@/features/auth/authStore';
import { normalizeBackendRole } from '@/features/auth/roleUtils';
import type { ApiError, BackendRole, User } from '@/types';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

const normalizeRefreshedUser = (payload: unknown): User | null => {
  if (!payload || typeof payload !== 'object') return null;

  const data = payload as { user?: { id?: string; email?: string; fullName?: string; companyName?: string; roles?: string[]; role?: string; primaryRole?: string }; roles?: string[]; role?: string; primaryRole?: string };
  const rawRoles = data.user?.roles ?? data.roles ?? (data.user?.role ? [data.user.role] : data.role ? [data.role] : []);
  const roles = Array.from(new Set(rawRoles
    .map((role) => normalizeBackendRole(role))
    .filter((role): role is BackendRole => Boolean(role))));

  if (!roles.length) return null;

  return {
    id: data.user?.id ?? '',
    email: data.user?.email ?? '',
    fullName: data.user?.fullName,
    companyName: data.user?.companyName,
    roles,
    primaryRole: normalizeBackendRole(data.user?.primaryRole ?? data.primaryRole ?? data.user?.role ?? data.role) ?? roles[0],
  };
};

export const apiClient = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  const token = authStore.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const requestMethod = error.config?.method?.toUpperCase() ?? 'UNKNOWN';
    const requestUrl = error.config?.url ?? 'UNKNOWN_URL';
    const responseStatus = error.response?.status;

    if (error.response?.status === 401 && authStore.getRefreshToken()) {
      try {
        const response = await axios.post(`${baseURL}/auth/refresh`, {
          refreshToken: authStore.getRefreshToken(),
        });
        authStore.setTokens(response.data.accessToken, response.data.refreshToken);
        const refreshedUser = normalizeRefreshedUser(response.data);
        if (refreshedUser) {
          if (import.meta.env.DEV) {
            console.info('[auth] restored session role', { email: refreshedUser.email, roles: refreshedUser.roles, primaryRole: refreshedUser.primaryRole });
          }
          authStore.setUser(refreshedUser);
        }
        error.config.headers.Authorization = `Bearer ${response.data.accessToken}`;
        return apiClient.request(error.config);
      } catch {
        authStore.clear();
      }
    }
    if (import.meta.env.DEV) {
      console.error(`[api] ${requestMethod} ${requestUrl} failed`, { status: responseStatus, response: error.response?.data });
    }

    const normalized: ApiError = {
      message: error.response?.data?.message ?? 'Unexpected error occurred',
      status: error.response?.status,
      details: error.response?.data?.errors,
    };
    return Promise.reject(normalized);
  },
);
