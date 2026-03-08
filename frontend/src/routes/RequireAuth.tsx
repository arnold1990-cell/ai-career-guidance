import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';

export const RequireAuth = () => {
  const { isAuthenticated, isHydrated } = useAuth();
  const location = useLocation();

  if (!isHydrated) {
    return <div className="p-6 text-sm text-slate-500">Loading your session...</div>;
  }

  return isAuthenticated ? <Outlet /> : <Navigate to="/auth/login" replace state={{ from: location }} />;
};
