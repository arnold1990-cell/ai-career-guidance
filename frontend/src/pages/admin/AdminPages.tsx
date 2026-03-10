import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/tables/DataTable';
import { MetricCard } from '@/components/cards/MetricCard';
import { PlaceholderChart } from '@/components/charts/PlaceholderChart';
import { useAppQuery } from '@/hooks/useAppQuery';
import { adminService } from '@/services/adminService';
import { analyticsService } from '@/services/analyticsService';

const Header = ({ title, subtitle }: { title: string; subtitle: string }) => (
  <div>
    <h1 className="text-2xl font-bold">{title}</h1>
    <p className="text-sm text-slate-600">{subtitle}</p>
  </div>
);

export const AdminDashboardPage = () => {
  const users = useAppQuery({ queryKey: ['admin', 'users'], queryFn: () => adminService.getUsers() });
  const pending = useAppQuery({ queryKey: ['admin', 'pending-bursaries'], queryFn: () => adminService.getPendingBursaries() });
  return (
    <section className="space-y-6">
      <Header title="Admin Dashboard" subtitle="Monitor platform health, operational queues, and governance metrics." />
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard title="Total users" value={Array.isArray(users.data) ? users.data.length : 15820} />
        <MetricCard title="Pending approvals" value={Array.isArray(pending.data) ? pending.data.length : 42} />
        <MetricCard title="Monthly revenue" value="R 184,200" />
        <MetricCard title="System incidents" value={2} subtitle="Open incidents this week" />
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <PlaceholderChart title="User growth and subscription trend" />
        <div className="card p-5">
          <h2 className="font-semibold">Operational highlights</h2>
          <ul className="mt-3 space-y-2 text-sm text-slate-600"><li>• 14 new companies onboarded this week</li><li>• Median moderation turnaround: 9.6 hours</li><li>• 99.95% API uptime over the last 30 days</li></ul>
        </div>
      </div>
    </section>
  );
};

export const AdminUsersPage = () => {
  const users = useAppQuery({ queryKey: ['admin', 'users-management'], queryFn: () => adminService.getUsers() });
  const rows = Array.isArray(users.data) ? users.data : [{ id: 'u1', fullName: 'Sam Ncube', email: 'sam@example.com', role: 'STUDENT', active: true }];
  return (
    <section className="space-y-6">
      <Header title="Users Management" subtitle="Search, filter, and manage user account status across all roles." />
      <DataTable columns={[{ key: 'fullName', header: 'User' }, { key: 'email', header: 'Email' }, { key: 'role', header: 'Role' }, { key: 'active', header: 'Status', render: (row) => <Badge color={row.active ? 'emerald' : 'amber'}>{row.active ? 'Active' : 'Suspended'}</Badge> }]} data={rows} />
    </section>
  );
};

export const AdminRolesPage = () => {
  const roles = useAppQuery({ queryKey: ['admin', 'roles'], queryFn: () => adminService.getRoles() });
  const rows = Array.isArray(roles.data) ? roles.data : [{ id: 'r1', name: 'Moderator', permissions: 'Review bursaries, manage flags' }];
  return (
    <section className="space-y-6">
      <Header title="Roles Management" subtitle="Maintain role templates and permissions for administrative teams." />
      <DataTable columns={[{ key: 'name', header: 'Role' }, { key: 'permissions', header: 'Permissions' }]} data={rows} />
      <Button className="w-fit">Create role</Button>
    </section>
  );
};

export const AdminPendingApprovalsPage = () => {
  const pending = useAppQuery({ queryKey: ['admin', 'pending'], queryFn: () => adminService.getPendingBursaries() });
  const rows = Array.isArray(pending.data) ? pending.data : [{ id: 'p1', title: 'Future Women in Tech', provider: 'TechWave', submittedAt: '2026-01-10' }];
  return (
    <section className="space-y-6">
      <Header title="Pending Approvals" subtitle="Review items awaiting moderation and policy approval." />
      <DataTable columns={[{ key: 'title', header: 'Bursary' }, { key: 'provider', header: 'Company' }, { key: 'submittedAt', header: 'Submitted' }]} data={rows} />
    </section>
  );
};

export const AdminBursaryModerationPage = () => (
  <section className="space-y-6">
    <Header title="Bursary Moderation" subtitle="Assess bursary quality, compliance, and fairness before publishing." />
    <div className="card p-5 text-sm text-slate-600">Moderation queue is synced with backend review APIs. Select a bursary to approve, reject, or request changes.</div>
  </section>
);

export const AdminSubscriptionsPage = () => (
  <section className="space-y-6">
    <Header title="Subscriptions" subtitle="Track active plans, churn risk indicators, and upgrade opportunities." />
    <DataTable columns={[{ key: 'plan', header: 'Plan' }, { key: 'accounts', header: 'Active accounts' }, { key: 'mrr', header: 'MRR' }]} data={[{ id: 'sub1', plan: 'Student Pro', accounts: 5420, mrr: 'R 482,380' }, { id: 'sub2', plan: 'Company Growth', accounts: 410, mrr: 'R 327,180' }]} />
  </section>
);

export const AdminPaymentsPage = () => (
  <section className="space-y-6">
    <Header title="Payments" subtitle="Monitor transaction statuses, settlement summaries, and failed payment retries." />
    <DataTable columns={[{ key: 'reference', header: 'Reference' }, { key: 'amount', header: 'Amount' }, { key: 'status', header: 'Status', render: (row) => <Badge color={row.status === 'Paid' ? 'emerald' : 'amber'}>{row.status}</Badge> }]} data={[{ id: 'pay1', reference: 'INV-4021', amount: 'R 2,499', status: 'Paid' }, { id: 'pay2', reference: 'INV-4022', amount: 'R 2,499', status: 'Retrying' }]} />
  </section>
);

export const AdminNotificationTemplatesPage = () => (
  <section className="space-y-6">
    <Header title="Notification Templates" subtitle="Configure platform-wide email and in-app communication templates." />
    <div className="card p-5 space-y-3 text-sm"><p>Template sets: onboarding, moderation decisions, payment receipts, reminders.</p><Button className="w-fit">Edit templates</Button></div>
  </section>
);

export const AdminAnalyticsPage = () => {
  useAppQuery({ queryKey: ['admin', 'analytics'], queryFn: () => analyticsService.adminOverview() });
  return (
    <section className="space-y-6">
      <Header title="Analytics" subtitle="Explore trends across user engagement, conversion funnels, and recommendation quality." />
      <div className="grid gap-4 lg:grid-cols-2"><PlaceholderChart title="Acquisition and conversion funnel" /><PlaceholderChart title="Recommendation click-through and outcomes" /></div>
    </section>
  );
};

export const AdminAuditLogsPage = () => (
  <section className="space-y-6">
    <Header title="Audit Logs" subtitle="Review administrator actions and critical system events for compliance." />
    <DataTable columns={[{ key: 'actor', header: 'Actor' }, { key: 'action', header: 'Action' }, { key: 'timestamp', header: 'Timestamp' }]} data={[{ id: 'log1', actor: 'admin@edurite.app', action: 'Approved bursary #321', timestamp: '2026-01-12 09:42' }, { id: 'log2', actor: 'ops@edurite.app', action: 'Suspended user #87', timestamp: '2026-01-12 08:10' }]} />
  </section>
);

export const AdminSettingsPage = () => (
  <section className="space-y-6">
    <Header title="Settings" subtitle="Control global policies, moderation defaults, and operational thresholds." />
    <div className="card p-5"><Button>Save platform settings</Button></div>
  </section>
);
