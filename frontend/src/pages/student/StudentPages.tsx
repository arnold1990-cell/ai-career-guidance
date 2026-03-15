import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAppQuery } from '@/hooks/useAppQuery';
import { studentService } from '@/services/studentService';
import { recommendationService } from '@/services/recommendationService';
import { aiGuidanceService } from '@/services/aiGuidanceService';
import { careerService } from '@/services/careerService';
import { bursaryService } from '@/services/bursaryService';
import { notificationService } from '@/services/notificationService';
import { applicationService } from '@/services/applicationService';
import { subscriptionService } from '@/services/subscriptionService';
import { settingsService } from '@/services/settingsService';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import type { UniversityRecommendedCareer, UniversityRecommendedProgramme } from '@/types';

const Section = ({ title, children }: { title: string; children: React.ReactNode }) => <section className="card p-5 space-y-4"><h1 className="text-xl font-semibold">{title}</h1>{children}</section>;
const Card = ({ label, value }: { label: string; value: string | number }) => <div className="rounded border p-3"><p className="text-xs text-slate-500">{label}</p><p className="text-2xl font-semibold">{value}</p></div>;
const asList = <T,>(value: T[] | { content: T[] } | undefined) => (Array.isArray(value) ? value : value?.content ?? []);

export const StudentDashboardPage = () => {
  const dashboard = useAppQuery({ queryKey: ['dash'], queryFn: studentService.getDashboard });
  const recs = useAppQuery({ queryKey: ['recs'], queryFn: recommendationService.mine });
  if (dashboard.isLoading) return <LoadingState />;
  if (dashboard.isError) return <ErrorState message="Could not load dashboard. Please refresh and try again." />;
  const d = dashboard.data ?? {};
  const careers = recs.data?.suggestedCareers?.slice(0, 3) ?? [];
  const bursaries = recs.data?.suggestedBursaries?.slice(0, 3) ?? [];
  return <Section title="Student Dashboard">
    <div className="grid gap-3 md:grid-cols-4">
      <Card label="Profile completeness" value={`${d.profileCompleteness ?? 0}%`} />
      <Card label="Saved careers" value={d.savedCareers ?? 0} />
      <Card label="Saved bursaries" value={d.savedBursaries ?? 0} />
      <Card label="Applications in progress" value={d.activeApplications ?? 0} />
    </div>
    <div className="grid gap-4 md:grid-cols-2">
      <div className="rounded border p-3"><h3 className="font-semibold mb-2">Skill gaps</h3>{(d.skillGaps ?? []).map((s: string) => <p key={s}>• {s}</p>)}</div>
      <div className="rounded border p-3"><h3 className="font-semibold mb-2">Recommended improvements</h3>{(d.recommendedImprovements ?? []).map((s: string) => <p key={s}>• {s}</p>)}</div>
      <div className="rounded border p-3"><h3 className="font-semibold mb-2">Recommended careers</h3>{careers.map((r) => <p key={r.id}>• {r.title}</p>)}</div>
      <div className="rounded border p-3"><h3 className="font-semibold mb-2">Recommended bursaries</h3>{bursaries.map((r) => <p key={r.id}>• {r.title}</p>)}</div>
    </div>
  </Section>;
};

export const StudentProfilePage = () => {
  const qc = useQueryClient();
  const profile = useAppQuery({ queryKey: ['me'], queryFn: studentService.getMe });
  const [form, setForm] = useState<Record<string, string>>({});
  const update = useMutation({ mutationFn: () => studentService.updateMe({ ...form, qualifications: (form.qualifications ?? '').split(',').map((v) => v.trim()).filter(Boolean), experience: (form.experience ?? '').split(',').map((v) => v.trim()).filter(Boolean), skills: (form.skills ?? '').split(',').map((v) => v.trim()).filter(Boolean), interests: (form.interests ?? '').split(',').map((v) => v.trim()).filter(Boolean) }), onSuccess: () => qc.invalidateQueries({ queryKey: ['me'] }) });
  const cvUpload = useMutation({ mutationFn: (file: File) => studentService.uploadCv(file), onSuccess: () => qc.invalidateQueries({ queryKey: ['me'] }) });
  const transcriptUpload = useMutation({ mutationFn: (file: File) => studentService.uploadTranscript(file), onSuccess: () => qc.invalidateQueries({ queryKey: ['me'] }) });
  const p = profile.data;
  const value = (key: string, fallback?: string) => form[key] ?? fallback ?? '';
  if (profile.isLoading) return <LoadingState />;
  return <Section title="My Profile">
    <p className="text-sm">Profile completeness: <span className="font-semibold">{p?.profileCompleteness ?? 0}%</span></p>
    <div className="grid gap-3 md:grid-cols-2">
      <Input placeholder="First name" value={value('firstName', p?.firstName)} onChange={(e) => setForm((s) => ({ ...s, firstName: e.target.value }))} />
      <Input placeholder="Last name" value={value('lastName', p?.lastName)} onChange={(e) => setForm((s) => ({ ...s, lastName: e.target.value }))} />
      <Input placeholder="Phone" value={value('phone', p?.phone)} onChange={(e) => setForm((s) => ({ ...s, phone: e.target.value }))} />
      <Input placeholder="Location" value={value('location', p?.location)} onChange={(e) => setForm((s) => ({ ...s, location: e.target.value }))} />
      <Input placeholder="Qualification level" value={value('qualificationLevel', p?.qualificationLevel)} onChange={(e) => setForm((s) => ({ ...s, qualificationLevel: e.target.value }))} />
      <Input placeholder="Career goals" value={value('careerGoals', p?.careerGoals)} onChange={(e) => setForm((s) => ({ ...s, careerGoals: e.target.value }))} />
      <Input placeholder="Qualifications (comma separated)" value={value('qualifications', p?.qualifications?.join(', ') ?? '')} onChange={(e) => setForm((s) => ({ ...s, qualifications: e.target.value }))} />
      <Input placeholder="Experience (comma separated)" value={value('experience', p?.experience?.join(', ') ?? '')} onChange={(e) => setForm((s) => ({ ...s, experience: e.target.value }))} />
      <Input placeholder="Skills (comma separated)" value={value('skills', p?.skills?.join(', ') ?? '')} onChange={(e) => setForm((s) => ({ ...s, skills: e.target.value }))} />
      <Input placeholder="Interests (comma separated)" value={value('interests', p?.interests?.join(', ') ?? '')} onChange={(e) => setForm((s) => ({ ...s, interests: e.target.value }))} />
    </div>
    <div className="space-y-2">
      <p className="text-sm">CV upload {p?.cvFileUrl ? '✓' : ''}</p>
      <Input type="file" onChange={(e) => e.target.files?.[0] && cvUpload.mutate(e.target.files[0])} />
      <p className="text-sm">Transcript upload {p?.transcriptFileUrl ? '✓' : ''}</p>
      <Input type="file" onChange={(e) => e.target.files?.[0] && transcriptUpload.mutate(e.target.files[0])} />
    </div>
    <Button onClick={() => update.mutate()} disabled={update.isPending}>Save profile</Button>
  </Section>;
};

export const StudentAcademicProfilePage = StudentProfilePage;
export const StudentDocumentsPage = StudentProfilePage;
export const StudentQualificationsPage = StudentProfilePage;
export const StudentExperiencePage = StudentProfilePage;

export const StudentCareerRecommendationsPage = () => {
  const profile = useAppQuery({ queryKey: ['me'], queryFn: studentService.getMe });
  const defaultSources = useAppQuery({
    queryKey: ['default-university-sources'],
    enabled: !aiGuidanceService.demoModeEnabled,
    queryFn: aiGuidanceService.getDefaultUniversitySources,
  });

  const aiAdvice = useAppQuery({
    queryKey: ['ai-guidance-university-sources', profile.data?.id, profile.data?.profileCompleteness],
    enabled: Boolean(profile.data) && !aiGuidanceService.demoModeEnabled,
    queryFn: async () => {
      const currentProfile = profile.data;
      if (!currentProfile) {
        throw new Error('Student profile is required before requesting AI guidance.');
      }

      const qualificationLevel = currentProfile.qualificationLevel?.trim() ?? '';
      const careerInterest = (currentProfile.interests ?? []).map((item) => item.trim()).filter(Boolean).join(', ');
      const targetProgram = currentProfile.careerGoals?.trim() || careerInterest;

      if (!qualificationLevel || !careerInterest) {
        throw new Error('Please complete your profile (qualification level and interests) before generating AI guidance.');
      }

      return aiGuidanceService.analyseUniversitySources({
        urls: [], // Empty list triggers backend default-source mode.
        targetProgram,
        careerInterest,
        qualificationLevel,
        maxRecommendations: 10,
      });
    },
    retry: false,
  });

  const demoAdvice = useAppQuery({
    queryKey: ['ai-guidance-demo'],
    enabled: aiGuidanceService.demoModeEnabled,
    queryFn: aiGuidanceService.getDemoGuidance,
  });

  if (profile.isLoading || aiAdvice.isLoading || demoAdvice.isLoading || defaultSources.isLoading) return <LoadingState message="Generating AI guidance..." />;
  if (profile.isError) return <ErrorState message="Could not load your profile. Please refresh and try again." />;

  const apiErrorMessage = aiAdvice.error?.message;
  const isDemoMode = aiGuidanceService.demoModeEnabled;
  if (!isDemoMode && aiAdvice.isError) return <ErrorState message={apiErrorMessage ?? 'AI guidance failed. Please try again.'} />;

  const demoRecommendations = (demoAdvice.data?.suggestedCareers ?? []).map((item) => item.title);
  const careers = aiAdvice.data?.recommendedCareers ?? [];
  const programmes = aiAdvice.data?.recommendedProgrammes ?? [];
  const universities = aiAdvice.data?.recommendedUniversities ?? [];
  const minimumRequirements = aiAdvice.data?.minimumRequirements ?? [];
  const skillGaps = aiAdvice.data?.skillGaps ?? [];
  const nextSteps = aiAdvice.data?.recommendedNextSteps ?? [];
  const warnings = aiAdvice.data?.warnings ?? [];

  const renderSimpleList = (items: string[], emptyText: string) => {
    if (items.length === 0) {
      return <p className="text-sm text-slate-500">{emptyText}</p>;
    }
    return <div className="grid gap-2 md:grid-cols-2">{items.map((item) => <div key={item} className="border p-2 rounded text-sm">{item}</div>)}</div>;
  };

  const renderCareerCards = (items: UniversityRecommendedCareer[]) => {
    if (items.length === 0) {
      return <p className="text-sm text-slate-500">No career recommendations yet.</p>;
    }
    return <div className="grid gap-3 md:grid-cols-2">{items.slice(0, 10).map((career) => <article key={career.name} className="rounded border p-3 space-y-2 bg-white">
      <h4 className="font-semibold">{career.name}</h4>
      <p className="text-sm text-slate-600">{career.reason}</p>
      <div>
        <p className="text-xs uppercase tracking-wide text-slate-500">Requirements</p>
        <ul className="list-disc ml-5 text-sm">{career.requirements.map((requirement) => <li key={`${career.name}-${requirement}`}>{requirement}</li>)}</ul>
      </div>
      <div>
        <p className="text-xs uppercase tracking-wide text-slate-500">Related programmes</p>
        <ul className="list-disc ml-5 text-sm">{career.relatedProgrammes.map((programme) => <li key={`${career.name}-${programme}`}>{programme}</li>)}</ul>
      </div>
    </article>)}</div>;
  };

  const renderProgrammeCards = (items: UniversityRecommendedProgramme[]) => {
    if (items.length === 0) {
      return <p className="text-sm text-slate-500">No programme recommendations yet.</p>;
    }
    return <div className="grid gap-3 md:grid-cols-2">{items.slice(0, 10).map((programme) => <article key={`${programme.name}-${programme.university}`} className="rounded border p-3 space-y-2 bg-white">
      <h4 className="font-semibold">{programme.name}</h4>
      <p className="text-sm text-slate-600">{programme.university}</p>
      <div>
        <p className="text-xs uppercase tracking-wide text-slate-500">Admission requirements</p>
        <ul className="list-disc ml-5 text-sm">{programme.admissionRequirements.map((requirement) => <li key={`${programme.name}-${requirement}`}>{requirement}</li>)}</ul>
      </div>
      <div className="text-sm">
        <p><span className="font-medium">Notes:</span> {programme.notes}</p>
      </div>
    </article>)}</div>;
  };

  return <Section title="AI Guidance">
    <p className="text-sm text-slate-500">Mode: {isDemoMode ? 'demo (seeded)' : 'live Gemini multi-source'}</p>
    {!isDemoMode && <div className="grid gap-3 md:grid-cols-3">
      <Card label="Sources used" value={aiAdvice.data?.totalSourcesUsed ?? 0} />
      <Card label="Requested sources" value={aiAdvice.data?.sourceUrls?.length ?? 0} />
      <Card label="Suitability score" value={`${aiAdvice.data?.suitabilityScore ?? 0}%`} />
    </div>}

    <div className="space-y-2">
      <h3 className="font-semibold">Recommended careers</h3>
      {isDemoMode ? renderSimpleList(demoRecommendations.slice(0, 10), 'No career recommendations yet.') : renderCareerCards(careers)}
    </div>

    {!isDemoMode && <>
      <div className="space-y-2">
        <h3 className="font-semibold">Recommended programmes</h3>
        {renderProgrammeCards(programmes)}
      </div>

      <div className="space-y-2">
        <h3 className="font-semibold">Recommended universities</h3>
        {renderSimpleList(universities, 'No university recommendations yet.')}
      </div>

      <div className="space-y-2">
        <h3 className="font-semibold">Minimum requirements</h3>
        {renderSimpleList(minimumRequirements, 'Minimum requirements are currently unavailable.')}
      </div>

      <div className="space-y-2">
        <h3 className="font-semibold">Skill gaps</h3>
        {renderSimpleList(skillGaps, 'No skill gaps identified.')}
      </div>

      <div className="space-y-2">
        <h3 className="font-semibold">Recommended next steps</h3>
        {renderSimpleList(nextSteps, 'No next steps provided.')}
      </div>

      {warnings.length > 0 && <div className="space-y-2">
        <h3 className="font-semibold">Warnings</h3>
        {renderSimpleList(warnings, 'No warnings.')}
      </div>}

      {aiAdvice.data?.summary && <div className="rounded border p-3 bg-slate-50">
        <h3 className="font-semibold mb-1">Summary</h3>
        <p className="text-sm">{aiAdvice.data.summary}</p>
      </div>}
    </>}
  </Section>;
};
export const StudentBursaryRecommendationsPage = StudentCareerRecommendationsPage;

export const StudentSavedPage = () => {
  const qc = useQueryClient();
  const [filters, setFilters] = useState({ q: '', field: '', industry: '', qualificationLevel: '', location: '', demand: '', salaryRange: '' });
  const careers = useAppQuery({ queryKey: ['careers', filters], queryFn: () => careerService.list(filters) });
  const saved = useAppQuery({ queryKey: ['saved-career-ids'], queryFn: studentService.savedCareers });
  const toggle = useMutation({ mutationFn: ({ id, exists }: { id: string; exists: boolean }) => exists ? studentService.unsaveCareer(id) : studentService.saveCareer(id), onSuccess: () => qc.invalidateQueries({ queryKey: ['saved-career-ids'] }) });
  const items = asList(careers.data);
  return <Section title="Career Search">
    <div className="grid gap-2 md:grid-cols-3">
      <Input placeholder="Search" value={filters.q} onChange={(e) => setFilters((s) => ({ ...s, q: e.target.value }))} />
      <Input placeholder="Field" value={filters.field} onChange={(e) => setFilters((s) => ({ ...s, field: e.target.value }))} />
      <Input placeholder="Industry" value={filters.industry} onChange={(e) => setFilters((s) => ({ ...s, industry: e.target.value }))} />
      <Input placeholder="Qualification" value={filters.qualificationLevel} onChange={(e) => setFilters((s) => ({ ...s, qualificationLevel: e.target.value }))} />
      <Input placeholder="Location" value={filters.location} onChange={(e) => setFilters((s) => ({ ...s, location: e.target.value }))} />
      <Input placeholder="Demand" value={filters.demand} onChange={(e) => setFilters((s) => ({ ...s, demand: e.target.value }))} />
    </div>
    {items.map((c) => {
      const exists = (saved.data ?? []).includes(c.id);
      return <div key={c.id} className="flex justify-between border p-2 rounded"><span>{c.title} - {c.industry}</span><Button onClick={() => toggle.mutate({ id: c.id, exists })}>{exists ? 'Saved' : 'Save'}</Button></div>;
    })}
  </Section>;
};

export const StudentApplicationsPage = () => {
  const qc = useQueryClient();
  const [filters, setFilters] = useState({ q: '', qualificationLevel: '', region: '', eligibility: '' });
  const apps = useAppQuery({ queryKey: ['apps'], queryFn: applicationService.listMine });
  const bursaries = useAppQuery({ queryKey: ['burs', filters], queryFn: () => bursaryService.list(filters) });
  const saved = useAppQuery({ queryKey: ['saved-bursary-ids'], queryFn: studentService.savedBursaries });
  const toggle = useMutation({ mutationFn: ({ id, exists }: { id: string; exists: boolean }) => exists ? studentService.unsaveBursary(id) : studentService.saveBursary(id), onSuccess: () => qc.invalidateQueries({ queryKey: ['saved-bursary-ids'] }) });
  return <Section title="Bursary Finder">
    <div className="grid gap-2 md:grid-cols-2">
      <Input placeholder="Search bursaries" value={filters.q} onChange={(e) => setFilters((s) => ({ ...s, q: e.target.value }))} />
      <Input placeholder="Qualification" value={filters.qualificationLevel} onChange={(e) => setFilters((s) => ({ ...s, qualificationLevel: e.target.value }))} />
      <Input placeholder="Region" value={filters.region} onChange={(e) => setFilters((s) => ({ ...s, region: e.target.value }))} />
      <Input placeholder="Eligibility" value={filters.eligibility} onChange={(e) => setFilters((s) => ({ ...s, eligibility: e.target.value }))} />
    </div>
    {asList(bursaries.data).map((b) => {
      const exists = (saved.data ?? []).includes(b.id);
      return <div key={b.id} className="flex justify-between border p-2 rounded"><span>{b.title} - {b.region}</span><div className="space-x-2"><Button onClick={() => toggle.mutate({ id: b.id, exists })}>{exists ? 'Saved' : 'Bookmark'}</Button><Button onClick={() => applicationService.submit(b.id)}>Apply</Button></div></div>;
    })}
    <p className="font-medium">My applications: {(apps.data ?? []).length}</p>
  </Section>;
};

export const StudentNotificationsPage = () => {
  const qc = useQueryClient();
  const notes = useAppQuery({ queryKey: ['notes'], queryFn: notificationService.mine });
  const markRead = useMutation({ mutationFn: (id: string) => notificationService.markRead(id), onSuccess: () => qc.invalidateQueries({ queryKey: ['notes'] }) });
  if (notes.isLoading) return <LoadingState />;
  if (!notes.data?.length) return <EmptyState title="No notifications yet" message="We'll show bursary alerts and reminders here." />;
  return <Section title="Notifications">{(notes.data ?? []).map((n) => <div key={n.id} className="border p-2 rounded"><p className="font-medium">{n.title}</p><p>{n.message}</p><Button onClick={() => markRead.mutate(n.id)}>{n.read ? 'Read' : 'Mark read'}</Button></div>)}</Section>;
};

export const StudentSubscriptionPage = () => {
  const qc = useQueryClient();
  const [purchaseMessage, setPurchaseMessage] = useState('');
  const current = useAppQuery({ queryKey: ['sub'], queryFn: subscriptionService.current });
  const purchase = useMutation({
    mutationFn: (plan: 'BASIC' | 'PREMIUM') => subscriptionService.purchase(plan),
    onSuccess: (response, plan) => {
      const planCode = response?.subscription?.planCode ?? (plan === 'PREMIUM' ? 'PLAN_PREMIUM' : 'PLAN_BASIC');
      setPurchaseMessage(`Plan updated successfully: ${planCode.replace('PLAN_', '')}.`);
      qc.invalidateQueries({ queryKey: ['sub'] });
      qc.invalidateQueries({ queryKey: ['dash'] });
      qc.invalidateQueries({ queryKey: ['recs'] });
    },
  });

  if (current.isLoading) return <LoadingState />;
  if (current.isError) return <ErrorState message="Could not load your subscription." />;

  return <Section title="Subscription & Payment">
    <p>Current: {current.data?.planCode ?? 'PLAN_BASIC'} ({current.data?.status ?? 'ACTIVE'})</p>
    {purchaseMessage && <p className="text-sm text-emerald-700">{purchaseMessage}</p>}
    {purchase.isError && <p className="text-sm text-red-600">Could not update subscription. Please retry.</p>}
    <div className="grid gap-3 md:grid-cols-2">
      <div className="rounded border p-3"><h3 className="font-semibold">Basic</h3><p className="text-sm">Essential recommendations and profile tools.</p><Button onClick={() => purchase.mutate('BASIC')} disabled={purchase.isPending}>Choose Basic</Button></div>
      <div className="rounded border p-3"><h3 className="font-semibold">Premium</h3><p className="text-sm">Advanced AI guidance, deeper insight analytics.</p><Button onClick={() => purchase.mutate('PREMIUM')} disabled={purchase.isPending}>Checkout Premium</Button></div>
    </div>
  </Section>;
};

export const StudentSettingsPage = () => {
  const qc = useQueryClient();
  const settings = useAppQuery({ queryKey: ['student-settings'], queryFn: settingsService.get });
  const save = useMutation({
    mutationFn: settingsService.update,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['student-settings'] }),
  });

  if (settings.isLoading) return <LoadingState />;
  if (settings.isError || !settings.data) return <ErrorState message="Could not load settings." />;

  const data = settings.data;
  const toggle = (key: 'inAppNotificationsEnabled' | 'emailNotificationsEnabled' | 'smsNotificationsEnabled') => {
    save.mutate({ ...data, [key]: !data[key] });
  };

  return <Section title="Settings">
    <p className="text-sm text-slate-600">Manage how EduRite sends student notifications.</p>
    <div className="space-y-3">
      <div className="flex items-center justify-between rounded border p-3">
        <div><p className="font-medium">In-app notifications</p><p className="text-sm text-slate-500">Receive alerts in your dashboard and notifications page.</p></div>
        <Button onClick={() => toggle('inAppNotificationsEnabled')} disabled={save.isPending}>{data.inAppNotificationsEnabled ? 'Enabled' : 'Disabled'}</Button>
      </div>
      <div className="flex items-center justify-between rounded border p-3">
        <div><p className="font-medium">Email notifications</p><p className="text-sm text-slate-500">Receive bursary and subscription updates via email.</p></div>
        <Button onClick={() => toggle('emailNotificationsEnabled')} disabled={save.isPending}>{data.emailNotificationsEnabled ? 'Enabled' : 'Disabled'}</Button>
      </div>
      <div className="flex items-center justify-between rounded border p-3">
        <div><p className="font-medium">SMS notifications</p><p className="text-sm text-slate-500">Receive urgent reminders by SMS.</p></div>
        <Button onClick={() => toggle('smsNotificationsEnabled')} disabled={save.isPending}>{data.smsNotificationsEnabled ? 'Enabled' : 'Disabled'}</Button>
      </div>
    </div>
    {save.isSuccess && <p className="text-sm text-emerald-700">Settings saved.</p>}
    {save.isError && <p className="text-sm text-red-600">Unable to save settings right now.</p>}
  </Section>;
};
