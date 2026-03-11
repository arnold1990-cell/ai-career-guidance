import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/tables/DataTable';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import { useAppQuery } from '@/hooks/useAppQuery';
import { companyService } from '@/services/companyService';

const Header = ({ title, subtitle }: { title: string; subtitle: string }) => (
  <div>
    <h1 className="text-2xl font-bold">{title}</h1>
    <p className="text-sm text-slate-600">{subtitle}</p>
  </div>
);

type CompanyProfile = {
  id: string;
  companyName: string;
  registrationNumber: string;
  industry?: string;
  officialEmail: string;
  mobileNumber?: string;
  contactPersonName?: string;
  address?: string;
  website?: string;
  description?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'MORE_INFO_REQUIRED';
  reviewNotes?: string;
};

type CompanyBursary = {
  id: string;
  bursaryName: string;
  description: string;
  fieldOfStudy: string;
  academicLevel: string;
  applicationStartDate: string;
  applicationEndDate: string;
  fundingAmount: number;
  benefits?: string;
  requiredSubjects?: string;
  minimumGrade?: string;
  demographics?: string;
  location?: string;
  eligibility?: string;
  status: string;
};

type StudentRow = {
  id: string;
  firstName: string;
  lastName: string;
  location?: string;
  qualificationLevel?: string;
  skills: string[];
  interests: string[];
};

export const CompanyPendingApprovalPage = () => {
  const profile = useAppQuery<CompanyProfile>({ queryKey: ['company', 'me', 'pending'], queryFn: () => companyService.getMe() });
  if (profile.isLoading) return <LoadingState />;
  if (profile.error) return <ErrorState message="Unable to load company profile." />;

  const status = profile.data?.status ?? 'PENDING';
  const notes = profile.data?.reviewNotes;

  return (
    <section className="space-y-6">
      <Header title="Approval in Progress" subtitle="Your company account is restricted until an admin review is completed." />
      <div className="card p-5 space-y-3 text-sm">
        <p><span className="font-semibold">Status:</span> <Badge color={status === 'APPROVED' ? 'emerald' : status === 'REJECTED' ? 'slate' : 'amber'}>{status}</Badge></p>
        <p><span className="font-semibold">Company:</span> {profile.data?.companyName}</p>
        <p><span className="font-semibold">Official email:</span> {profile.data?.officialEmail}</p>
        {notes ? <p><span className="font-semibold">Admin notes:</span> {notes}</p> : null}
        <p className="text-slate-500">You can still update profile details and upload verification documents while your account is being reviewed.</p>
        <div className="flex gap-3">
          <Link to="/company/profile"><Button>Update Profile</Button></Link>
          <Link to="/company/verification-docs"><Button className="bg-slate-700 hover:bg-slate-600">Upload Documents</Button></Link>
        </div>
      </div>
    </section>
  );
};

export const CompanyDashboardPage = () => {
  const bursaries = useAppQuery<CompanyBursary[]>({ queryKey: ['company', 'bursaries'], queryFn: () => companyService.getBursaries() });
  const profile = useAppQuery<CompanyProfile>({ queryKey: ['company', 'me', 'dashboard'], queryFn: () => companyService.getMe() });

  if (profile.data?.status && profile.data.status !== 'APPROVED') {
    return <CompanyPendingApprovalPage />;
  }

  const rows = Array.isArray(bursaries.data) ? bursaries.data : [];
  return (
    <section className="space-y-6">
      <Header title="Company Dashboard" subtitle="Monitor bursary activity and manage your funding opportunities." />
      <div className="grid gap-4 md:grid-cols-3">
        <div className="card p-5"><p className="text-sm text-slate-500">Total bursaries</p><p className="text-2xl font-bold">{rows.length}</p></div>
        <div className="card p-5"><p className="text-sm text-slate-500">Active/Open</p><p className="text-2xl font-bold">{rows.filter((b) => b.status === 'ACTIVE').length}</p></div>
        <div className="card p-5"><p className="text-sm text-slate-500">Closed</p><p className="text-2xl font-bold">{rows.filter((b) => b.status === 'CLOSED').length}</p></div>
      </div>
    </section>
  );
};

export const CompanyProfilePage = () => {
  const { register, handleSubmit, reset } = useForm<CompanyProfile>();
  const [saved, setSaved] = useState<string>('');
  const profile = useAppQuery<CompanyProfile>({ queryKey: ['company', 'me'], queryFn: () => companyService.getMe() });

  useEffect(() => { if (profile.data) reset(profile.data); }, [profile.data, reset]);

  return (
    <section className="space-y-6">
      <Header title="Company Profile" subtitle="Maintain your organization and contact details." />
      {profile.isLoading ? <LoadingState /> : null}
      {profile.error ? <ErrorState message="Unable to load company profile." /> : null}
      <form className="card p-5 grid gap-4 md:grid-cols-2" onSubmit={handleSubmit(async (values) => {
        await companyService.updateMe(values);
        setSaved('Profile updated successfully.');
      })}>
        <label className="text-sm">Company Name<input disabled className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" defaultValue={profile.data?.companyName} /></label>
        <label className="text-sm">Industry<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('industry')} /></label>
        <label className="text-sm">Mobile Number<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('mobileNumber')} /></label>
        <label className="text-sm">Contact Person<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('contactPersonName')} /></label>
        <label className="text-sm md:col-span-2">Address<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('address')} /></label>
        <label className="text-sm">Website<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('website')} /></label>
        <label className="text-sm">Status<input disabled className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" value={profile.data?.status ?? ''} /></label>
        <label className="text-sm md:col-span-2">Description<textarea rows={4} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('description')} /></label>
        <div className="md:col-span-2 flex items-center gap-3">
          <Button type="submit">Save Profile</Button>
          {saved ? <span className="text-sm text-emerald-700">{saved}</span> : null}
        </div>
      </form>
    </section>
  );
};

export const CompanyVerificationDocsPage = () => {
  const [file, setFile] = useState<File | null>(null);
  const [documentType, setDocumentType] = useState('REGISTRATION_CERTIFICATE');
  const [message, setMessage] = useState('');
  const docs = useAppQuery({ queryKey: ['company', 'docs'], queryFn: () => companyService.getDocuments() });

  return (
    <section className="space-y-6">
      <Header title="Verification Documents" subtitle="Upload documents required for admin review and verification." />
      <div className="card p-5 space-y-3">
        <div className="grid gap-3 md:grid-cols-3">
          <select value={documentType} onChange={(e) => setDocumentType(e.target.value)} className="rounded-lg border border-slate-300 px-3 py-2 text-sm">
            <option value="REGISTRATION_CERTIFICATE">Registration Certificate</option>
            <option value="TAX_ID">Tax ID</option>
            <option value="OTHER">Other</option>
          </select>
          <input type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} className="rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          <Button onClick={async () => {
            if (!file) return;
            await companyService.uploadDocument(file, documentType);
            setMessage('Document uploaded successfully.');
            window.location.reload();
          }}>Upload</Button>
        </div>
        {message ? <p className="text-sm text-emerald-700">{message}</p> : null}
      </div>
      {docs.isLoading ? <LoadingState /> : null}
      <DataTable columns={[{ key: 'fileName', header: 'File' }, { key: 'documentType', header: 'Type' }, { key: 'verificationStatus', header: 'Status' }, { key: 'createdAt', header: 'Uploaded' }]} data={Array.isArray(docs.data) ? docs.data : []} />
    </section>
  );
};

export const CompanyBursariesPage = () => {
  const bursaries = useAppQuery<CompanyBursary[]>({ queryKey: ['company', 'bursary-list'], queryFn: () => companyService.getBursaries() });
  const rows = Array.isArray(bursaries.data) ? bursaries.data : [];

  if (bursaries.isLoading) return <LoadingState />;
  return (
    <section className="space-y-6">
      <Header title="My Bursaries" subtitle="Create, update, and close bursaries managed by your company." />
      <Link to="/company/bursaries/new"><Button>Create bursary</Button></Link>
      {rows.length === 0 ? <EmptyState title="No bursaries yet" message="Create your first bursary listing to start receiving applications." /> : (
        <DataTable columns={[
          { key: 'bursaryName', header: 'Bursary' },
          { key: 'fieldOfStudy', header: 'Field' },
          { key: 'status', header: 'Status', render: (row) => <Badge color={row.status === 'ACTIVE' ? 'emerald' : 'amber'}>{row.status}</Badge> },
          { key: 'id', header: 'Actions', render: (row) => <div className="flex gap-2"><Link className="text-primary-600" to={`/company/bursaries/${row.id}/edit`}>Edit</Link><button className="text-primary-600" onClick={async () => {
            if (row.status === 'ACTIVE') await companyService.closeBursary(row.id);
            else await companyService.reopenBursary(row.id);
            window.location.reload();
          }}>{row.status === 'ACTIVE' ? 'Close' : 'Reopen'}</button><button className="text-slate-600" onClick={async () => {
            await companyService.unpublishBursary(row.id);
            window.location.reload();
          }}>Unpublish</button></div> },
        ]} data={rows} />
      )}
    </section>
  );
};

const BursaryForm = ({ mode }: { mode: 'create' | 'edit' }) => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { register, handleSubmit, reset } = useForm<CompanyBursary>();
  const bursary = useAppQuery<CompanyBursary>({ queryKey: ['company', 'bursary', id], enabled: mode === 'edit' && Boolean(id), queryFn: () => companyService.getBursary(String(id)) });

  useEffect(() => { if (mode === 'edit' && bursary.data) reset(bursary.data); }, [mode, bursary.data, reset]);

  return (
    <section className="space-y-6">
      <Header title={mode === 'create' ? 'Create Bursary' : 'Edit Bursary'} subtitle="Define bursary details and eligibility filters for matching." />
      <form className="card p-5 grid gap-4 md:grid-cols-2" onSubmit={handleSubmit(async (form) => {
        const payload = {
          bursaryName: form.bursaryName,
          description: form.description,
          fieldOfStudy: form.fieldOfStudy,
          academicLevel: form.academicLevel,
          applicationStartDate: form.applicationStartDate,
          applicationEndDate: form.applicationEndDate,
          fundingAmount: Number(form.fundingAmount),
          benefits: form.benefits,
          requiredSubjects: (form.requiredSubjects ?? '').split(',').map((v) => v.trim()).filter(Boolean),
          minimumGrade: form.minimumGrade,
          demographics: (form.demographics ?? '').split(',').map((v) => v.trim()).filter(Boolean),
          location: form.location,
          eligibilityFilters: (form.eligibility ?? '').split(',').map((v) => v.trim()).filter(Boolean),
        };
        if (mode === 'create') await companyService.createBursary(payload);
        else if (id) await companyService.updateBursary(id, payload);
        navigate('/company/bursaries');
      })}>
        <label className="text-sm">Bursary Name<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('bursaryName', { required: true })} /></label>
        <label className="text-sm">Field of Study<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('fieldOfStudy', { required: true })} /></label>
        <label className="text-sm">Academic Level<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('academicLevel', { required: true })} /></label>
        <label className="text-sm">Funding Amount<input type="number" step="0.01" className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('fundingAmount', { required: true })} /></label>
        <label className="text-sm">Application Start<input type="date" className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('applicationStartDate', { required: true })} /></label>
        <label className="text-sm">Application End<input type="date" className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('applicationEndDate', { required: true })} /></label>
        <label className="text-sm md:col-span-2">Description<textarea rows={4} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('description', { required: true })} /></label>
        <label className="text-sm">Required Subjects (comma separated)<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('requiredSubjects')} /></label>
        <label className="text-sm">Minimum Grade<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('minimumGrade')} /></label>
        <label className="text-sm">Demographics (comma separated)<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('demographics')} /></label>
        <label className="text-sm">Location<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('location')} /></label>
        <label className="text-sm md:col-span-2">Benefits<textarea rows={3} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('benefits')} /></label>
        <label className="text-sm md:col-span-2">Eligibility Filters (comma separated)<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('eligibility')} /></label>
        <Button type="submit" className="md:col-span-2 w-fit">{mode === 'create' ? 'Create bursary' : 'Save changes'}</Button>
      </form>
    </section>
  );
};

export const CompanyCreateBursaryPage = () => <BursaryForm mode="create" />;
export const CompanyEditBursaryPage = () => <BursaryForm mode="edit" />;

export const CompanyApplicantsPage = () => <CompanyTalentSearchPage />;

export const CompanyTalentSearchPage = () => {
  const [filters, setFilters] = useState({ fieldOfInterest: '', qualificationLevel: '', skills: '', location: '' });
  const [search, setSearch] = useState(filters);
  const students = useAppQuery<StudentRow[]>({ queryKey: ['company', 'students', search], queryFn: () => companyService.searchStudents(search) });
  const rows = useMemo(() => (Array.isArray(students.data) ? students.data : []), [students.data]);

  return (
    <section className="space-y-6">
      <Header title="Student Search" subtitle="Find students by field, skills, qualification level, and location." />
      <div className="card p-5 space-y-3">
        <div className="grid gap-3 md:grid-cols-4">
          <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="Field of interest" value={filters.fieldOfInterest} onChange={(e) => setFilters((s) => ({ ...s, fieldOfInterest: e.target.value }))} />
          <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="Qualification" value={filters.qualificationLevel} onChange={(e) => setFilters((s) => ({ ...s, qualificationLevel: e.target.value }))} />
          <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="Skills" value={filters.skills} onChange={(e) => setFilters((s) => ({ ...s, skills: e.target.value }))} />
          <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="Location" value={filters.location} onChange={(e) => setFilters((s) => ({ ...s, location: e.target.value }))} />
        </div>
        <Button onClick={() => setSearch(filters)}>Search</Button>
      </div>
      {students.isLoading ? <LoadingState /> : null}
      <DataTable columns={[
        { key: 'firstName', header: 'First Name' },
        { key: 'lastName', header: 'Last Name' },
        { key: 'qualificationLevel', header: 'Qualification Level' },
        { key: 'location', header: 'Location' },
        { key: 'skills', header: 'Skills', render: (row) => row.skills.join(', ') },
      ]} data={rows} />
    </section>
  );
};

export const CompanyShortlistedPage = () => <CompanyTalentSearchPage />;
export const CompanyNotificationsPage = () => <CompanyPendingApprovalPage />;
export const CompanySettingsPage = () => <CompanyProfilePage />;
