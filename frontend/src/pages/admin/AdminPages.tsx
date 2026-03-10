import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/tables/DataTable';
import { MetricCard } from '@/components/cards/MetricCard';
import { PlaceholderChart } from '@/components/charts/PlaceholderChart';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import { useAppQuery } from '@/hooks/useAppQuery';
import { adminService } from '@/services/adminService';
import { analyticsService } from '@/services/analyticsService';

const Header = ({ title, subtitle }: { title: string; subtitle: string }) => (
  <div>
    <h1 className="text-2xl font-bold">{title}</h1>
    <p className="text-sm text-slate-600">{subtitle}</p>
  </div>
);

type CompanyReview = {
  id: string;
  companyName: string;
  registrationNumber: string;
  industry?: string;
  officialEmail: string;
  mobileNumber?: string;
  contactPersonName?: string;
  status: string;
  reviewNotes?: string;
};

export const AdminDashboardPage = () => {
  const users = useAppQuery({ queryKey: ['admin', 'users'], queryFn: () => adminService.getUsers() });
  const pendingCompanies = useAppQuery<CompanyReview[]>({ queryKey: ['admin', 'pending-companies-count'], queryFn: () => adminService.listPendingCompanies() });
  return (
    <section className="space-y-6">
      <Header title="Admin Dashboard" subtitle="Monitor platform health, operational queues, and governance metrics." />
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard title="Total users" value={Array.isArray(users.data) ? users.data.length : 0} />
        <MetricCard title="Pending company reviews" value={Array.isArray(pendingCompanies.data) ? pendingCompanies.data.length : 0} />
        <MetricCard title="Monthly revenue" value="R 184,200" />
        <MetricCard title="System incidents" value={2} subtitle="Open incidents this week" />
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <PlaceholderChart title="User growth and subscription trend" />
        <div className="card p-5"><h2 className="font-semibold">Operational highlights</h2><ul className="mt-3 space-y-2 text-sm text-slate-600"><li>• Company review queue synchronized with company onboarding APIs.</li></ul></div>
      </div>
    </section>
  );
};

export const AdminUsersPage = () => {
  const users = useAppQuery({ queryKey: ['admin', 'users-management'], queryFn: () => adminService.getUsers() });
  const rows = Array.isArray(users.data) ? users.data : [];
  return <section className="space-y-6"><Header title="Users Management" subtitle="Search, filter, and manage user account status across all roles." />{rows.length === 0 ? <EmptyState title="No users" message="No user records found." /> : <DataTable columns={[{ key: 'fullName', header: 'User' }, { key: 'email', header: 'Email' }, { key: 'role', header: 'Role' }, { key: 'active', header: 'Status', render: (row) => <Badge color={row.active ? 'emerald' : 'amber'}>{row.active ? 'Active' : 'Suspended'}</Badge> }]} data={rows} />}</section>;
};

export const AdminRolesPage = () => {
  const roles = useAppQuery({ queryKey: ['admin', 'roles'], queryFn: () => adminService.getRoles() });
  const rows = Array.isArray(roles.data) ? roles.data : [];
  return <section className="space-y-6"><Header title="Roles Management" subtitle="Maintain role templates and permissions for administrative teams." />{rows.length === 0 ? <EmptyState title="No roles" message="No role definitions found." /> : <DataTable columns={[{ key: 'name', header: 'Role' }, { key: 'permissions', header: 'Permissions' }]} data={rows} />}</section>;
};

export const AdminPendingApprovalsPage = () => {
  const pending = useAppQuery<CompanyReview[]>({ queryKey: ['admin', 'pending-companies'], queryFn: () => adminService.listPendingCompanies() });
  const rows = Array.isArray(pending.data) ? pending.data : [];
  if (pending.isLoading) return <LoadingState />;
  if (pending.error) return <ErrorState message="Unable to load pending companies." />;

  return (
    <section className="space-y-6">
      <Header title="Company Approval Queue" subtitle="Review and process company onboarding submissions." />
      {rows.length === 0 ? <EmptyState title="Queue is clear" message="There are no pending company approval requests." /> : (
        <DataTable columns={[
          { key: 'companyName', header: 'Company' },
          { key: 'officialEmail', header: 'Official Email' },
          { key: 'registrationNumber', header: 'Registration Number' },
          { key: 'id', header: 'Actions', render: (row) => <Link className="text-primary-600" to={`/admin/companies/${row.id}`}>Review</Link> },
        ]} data={rows} />
      )}
    </section>
  );
};

export const AdminCompanyReviewPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [feedback, setFeedback] = useState('');
  const detail = useAppQuery<CompanyReview>({ queryKey: ['admin', 'company-detail', id], enabled: Boolean(id), queryFn: () => adminService.getCompanyDetail(String(id)) });
  const { register, handleSubmit } = useForm<{ notes: string }>({ defaultValues: { notes: '' } });

  if (detail.isLoading) return <LoadingState />;
  if (detail.error || !detail.data) return <ErrorState message="Unable to load company details." />;

  return (
    <section className="space-y-6">
      <Header title="Review Company" subtitle="Approve, reject, or request additional information." />
      <div className="card p-5 text-sm space-y-2">
        <p><span className="font-semibold">Company:</span> {detail.data.companyName}</p>
        <p><span className="font-semibold">Registration Number:</span> {detail.data.registrationNumber}</p>
        <p><span className="font-semibold">Industry:</span> {detail.data.industry ?? '-'}</p>
        <p><span className="font-semibold">Email:</span> {detail.data.officialEmail}</p>
        <p><span className="font-semibold">Status:</span> <Badge color="amber">{detail.data.status}</Badge></p>
      </div>
      <form className="card p-5 space-y-3" onSubmit={handleSubmit(async ({ notes }) => {
        await adminService.approveCompany(String(id), notes);
        setFeedback('Company approved successfully.');
        navigate('/admin/pending-approvals');
      })}>
        <label className="text-sm">Admin notes<textarea className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" rows={3} {...register('notes')} /></label>
        <div className="flex gap-3">
          <Button type="submit">Approve</Button>
          <Button type="button" className="bg-red-600 hover:bg-red-500" onClick={handleSubmit(async ({ notes }) => {
            await adminService.rejectCompany(String(id), notes);
            navigate('/admin/pending-approvals');
          })}>Reject</Button>
          <Button type="button" className="bg-amber-600 hover:bg-amber-500" onClick={handleSubmit(async ({ notes }) => {
            await adminService.requestCompanyMoreInfo(String(id), notes);
            navigate('/admin/pending-approvals');
          })}>Request more info</Button>
        </div>
        {feedback ? <p className="text-sm text-emerald-700">{feedback}</p> : null}
      </form>
    </section>
  );
};

export const AdminBursaryModerationPage = () => <section className="space-y-6"><Header title="Bursary Moderation" subtitle="Assess bursary quality, compliance, and fairness before publishing." /><div className="card p-5 text-sm text-slate-600">Moderation queue is synced with backend review APIs.</div></section>;
export const AdminSubscriptionsPage = () => <section className="space-y-6"><Header title="Subscriptions" subtitle="Track active plans and churn indicators." /></section>;
export const AdminPaymentsPage = () => <section className="space-y-6"><Header title="Payments" subtitle="Monitor transaction statuses and settlement summaries." /></section>;
export const AdminNotificationTemplatesPage = () => <section className="space-y-6"><Header title="Notification Templates" subtitle="Configure platform-wide communication templates." /></section>;
export const AdminAnalyticsPage = () => { useAppQuery({ queryKey: ['admin', 'analytics'], queryFn: () => analyticsService.adminOverview() }); return <section className="space-y-6"><Header title="Analytics" subtitle="Explore trends across platform metrics." /></section>; };
export const AdminAuditLogsPage = () => <section className="space-y-6"><Header title="Audit Logs" subtitle="Review administrator actions and critical system events." /></section>;
export const AdminSettingsPage = () => <section className="space-y-6"><Header title="Settings" subtitle="Control global policies and operational thresholds." /></section>;
