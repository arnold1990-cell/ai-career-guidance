import { Link, useParams } from 'react-router-dom';
import dashboardImage from '@/assets/Edurite-dashboard.jpeg';
import { MetricCard } from '@/components/cards/MetricCard';
import { PlaceholderChart } from '@/components/charts/PlaceholderChart';
import { BackgroundSection } from '@/components/sections/BackgroundSection';
import { DataTable } from '@/components/tables/DataTable';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { useAppQuery } from '@/hooks/useAppQuery';
import { bursaryService } from '@/services/bursaryService';
import { careerService } from '@/services/careerService';
import { courseService } from '@/services/courseService';
import { institutionService } from '@/services/institutionService';
import type { Bursary, Career, Course, Institution, PaginatedResponse } from '@/types';

interface PageIntroProps {
  title: string;
  subtitle: string;
}

interface FilterBarProps {
  placeholder: string;
}

interface FeatureCard {
  title: string;
  description: string;
}

interface LandingBackgroundPanel extends FeatureCard {
  badge: string;
  imagePositionClassName?: string;
}

interface PricingPlan {
  name: string;
  price: string;
  desc: string;
  features: readonly string[];
  featured?: boolean;
}

const LANDING_HIGHLIGHTS: FeatureCard[] = [
  {
    title: 'Career discovery',
    description:
      'Assess strengths, interests, and subject performance to identify high-fit career paths.',
  },
  {
    title: 'Funding marketplace',
    description:
      'Browse bursaries with transparent eligibility criteria and deadline tracking.',
  },
  {
    title: 'Talent sourcing',
    description:
      'Company dashboards to shortlist top student candidates with structured scorecards.',
  },
];

const NOTIFICATION_FEATURES = [
  'Receive alerts for new bursary opportunities',
  'Get reminders before application deadlines',
  'Stay updated with new career insights and recommendations',
] as const;

const HUMAN_CENTERED_WORKFLOWS = [
  'Students to discover opportunities and receive personalized guidance',
  'Companies to identify and recruit suitable talent',
  'Administrators to manage approvals, bursaries, and platform operations efficiently',
] as const;

const LANDING_BACKGROUND_PANELS: LandingBackgroundPanel[] = [
  {
    badge: 'Careers',
    title: 'Explore career paths with confidence',
    description:
      'Match student strengths to in-demand roles with clearer pathways, recommendations, and skill-aligned discovery.',
    imagePositionClassName: 'object-center',
  },
  {
    badge: 'Courses',
    title: 'Compare the right courses faster',
    description:
      'Review accredited programs, institutions, and learning options without overwhelming first-time applicants.',
    imagePositionClassName: 'object-[center_35%]',
  },
  {
    badge: 'Bursaries',
    title: 'Keep funding opportunities easy to scan',
    description:
      'Present bursaries with stronger contrast, better readability, and a cleaner layout across mobile and desktop screens.',
    imagePositionClassName: 'object-[center_60%]',
  },
];

const pricingPlans: readonly PricingPlan[] = [
  {
    name: 'Starter',
    price: 'Free',
    desc: 'Get started with essential tools',
    features: ['Basic profile creation', 'Explore careers and courses', 'Limited discovery features'],
  },
  {
    name: 'Student Pro',
    price: 'R15 / month',
    desc: 'Unlock smarter guidance and opportunities',
    features: [
      'Advanced AI recommendations',
      'Personalized alerts (bursaries & careers)',
      'Enhanced search and insights',
    ],
    featured: true,
  },
  {
    name: 'Company Growth',
    price: 'R49.99 / month',
    desc: 'Grow your talent pipeline efficiently',
    features: ['Applicant pipeline management', 'Talent search and filtering', 'Post opportunities and bursaries'],
  },
];

const FALLBACK_CAREERS: Career[] = [
  {
    id: '1',
    title: 'Software Engineer',
    description: 'Build scalable digital products.',
    matchScore: 89,
  },
];

const FALLBACK_COURSES: Course[] = [
  {
    id: 'c1',
    name: 'BSc Computer Science',
    institutionName: 'University of Cape Town',
    duration: '3 years',
  },
];

const FALLBACK_INSTITUTIONS: Institution[] = [
  {
    id: 'i1',
    name: 'University of Pretoria',
    location: 'Pretoria',
  },
];

const FALLBACK_BURSARIES: Bursary[] = [
  {
    id: 'b1',
    title: 'Women in STEM Fund',
    provider: 'FutureTech',
    status: 'PUBLISHED',
  },
];

const resolveRows = <T,>(
  data: T[] | PaginatedResponse<T> | undefined,
  fallbackRows: T[],
): T[] => {
  if (Array.isArray(data)) {
    return data;
  }

  if (data && 'content' in data && Array.isArray(data.content)) {
    return data.content;
  }

  return fallbackRows;
};

const PageIntro = ({ title, subtitle }: PageIntroProps) => (
  <div>
    <h1 className="text-3xl font-bold text-slate-900">{title}</h1>
    <p className="mt-1 text-sm text-slate-600">{subtitle}</p>
  </div>
);

const BulletList = ({ items }: { items: readonly string[] }) => (
  <ul className="space-y-3 text-sm leading-6 text-slate-700 sm:text-base">
    {items.map((item) => (
      <li key={item} className="flex gap-3">
        <span
          className="mt-1 h-2.5 w-2.5 shrink-0 rounded-full bg-primary-600"
          aria-hidden="true"
        />
        <span>{item}</span>
      </li>
    ))}
  </ul>
);

const FilterBar = ({ placeholder }: FilterBarProps) => (
  <div className="card p-4">
    <div className="grid gap-3 md:grid-cols-4">
      <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder={placeholder} />
      <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="Location" />
      <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="Category" />
      <Button type="button">Apply filters</Button>
    </div>
  </div>
);

export const LandingPage = () => (
  <div className="space-y-8">
    <section className="rounded-2xl border border-slate-200 bg-white p-8 shadow-sm lg:grid lg:grid-cols-[1.05fr_1fr] lg:gap-10">
      <div>
        <Badge color="blue">Career intelligence for students and sponsors</Badge>
        <h1 className="mt-5 text-4xl font-bold leading-tight text-slate-900 md:text-5xl">
          Build brighter futures with smarter career and bursary matching.
        </h1>
        <p className="mt-5 max-w-2xl text-lg text-slate-600 md:text-xl">
          EduRite helps students discover pathways, helps companies invest in high-potential
          talent, and gives admins enterprise-grade oversight.
        </p>
        <div className="mt-8 flex flex-wrap gap-3">
          <Link to="/auth/register/student">
            <Button className="rounded-xl px-6 py-3 text-base">Start as Student</Button>
          </Link>
          <Link to="/auth/register/company">
            <Button className="rounded-xl px-6 py-3 text-base">Hire &amp; Fund Talent</Button>
          </Link>
        </div>
      </div>
      <div className="mt-8 grid gap-4 lg:mt-0">
        <MetricCard
          title="Students matched"
          value="45,000+"
          subtitle="AI career fit and funding recommendations"
        />
        <MetricCard
          title="Bursary applications"
          value="120,000+"
          subtitle="Managed through the EduRite workflow"
        />
        <MetricCard
          title="Partner organizations"
          value="1,300+"
          subtitle="Companies, institutions, and nonprofits"
        />
      </div>
    </section>

    <section className="grid gap-4 md:grid-cols-3">
      {LANDING_HIGHLIGHTS.map((item) => (
        <article key={item.title} className="card p-5">
          <h3 className="font-semibold">{item.title}</h3>
          <p className="mt-2 text-sm text-slate-600">{item.description}</p>
        </article>
      ))}
    </section>

    <section className="grid gap-4 xl:grid-cols-3">
      {LANDING_BACKGROUND_PANELS.map((panel) => (
        <BackgroundSection
          key={panel.badge}
          eyebrow={<Badge color="blue">{panel.badge}</Badge>}
          title={panel.title}
          description={panel.description}
          imageSrc={dashboardImage}
          imagePositionClassName={panel.imagePositionClassName}
          contentClassName="min-h-[260px] justify-end"
        />
      ))}
    </section>

    <section className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
      <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="space-y-3">
          <Badge color="blue">Student experience</Badge>
          <div>
            <h2 className="text-2xl font-semibold tracking-tight text-slate-900">
              Notifications &amp; Alerts
            </h2>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
              Stay informed with timely updates delivered directly through the platform and via
              email or SMS.
            </p>
          </div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-slate-50/80 p-5">
          <BulletList items={NOTIFICATION_FEATURES} />
          <p className="mt-4 border-t border-slate-200 pt-4 text-sm font-medium leading-6 text-slate-700 sm:text-base">
            Log out securely from your account whenever needed.
          </p>
        </div>
      </div>
    </section>

    <section className="grid gap-4 lg:grid-cols-2">
      <PlaceholderChart title="Platform growth snapshot" />
      <PlaceholderChart title="Application and conversion trends" />
    </section>
  </div>
);

export const AboutPage = () => (
  <section className="space-y-6">
    <PageIntro
      title="About EduRite"
      subtitle="A smarter, more connected pathway from education to opportunity."
    />
    <div className="mx-auto max-w-4xl rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm sm:p-8 lg:p-10">
      <div className="max-w-3xl space-y-5 text-sm leading-7 text-slate-600 sm:text-base">
        <h2 className="text-2xl font-semibold tracking-tight text-slate-900">About EduRite</h2>
        <p>
          EduRite is a smart education and career platform designed to expand access to
          opportunities for students, institutions, and organizations.
        </p>
        <p>
          We leverage AI-powered recommendations to guide students toward the right careers,
          courses, and bursaries based on their skills, interests, and academic background.
        </p>
        <div className="rounded-2xl border border-slate-200 bg-slate-50/80 p-5">
          <h3 className="text-lg font-semibold text-slate-900">Human-centered workflows</h3>
          <p className="mt-2">
            At the same time, EduRite integrates human-centered workflows that allow:
          </p>
          <div className="mt-4">
            <BulletList items={HUMAN_CENTERED_WORKFLOWS} />
          </div>
        </div>
        <p>
          Our mission is to bridge the gap between education and employment, ensuring that every
          learner has a clear, data-driven pathway to success.
        </p>
      </div>
    </div>
  </section>
);

export const CareersPage = () => {
  const careers = useAppQuery<Career[] | PaginatedResponse<Career>>({
    queryKey: ['public', 'careers'],
    queryFn: () => careerService.list(),
  });
  const rows = resolveRows(careers.data, FALLBACK_CAREERS);

  return (
    <section className="space-y-6">
      <PageIntro
        title="Career Listings"
        subtitle="Explore in-demand careers, role expectations, and growth potential."
      />
      <FilterBar placeholder="Search careers" />
      <DataTable
        columns={[
          { key: 'title', header: 'Career' },
          { key: 'description', header: 'Overview' },
          {
            key: 'matchScore',
            header: 'Demand score',
            render: (row) => <Badge color="blue">{row.matchScore ?? 80}%</Badge>,
          },
        ]}
        data={rows}
      />
    </section>
  );
};

export const CareerDetailsPage = () => {
  const { id = '' } = useParams();

  useAppQuery({
    queryKey: ['public', 'career', id],
    queryFn: () => careerService.details(id),
    enabled: Boolean(id),
  });

  return (
    <section className="space-y-4">
      <PageIntro
        title="Career Details"
        subtitle={`Deep dive into required skills, qualifications, and opportunities for career #${id}.`}
      />
      <div className="card p-6 text-sm text-slate-600">
        Expected salary bands, learning roadmap, and top institutions are shown in this detail
        view.
      </div>
    </section>
  );
};

export const CoursesPage = () => {
  const courses = useAppQuery<Course[]>({
    queryKey: ['public', 'courses'],
    queryFn: () => courseService.list(),
  });
  const rows = resolveRows(courses.data, FALLBACK_COURSES);

  return (
    <section className="space-y-6">
      <PageIntro
        title="Courses"
        subtitle="Compare accredited courses aligned with your career ambitions."
      />
      <FilterBar placeholder="Search courses" />
      <DataTable
        columns={[
          { key: 'name', header: 'Course' },
          { key: 'institutionName', header: 'Institution' },
          { key: 'duration', header: 'Duration' },
        ]}
        data={rows}
      />
    </section>
  );
};

export const CourseDetailsPage = () => {
  const { id = '' } = useParams();

  useAppQuery({
    queryKey: ['public', 'course', id],
    queryFn: () => courseService.details(id),
    enabled: Boolean(id),
  });

  return (
    <section className="space-y-4">
      <PageIntro
        title="Course Details"
        subtitle={`Program outline, admission criteria, and graduate outcomes for course #${id}.`}
      />
      <div className="card p-6 text-sm text-slate-600">
        Review modules by year, tuition estimate, and application timeline milestones.
      </div>
    </section>
  );
};

export const InstitutionsPage = () => {
  const institutions = useAppQuery<Institution[]>({
    queryKey: ['public', 'institutions'],
    queryFn: () => institutionService.list(),
  });
  const rows = resolveRows(institutions.data, FALLBACK_INSTITUTIONS);

  return (
    <section className="space-y-6">
      <PageIntro
        title="Institutions"
        subtitle="Discover universities and colleges that best match your profile and goals."
      />
      <FilterBar placeholder="Search institutions" />
      <DataTable
        columns={[
          { key: 'name', header: 'Institution' },
          { key: 'location', header: 'Location' },
        ]}
        data={rows}
      />
    </section>
  );
};

export const InstitutionDetailsPage = () => {
  const { id = '' } = useParams();

  useAppQuery({
    queryKey: ['public', 'institution', id],
    queryFn: () => institutionService.details(id),
    enabled: Boolean(id),
  });

  return (
    <section className="space-y-4">
      <PageIntro
        title="Institution Details"
        subtitle={`Campus profile, programs, and admission windows for institution #${id}.`}
      />
      <div className="card p-6 text-sm text-slate-600">
        View ranking indicators, supported bursaries, and location insights.
      </div>
    </section>
  );
};

export const BursariesPage = () => {
  const bursaries = useAppQuery<Bursary[] | PaginatedResponse<Bursary>>({
    queryKey: ['public', 'bursaries'],
    queryFn: () => bursaryService.list(),
  });
  const rows = resolveRows(bursaries.data, FALLBACK_BURSARIES);

  return (
    <section className="space-y-6">
      <PageIntro
        title="Bursaries"
        subtitle="Browse funding opportunities by field, location, and eligibility criteria."
      />
      <FilterBar placeholder="Search bursaries" />
      <DataTable
        columns={[
          { key: 'title', header: 'Bursary' },
          { key: 'provider', header: 'Provider' },
          {
            key: 'status',
            header: 'Status',
            render: (row) => (
              <Badge color={row.status === 'PUBLISHED' ? 'emerald' : 'amber'}>{row.status}</Badge>
            ),
          },
        ]}
        data={rows}
      />
    </section>
  );
};

export const BursaryDetailsPage = () => {
  const { id = '' } = useParams();

  useAppQuery({
    queryKey: ['public', 'bursary', id],
    queryFn: () => bursaryService.details(id),
    enabled: Boolean(id),
  });

  return (
    <section className="space-y-4">
      <PageIntro
        title="Bursary Details"
        subtitle={`Eligibility requirements, benefits, and process details for bursary #${id}.`}
      />
      <div className="card p-6 text-sm text-slate-600">
        Prepare your supporting documents and track all key submission dates.
      </div>
    </section>
  );
};

export const PricingPage = () => (
  <section className="space-y-6">
    <PageIntro
      title="Pricing Plans"
      subtitle="Simple options for students and organizations looking to grow with EduRite."
    />
    <div className="grid gap-5 md:grid-cols-3">
      {pricingPlans.map((plan) => (
        <article
          key={plan.name}
          className={`flex h-full flex-col rounded-[28px] border p-6 shadow-sm transition-transform duration-200 sm:p-7 ${
            plan.featured
              ? 'border-primary-200 bg-gradient-to-b from-primary-50 via-white to-white shadow-lg shadow-primary-200/40'
              : 'border-slate-200 bg-white hover:-translate-y-0.5'
          }`}
        >
          <div className="flex items-start justify-between gap-3">
            <div>
              <h2 className="text-xl font-semibold text-slate-900">{plan.name}</h2>
              <p className="mt-3 text-3xl font-bold tracking-tight text-slate-900">{plan.price}</p>
            </div>
            {plan.featured ? (
              <span className="rounded-full bg-primary-600 px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-white">
                Recommended
              </span>
            ) : null}
          </div>
          <p className="mt-4 text-sm leading-6 text-slate-600">{plan.desc}</p>
          <ul className="mt-6 space-y-3 text-sm leading-6 text-slate-700">
            {plan.features.map((feature) => (
              <li key={feature} className="flex gap-3">
                <span
                  className={`mt-1 inline-flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
                    plan.featured
                      ? 'bg-primary-100 text-primary-700'
                      : 'bg-slate-100 text-slate-700'
                  }`}
                >
                  ✓
                </span>
                <span>{feature}</span>
              </li>
            ))}
          </ul>
          <Button
            type="button"
            className={`mt-8 w-full rounded-2xl px-5 py-3 text-sm ${
              plan.featured ? 'shadow-lg shadow-primary-600/20' : ''
            }`}
          >
            Choose Plan
          </Button>
        </article>
      ))}
    </div>
  </section>
);
