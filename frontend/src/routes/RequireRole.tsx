import { Navigate, Outlet } from 'react-router-dom';
import { getDashboardPathForRole, getDashboardPathForUser } from '@/features/auth/roleUtils';
import type { Role } from '@/types';
import { useAuth } from '@/hooks/useAuth';

export const RequireRole = ({ role }: { role: Role }) => {
  const { isAuthenticated, hasRole, getPrimaryRole, user } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/auth/login" replace />;
  }

  if (hasRole(role)) {
    return <Outlet />;
  }

  const primaryRole = getPrimaryRole();
  if (import.meta.env.DEV) {
    console.info('[auth] protected route denied', { requestedRole: role, primaryRole, approvalStatus: user?.approvalStatus, redirectPath: getDashboardPathForUser(user) ?? getDashboardPathForRole(primaryRole) ?? '/auth/login' });
  }
  return <Navigate to={getDashboardPathForUser(user) ?? getDashboardPathForRole(primaryRole) ?? '/auth/login'} replace />;
};
