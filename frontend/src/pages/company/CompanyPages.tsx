import { useParams } from 'react-router-dom';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/tables/DataTable';
import { LoadingState } from '@/components/feedback/States';
import { MetricCard } from '@/components/cards/MetricCard';
import { PlaceholderChart } from '@/components/charts/PlaceholderChart';
import { useAppQuery } from '@/hooks/useAppQuery';
import { companyService } from '@/services/companyService';
import type { Bursary } from '@/types';

const Header = ({ title, subtitle }: { title: string; subtitle: string }) => (
  <div>
    <h1 className="text-2xl font-bold">{title}</h1>
    <p className="text-sm text-slate-600">{subtitle}</p>
  </div>
);

export const CompanyDashboardPage = () => {
  const bursaries = useAppQuery<Bursary[]>({ queryKey: ['company', 'bursaries'], queryFn: () => companyService.getBursaries() });
  const applicants = useAppQuery({ queryKey: ['company', 'applicants'], queryFn: () => companyService.getApplicants() });

  return (
    <section className="space-y-6">
      <Header title="Company Dashboard" subtitle="Monitor bursary performance, applicant quality, and campaign outcomes." />
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard title="Total bursaries" value={Array.isArray(bursaries.data) ? bursaries.data.length : 12} />
        <MetricCard title="Published bursaries" value={Array.isArray(bursaries.data) ? bursaries.data.filter((item) => item.status === 'PUBLISHED').length : 8} />
        <MetricCard title="New applicants" value={Array.isArray(applicants.data) ? applicants.data.length : 47} subtitle="Last 30 days" />
        <MetricCard title="Shortlist conversion" value="26%" subtitle="Applicants moved to shortlist" />
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <PlaceholderChart title="Applicant pipeline by bursary" />
        <div className="card p-5">
          <h2 className="text-lg font-semibold">Action center</h2>
          <ul className="mt-3 space-y-2 text-sm text-slate-600">
            <li>• 3 bursaries close in the next 7 days</li>
            <li>• 12 applications require verification review</li>
            <li>• 2 listings flagged by moderation rules</li>
          </ul>
        </div>
      </div>
    </section>
  );
};

export const CompanyProfilePage = () => {
  const profile = useAppQuery({ queryKey: ['company', 'me'], queryFn: () => companyService.getMe() });
  return (
    <section className="space-y-6">
      <Header title="Company Profile" subtitle="Manage branding, company details, and recruitment preferences." />
      {profile.isLoading ? <LoadingState /> : null}
      <div className="grid gap-4 md:grid-cols-2">
        <div className="card p-5 space-y-2 text-sm"><p><span className="font-semibold">Organization:</span> {profile.data?.companyName ?? 'Acme Holdings'}</p><p><span className="font-semibold">Industry:</span> {profile.data?.industry ?? 'Technology'}</p><p><span className="font-semibold">Verification:</span> {profile.data?.verified ? 'Verified' : 'Pending'}</p></div>
        <div className="card p-5"><h2 className="font-semibold">Employer brand health</h2><p className="mt-2 text-sm text-slate-600">Complete your employer profile to improve candidate confidence and conversion.</p><Button className="mt-4">Update profile</Button></div>
      </div>
    </section>
  );
};

export const CompanyVerificationDocsPage = () => (
  <section className="space-y-6">
    <Header title="Verification Documents" subtitle="Upload legal and compliance documentation for account validation." />
    <DataTable columns={[{ key: 'name', header: 'Document' }, { key: 'status', header: 'Status', render: (row) => <Badge color={row.status === 'Approved' ? 'emerald' : 'amber'}>{row.status}</Badge> }, { key: 'updatedAt', header: 'Updated' }]} data={[{ id: '1', name: 'Company Registration', status: 'Approved', updatedAt: '10 Jan 2026' }, { id: '2', name: 'Tax Clearance', status: 'Pending', updatedAt: '08 Jan 2026' }]} />
  </section>
);

export const CompanyBursariesPage = () => {
  const bursaries = useAppQuery<Bursary[]>({ queryKey: ['company', 'bursary-list'], queryFn: () => companyService.getBursaries() });
  const rows = Array.isArray(bursaries.data) ? bursaries.data : [];
  return (
    <section className="space-y-6">
      <Header title="My Bursaries" subtitle="Create, manage, and track all bursary opportunities from one dashboard." />
      <DataTable columns={[{ key: 'title', header: 'Bursary' }, { key: 'provider', header: 'Provider' }, { key: 'status', header: 'Status', render: (row) => <Badge color={row.status === 'PUBLISHED' ? 'emerald' : row.status === 'PENDING' ? 'amber' : 'slate'}>{row.status}</Badge> }]} data={rows.length ? rows : [{ id: '1', title: 'Engineering Future Leaders', provider: 'EduRite Company', status: 'PUBLISHED' }]} />
    </section>
  );
};

const BursaryForm = ({ mode }: { mode: 'create' | 'edit' }) => {
  const { id } = useParams();
  return (
    <section className="space-y-6">
      <Header title={mode === 'create' ? 'Create Bursary' : `Edit Bursary ${id ? `#${id}` : ''}`} subtitle="Define funding amount, eligibility criteria, and submission requirements." />
      <div className="card p-5 grid gap-4 md:grid-cols-2 text-sm">
        <label className="space-y-1"><span className="font-medium">Bursary title</span><input className="w-full rounded-lg border border-slate-300 px-3 py-2" placeholder="e.g., Women in STEM Scholarship" /></label>
        <label className="space-y-1"><span className="font-medium">Funding amount</span><input className="w-full rounded-lg border border-slate-300 px-3 py-2" placeholder="R 120 000" /></label>
        <label className="space-y-1 md:col-span-2"><span className="font-medium">Eligibility criteria</span><textarea className="w-full rounded-lg border border-slate-300 px-3 py-2" rows={4} placeholder="Academic requirements, field of study, citizenship..." /></label>
        <Button className="w-fit">{mode === 'create' ? 'Publish bursary' : 'Save changes'}</Button>
      </div>
    </section>
  );
};

export const CompanyCreateBursaryPage = () => <BursaryForm mode="create" />;
export const CompanyEditBursaryPage = () => <BursaryForm mode="edit" />;

export const CompanyApplicantsPage = () => {
  const applicants = useAppQuery({ queryKey: ['company', 'all-applicants'], queryFn: () => companyService.getApplicants() });
  const rows = Array.isArray(applicants.data) ? applicants.data : [{ id: 'a1', name: 'Lebo Mokoena', score: 88, status: 'Interview' }];
  return (
    <section className="space-y-6">
      <Header title="Applicants" subtitle="Evaluate applicants, compare readiness, and move candidates through the pipeline." />
      <DataTable columns={[{ key: 'name', header: 'Candidate' }, { key: 'score', header: 'Readiness score' }, { key: 'status', header: 'Stage', render: (row) => <Badge color="blue">{String(row.status ?? 'Review')}</Badge> }]} data={rows} />
    </section>
  );
};

export const CompanyTalentSearchPage = () => (
  <section className="space-y-6">
    <Header title="Talent Search" subtitle="Find students by skills, location, academic performance, and role-fit score." />
    <div className="card p-5">
      <div className="grid gap-3 md:grid-cols-4">
        {['Skills', 'Province', 'Field of study', 'Minimum fit score'].map((filter) => <input key={filter} className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder={filter} />)}
      </div>
      <p className="mt-3 text-sm text-slate-500">No live search results yet. Apply filters to begin discovering candidates.</p>
    </div>
  </section>
);

export const CompanyShortlistedPage = () => (
  <section className="space-y-6">
    <Header title="Shortlisted Candidates" subtitle="Manage and collaborate on your final candidate shortlist." />
    <DataTable columns={[{ key: 'name', header: 'Candidate' }, { key: 'bursary', header: 'Bursary' }, { key: 'nextStep', header: 'Next step' }]} data={[{ id: 's1', name: 'Anele Jacobs', bursary: 'Engineering Future Leaders', nextStep: 'Panel review' }, { id: 's2', name: 'Thato Molefe', bursary: 'Digital Skills Fund', nextStep: 'Reference check' }]} />
  </section>
);

export const CompanyNotificationsPage = () => (
  <section className="space-y-6">
    <Header title="Notifications" subtitle="Review moderation updates, applicant actions, and platform announcements." />
    <div className="card p-5 space-y-2 text-sm"><p>• Bursary "Engineering Future Leaders" approved by moderation.</p><p>• 5 new applications received for Digital Skills Fund.</p></div>
  </section>
);

export const CompanySettingsPage = () => (
  <section className="space-y-6">
    <Header title="Settings" subtitle="Control team access, notification channels, and account preferences." />
    <div className="card p-5"><Button>Invite team member</Button></div>
  </section>
);
