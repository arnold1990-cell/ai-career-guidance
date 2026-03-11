import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAppQuery } from '@/hooks/useAppQuery';
import { companyService } from '@/services/companyService';

type CompanyProfileStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'MORE_INFO_REQUIRED';

type CompanyProfile = {
  status: CompanyProfileStatus;
};

const allowedPendingRoutes = ['/company/pending', '/company/profile', '/company/verification-docs', '/company/settings'];

export const RequireCompanyApproval = () => {
  const location = useLocation();
  const profile = useAppQuery<CompanyProfile>({
    queryKey: ['company', 'approval-status'],
    queryFn: () => companyService.getMe(),
  });

  if (profile.isLoading) {
    return <div className="p-6 text-sm text-slate-500">Loading company approval status...</div>;
  }

  if (profile.error) {
    return <div className="p-6 text-sm text-red-600">Unable to load company approval status.</div>;
  }

  if (profile.data?.status === 'APPROVED') {
    return <Outlet />;
  }

  if (allowedPendingRoutes.some((route) => location.pathname.startsWith(route))) {
    return <Outlet />;
  }

  return <Navigate to="/company/pending" replace />;
};
