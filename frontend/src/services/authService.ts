import { apiClient } from '@/services/apiClient';
import type { AuthResponse, AuthResponseRaw, BackendRole, CompanyRegisterPayload, StudentRegisterPayload, User } from '@/types';

const normalizeRole = (role?: string): BackendRole | null => {
  if (!role) return null;
  const normalized = role.startsWith('ROLE_') ? role : `ROLE_${role}`;
  return ['ROLE_STUDENT', 'ROLE_COMPANY', 'ROLE_ADMIN'].includes(normalized) ? (normalized as BackendRole) : null;
};

const normalizeAuthResponse = (payload: AuthResponseRaw): AuthResponse => {
  const userRoles = payload.user?.roles ?? payload.roles ?? (payload.user?.role ? [payload.user.role] : payload.role ? [payload.role] : []);
  const normalizedRoles = Array.from(new Set(userRoles.map((role) => normalizeRole(role)).filter((role): role is BackendRole => Boolean(role))));

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
  login: (payload: { email: string; password: string }) => apiClient.post<AuthResponseRaw>('/auth/login', payload).then((r) => normalizeAuthResponse(r.data)),
  registerStudent: (payload: StudentRegisterPayload) => apiClient.post<AuthResponseRaw>('/auth/register/student', payload).then((r) => normalizeAuthResponse(r.data)),
  registerCompany: (payload: CompanyRegisterPayload) => apiClient.post<AuthResponseRaw>('/auth/register/company', payload).then((r) => normalizeAuthResponse(r.data)),
  forgotPassword: (email: string) => apiClient.post('/auth/forgot-password', { email }),
  resetPassword: (payload: { token: string; newPassword: string }) => apiClient.post('/auth/reset-password', payload),
  logout: () => apiClient.post('/auth/logout'),
};
