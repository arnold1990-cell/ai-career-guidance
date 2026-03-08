import { Navigate, Outlet } from 'react-router-dom';
import type { Role } from '@/types';
import { useAuth } from '@/hooks/useAuth';

const roleHome: Record<Role, string> = {
  STUDENT: '/student/dashboard',
  COMPANY: '/company/dashboard',
  ADMIN: '/admin/dashboard',
};

export const RequireRole = ({ role }: { role: Role }) => {
  const { isAuthenticated, hasRole, getPrimaryRole } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/auth/login" replace />;
  }

  if (hasRole(role)) {
    return <Outlet />;
  }

  const primaryRole = getPrimaryRole();
  return <Navigate to={primaryRole ? roleHome[primaryRole] : '/auth/login'} replace />;
};
