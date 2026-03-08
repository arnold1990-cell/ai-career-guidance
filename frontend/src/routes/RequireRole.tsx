import { Navigate, Outlet } from 'react-router-dom';
import type { BackendRole, Role } from '@/types';
import { useAuth } from '@/hooks/useAuth';

export const RequireRole = ({ role }: { role: Role }) => {
  const { user } = useAuth();
  const requiredBackendRole = `ROLE_${role}` as BackendRole;
  return user?.roles?.includes(requiredBackendRole) ? <Outlet /> : <Navigate to="/auth/login" replace />;
};
