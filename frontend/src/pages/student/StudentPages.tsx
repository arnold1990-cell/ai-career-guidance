import { MetricCard } from '@/components/cards/MetricCard';
import { PageShell } from '@/pages/PageShell';

export const StudentDashboardPage = () => (
  <section className="space-y-4">
    <h1 className="text-2xl font-bold">Student Dashboard</h1>
    <div className="grid gap-4 md:grid-cols-3">
      <MetricCard title="Recommended careers" value={12} subtitle="AI-ranked by fit score" />
      <MetricCard title="Recommended bursaries" value={8} subtitle="Eligibility matched" />
      <MetricCard title="Application progress" value="67%" subtitle="Across active submissions" />
      <MetricCard title="Saved opportunities" value={14} />
      <MetricCard title="Skill gap insights" value={5} subtitle="Priority learning tracks" />
      <MetricCard title="Profile completeness" value="82%" subtitle="Improve for stronger recommendations" />
    </div>
  </section>
);

export const StudentProfilePage = () => <PageShell title="My Profile" description="Update personal details and contact information." />;
export const StudentAcademicProfilePage = () => <PageShell title="Academic Profile" description="Manage school history, subjects, and grades." />;
export const StudentDocumentsPage = () => <PageShell title="Documents" description="Upload CV and transcript metadata." />;
export const StudentQualificationsPage = () => <PageShell title="Qualifications" description="Add or edit qualifications and certificates." />;
export const StudentExperiencePage = () => <PageShell title="Experience" description="Capture internships, volunteer work, and projects." />;
export const StudentCareerRecommendationsPage = () => <PageShell title="Career Recommendations" description="AI recommendations with score and rationale." />;
export const StudentBursaryRecommendationsPage = () => <PageShell title="Bursary Recommendations" description="Funding recommendations ranked by eligibility." />;
export const StudentSavedPage = () => <PageShell title="Saved Opportunities" description="Bookmarked bursaries, courses, and careers." />;
export const StudentApplicationsPage = () => <PageShell title="Applications" description="Track statuses and next actions." />;
export const StudentNotificationsPage = () => <PageShell title="Notifications" description="Updates about deadlines and platform activity." />;
export const StudentSubscriptionPage = () => <PageShell title="Subscription" description="Manage your active plan and billing details." />;
export const StudentSettingsPage = () => <PageShell title="Settings" description="Account preferences and security options." />;
