import type { BackendRole, Role, User } from '@/types';

const ROLE_PRIORITY: Role[] = ['ADMIN', 'COMPANY', 'STUDENT'];
const DASHBOARD_PATHS: Record<Role, string> = {
  ADMIN: '/admin/dashboard',
  COMPANY: '/company/dashboard',
  STUDENT: '/student/dashboard',
};

export const normalizeBackendRole = (role?: string | null): BackendRole | null => {
  if (!role) return null;
  const normalized = role.trim().toUpperCase();
  const prefixed = normalized.startsWith('ROLE_') ? normalized : `ROLE_${normalized}`;
  return ['ROLE_STUDENT', 'ROLE_COMPANY', 'ROLE_ADMIN'].includes(prefixed) ? (prefixed as BackendRole) : null;
};

export const getNormalizedUserRoles = (user: Pick<User, 'roles'> | null | undefined): BackendRole[] => Array.from(
  new Set((user?.roles ?? []).map((role) => normalizeBackendRole(role)).filter((role): role is BackendRole => Boolean(role))),
);

export const resolvePrimaryRole = (user: Pick<User, 'roles'> | null | undefined): Role | null => {
  const roles = new Set(getNormalizedUserRoles(user));
  const resolved = ROLE_PRIORITY.find((role) => roles.has(`ROLE_${role}` as BackendRole));
  return resolved ?? null;
};

export const getDashboardPathForRole = (role: Role | null): string | null => role ? DASHBOARD_PATHS[role] : null;

export const getDashboardPathForUser = (user: Pick<User, 'roles'> | null | undefined): string | null => getDashboardPathForRole(resolvePrimaryRole(user));

export const isAuthorizedPathForRole = (pathname: string | null | undefined, role: Role | null): boolean => {
  if (!pathname || !role) return false;
  return pathname === DASHBOARD_PATHS[role] || pathname.startsWith(`/${role.toLowerCase()}/`);
};
