import { apiClient } from '@/services/apiClient';
import { authStore } from '@/features/auth/authStore';
import { normalizeBackendRole } from '@/features/auth/roleUtils';
import type { ApprovalStatus, AuthResponse, AuthResponseRaw, BackendRole, CompanyRegisterPayload, StudentRegisterPayload, User } from '@/types';

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

const normalizeApprovalStatus = (status?: string | null): ApprovalStatus | undefined => {
  if (!status) return undefined;
  return ['PENDING', 'APPROVED', 'REJECTED', 'MORE_INFO_REQUIRED'].includes(status) ? status as ApprovalStatus : undefined;
};

const normalizeRoles = (roles: string[]): BackendRole[] => Array.from(
  new Set(roles.map((role) => normalizeBackendRole(role)).filter((role): role is BackendRole => Boolean(role))),
);

const normalizeAuthResponse = (payload: AuthResponseRaw): AuthResponse => {
  const responseRoles = payload.user?.roles ?? payload.roles ?? (payload.user?.role ? [payload.user.role] : payload.role ? [payload.role] : []);
  const normalizedRoles = normalizeRoles([...responseRoles, ...getRolesFromAccessToken(payload.accessToken)]);
  const normalizedPrimaryRole = normalizeBackendRole(payload.user?.primaryRole ?? payload.primaryRole ?? payload.user?.role ?? payload.role);
  const approvalStatus = normalizeApprovalStatus(payload.user?.approvalStatus ?? payload.approvalStatus);

  if (import.meta.env.DEV) {
    console.info('[auth] frontend role normalization', {
      responseRoles,
      tokenRoles: getRolesFromAccessToken(payload.accessToken),
      normalizedRoles,
      normalizedPrimaryRole,
      approvalStatus,
    });
  }

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
    if (import.meta.env.DEV) {
      console.info('[auth] login payload', { email: payload.email, passwordLength: payload.password.length });
    }
    return apiClient.post<AuthResponseRaw>('/auth/login', payload)
      .then((response) => {
        if (import.meta.env.DEV) {
          console.info('[auth] login response', { status: response.status, body: response.data });
        }
        return normalizeAuthResponse(response.data);
      })
      .catch((error: unknown) => {
        if (import.meta.env.DEV) {
          console.error('[auth] login request failed', {
            payload: { email: payload.email, passwordLength: payload.password.length },
            error,
          });
        }
        throw error;
      });
  },
  registerStudent: (payload: StudentRegisterPayload) => apiClient.post<AuthResponseRaw>('/auth/register/student', payload).then((r) => normalizeAuthResponse(r.data)),
  registerCompany: (payload: CompanyRegisterPayload) => apiClient.post<AuthResponseRaw>('/auth/register/company', payload).then((r) => normalizeAuthResponse(r.data)),
  forgotPassword: (payload: { email?: string; mobileNumber?: string }) => apiClient.post('/auth/forgot-password', payload),
  resetPassword: (payload: { token: string; newPassword: string; confirmPassword: string }) => apiClient.post('/auth/reset-password', payload),
  logout: () => apiClient.post('/auth/logout'),
};
