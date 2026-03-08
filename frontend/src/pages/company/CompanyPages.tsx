import { MetricCard } from '@/components/cards/MetricCard';
import { PageShell } from '@/pages/PageShell';

export const CompanyDashboardPage = () => (
  <section className="space-y-4">
    <h1 className="text-2xl font-bold">Company Dashboard</h1>
    <div className="grid gap-4 md:grid-cols-3">
      <MetricCard title="Total bursaries" value={32} />
      <MetricCard title="Active bursaries" value={20} />
      <MetricCard title="Pending approval bursaries" value={4} />
      <MetricCard title="Total applicants" value={842} />
      <MetricCard title="Views / impressions" value="12.5k" />
      <MetricCard title="Conversion rate" value="24%" subtitle="Applications completed" />
    </div>
  </section>
);

export const CompanyProfilePage = () => <PageShell title="Company Profile" description="Manage organization details and branding metadata." />;
export const CompanyVerificationDocsPage = () => <PageShell title="Verification Documents" description="Upload legal and verification documents." />;
export const CompanyBursariesPage = () => <PageShell title="My Bursaries" description="Manage bursary lifecycle: create, edit, publish." />;
export const CompanyCreateBursaryPage = () => <PageShell title="Create Bursary" description="Define eligibility, timeline, and application criteria." />;
export const CompanyEditBursaryPage = () => <PageShell title="Edit Bursary" description="Update bursary details and publishing status." />;
export const CompanyApplicantsPage = () => <PageShell title="Applicants" description="Review applications and shortlist candidates." />;
export const CompanyTalentSearchPage = () => <PageShell title="Talent Search" description="Search students by profile strength and skills." />;
export const CompanyShortlistedPage = () => <PageShell title="Shortlisted Candidates" description="Manage shortlisted candidate pool." />;
export const CompanyNotificationsPage = () => <PageShell title="Notifications" description="Application and platform update alerts." />;
export const CompanySettingsPage = () => <PageShell title="Settings" description="Team permissions, account, and preferences." />;
