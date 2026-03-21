import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAppQuery } from '@/hooks/useAppQuery';
import { studentService } from '@/services/studentService';
import { keepPreviousData } from '@tanstack/react-query';
import { recommendationService } from '@/services/recommendationService';
import { aiGuidanceService } from '@/services/aiGuidanceService';
import { bursaryService } from '@/services/bursaryService';
import { notificationService } from '@/services/notificationService';
import { applicationService } from '@/services/applicationService';
import { subscriptionService } from '@/services/subscriptionService';
import { settingsService } from '@/services/settingsService';
import { careerService } from '@/services/careerService';
import { Bell, BriefcaseBusiness, CheckCheck, CircleDollarSign, Clock3, Settings } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Input } from '@/components/ui/Input';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import type { ApiError, Notification, OpportunityType, UnifiedOpportunity, UniversityRecommendedCareer, UniversityRecommendedProgramme } from '@/types';

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

  const currentProfile = profile.data;
  const qualificationLevel = currentProfile?.qualificationLevel?.trim() ?? '';
  const careerInterest = (currentProfile?.interests ?? []).map((item) => item.trim()).filter(Boolean).join(', ');
  const targetProgram = currentProfile?.careerGoals?.trim() || careerInterest;
  const profileReadinessMessage = !currentProfile
    ? 'Student profile is required before requesting AI guidance.'
    : !qualificationLevel || !careerInterest
      ? 'Please complete your profile (qualification level and interests) before generating AI guidance.'
      : null;

  const defaultSources = useAppQuery({
    queryKey: ['default-university-sources'],
    enabled: !aiGuidanceService.demoModeEnabled,
    queryFn: aiGuidanceService.getDefaultUniversitySources,
  });

  const aiAdvice = useAppQuery({
    queryKey: ['ai-guidance-university-sources', currentProfile?.id, currentProfile?.profileCompleteness, qualificationLevel, careerInterest, targetProgram],
    enabled: Boolean(currentProfile) && !aiGuidanceService.demoModeEnabled && !profileReadinessMessage,
    queryFn: async () => aiGuidanceService.analyseUniversitySources({
      urls: [], // Empty list triggers backend default-source mode.
      targetProgram,
      careerInterest,
      qualificationLevel,
      maxRecommendations: 10,
    }),
    retry: false,
    placeholderData: keepPreviousData,
  });

  const demoAdvice = useAppQuery({
    queryKey: ['ai-guidance-demo'],
    enabled: aiGuidanceService.demoModeEnabled,
    queryFn: aiGuidanceService.getDemoGuidance,
  });

  if (profile.isLoading || demoAdvice.isLoading) return <LoadingState message="Generating AI guidance..." detail="We are matching your profile with the best-fit careers and programmes." />;
  if (profile.isError) return <ErrorState message="Could not load your profile. Please refresh and try again." />;

  const isDemoMode = aiGuidanceService.demoModeEnabled;
  const isSearching = !isDemoMode && (aiAdvice.isLoading || aiAdvice.isFetching || defaultSources.isLoading || defaultSources.isFetching);
  if (!isDemoMode && profileReadinessMessage) return <ErrorState message={profileReadinessMessage} />;

  const aiAdviceErrorMessage = (aiAdvice.error as ApiError | null)?.message;
  if (!isDemoMode && aiAdvice.isError) return <ErrorState message={aiAdviceErrorMessage || 'Unable to generate AI guidance right now. Please try again shortly.'} />;

  const demoRecommendations = (demoAdvice.data?.suggestedCareers ?? []).map((item) => item.title);
  const careers = aiAdvice.data?.recommendedCareers ?? [];
  const programmes = aiAdvice.data?.recommendedProgrammes ?? [];
  const universities = aiAdvice.data?.recommendedUniversities ?? [];
  const minimumRequirements = aiAdvice.data?.minimumRequirements ?? [];
  const skillGaps = aiAdvice.data?.skillGaps ?? [];
  const nextSteps = aiAdvice.data?.recommendedNextSteps ?? [];
  const warnings = aiAdvice.data?.warnings ?? [];
  const sourceDiagnostics = aiAdvice.data?.sourceDiagnostics ?? [];
  const sourceCoverage = aiAdvice.data?.sourceCoverage;
  const backendMode = aiAdvice.data?.fallbackUsed ? 'Fallback recommendations' : aiAdvice.data?.aiLive ? 'Live Gemini multi-source' : 'Unavailable';
  const requestedSources = aiAdvice.data?.sourceUrls?.length ?? 0;
  const analysedSources = aiAdvice.data?.totalSourcesUsed ?? 0;

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
      {career.rankingCategory && <p className="text-xs font-medium text-emerald-700">{career.rankingCategory}</p>}
      {career.recommendationReason && <p className="text-sm text-slate-700"><span className="font-medium">Why this was recommended:</span> {career.recommendationReason}</p>}
      <div>
        <p className="text-xs uppercase tracking-wide text-slate-500">Requirements</p>
        <ul className="list-disc ml-5 text-sm">{career.requirements.map((requirement) => <li key={`${career.name}-${requirement}`}>{requirement}</li>)}</ul>
      </div>
      <div>
        <p className="text-xs uppercase tracking-wide text-slate-500">Related programmes</p>
        <ul className="list-disc ml-5 text-sm">{career.relatedProgrammes.map((programme) => <li key={`${career.name}-${programme}`}>{programme}</li>)}</ul>
      </div>
      {career.verifiedFacts?.length ? <div><p className="text-xs uppercase tracking-wide text-slate-500">Verified facts</p><ul className="list-disc ml-5 text-sm">{career.verifiedFacts.map((fact) => <li key={`${career.name}-${fact}`}>{fact}</li>)}</ul></div> : null}
      {career.nextBestActions?.length ? <div><p className="text-xs uppercase tracking-wide text-slate-500">Next best actions</p><ul className="list-disc ml-5 text-sm">{career.nextBestActions.map((action) => <li key={`${career.name}-${action}`}>{action}</li>)}</ul></div> : null}
    </article>)}</div>;
  };

  const renderProgrammeCards = (items: UniversityRecommendedProgramme[]) => {
    if (items.length === 0) {
      return <p className="text-sm text-slate-500">No programme recommendations yet.</p>;
    }
    return <div className="grid gap-3 md:grid-cols-2">{items.slice(0, 10).map((programme) => <article key={`${programme.name}-${programme.university}`} className="rounded border p-3 space-y-2 bg-white">
      <h4 className="font-semibold">{programme.name}</h4>
      <p className="text-sm text-slate-600">{programme.university}</p>
      <div className="flex gap-2 text-xs">{programme.rankingCategory && <span className="rounded bg-emerald-50 px-2 py-1 text-emerald-700">{programme.rankingCategory}</span>}{programme.confidenceLevel && <span className="rounded bg-slate-100 px-2 py-1 text-slate-700">Confidence: {programme.confidenceLevel}</span>}{programme.sourceStatus && <span className="rounded bg-blue-50 px-2 py-1 text-blue-700">Source: {programme.sourceStatus}</span>}</div>
      {programme.recommendationReason && <p className="text-sm text-slate-700"><span className="font-medium">Why this was recommended:</span> {programme.recommendationReason}</p>}
      <div>
        <p className="text-xs uppercase tracking-wide text-slate-500">Admission requirements</p>
        <ul className="list-disc ml-5 text-sm">{programme.admissionRequirements.map((requirement) => <li key={`${programme.name}-${requirement}`}>{requirement}</li>)}</ul>
      </div>
      <div className="text-sm">
        <p><span className="font-medium">Notes:</span> {programme.notes}</p>
      </div>
      {programme.verifiedFacts?.length ? <div><p className="text-xs uppercase tracking-wide text-slate-500">Verified facts</p><ul className="list-disc ml-5 text-sm">{programme.verifiedFacts.map((fact) => <li key={`${programme.name}-${fact}`}>{fact}</li>)}</ul></div> : null}
      {programme.missingData?.length ? <div><p className="text-xs uppercase tracking-wide text-slate-500">Missing information</p><ul className="list-disc ml-5 text-sm">{programme.missingData.map((fact) => <li key={`${programme.name}-${fact}`}>{fact}</li>)}</ul></div> : null}
      {programme.nextBestActions?.length ? <div><p className="text-xs uppercase tracking-wide text-slate-500">Next best actions</p><ul className="list-disc ml-5 text-sm">{programme.nextBestActions.map((action) => <li key={`${programme.name}-${action}`}>{action}</li>)}</ul></div> : null}
    </article>)}</div>;
  };

  return <Section title="AI Guidance">
    <p className="text-sm text-slate-500">Mode: {isDemoMode ? 'demo (seeded)' : backendMode}</p>
    {isSearching ? <LoadingState message="Searching for guidance results..." detail="Please wait while we analyse your profile and university sources." /> : null}
    {!isDemoMode && <>
      <div className="grid gap-3 md:grid-cols-3">
        <Card label="Sources used" value={analysedSources} />
        <Card label="Requested sources" value={sourceCoverage?.requestedSourcesCount ?? requestedSources} />
        <Card label="Suitability score" value={`${aiAdvice.data?.suitabilityScore ?? 0}%`} />
      </div>
      {aiAdvice.data?.warningMessage && <div className="rounded border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
        <span className="font-semibold">Warning:</span> {aiAdvice.data.warningMessage}
      </div>}
      {analysedSources === 0 && <div className="rounded border border-rose-300 bg-rose-50 p-3 text-sm text-rose-900">
        No public university sources were analysed for this response. Please verify every recommendation on official university pages.
      </div>}
      {aiAdvice.data?.suitabilityScoreReason && <div className="rounded border p-3 bg-slate-50 text-sm">
        <p><span className="font-semibold">Suitability explanation:</span> {aiAdvice.data.suitabilityScoreReason}</p>
        {!!aiAdvice.data.suitabilitySignalsUsed?.length && <p className="mt-2"><span className="font-semibold">Signals used:</span> {aiAdvice.data.suitabilitySignalsUsed.join(', ')}</p>}
        {!!aiAdvice.data.suitabilityScoreLimitations?.length && <p className="mt-2"><span className="font-semibold">What lowered the score:</span> {aiAdvice.data.suitabilityScoreLimitations.join(', ')}</p>}
      </div>}
      {sourceCoverage && <div className="rounded border p-3 bg-white text-sm">
        <h3 className="font-semibold mb-1">Source coverage</h3>
        <p>Successful: {sourceCoverage.successfulSourcesCount} · Failed: {sourceCoverage.failedSourcesCount} · Partial: {sourceCoverage.partialSourcesCount}</p>
        {!!sourceCoverage.universitiesWithUsableProgrammeData?.length && <p className="mt-1">Universities with usable programme data: {sourceCoverage.universitiesWithUsableProgrammeData.join(', ')}</p>}
      </div>}
    </>}

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

      {sourceDiagnostics.length > 0 && <div className="space-y-2">
        <h3 className="font-semibold">Source fetch status</h3>
        <div className="grid gap-2 md:grid-cols-2">{sourceDiagnostics.map((item) => <div key={item.sourceUrl} className="rounded border p-3 text-sm"><p className="font-medium">{item.university ?? 'University Source'}</p><p className="break-all text-slate-500">{item.sourceUrl}</p><p className="mt-1"><span className="font-medium">Status:</span> {item.fetchStatus}</p>{item.failureReason && <p><span className="font-medium">Reason:</span> {item.failureReason}</p>}</div>)}</div>
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
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [filters, setFilters] = useState({ q: '', field: '', industry: '', qualification: '', location: '', demand: '', opportunityType: 'ALL' as OpportunityType });
  const opportunities = useAppQuery({ queryKey: ['student-opportunities', filters], queryFn: () => studentService.searchOpportunities(filters) });
  const toggle = useMutation({
    mutationFn: ({ item }: { item: UnifiedOpportunity }) => item.saved
      ? studentService.unsaveOpportunity(item.type, item.id)
      : studentService.saveOpportunity(item.type, item.id, item.title),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['student-opportunities'] }),
  });
  const items = opportunities.data ?? [];
  const badgeColor = (type: UnifiedOpportunity['type']): 'blue' | 'emerald' | 'amber' => type === 'CAREER' ? 'blue' : type === 'JOB' ? 'emerald' : 'amber';
  return <Section title="Explore Careers, Jobs & Internships">
    <p className="text-sm text-slate-500">Explore careers, jobs, and internships tailored to your skills, interests, and academic background.</p>
    <div className="grid gap-2 md:grid-cols-4">
      <Input placeholder="Search" value={filters.q} onChange={(e) => setFilters((s) => ({ ...s, q: e.target.value }))} />
      <Input placeholder="Field" value={filters.field} onChange={(e) => setFilters((s) => ({ ...s, field: e.target.value }))} />
      <Input placeholder="Industry" value={filters.industry} onChange={(e) => setFilters((s) => ({ ...s, industry: e.target.value }))} />
      <Input placeholder="Qualification" value={filters.qualification} onChange={(e) => setFilters((s) => ({ ...s, qualification: e.target.value }))} />
      <Input placeholder="Location" value={filters.location} onChange={(e) => setFilters((s) => ({ ...s, location: e.target.value }))} />
      <Input placeholder="Demand" value={filters.demand} onChange={(e) => setFilters((s) => ({ ...s, demand: e.target.value }))} />
      <select className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary-500" value={filters.opportunityType} onChange={(e) => setFilters((s) => ({ ...s, opportunityType: e.target.value as OpportunityType }))}>
        <option value="ALL">All opportunity types</option>
        <option value="CAREER">Career</option>
        <option value="JOB">Job</option>
        <option value="INTERNSHIP">Internship</option>
      </select>
    </div>
    <div className="grid gap-3 md:grid-cols-2">
      {items.map((item) => <article key={`${item.type}-${item.id}`} className="rounded border p-4 space-y-3 bg-white">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h3 className="text-base font-semibold">{item.title}</h3>
              <Badge color={badgeColor(item.type)}>{item.type}</Badge>
              {item.recommended ? <span className="rounded bg-emerald-50 px-2 py-1 text-xs font-medium text-emerald-700">Recommended</span> : null}
            </div>
            <p className="text-sm text-slate-600">{item.industry ?? item.field ?? 'General opportunities'}{item.location ? ` • ${item.location}` : ''}</p>
          </div>
          <Button onClick={() => toggle.mutate({ item })} disabled={toggle.isPending}>{item.saved ? 'Saved ✓' : 'Save'}</Button>
        </div>
        <div className="grid gap-2 text-sm text-slate-600 sm:grid-cols-2">
          <p><span className="font-medium text-slate-800">Field:</span> {item.field ?? '—'}</p>
          <p><span className="font-medium text-slate-800">Industry:</span> {item.industry ?? '—'}</p>
          <p><span className="font-medium text-slate-800">Qualification:</span> {item.qualification ?? '—'}</p>
          <p><span className="font-medium text-slate-800">Demand:</span> {item.demand ?? '—'}</p>
        </div>
        {item.type === 'CAREER' ? <div><Button className="bg-slate-700 hover:bg-slate-600" onClick={() => navigate(`/student/careers/${item.id}`)}>View details</Button></div> : null}
      </article>)}
    </div>
    {!items.length && !opportunities.isLoading ? <EmptyState title="No opportunities found" message="Try broadening your filters to explore more careers, jobs, and internships." /> : null}
  </Section>;
};

export const StudentCareerDetailsPage = () => {
  const { id = '' } = useParams();
  const career = useAppQuery({
    queryKey: ['student', 'career', id],
    queryFn: () => careerService.details(id),
    enabled: Boolean(id),
  });

  if (career.isLoading) return <LoadingState message="Loading career details..." />;
  if (career.isError || !career.data) return <ErrorState message="Could not load this career right now. Please return to search results and try again." />;

  return <Section title={career.data.title}>
    <p className="text-sm text-slate-500">Detailed information for the selected career path.</p>
    <div className="grid gap-3 md:grid-cols-2">
      <div className="rounded border p-4 bg-white">
        <p className="text-xs uppercase tracking-wide text-slate-500">Industry</p>
        <p className="mt-1 text-sm text-slate-700">{career.data.industry || 'Not specified'}</p>
      </div>
      <div className="rounded border p-4 bg-white">
        <p className="text-xs uppercase tracking-wide text-slate-500">Location</p>
        <p className="mt-1 text-sm text-slate-700">{career.data.location || 'Not specified'}</p>
      </div>
      <div className="rounded border p-4 bg-white">
        <p className="text-xs uppercase tracking-wide text-slate-500">Qualification level</p>
        <p className="mt-1 text-sm text-slate-700">{career.data.qualificationLevel || 'Not specified'}</p>
      </div>
      <div className="rounded border p-4 bg-white">
        <p className="text-xs uppercase tracking-wide text-slate-500">Career ID</p>
        <p className="mt-1 break-all text-sm text-slate-700">{career.data.id}</p>
      </div>
    </div>
    <div className="rounded border p-4 bg-white">
      <p className="text-xs uppercase tracking-wide text-slate-500">Overview</p>
      <p className="mt-2 text-sm text-slate-700">{career.data.description || 'Detailed overview is not available for this career yet.'}</p>
    </div>
  </Section>;
};

export const StudentApplicationsPage = () => {
  const qc = useQueryClient();
  const [filters, setFilters] = useState({ q: '', qualificationLevel: '', region: '', eligibility: '' });
  const apps = useAppQuery({ queryKey: ['apps'], queryFn: applicationService.listMine });
  const bursaries = useAppQuery({ queryKey: ['burs-search', filters], queryFn: () => bursaryService.search({ q: filters.q, qualification: filters.qualificationLevel, region: filters.region, eligibility: filters.eligibility }) });
  const saved = useAppQuery({ queryKey: ['saved-bursary-ids'], queryFn: studentService.savedBursaries });
  const recommendations = useAppQuery({ queryKey: ['recs-bursary-finder'], queryFn: bursaryService.recommended });
  const toggle = useMutation({ mutationFn: ({ id, exists }: { id: string; exists: boolean }) => exists ? studentService.unsaveBursary(id) : studentService.saveBursary(id), onSuccess: () => qc.invalidateQueries({ queryKey: ['saved-bursary-ids'] }) });
  return <Section title="Bursary Finder">
    <div className="rounded border p-3">
      <h3 className="font-semibold mb-2">AI Recommended Bursaries</h3>
      {(recommendations.data ?? []).slice(0, 3).map((item) => <p key={item.externalId}>• {item.title} ({item.relevanceScore}%)</p>)}
      {!((recommendations.data ?? []).length) && <p className="text-sm text-slate-500">No AI bursary suggestions yet. Complete your profile for better matches.</p>}
    </div>
    <div className="grid gap-2 md:grid-cols-2">
      <Input placeholder="Search bursaries" value={filters.q} onChange={(e) => setFilters((s) => ({ ...s, q: e.target.value }))} />
      <Input placeholder="Qualification" value={filters.qualificationLevel} onChange={(e) => setFilters((s) => ({ ...s, qualificationLevel: e.target.value }))} />
      <Input placeholder="Region" value={filters.region} onChange={(e) => setFilters((s) => ({ ...s, region: e.target.value }))} />
      <Input placeholder="Eligibility" value={filters.eligibility} onChange={(e) => setFilters((s) => ({ ...s, eligibility: e.target.value }))} />
    </div>
    {((bursaries.data?.items ?? []) as Array<any>).map((b) => {
      const exists = (saved.data ?? []).includes(b.id);
      return <div key={b.id} className="flex justify-between border p-2 rounded"><span>{b.title} - {b.region}</span><div className="space-x-2"><Button onClick={() => toggle.mutate({ id: b.id, exists })}>{exists ? 'Saved' : 'Bookmark'}</Button><Button onClick={() => applicationService.submit(b.id)}>Apply</Button></div></div>;
    })}
    <p className="font-medium">My applications: {(apps.data ?? []).length}</p>
  </Section>;
};

type NotificationType = 'SYSTEM' | 'BURSARY' | 'DEADLINE' | 'CAREER' | 'SUBSCRIPTION';

type NotificationTypeMeta = {
  label: string;
  icon: typeof Bell;
  unreadAccent: string;
  iconClassName: string;
  pill: 'slate' | 'emerald' | 'amber' | 'blue';
};

const notificationTypeMeta: Record<NotificationType, NotificationTypeMeta> = {
  SYSTEM: {
    label: 'System',
    icon: Settings,
    unreadAccent: 'border-l-slate-400 bg-slate-50/80',
    iconClassName: 'bg-slate-100 text-slate-700',
    pill: 'slate',
  },
  BURSARY: {
    label: 'Bursary',
    icon: CircleDollarSign,
    unreadAccent: 'border-l-emerald-400 bg-emerald-50/80',
    iconClassName: 'bg-emerald-100 text-emerald-700',
    pill: 'emerald',
  },
  DEADLINE: {
    label: 'Deadline',
    icon: Clock3,
    unreadAccent: 'border-l-amber-400 bg-amber-50/80',
    iconClassName: 'bg-amber-100 text-amber-700',
    pill: 'amber',
  },
  CAREER: {
    label: 'Career',
    icon: BriefcaseBusiness,
    unreadAccent: 'border-l-blue-400 bg-blue-50/80',
    iconClassName: 'bg-blue-100 text-blue-700',
    pill: 'blue',
  },
  SUBSCRIPTION: {
    label: 'Subscription',
    icon: Bell,
    unreadAccent: 'border-l-violet-400 bg-violet-50/80',
    iconClassName: 'bg-violet-100 text-violet-700',
    pill: 'blue',
  },
};

const inferNotificationType = (note: Notification): NotificationType => {
  const backendType = (note.type ?? '').toUpperCase();
  if (backendType in notificationTypeMeta) return backendType as NotificationType;

  const haystack = `${note.title} ${note.message}`.toLowerCase();
  if (haystack.includes('subscription') || haystack.includes('plan') || haystack.includes('premium')) return 'SUBSCRIPTION';
  if (haystack.includes('deadline') || haystack.includes('closes') || haystack.includes('due')) return 'DEADLINE';
  if (haystack.includes('bursary') || haystack.includes('scholarship') || haystack.includes('funding')) return 'BURSARY';
  if (haystack.includes('career') || haystack.includes('job') || haystack.includes('internship') || haystack.includes('match')) return 'CAREER';
  return 'SYSTEM';
};

const formatNotificationTimestamp = (value?: string) => {
  if (!value) return 'Recently';

  const timestamp = new Date(value);
  if (Number.isNaN(timestamp.getTime())) return 'Recently';

  const diffMs = Date.now() - timestamp.getTime();
  if (diffMs < 0) {
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' }).format(timestamp);
  }

  const diffMinutes = Math.floor(diffMs / 60000);
  if (diffMinutes < 1) return 'Just now';
  if (diffMinutes < 60) return `${diffMinutes} minute${diffMinutes === 1 ? '' : 's'} ago`;

  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) return `${diffHours} hour${diffHours === 1 ? '' : 's'} ago`;

  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) return `${diffDays} day${diffDays === 1 ? '' : 's'} ago`;

  return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' }).format(timestamp);
};

export const StudentNotificationsPage = () => {
  const qc = useQueryClient();
  const notes = useAppQuery({ queryKey: ['notes'], queryFn: notificationService.mine });
  const markRead = useMutation({ mutationFn: (id: string) => notificationService.markRead(id), onSuccess: () => qc.invalidateQueries({ queryKey: ['notes'] }) });

  const notifications = useMemo(() => (notes.data ?? []).map((note) => {
    const type = inferNotificationType(note);
    const read = note.read ?? note.isRead ?? false;
    return { ...note, read, type, meta: notificationTypeMeta[type], timestampLabel: formatNotificationTimestamp(note.createdAt) };
  }), [notes.data]);

  if (notes.isLoading) return <LoadingState />;
  if (notes.isError) return <ErrorState message="Could not load your notifications. Please refresh and try again." />;
  if (!notes.data?.length) return <EmptyState title="No notifications yet" message="We’ll notify you when something important comes up." />;

  return <Section title="Notifications">
    <div className="grid gap-3">
      {notifications.map((n) => {
        const Icon = n.meta.icon;
        return <article
          key={n.id}
          className={`group rounded-2xl border border-slate-200 p-4 transition hover:-translate-y-0.5 hover:shadow-md md:p-5 ${n.read ? 'bg-slate-50/70 opacity-90' : `border-l-4 ${n.meta.unreadAccent} shadow-sm`}`}
        >
          <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
            <div className="flex gap-3">
              <div className={`mt-0.5 flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl ${n.read ? 'bg-slate-100 text-slate-500' : `${n.meta.iconClassName} shadow-sm`}`}>
                <Icon size={20} />
              </div>
              <div className="min-w-0 space-y-3">
                <div className="flex flex-wrap items-center gap-2">
                  <p className={`text-base ${n.read ? 'font-medium text-slate-700' : 'font-semibold text-slate-900'}`}>{n.title}</p>
                  <Badge color={n.meta.pill}>{n.meta.label}</Badge>
                  {!n.read ? <span className="inline-flex items-center gap-1 rounded-full bg-primary-50 px-2 py-1 text-xs font-medium text-primary-700"><span className="h-2 w-2 rounded-full bg-primary-600" />Unread</span> : null}
                </div>
                <p className={`text-sm leading-6 ${n.read ? 'text-slate-500' : 'text-slate-600'}`}>{n.message}</p>
                <div className="flex flex-wrap items-center gap-2 text-xs text-slate-500">
                  <span>{n.timestampLabel}</span>
                </div>
              </div>
            </div>
            <div className="flex shrink-0 items-center md:pl-4">
              {n.read ? (
                <span className="inline-flex cursor-default items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-500">
                  <CheckCheck size={16} />
                  ✓ Read
                </span>
              ) : (
                <Button
                  onClick={() => markRead.mutate(n.id)}
                  disabled={markRead.isPending}
                  className="inline-flex items-center gap-2 bg-primary-600 shadow-sm hover:bg-primary-500"
                >
                  <CheckCheck size={16} />
                  Mark as Read
                </Button>
              )}
            </div>
          </div>
        </article>;
      })}
    </div>
  </Section>;
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
export { StudentUniversitiesPage } from './StudentUniversitiesPage';
