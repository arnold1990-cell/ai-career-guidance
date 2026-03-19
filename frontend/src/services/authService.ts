import { apiClient } from '@/services/apiClient';
import { authStore } from '@/features/auth/authStore';
import { normalizeBackendRole } from '@/features/auth/roleUtils';
import type { AuthResponse, AuthResponseRaw, BackendRole, CompanyRegisterPayload, StudentRegisterPayload, User } from '@/types';

const decodeBase64Url = (value: string): string | null => {
  try {
    const padded = value.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(value.length / 4) * 4, '=');
    return atob(padded);
  } catch {
    return null;
  }
};

const getRolesFromAccessToken = (accessToken?: string): string[] => {
  if (!accessToken) return [];

  const [, payload] = accessToken.split('.');
  if (!payload) return [];

  const decodedPayload = decodeBase64Url(payload);
  if (!decodedPayload) return [];

  try {
    const claims = JSON.parse(decodedPayload) as { roles?: unknown; role?: unknown; authorities?: unknown };
    if (Array.isArray(claims.roles)) return claims.roles.filter((role): role is string => typeof role === 'string');
    if (Array.isArray(claims.authorities)) return claims.authorities.filter((role): role is string => typeof role === 'string');
    if (typeof claims.role === 'string') return [claims.role];
  } catch {
    return [];
  }

  return [];
};

const normalizeRoles = (roles: string[]): BackendRole[] => Array.from(
  new Set(roles.map((role) => normalizeBackendRole(role)).filter((role): role is BackendRole => Boolean(role))),
);

const normalizeAuthResponse = (payload: AuthResponseRaw): AuthResponse => {
  const responseRoles = payload.user?.roles ?? payload.roles ?? (payload.user?.role ? [payload.user.role] : payload.role ? [payload.role] : []);
  const normalizedRoles = normalizeRoles([...responseRoles, ...getRolesFromAccessToken(payload.accessToken)]);

  const user: User = {
    id: payload.user?.id ?? '',
    email: payload.user?.email ?? '',
    fullName: payload.user?.fullName,
    companyName: payload.user?.companyName,
    roles: normalizedRoles,
  };

  return {
    accessToken: payload.accessToken ?? '',
    refreshToken: payload.refreshToken,
    tokenType: payload.tokenType,
    accessTokenExpiresIn: payload.accessTokenExpiresIn,
    user,
  };
};

export const authService = {
  login: (payload: { email: string; password: string }) => {
    authStore.clear();
    return apiClient.post<AuthResponseRaw>('/auth/login', payload).then((response) => {
      if (import.meta.env.DEV) {
        console.info('[auth] login response', response.data);
      }
      return normalizeAuthResponse(response.data);
    });
  },
  registerStudent: (payload: StudentRegisterPayload) => apiClient.post<AuthResponseRaw>('/auth/register/student', payload).then((r) => normalizeAuthResponse(r.data)),
  registerCompany: (payload: CompanyRegisterPayload) => apiClient.post<AuthResponseRaw>('/auth/register/company', payload).then((r) => normalizeAuthResponse(r.data)),
  forgotPassword: (payload: { email?: string; mobileNumber?: string }) => apiClient.post('/auth/forgot-password', payload),
  resetPassword: (payload: { token: string; newPassword: string; confirmPassword: string }) => apiClient.post('/auth/reset-password', payload),
  logout: () => apiClient.post('/auth/logout'),
};
