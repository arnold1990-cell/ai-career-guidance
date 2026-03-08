import { Link, useParams } from 'react-router-dom';
import { PageShell } from '@/pages/PageShell';
import { Button } from '@/components/ui/Button';

export const LandingPage = () => (
  <div className="space-y-10">
    <section className="card grid gap-6 p-8 md:grid-cols-2">
      <div>
        <h1 className="text-4xl font-bold">AI-powered career guidance for every student.</h1>
        <p className="mt-4 text-slate-600">EduRite helps students discover best-fit careers, bursaries, and institutions while companies find high-potential talent.</p>
        <div className="mt-6 flex gap-3"><Link to="/auth/register/student"><Button>Get Started</Button></Link><Link to="/pricing"><Button className="bg-emerald-600 hover:bg-emerald-500">View Plans</Button></Link></div>
      </div>
      <div className="rounded-xl bg-gradient-to-br from-blue-100 to-emerald-100 p-6 text-sm text-slate-700">Hero analytics panel: match scores, application velocity, and profile readiness insights.</div>
    </section>
    <section className="grid gap-4 md:grid-cols-3">{['Features', 'How It Works', 'Benefits for Students', 'Benefits for Companies', 'Pricing Teaser', 'Call to Action'].map((name) => <div key={name} className="card p-5"><h3 className="font-semibold">{name}</h3><p className="mt-2 text-sm text-slate-600">Modern SaaS section built with responsive card UI.</p></div>)}</section>
  </div>
);

export const AboutPage = () => <PageShell title="About EduRite" description="Mission-driven career enablement platform for students and employers." />;
export const CareersPage = () => <PageShell title="Career Listings" description="Browse in-demand career paths and future opportunities." />;
export const CareerDetailsPage = () => { const { id } = useParams(); return <PageShell title={`Career Details #${id ?? ''}`} description="Detailed overview, requirements, and growth path." />; };
export const CoursesPage = () => <PageShell title="Courses" description="Explore courses aligned to recommended careers." />;
export const CourseDetailsPage = () => { const { id } = useParams(); return <PageShell title={`Course Details #${id ?? ''}`} description="Course modules, duration, and admission requirements." />; };
export const InstitutionsPage = () => <PageShell title="Institutions" description="Discover universities and colleges matched to your goals." />;
export const InstitutionDetailsPage = () => { const { id } = useParams(); return <PageShell title={`Institution #${id ?? ''}`} description="Institution profile, programs, and location." />; };
export const BursariesPage = () => <PageShell title="Bursaries" description="Find funding opportunities and deadlines." />;
export const BursaryDetailsPage = () => { const { id } = useParams(); return <PageShell title={`Bursary #${id ?? ''}`} description="Eligibility, benefits, and application process." />; };
export const PricingPage = () => <PageShell title="Pricing" description="Flexible plans for students, companies, and institutions." />;
