import { useMemo } from 'react';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/tables/DataTable';
import { LoadingState } from '@/components/feedback/States';
import { MetricCard } from '@/components/cards/MetricCard';
import { PlaceholderChart } from '@/components/charts/PlaceholderChart';
import { useAppQuery } from '@/hooks/useAppQuery';
import { studentService } from '@/services/studentService';
import { notificationService } from '@/services/notificationService';
import { recommendationService } from '@/services/recommendationService';
import { applicationService } from '@/services/applicationService';
import type { Application, Notification, Recommendation } from '@/types';

const SectionHeader = ({ title, subtitle }: { title: string; subtitle: string }) => (
  <div>
    <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
    <p className="text-sm text-slate-600">{subtitle}</p>
  </div>
);

export const StudentDashboardPage = () => {
  const dashboard = useAppQuery({ queryKey: ['student', 'dashboard'], queryFn: () => studentService.getDashboard() });
  const recommendations = useAppQuery<Recommendation[]>({ queryKey: ['student', 'recommendations'], queryFn: () => recommendationService.mine() });

  const topRecommendations = useMemo(
    () => (Array.isArray(recommendations.data) ? recommendations.data : []).slice(0, 4),
    [recommendations.data],
  );

  return (
    <section className="space-y-6">
      <SectionHeader
        title="Student Dashboard"
        subtitle="Track your career readiness, application progress, and recommended opportunities in one place."
      />

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard title="Profile completeness" value={dashboard.data?.profileCompleteness ?? '82%'} subtitle="Target 90% for stronger matching" />
        <MetricCard title="Saved opportunities" value={dashboard.data?.savedOpportunities ?? 14} subtitle="Bursaries, courses, and careers" />
        <MetricCard title="Active applications" value={dashboard.data?.activeApplications ?? 6} subtitle="Across funding and study options" />
        <MetricCard title="Unread alerts" value={dashboard.data?.notifications ?? 3} subtitle="Deadlines in the next 14 days" />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <PlaceholderChart title="Application pipeline trend" />
        <div className="card p-5">
          <h2 className="text-lg font-semibold">Top recommendations</h2>
          <p className="text-sm text-slate-500">Personalized suggestions based on your profile, grades, and goals.</p>
          <div className="mt-4 space-y-3">
            {topRecommendations.length ? (
              topRecommendations.map((item) => (
                <div key={item.id} className="flex items-start justify-between rounded-lg border border-slate-200 p-3">
                  <div>
                    <p className="font-medium text-slate-800">{item.title}</p>
                    <p className="text-xs text-slate-500">{item.rationale}</p>
                  </div>
                  <Badge color={item.score >= 80 ? 'emerald' : 'amber'}>{item.score}% fit</Badge>
                </div>
              ))
            ) : (
              <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-500">
                No recommendation data yet. Complete your academic profile to unlock AI matches.
              </div>
            )}
          </div>
        </div>
      </div>
    </section>
  );
};

export const StudentProfilePage = () => {
  const profile = useAppQuery({ queryKey: ['student', 'me'], queryFn: () => studentService.getMe() });
  return (
    <section className="space-y-6">
      <SectionHeader title="My Profile" subtitle="Keep your contact details accurate so institutions and sponsors can reach you." />
      {profile.isLoading ? <LoadingState /> : null}
      <div className="grid gap-4 lg:grid-cols-3">
        <div className="card p-5 lg:col-span-2 space-y-4">
          <h2 className="text-lg font-semibold">Personal information</h2>
          <div className="grid gap-3 md:grid-cols-2 text-sm">
            <p><span className="font-semibold">Full name:</span> {profile.data?.fullName ?? 'Student Name'}</p>
            <p><span className="font-semibold">Grade level:</span> {profile.data?.gradeLevel ?? 'Grade 12'}</p>
            <p><span className="font-semibold">Primary email:</span> {profile.data?.id ? `${profile.data.id}@edurite.app` : 'student@edurite.app'}</p>
            <p><span className="font-semibold">Phone:</span> +27 71 123 4567</p>
          </div>
          <Button className="w-fit">Save profile updates</Button>
        </div>
        <div className="card p-5">
          <h2 className="text-lg font-semibold">Profile status</h2>
          <p className="mt-2 text-sm text-slate-500">Completeness score</p>
          <p className="text-3xl font-bold">{profile.data?.profileCompleteness ?? 82}%</p>
          <ul className="mt-4 space-y-2 text-sm text-slate-600">
            <li>• Add guardian details</li>
            <li>• Upload verified ID document</li>
            <li>• Include preferred study locations</li>
          </ul>
        </div>
      </div>
    </section>
  );
};

export const StudentAcademicProfilePage = () => (
  <section className="space-y-6">
    <SectionHeader title="Academic Profile" subtitle="Capture school performance, subjects, and target study pathways." />
    <div className="grid gap-4 md:grid-cols-2">
      <div className="card p-5">
        <h2 className="font-semibold">Current academics</h2>
        <ul className="mt-3 space-y-2 text-sm text-slate-600">
          <li>School: Westlake High School</li><li>Expected completion: Nov 2026</li><li>APS estimate: 37 points</li>
        </ul>
      </div>
      <div className="card p-5">
        <h2 className="font-semibold">Subject performance</h2>
        <div className="mt-3 grid gap-2 text-sm">
          {['Mathematics - 79%', 'Physical Sciences - 75%', 'English Home Language - 81%', 'Information Technology - 85%'].map((s) => (
            <div key={s} className="rounded-md bg-slate-50 px-3 py-2">{s}</div>
          ))}
        </div>
      </div>
    </div>
  </section>
);

export const StudentDocumentsPage = () => (
  <section className="space-y-6">
    <SectionHeader title="Documents" subtitle="Upload required files for bursaries, institutions, and verification checks." />
    <div className="card p-5">
      <h2 className="font-semibold">Document vault</h2>
      <DataTable
        columns={[
          { key: 'name', header: 'Document' },
          { key: 'type', header: 'Type' },
          { key: 'updatedAt', header: 'Last updated' },
          { key: 'status', header: 'Status', render: (row) => <Badge color={row.status === 'Verified' ? 'emerald' : 'amber'}>{row.status}</Badge> },
        ]}
        data={[
          { id: '1', name: 'National ID', type: 'Identity', updatedAt: '12 Jan 2026', status: 'Verified' },
          { id: '2', name: 'Latest transcript', type: 'Academic', updatedAt: '10 Jan 2026', status: 'Pending review' },
        ]}
      />
    </div>
  </section>
);

export const StudentQualificationsPage = () => (
  <section className="space-y-6">
    <SectionHeader title="Qualifications" subtitle="Maintain your earned qualifications, short courses, and certifications." />
    <div className="card p-5 space-y-3 text-sm">
      <div className="rounded-lg border border-slate-200 p-4">
        <p className="font-semibold">Google Data Analytics Certificate</p>
        <p className="text-slate-500">Completed: Aug 2025 • Credential ID: GDA-2032</p>
      </div>
      <div className="rounded-lg border border-dashed border-slate-300 p-4 text-slate-500">No additional qualifications yet. Add one to strengthen your recommendations.</div>
    </div>
  </section>
);

export const StudentExperiencePage = () => (
  <section className="space-y-6">
    <SectionHeader title="Experience" subtitle="Showcase internships, volunteer work, competitions, and leadership activities." />
    <div className="grid gap-4 lg:grid-cols-2">
      <div className="card p-5 space-y-3 text-sm">
        <h2 className="font-semibold">Experience timeline</h2>
        <div className="rounded-lg border border-slate-200 p-4"><p className="font-medium">STEM Mentor Volunteer</p><p className="text-slate-500">June 2025 – Present • 4 hrs/week</p></div>
        <div className="rounded-lg border border-slate-200 p-4"><p className="font-medium">Coding Club Vice Chair</p><p className="text-slate-500">Jan 2025 – Present</p></div>
      </div>
      <PlaceholderChart title="Experience impact score" />
    </div>
  </section>
);

const RecommendationPage = ({ type }: { type: 'CAREER' | 'BURSARY' }) => {
  const recommendations = useAppQuery<Recommendation[]>({ queryKey: ['student', 'recommendations', type], queryFn: () => recommendationService.mine() });
  const rows = (Array.isArray(recommendations.data) ? recommendations.data : []).filter((item) => item.type === type);

  return (
    <section className="space-y-6">
      <SectionHeader
        title={type === 'CAREER' ? 'Career Recommendations' : 'Bursary Recommendations'}
        subtitle={type === 'CAREER' ? 'Explore best-fit career paths with rationale and readiness scoring.' : 'Discover funding opportunities ranked by your eligibility profile.'}
      />
      {recommendations.isLoading ? <LoadingState /> : null}
      <DataTable
        columns={[
          { key: 'title', header: 'Opportunity' },
          { key: 'score', header: 'Fit score', render: (row) => <Badge color={row.score > 80 ? 'emerald' : 'blue'}>{row.score}%</Badge> },
          { key: 'rationale', header: 'Why recommended' },
        ]}
        data={rows.length ? rows : [{ id: 'fallback', type, title: 'Complete your profile for personalized insights', score: 0, rationale: 'No live recommendation data yet.' }]}
      />
    </section>
  );
};

export const StudentCareerRecommendationsPage = () => <RecommendationPage type="CAREER" />;
export const StudentBursaryRecommendationsPage = () => <RecommendationPage type="BURSARY" />;

export const StudentSavedPage = () => (
  <section className="space-y-6">
    <SectionHeader title="Saved Opportunities" subtitle="Manage bookmarked bursaries, courses, and career tracks for later action." />
    <DataTable
      columns={[
        { key: 'title', header: 'Saved item' },
        { key: 'type', header: 'Category' },
        { key: 'deadline', header: 'Deadline' },
      ]}
      data={[
        { id: '1', title: 'FutureTech Engineering Bursary', type: 'Bursary', deadline: '30 Mar 2026' },
        { id: '2', title: 'BSc Computer Science', type: 'Course', deadline: 'Rolling intake' },
      ]}
    />
  </section>
);

export const StudentApplicationsPage = () => {
  const applications = useAppQuery<Application[]>({ queryKey: ['student', 'applications'], queryFn: () => applicationService.listMine() });
  const rows = Array.isArray(applications.data) ? applications.data : [];

  return (
    <section className="space-y-6">
      <SectionHeader title="Applications" subtitle="Track submissions, required actions, and outcomes across all applications." />
      {applications.isLoading ? <LoadingState /> : null}
      <DataTable
        columns={[
          { key: 'opportunityType', header: 'Type' },
          { key: 'status', header: 'Status', render: (row) => <Badge color={row.status === 'APPROVED' ? 'emerald' : row.status === 'REJECTED' ? 'amber' : 'blue'}>{row.status}</Badge> },
          { key: 'submittedAt', header: 'Submitted' },
        ]}
        data={rows.length ? rows : [{ id: '1', opportunityType: 'BURSARY', status: 'IN_REVIEW', submittedAt: '2026-01-08' }]}
      />
    </section>
  );
};

export const StudentNotificationsPage = () => {
  const notifications = useAppQuery<Notification[]>({ queryKey: ['student', 'notifications'], queryFn: () => notificationService.mine() });
  const rows = Array.isArray(notifications.data) ? notifications.data : [];
  return (
    <section className="space-y-6">
      <SectionHeader title="Notifications" subtitle="Stay informed about deadlines, application changes, and recommendation updates." />
      <div className="card p-5 space-y-3">
        {(rows.length ? rows : [{ id: 'n1', title: 'Application update', message: 'Your BSc Computer Science application is now in review.', read: false }]).map((note) => (
          <div key={note.id} className="rounded-lg border border-slate-200 p-4">
            <div className="flex items-center justify-between">
              <p className="font-semibold">{note.title}</p>
              <Badge color={note.read ? 'slate' : 'blue'}>{note.read ? 'Read' : 'New'}</Badge>
            </div>
            <p className="mt-1 text-sm text-slate-600">{note.message}</p>
          </div>
        ))}
      </div>
    </section>
  );
};

export const StudentSubscriptionPage = () => (
  <section className="space-y-6">
    <SectionHeader title="Subscription" subtitle="Review your plan benefits, billing cycle, and upgrade options." />
    <div className="grid gap-4 md:grid-cols-3">
      <MetricCard title="Current plan" value="Student Pro" subtitle="Renews on 04 Feb 2026" />
      <MetricCard title="Monthly cost" value="R89" subtitle="Includes AI recommendation suite" />
      <MetricCard title="Usage" value="71%" subtitle="Career + bursary recommendation quota" />
    </div>
    <div className="card p-5 text-sm text-slate-600">Need more advanced guidance? Upgrade to unlock mentor sessions and CV optimization tools.</div>
  </section>
);

export const StudentSettingsPage = () => (
  <section className="space-y-6">
    <SectionHeader title="Settings" subtitle="Manage security, notification preferences, and communication channels." />
    <div className="card p-5 space-y-3 text-sm">
      <div className="flex items-center justify-between rounded-lg border border-slate-200 p-3"><span>Email notifications</span><Badge color="emerald">Enabled</Badge></div>
      <div className="flex items-center justify-between rounded-lg border border-slate-200 p-3"><span>Two-factor authentication</span><Badge color="amber">Not configured</Badge></div>
      <Button className="w-fit">Save settings</Button>
    </div>
  </section>
);
