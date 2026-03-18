import type { BackendRole, Role, User } from '@/types';

const ROLE_PRIORITY: Role[] = ['ADMIN', 'COMPANY', 'STUDENT'];

export const normalizeBackendRole = (role?: string | null): BackendRole | null => {
  if (!role) return null;
  const normalized = role.startsWith('ROLE_') ? role : `ROLE_${role}`;
  return ['ROLE_STUDENT', 'ROLE_COMPANY', 'ROLE_ADMIN'].includes(normalized) ? (normalized as BackendRole) : null;
};

export const getNormalizedUserRoles = (user: Pick<User, 'roles'> | null | undefined): BackendRole[] => Array.from(
  new Set((user?.roles ?? []).map((role) => normalizeBackendRole(role)).filter((role): role is BackendRole => Boolean(role))),
);

export const resolvePrimaryRole = (user: Pick<User, 'roles'> | null | undefined): Role | null => {
  const roles = new Set(getNormalizedUserRoles(user));
  const resolved = ROLE_PRIORITY.find((role) => roles.has(`ROLE_${role}` as BackendRole));
  return resolved ?? null;
};

export const getDashboardPathForRole = (role: Role | null): string => {
  if (role === 'ADMIN') return '/admin/dashboard';
  if (role === 'COMPANY') return '/company/dashboard';
  return '/student/dashboard';
};

export const getDashboardPathForUser = (user: Pick<User, 'roles'> | null | undefined): string => getDashboardPathForRole(resolvePrimaryRole(user));
