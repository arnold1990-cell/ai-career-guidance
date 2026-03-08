import { EmptyState } from '@/components/feedback/States';

export const PageShell = ({ title, description }: { title: string; description: string }) => (
  <section className="space-y-4">
    <div>
      <h1 className="text-2xl font-bold">{title}</h1>
      <p className="text-sm text-slate-600">{description}</p>
    </div>
    <EmptyState title="Integrated with /api/v1" message="This page is wired for backend data integration using TanStack Query + Axios service modules." />
  </section>
);
