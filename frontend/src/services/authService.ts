import { apiClient } from '@/services/apiClient';
import { normalizeBackendRole } from '@/features/auth/roleUtils';
import type { AuthResponse, AuthResponseRaw, BackendRole, CompanyRegisterPayload, StudentRegisterPayload, User } from '@/types';

const normalizeAuthResponse = (payload: AuthResponseRaw): AuthResponse => {
  const userRoles = payload.user?.roles ?? payload.roles ?? (payload.user?.role ? [payload.user.role] : payload.role ? [payload.role] : []);
  const normalizedRoles = Array.from(new Set(userRoles.map((role) => normalizeBackendRole(role)).filter((role): role is BackendRole => Boolean(role))));

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
  forgotPassword: (payload: { email?: string; mobileNumber?: string }) => apiClient.post('/auth/forgot-password', payload),
  resetPassword: (payload: { token: string; newPassword: string; confirmPassword: string }) => apiClient.post('/auth/reset-password', payload),
  logout: () => apiClient.post('/auth/logout'),
};
