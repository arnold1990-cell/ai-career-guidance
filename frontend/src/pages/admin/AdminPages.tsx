import { MetricCard } from '@/components/cards/MetricCard';
import { PageShell } from '@/pages/PageShell';

export const AdminDashboardPage = () => (
  <section className="space-y-4">
    <h1 className="text-2xl font-bold">Admin Dashboard</h1>
    <div className="grid gap-4 md:grid-cols-3">
      <MetricCard title="Total students" value={14520} />
      <MetricCard title="Total companies" value={1300} />
      <MetricCard title="Active subscriptions" value={7450} />
      <MetricCard title="Pending approvals" value={51} />
      <MetricCard title="Application volume" value={42300} />
      <MetricCard title="Revenue" value="$184,200" subtitle="Placeholder until payment integration." />
    </div>
  </section>
);

export const AdminUsersPage = () => <PageShell title="Users Management" description="Search/filter users and manage account statuses." />;
export const AdminRolesPage = () => <PageShell title="Roles Management" description="Create, update, and remove roles and permissions." />;
export const AdminPendingApprovalsPage = () => <PageShell title="Pending Bursary Approvals" description="Review bursaries waiting for moderation." />;
export const AdminBursaryModerationPage = () => <PageShell title="Bursary Moderation" description="Approve or reject bursaries with review notes." />;
export const AdminSubscriptionsPage = () => <PageShell title="Subscriptions" description="View active plans and churn indicators." />;
export const AdminPaymentsPage = () => <PageShell title="Payments" description="Payment summaries and status monitoring." />;
export const AdminNotificationTemplatesPage = () => <PageShell title="Notification Templates" description="Manage email and in-app template content." />;
export const AdminAnalyticsPage = () => <PageShell title="Analytics" description="Platform growth, engagement, and funnel metrics." />;
export const AdminAuditLogsPage = () => <PageShell title="Audit Logs" description="Track administrative and system events." />;
export const AdminSettingsPage = () => <PageShell title="Settings" description="Global platform configurations." />;
