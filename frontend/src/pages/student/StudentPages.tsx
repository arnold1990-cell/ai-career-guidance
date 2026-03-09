import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAppQuery } from '@/hooks/useAppQuery';
import { studentService } from '@/services/studentService';
import { recommendationService } from '@/services/recommendationService';
import { careerService } from '@/services/careerService';
import { bursaryService } from '@/services/bursaryService';
import { notificationService } from '@/services/notificationService';
import { applicationService } from '@/services/applicationService';
import { subscriptionService } from '@/services/subscriptionService';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';

const Section = ({ title, children }: { title: string; children: React.ReactNode }) => <section className="card p-5 space-y-4"><h1 className="text-xl font-semibold">{title}</h1>{children}</section>;
const Card = ({ label, value }: { label: string; value: string | number }) => <div className="rounded border p-3"><p className="text-xs text-slate-500">{label}</p><p className="text-2xl font-semibold">{value}</p></div>;
const asList = <T,>(value: T[] | { content: T[] } | undefined) => (Array.isArray(value) ? value : value?.content ?? []);

export const StudentDashboardPage = () => {
  const dashboard = useAppQuery({ queryKey: ['dash'], queryFn: studentService.getDashboard });
  const recs = useAppQuery({ queryKey: ['recs'], queryFn: recommendationService.mine });
  if (dashboard.isLoading) return <LoadingState />;
  if (dashboard.isError) return <ErrorState message="Could not load dashboard. Please refresh and try again." />;
  const d = dashboard.data ?? {};
  const careers = (recs.data ?? []).filter((r) => r.type === 'CAREER').slice(0, 3);
  const bursaries = (recs.data ?? []).filter((r) => r.type === 'BURSARY').slice(0, 3);
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
  const rec = useAppQuery({ queryKey: ['recs'], queryFn: recommendationService.mine });
  return <Section title="AI Guidance">
    {(rec.data ?? []).map((r) => <div key={r.id} className="border p-2 rounded">{r.title} ({r.type}) - {r.rationale}</div>)}
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
  const current = useAppQuery({ queryKey: ['sub'], queryFn: subscriptionService.current });
  const purchase = useMutation({ mutationFn: (plan: 'BASIC' | 'PREMIUM') => subscriptionService.purchase(plan), onSuccess: () => qc.invalidateQueries({ queryKey: ['sub'] }) });
  return <Section title="Subscription & Payment">
    <p>Current: {current.data?.planCode ?? 'PLAN_BASIC'} ({current.data?.status ?? 'ACTIVE'})</p>
    <div className="grid gap-3 md:grid-cols-2">
      <div className="rounded border p-3"><h3 className="font-semibold">Basic</h3><p className="text-sm">Essential recommendations and profile tools.</p><Button onClick={() => purchase.mutate('BASIC')}>Choose Basic</Button></div>
      <div className="rounded border p-3"><h3 className="font-semibold">Premium</h3><p className="text-sm">Advanced AI guidance, deeper insight analytics.</p><Button onClick={() => purchase.mutate('PREMIUM')}>Checkout Premium</Button></div>
    </div>
  </Section>;
};

export const StudentSettingsPage = () => <Section title="Settings">Notification channels ready for in-app now, with backend email/SMS stubs configured.</Section>;
