import { Navigate, Outlet } from 'react-router-dom';
import type { Role } from '@/types';
import { useAuth } from '@/hooks/useAuth';

export const RequireRole = ({ role }: { role: Role }) => {
  const { user } = useAuth();
  return user?.role === role ? <Outlet /> : <Navigate to="/auth/login" replace />;
};
