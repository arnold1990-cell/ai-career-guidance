import { Navigate, Outlet } from 'react-router-dom';
import { getDashboardPathForRole } from '@/features/auth/roleUtils';
import type { Role } from '@/types';
import { useAuth } from '@/hooks/useAuth';

export const RequireRole = ({ role }: { role: Role }) => {
  const { isAuthenticated, hasRole, getPrimaryRole } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/auth/login" replace />;
  }

  if (hasRole(role)) {
    return <Outlet />;
  }

  const primaryRole = getPrimaryRole();
  return <Navigate to={primaryRole ? getDashboardPathForRole(primaryRole) : '/auth/login'} replace />;
};
