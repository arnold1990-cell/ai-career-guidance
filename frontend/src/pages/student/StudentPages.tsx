import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
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

const Section = ({ title, children }: { title: string; children: React.ReactNode }) => <section className="card p-5 space-y-4"><h1 className="text-xl font-semibold">{title}</h1>{children}</section>;

export const StudentDashboardPage = () => {
  const dashboard = useAppQuery({ queryKey: ['dash'], queryFn: studentService.getDashboard });
  return <Section title="Dashboard"><pre className="text-sm">{JSON.stringify(dashboard.data ?? {}, null, 2)}</pre></Section>;
};

export const StudentProfilePage = () => {
  const profile = useAppQuery({ queryKey: ['me'], queryFn: studentService.getMe });
  const [firstName, setFirstName] = useState('');
  const update = useMutation({ mutationFn: () => studentService.updateMe({ firstName }) });
  return <Section title="Profile">
    <p>Completeness: {profile.data?.profileCompleteness ?? 0}%</p>
    <Input placeholder="First name" value={firstName} onChange={(e) => setFirstName(e.target.value)} />
    <Button onClick={() => update.mutate()}>Save</Button>
  </Section>;
};

export const StudentAcademicProfilePage = StudentProfilePage;
export const StudentDocumentsPage = () => {
  const uploadCv = useMutation({ mutationFn: (file: File) => studentService.upload(file, 'cv') });
  const uploadTranscript = useMutation({ mutationFn: (file: File) => studentService.upload(file, 'transcript') });
  return <Section title="Documents">
    <Input type="file" onChange={(e) => e.target.files?.[0] && uploadCv.mutate(e.target.files[0])} />
    <Input type="file" onChange={(e) => e.target.files?.[0] && uploadTranscript.mutate(e.target.files[0])} />
  </Section>;
};
export const StudentQualificationsPage = StudentProfilePage;
export const StudentExperiencePage = StudentProfilePage;

const RecommendationList = () => {
  const rec = useAppQuery({ queryKey: ['recs'], queryFn: recommendationService.mine });
  return <div className="space-y-2">{(rec.data ?? []).map((r) => <div key={r.id} className="border p-2 rounded">{r.title} ({r.type}) - {r.score}%</div>)}</div>;
};

export const StudentCareerRecommendationsPage = () => <Section title="AI Guidance - Careers"><RecommendationList /></Section>;
export const StudentBursaryRecommendationsPage = () => <Section title="AI Guidance - Bursaries"><RecommendationList /></Section>;

export const StudentSavedPage = () => {
  const [q, setQ] = useState('');
  const careers = useAppQuery({ queryKey: ['careers', q], queryFn: () => careerService.list({ q }) });
  return <Section title="Career Search">
    <Input placeholder="Search careers" value={q} onChange={(e) => setQ(e.target.value)} />
    {(Array.isArray(careers.data) ? careers.data : careers.data?.content ?? []).map((c) => <div key={c.id} className="flex justify-between border p-2 rounded"><span>{c.title}</span><Button onClick={() => studentService.saveCareer(c.id)}>Save</Button></div>)}
  </Section>;
};

export const StudentApplicationsPage = () => {
  const apps = useAppQuery({ queryKey: ['apps'], queryFn: applicationService.listMine });
  const bursaries = useAppQuery({ queryKey: ['burs'], queryFn: () => bursaryService.list({}) });
  return <Section title="Bursary Finder & Applications">
    {(Array.isArray(bursaries.data) ? bursaries.data : bursaries.data?.content ?? []).slice(0, 5).map((b) => <div key={b.id} className="flex justify-between border p-2 rounded"><span>{b.title}</span><div className="space-x-2"><Button onClick={() => studentService.saveBursary(b.id)}>Bookmark</Button><Button onClick={() => applicationService.submit(b.id)}>Apply</Button></div></div>)}
    <p className="font-medium">My applications: {(apps.data ?? []).length}</p>
  </Section>;
};

export const StudentNotificationsPage = () => {
  const notes = useAppQuery({ queryKey: ['notes'], queryFn: notificationService.mine });
  return <Section title="Notifications">{(notes.data ?? []).map((n) => <div key={n.id} className="border p-2 rounded"><p>{n.title}</p><p>{n.message}</p><Button onClick={() => notificationService.markRead(n.id)}>Mark read</Button></div>)}</Section>;
};

export const StudentSubscriptionPage = () => {
  const current = useAppQuery({ queryKey: ['sub'], queryFn: subscriptionService.current });
  return <Section title="Subscription">
    <p>Current: {current.data?.planCode ?? 'PLAN_BASIC'}</p>
    <Button onClick={() => subscriptionService.purchase('BASIC')}>Choose Basic</Button>
    <Button onClick={() => subscriptionService.purchase('PREMIUM')}>Choose Premium</Button>
  </Section>;
};

export const StudentSettingsPage = () => <Section title="Settings">Notification channels: Email + SMS placeholder configured.</Section>;
