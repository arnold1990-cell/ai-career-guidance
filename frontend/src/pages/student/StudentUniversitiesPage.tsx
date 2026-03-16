import { useMemo, useRef } from 'react';
import { useAppQuery } from '@/hooks/useAppQuery';
import { institutionService } from '@/services/institutionService';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import type { Institution } from '@/types';

const featuredUniversities = new Set([
  'University of KwaZulu-Natal',
  'Tshwane University of Technology',
  'University of Pretoria',
  'University of Johannesburg',
  'University of the Free State',
  'University of Cape Town',
  'University of the Witwatersrand',
  'Stellenbosch University',
]);

const UniversityCard = ({ university }: { university: Institution }) => {
  const website = university.website?.trim();
  const cardBody = (
    <article className="h-full rounded-xl border border-slate-200 bg-white p-4 transition hover:scale-[1.01] hover:shadow-md">
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500">{university.category ?? 'University'}</p>
      <h3 className="mt-1 text-base font-semibold text-slate-900">{university.name}</h3>
      <p className="mt-1 text-sm text-slate-600">{university.city ?? university.location ?? university.country ?? 'South Africa'}</p>
      {website ? (
        <span className="mt-3 inline-flex rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">Visit Website</span>
      ) : (
        <span className="mt-3 inline-flex rounded-full border border-slate-200 bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-500">Website unavailable</span>
      )}
    </article>
  );

  if (!website) {
    return <div className="cursor-not-allowed opacity-80">{cardBody}</div>;
  }

  return (
    <a
      href={website}
      target="_blank"
      rel="noopener noreferrer"
      className="block h-full cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2"
      aria-label={`Visit ${university.name} official website`}
    >
      {cardBody}
    </a>
  );
};

export const StudentUniversitiesPage = () => {
  const carouselRef = useRef<HTMLDivElement | null>(null);
  const institutions = useAppQuery<Institution[]>({ queryKey: ['student', 'institutions'], queryFn: () => institutionService.list() });

  const allUniversities = useMemo(() => (institutions.data ?? []).filter((item) => item.active !== false), [institutions.data]);
  const featured = useMemo(
    () => allUniversities.filter((item) => item.featured || featuredUniversities.has(item.name)).slice(0, 10),
    [allUniversities],
  );

  const scrollFeatured = (direction: 'left' | 'right') => {
    const container = carouselRef.current;
    if (!container) return;
    const offset = direction === 'left' ? -320 : 320;
    container.scrollBy({ left: offset, behavior: 'smooth' });
  };

  if (institutions.isLoading) return <LoadingState />;
  if (institutions.isError) return <ErrorState message="Could not load universities right now." />;
  if (allUniversities.length === 0) return <EmptyState title="No universities yet" message="No universities available at the moment." />;

  return (
    <section className="space-y-6">
      <header>
        <h1 className="text-xl font-semibold">Universities</h1>
        <p className="text-sm text-slate-600">Browse South African public universities and open their official sites for programmes, admissions, and fees.</p>
      </header>

      <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-base font-semibold">Featured institutions</h2>
          <div className="flex items-center gap-2">
            <button type="button" onClick={(e) => { e.stopPropagation(); scrollFeatured('left'); }} className="rounded border bg-white px-3 py-1 text-sm hover:bg-slate-100" aria-label="Scroll featured institutions left">
              ←
            </button>
            <button type="button" onClick={(e) => { e.stopPropagation(); scrollFeatured('right'); }} className="rounded border bg-white px-3 py-1 text-sm hover:bg-slate-100" aria-label="Scroll featured institutions right">
              →
            </button>
          </div>
        </div>

        <div ref={carouselRef} className="flex snap-x snap-mandatory gap-3 overflow-x-auto pb-2">
          {featured.map((university) => (
            <div key={`featured-${university.id}`} className="min-w-[270px] max-w-[270px] snap-start">
              <UniversityCard university={university} />
            </div>
          ))}
        </div>
      </div>

      <div>
        <h2 className="mb-3 text-base font-semibold">All universities ({allUniversities.length})</h2>
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          {allUniversities.map((university) => (
            <UniversityCard key={university.id} university={university} />
          ))}
        </div>
      </div>
    </section>
  );
};
