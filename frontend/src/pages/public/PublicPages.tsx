import { Link, useParams } from 'react-router-dom';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/tables/DataTable';
import { MetricCard } from '@/components/cards/MetricCard';
import { PlaceholderChart } from '@/components/charts/PlaceholderChart';
import { useAppQuery } from '@/hooks/useAppQuery';
import { careerService } from '@/services/careerService';
import { courseService } from '@/services/courseService';
import { institutionService } from '@/services/institutionService';
import { bursaryService } from '@/services/bursaryService';
import type { Bursary, Career, Course, Institution } from '@/types';

const PageIntro = ({ title, subtitle }: { title: string; subtitle: string }) => (
  <div>
    <h1 className="text-3xl font-bold text-slate-900">{title}</h1>
    <p className="mt-1 text-sm text-slate-600">{subtitle}</p>
  </div>
);

export const LandingPage = () => (
  <div className="space-y-8">
    <section className="card grid gap-8 p-8 lg:grid-cols-2">
      <div>
        <Badge color="blue">Career intelligence for students and sponsors</Badge>
        <h1 className="mt-4 text-4xl font-bold leading-tight">Build brighter futures with smarter career and bursary matching.</h1>
        <p className="mt-3 text-slate-600">EduRite helps students discover pathways, helps companies invest in high-potential talent, and gives admins enterprise-grade oversight.</p>
        <div className="mt-6 flex flex-wrap gap-3"><Link to="/auth/register/student"><Button>Start as Student</Button></Link><Link to="/auth/register/company"><Button className="bg-emerald-600 hover:bg-emerald-500">Hire & Fund Talent</Button></Link></div>
      </div>
      <div className="grid gap-3">
        <MetricCard title="Students matched" value="45,000+" subtitle="AI career fit and funding recommendations" />
        <MetricCard title="Bursary applications" value="120,000+" subtitle="Managed through the EduRite workflow" />
        <MetricCard title="Partner organizations" value="1,300+" subtitle="Companies, institutions, and nonprofits" />
      </div>
    </section>

    <section className="grid gap-4 md:grid-cols-3">
      {[
        { t: 'Career discovery', d: 'Assess strengths, interests, and subject performance to identify high-fit career paths.' },
        { t: 'Funding marketplace', d: 'Browse bursaries with transparent eligibility criteria and deadline tracking.' },
        { t: 'Talent sourcing', d: 'Company dashboards to shortlist top student candidates with structured scorecards.' },
      ].map((item) => <article key={item.t} className="card p-5"><h3 className="font-semibold">{item.t}</h3><p className="mt-2 text-sm text-slate-600">{item.d}</p></article>)}
    </section>

    <section className="grid gap-4 lg:grid-cols-2"><PlaceholderChart title="Platform growth snapshot" /><PlaceholderChart title="Application and conversion trends" /></section>
  </div>
);

export const AboutPage = () => (
  <section className="space-y-4">
    <PageIntro title="About EduRite" subtitle="EduRite powers equitable access to education and career opportunities." />
    <div className="card p-6 text-sm text-slate-600">We combine AI-driven recommendations with human-centered workflows for students, companies, and administrators.</div>
  </section>
);

const FilterBar = ({ placeholder }: { placeholder: string }) => (
  <div className="card p-4"><div className="grid gap-3 md:grid-cols-4"><input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder={placeholder} /><input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="Location" /><input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="Category" /><Button>Apply filters</Button></div></div>
);

export const CareersPage = () => {
  const careers = useAppQuery<Career[]>({ queryKey: ['public', 'careers'], queryFn: () => careerService.list() });
  const rows = Array.isArray(careers.data) ? careers.data : [{ id: '1', title: 'Software Engineer', description: 'Build scalable digital products.', matchScore: 89 }];
  return (
    <section className="space-y-6">
      <PageIntro title="Career Listings" subtitle="Explore in-demand careers, role expectations, and growth potential." />
      <FilterBar placeholder="Search careers" />
      <DataTable columns={[{ key: 'title', header: 'Career' }, { key: 'description', header: 'Overview' }, { key: 'matchScore', header: 'Demand score', render: (row) => <Badge color="blue">{row.matchScore ?? 80}%</Badge> }]} data={rows} />
    </section>
  );
};

export const CareerDetailsPage = () => {
  const { id = '' } = useParams();
  useAppQuery({ queryKey: ['public', 'career', id], queryFn: () => careerService.details(id), enabled: Boolean(id) });
  return <section className="space-y-4"><PageIntro title="Career Details" subtitle={`Deep dive into required skills, qualifications, and opportunities for career #${id}.`} /><div className="card p-6 text-sm text-slate-600">Expected salary bands, learning roadmap, and top institutions are shown in this detail view.</div></section>;
};

export const CoursesPage = () => {
  const courses = useAppQuery<Course[]>({ queryKey: ['public', 'courses'], queryFn: () => courseService.list() });
  const rows = Array.isArray(courses.data) ? courses.data : [{ id: 'c1', name: 'BSc Computer Science', institutionName: 'University of Cape Town', duration: '3 years' }];
  return <section className="space-y-6"><PageIntro title="Courses" subtitle="Compare accredited courses aligned with your career ambitions." /><FilterBar placeholder="Search courses" /><DataTable columns={[{ key: 'name', header: 'Course' }, { key: 'institutionName', header: 'Institution' }, { key: 'duration', header: 'Duration' }]} data={rows} /></section>;
};

export const CourseDetailsPage = () => {
  const { id = '' } = useParams();
  useAppQuery({ queryKey: ['public', 'course', id], queryFn: () => courseService.details(id), enabled: Boolean(id) });
  return <section className="space-y-4"><PageIntro title="Course Details" subtitle={`Program outline, admission criteria, and graduate outcomes for course #${id}.`} /><div className="card p-6 text-sm text-slate-600">Review modules by year, tuition estimate, and application timeline milestones.</div></section>;
};

export const InstitutionsPage = () => {
  const institutions = useAppQuery<Institution[]>({ queryKey: ['public', 'institutions'], queryFn: () => institutionService.list() });
  const rows = Array.isArray(institutions.data) ? institutions.data : [{ id: 'i1', name: 'University of Pretoria', location: 'Pretoria' }];
  return <section className="space-y-6"><PageIntro title="Institutions" subtitle="Discover universities and colleges that best match your profile and goals." /><FilterBar placeholder="Search institutions" /><DataTable columns={[{ key: 'name', header: 'Institution' }, { key: 'location', header: 'Location' }]} data={rows} /></section>;
};

export const InstitutionDetailsPage = () => {
  const { id = '' } = useParams();
  useAppQuery({ queryKey: ['public', 'institution', id], queryFn: () => institutionService.details(id), enabled: Boolean(id) });
  return <section className="space-y-4"><PageIntro title="Institution Details" subtitle={`Campus profile, programs, and admission windows for institution #${id}.`} /><div className="card p-6 text-sm text-slate-600">View ranking indicators, supported bursaries, and location insights.</div></section>;
};

export const BursariesPage = () => {
  const bursaries = useAppQuery<Bursary[]>({ queryKey: ['public', 'bursaries'], queryFn: () => bursaryService.list() });
  const rows = Array.isArray(bursaries.data) ? bursaries.data : [{ id: 'b1', title: 'Women in STEM Fund', provider: 'FutureTech', status: 'PUBLISHED' }];
  return <section className="space-y-6"><PageIntro title="Bursaries" subtitle="Browse funding opportunities by field, location, and eligibility criteria." /><FilterBar placeholder="Search bursaries" /><DataTable columns={[{ key: 'title', header: 'Bursary' }, { key: 'provider', header: 'Provider' }, { key: 'status', header: 'Status', render: (row) => <Badge color={row.status === 'PUBLISHED' ? 'emerald' : 'amber'}>{row.status}</Badge> }]} data={rows} /></section>;
};

export const BursaryDetailsPage = () => {
  const { id = '' } = useParams();
  useAppQuery({ queryKey: ['public', 'bursary', id], queryFn: () => bursaryService.details(id), enabled: Boolean(id) });
  return <section className="space-y-4"><PageIntro title="Bursary Details" subtitle={`Eligibility requirements, benefits, and process details for bursary #${id}.`} /><div className="card p-6 text-sm text-slate-600">Prepare your supporting documents and track all key submission dates.</div></section>;
};

export const PricingPage = () => (
  <section className="space-y-6">
    <PageIntro title="Pricing" subtitle="Choose a plan that matches your goals—student growth, hiring, or platform administration." />
    <div className="grid gap-4 md:grid-cols-3">
      {[{ name: 'Starter', price: 'Free', desc: 'Basic discovery and profile tools' }, { name: 'Student Pro', price: 'R89/mo', desc: 'Advanced recommendations and alerts' }, { name: 'Company Growth', price: 'R2,499/mo', desc: 'Applicant pipeline and talent search' }].map((plan) => (
        <article key={plan.name} className="card p-5"><h2 className="text-lg font-semibold">{plan.name}</h2><p className="mt-2 text-2xl font-bold">{plan.price}</p><p className="mt-2 text-sm text-slate-600">{plan.desc}</p><Button className="mt-4 w-full">Choose plan</Button></article>
      ))}
    </div>
  </section>
);
