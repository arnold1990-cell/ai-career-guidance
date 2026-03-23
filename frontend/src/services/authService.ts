import { apiClient } from '@/services/apiClient';
import { authStore } from '@/features/auth/authStore';
import { normalizeBackendRole } from '@/features/auth/roleUtils';
import type { ApprovalStatus, AuthResponse, AuthResponseRaw, BackendRole, CompanyRegisterPayload, RegistrationResponse, StudentRegisterPayload, User, VerificationStatusResponse } from '@/types';

const decodeBase64Url = (value: string): string | null => {
  try {
    const padded = value.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(value.length / 4) * 4, '=');
    return atob(padded);
  } catch {
    return null;
  }
};

const getTokenPayload = (accessToken?: string): { roles?: unknown; role?: unknown; authorities?: unknown; primaryRole?: unknown; approvalStatus?: unknown } | null => {
  if (!accessToken) return null;

  const [, payload] = accessToken.split('.');
  if (!payload) return null;

  const decodedPayload = decodeBase64Url(payload);
  if (!decodedPayload) return null;

  try {
    return JSON.parse(decodedPayload) as { roles?: unknown; role?: unknown; authorities?: unknown; primaryRole?: unknown; approvalStatus?: unknown };
  } catch {
    return null;
  }
};

const getRolesFromAccessToken = (accessToken?: string): string[] => {
  const claims = getTokenPayload(accessToken);
  if (!claims) return [];
  if (Array.isArray(claims.roles)) return claims.roles.filter((role): role is string => typeof role === 'string');
  if (Array.isArray(claims.authorities)) return claims.authorities.filter((role): role is string => typeof role === 'string');
  if (typeof claims.role === 'string') return [claims.role];
  return [];
};

const normalizeApprovalStatus = (status?: string | null): ApprovalStatus | undefined => {
  if (!status) return undefined;
  return ['PENDING', 'APPROVED', 'REJECTED', 'MORE_INFO_REQUIRED'].includes(status) ? status as ApprovalStatus : undefined;
};

const normalizeRoles = (roles: string[]): BackendRole[] => Array.from(
  new Set(roles.map((role) => normalizeBackendRole(role)).filter((role): role is BackendRole => Boolean(role))),
);

const normalizeAuthResponse = (payload: AuthResponseRaw): AuthResponse => {
  const tokenPayload = getTokenPayload(payload.accessToken);
  const responseRoles = payload.user?.roles ?? payload.roles ?? (payload.user?.role ? [payload.user.role] : payload.role ? [payload.role] : []);
  const normalizedRoles = normalizeRoles([...responseRoles, ...getRolesFromAccessToken(payload.accessToken)]);
  const normalizedPrimaryRole = normalizeBackendRole(payload.user?.primaryRole ?? payload.primaryRole ?? payload.user?.role ?? payload.role);
  const approvalStatus = normalizeApprovalStatus(payload.user?.approvalStatus ?? payload.approvalStatus ?? (typeof tokenPayload?.approvalStatus === 'string' ? tokenPayload.approvalStatus : undefined));

  const user: User = {
    id: payload.user?.id ?? '',
    email: payload.user?.email ?? '',
    fullName: payload.user?.fullName,
    companyName: payload.user?.companyName,
    roles: normalizedRoles,
    role: (normalizedPrimaryRole ?? normalizedRoles[0])?.replace('ROLE_', '') as User['role'] | undefined,
    primaryRole: normalizedPrimaryRole ?? normalizedRoles[0],
    approvalStatus,
  };

  return {
    accessToken: payload.accessToken ?? '',
    refreshToken: payload.refreshToken,
    tokenType: payload.tokenType,
    accessTokenExpiresIn: payload.accessTokenExpiresIn,
    role: user.role,
    primaryRole: user.primaryRole,
    user,
  };
};

export const authService = {
  login: (payload: { email: string; password: string }) => {
    authStore.clear();
    return apiClient.post<AuthResponseRaw>('/auth/login', payload).then((response) => normalizeAuthResponse(response.data));
  },
  registerStudent: (payload: StudentRegisterPayload) => apiClient.post<RegistrationResponse>('/auth/register/student', payload).then((r) => r.data),
  registerCompany: (payload: CompanyRegisterPayload) => apiClient.post<RegistrationResponse>('/auth/register/company', payload).then((r) => r.data),
  verifyEmail: (token: string) => apiClient.get<VerificationStatusResponse>(`/auth/verify-email?token=${encodeURIComponent(token)}`).then((r) => r.data),
  resendVerification: (email: string) => apiClient.post<VerificationStatusResponse>('/auth/resend-verification', { email }).then((r) => r.data),
  forgotPassword: (payload: { email?: string; mobileNumber?: string }) => apiClient.post('/auth/forgot-password', payload),
  resetPassword: (payload: { token: string; newPassword: string; confirmPassword: string }) => apiClient.post('/auth/reset-password', payload),
  logout: () => apiClient.post('/auth/logout'),
};
