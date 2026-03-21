import axios from 'axios';
import { authStore } from '@/features/auth/authStore';
import { normalizeBackendRole } from '@/features/auth/roleUtils';
import type { ApiError, ApprovalStatus, BackendRole, User } from '@/types';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

const normalizeApprovalStatus = (status?: string | null): ApprovalStatus | undefined => {
  if (!status) return undefined;
  return ['PENDING', 'APPROVED', 'REJECTED', 'MORE_INFO_REQUIRED'].includes(status) ? status as ApprovalStatus : undefined;
};

const normalizeRefreshedUser = (payload: unknown): User | null => {
  if (!payload || typeof payload !== 'object') return null;

  const data = payload as { user?: { id?: string; email?: string; fullName?: string; companyName?: string; roles?: string[]; role?: string; primaryRole?: string; approvalStatus?: string }; roles?: string[]; role?: string; primaryRole?: string; approvalStatus?: string };
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
    role: (normalizeBackendRole(data.user?.primaryRole ?? data.primaryRole ?? data.user?.role ?? data.role) ?? roles[0])?.replace('ROLE_', '') as User['role'] | undefined,
    primaryRole: normalizeBackendRole(data.user?.primaryRole ?? data.primaryRole ?? data.user?.role ?? data.role) ?? roles[0],
    approvalStatus: normalizeApprovalStatus(data.user?.approvalStatus ?? data.approvalStatus),
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
    const responseBody = error.response?.data;

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
      console.error(`[api] ${requestMethod} ${requestUrl} failed`, { status: responseStatus, response: responseBody, error });
    }

    const code = responseBody?.code as string | undefined;
    const fallbackMessage = !error.response
      ? 'Could not connect to the server. Please check your connection and try again.'
      : responseStatus === 503
        ? 'AI service is temporarily unavailable.'
        : responseStatus === 502
          ? 'Invalid AI provider configuration.'
          : responseStatus === 504
            ? 'Could not connect to the AI provider.'
            : 'Unexpected error occurred';
    const message = responseBody?.message ?? fallbackMessage;
    const normalized: ApiError = {
      message,
      status: error.response?.status,
      details: responseBody?.errors,
      code,
    };
    return Promise.reject(Object.assign(new Error(message), normalized));
  },
);
